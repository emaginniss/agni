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
import org.emaginniss.agni.envelopefilters.EnvelopeFilter;
import org.emaginniss.agni.util.ExtendedDataInputStream;
import org.emaginniss.agni.util.ExtendedDataOutputStream;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Eric on 6/13/2015.
 */
@Component("defaultSocketsClient")
public class DefaultSocketsClient implements Connection, Runnable {

    private static final Logger log = Logger.getLogger(DefaultSocketsClient.class);

    private ConnectionParent parent;
    private int port;
    private String host;
    private EnvelopeFilter envelopeFilter;
    private String uuid;
    private String displayName;
    private Socket socket;
    private ExtendedDataInputStream in;
    private ExtendedDataOutputStream out;
    private Thread connectionThread;
    private boolean shutdown = false;
    private int failureCount = 0;
    private boolean exitOnFail = false;


    public DefaultSocketsClient(Configuration configuration, ConnectionParent parent) {
        this.parent = parent;

        host = configuration.getString("host", "127.0.0.1");
        port = configuration.getInt("port", 7350);
        envelopeFilter = Factory.instantiate(EnvelopeFilter.class, configuration.getChild("envelopeFilter"), parent.getNode());
        exitOnFail = configuration.getBoolean("exitOnFail", false);
        connectionThread = new Thread(parent.getNode().getThreadGroup(), this, parent.getDisplayName() + " - Client Connection Thread");
        connectionThread.start();
    }

    @Override
    public void shutdown() {
        log.info("Shutting down socket client");
        shutdown = true;
        try {
            socket.close();
        } catch (IOException e) {
            //do nothing
        }
        try {
            connectionThread.join(5000);
        } catch (InterruptedException e) {
            //do nothing
        }
        log.info("Socket client shut down");
    }

    @Override
    public boolean forwardMessage(Envelope envelope, String targetNodeUuid) {
        try {
            envelope.write(out);
            return true;
        } catch (IOException e) {
            log.error("Error forwarding message - " + envelope.getType() + " - " + envelope.getNodeUuid(), e);
            return false;
        }
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                if (socket == null) {
                    socket = new Socket(host, port);

                    out = new ExtendedDataOutputStream(socket.getOutputStream());
                    out.write(parent.getUuid());
                    out.write(parent.getDisplayName());

                    in = new ExtendedDataInputStream(socket.getInputStream());
                    uuid = in.readString();
                    displayName = in.readString();

                    parent.enableConnection(this, uuid, displayName);
                }
                Envelope e = Envelope.read(in);
                if (envelopeFilter.filter(e)) {
                    parent.handleIncomingEnvelope(e);
                }
                failureCount = 0;
            } catch (Exception e) {
                failureCount++;
                if (!shutdown && !(e instanceof EOFException) && !(e instanceof SocketException)) {
                    log.error("Error", e);
                }

                if (uuid != null) {
                    parent.disableConnection(this, uuid, displayName);
                }
                uuid = null;

                if (shutdown) {
                    return;
                }

                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Throwable t) {
                        //Do nothing
                    }
                    socket = null;
                }
                if (exitOnFail && failureCount > 3) {
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    log.error("Error", e1);
                }
            }
        }
    }
}
