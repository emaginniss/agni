/*
 * Copyright (c) 2015, Eric A Maginniss
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ERIC A MAGINNISS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.emaginniss.agni.connections;

import org.apache.log4j.Logger;
import org.emaginniss.agni.Configuration;
import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.Factory;
import org.emaginniss.agni.annotations.Component;
import org.emaginniss.agni.connectionfilters.ConnectionFilter;
import org.emaginniss.agni.envelopefilters.EnvelopeFilter;
import org.emaginniss.agni.util.ExtendedDataInputStream;
import org.emaginniss.agni.util.ExtendedDataOutputStream;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Eric on 6/13/2015.
 */
@Component("defaultSocketsServer")
public class DefaultSocketsServer implements Connection, Runnable {

    private static final Logger log = Logger.getLogger(DefaultSocketsServer.class);

    private ConnectionParent parent;
    private ServerSocket serverSocket;
    private ConnectionFilter connectionFilter;
    private EnvelopeFilter envelopeFilter;
    private boolean shutdown = false;
    private Thread listenerThread;
    private Map<String, SocketHandler> handlers = new ConcurrentHashMap<>();

    public DefaultSocketsServer(Configuration configuration, ConnectionParent parent) {
        this.parent = parent;
        int port = configuration.getInt("port", 7350);
        connectionFilter = Factory.instantiate(ConnectionFilter.class, configuration.getChild("connectionFilter"), parent.getNode());
        envelopeFilter = Factory.instantiate(EnvelopeFilter.class, configuration.getChild("envelopeFilter"), parent.getNode());

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException("Unable to bind to port " + port);
        }

        listenerThread = new Thread(parent.getNode().getThreadGroup(), this, parent.getDisplayName() + " - Server Listener Thread");
        listenerThread.start();
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                new SocketHandler(serverSocket.accept()).start();
            } catch (Exception e) {
                if (!shutdown) {
                    log.error("Error while waiting for incoming socket connection", e);
                }
            }
        }
    }

    @Override
    public boolean forwardMessage(Envelope envelope, String targetNodeUuid) {
        SocketHandler handler = handlers.get(targetNodeUuid);
        if (handler == null) {
            log.debug("Unable to forward message - handler missing");
            return false;
        }
        return handler.forwardMessage(envelope);
    }

    @Override
    public void shutdown() {
        log.info("Shutting down socket server");
        shutdown = true;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            listenerThread.join(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (SocketHandler sh : handlers.values()) {
            try {
                sh.socket.close();
            } catch (IOException e) {
                //do nothing
            }
        }
        for (SocketHandler sh : handlers.values()) {
            try {
                sh.join(5000);
            } catch (InterruptedException e) {
                //do nothing
            }
        }
        log.info("Socket server shut down");
    }

    private class SocketHandler extends Thread {

        private String uuid;
        private String displayName;
        private Socket socket;
        private ExtendedDataInputStream in;
        private ExtendedDataOutputStream out;

        public SocketHandler(Socket socket) {
            super(parent.getNode().getThreadGroup(), parent.getDisplayName() + " - Server Thread");
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new ExtendedDataInputStream(socket.getInputStream());
                uuid = in.readString();
                displayName = in.readString();

                out = new ExtendedDataOutputStream(socket.getOutputStream());
                out.write(parent.getUuid());
                out.write(parent.getDisplayName());

                if (!connectionFilter.filter(uuid, displayName)) {
                    socket.close();
                    return;
                }
            } catch (Exception e) {
                log.error("Error while initializing incoming socket connection", e);
            }

            handlers.put(uuid, this);
            parent.enableConnection(DefaultSocketsServer.this, uuid, displayName);

            while (!socket.isClosed() && !shutdown) {
                try {
                    Envelope e = Envelope.read(in);
                    if (envelopeFilter.filter(e)) {
                        parent.handleIncomingEnvelope(e);
                    }
                } catch (Exception e) {
                    if (!shutdown && !(e instanceof EOFException) && !(e instanceof SocketException)) {
                        log.error("Error while reading envelope", e);
                    }
                    try {
                        socket.close();
                    } catch (Exception e1) {
                        //do nothing
                    }
                }
            }
            handlers.remove(uuid);
            try {
                socket.close();
            } catch (Exception e) {
                //do nothing
            }
            parent.disableConnection(DefaultSocketsServer.this, uuid, displayName);
        }

        public synchronized boolean forwardMessage(Envelope envelope) {
            try {
                envelope.write(out);
                return true;
            } catch (IOException e) {
                log.error("Error forwarding message - " + envelope.getType() + " - " + envelope.getNodeUuid(), e);
                return false;
            }
        }
    }
}
