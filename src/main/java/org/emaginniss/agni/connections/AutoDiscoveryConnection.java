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
import org.emaginniss.agni.Node;
import org.emaginniss.agni.annotations.Component;

import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Eric on 7/14/2015.
 */
@Component(value = "autoDiscovery", isDefault = true)
public class AutoDiscoveryConnection implements Connection, ConnectionParent {

    private static final Logger log = Logger.getLogger(AutoDiscoveryConnection.class);

    private Configuration configuration;
    private ConnectionParent parent;
    private InetAddress groupName;
    private int multicastPort;
    private Connection socketsServer;
    private MulticastSocket multicastSocket;
    private boolean shutdown = false;
    private MulticastServerThead multicastServerThead;
    private MulticastClientThead multicastClientThead;
    private int listenPort;

    private final Map<String, Connection> connectionsByNodeUuid = new ConcurrentHashMap<>();
    private final Set<Connection> clients = new HashSet<>();

    public AutoDiscoveryConnection(Configuration configuration, ConnectionParent parent) {
        this.configuration = configuration;
        this.parent = parent;
        try {
            groupName = InetAddress.getByName(configuration.getString("groupName", "225.7.193.61"));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        multicastPort = configuration.getInt("multicastPort", 34001);

        listenPort = configuration.getInt("listenPort", 35000);
        while (listenPort < 65535) {
            try {
                ServerSocket ss = new ServerSocket(listenPort);
                ss.close();
                break;
            } catch (Exception e) {
                listenPort++;
            }
        }

        Configuration serverConfig = configuration.getChild("server");
        serverConfig.getObject().addProperty("port", listenPort);
        if (serverConfig.getString("type", null) == null) {
            serverConfig.getObject().addProperty("type", "defaultSocketsServer");
        }
        socketsServer = Factory.instantiate(Connection.class, serverConfig, this);

        try {
            multicastSocket = new MulticastSocket(multicastPort);
            multicastSocket.joinGroup(groupName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        multicastServerThead = new MulticastServerThead();
        multicastServerThead.start();

        multicastClientThead = new MulticastClientThead();
        multicastClientThead.start();
    }

    @Override
    public void shutdown() {
        shutdown = true;
        multicastClientThead.interrupt();
        try {
            multicastClientThead.join(5000);
        } catch (Exception e) {
            //do nothing
        }

        try {
            multicastSocket.leaveGroup(groupName);
        } catch (IOException e) {
            //do nothing
        }
        multicastSocket.close();
        try {
            multicastServerThead.join(5000);
        } catch (Exception e) {
            //do nothing
        }

        socketsServer.shutdown();

        for (Connection conn : new HashSet<>(this.clients)) {
            conn.shutdown();
        }
    }

    @Override
    public boolean forwardMessage(Envelope envelope, String targetNodeUuid) {
        Connection connection = connectionsByNodeUuid.get(targetNodeUuid);
        if (connection == null) {
            log.debug("Unable to forward message - connection missing");
            return false;
        }
        return connection.forwardMessage(envelope, targetNodeUuid);
    }

    private class MulticastServerThead extends Thread {

        public MulticastServerThead() {
            super(parent.getNode().getThreadGroup(), parent.getDisplayName() + " - Multicast Server Thread");
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    byte buf[] = new byte[1024];
                    DatagramPacket pack = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(pack);

                    String raw = new String(pack.getData(), 0, pack.getLength());
                    MulticastData data = (MulticastData) parent.getNode().getSerializer().deserialize(raw, MulticastData.class.getName());

                    if (!connectionsByNodeUuid.containsKey(data.getUuid()) && !parent.getUuid().equals(data.getUuid())) {
                        Configuration clientConfig = configuration.getChild("client");
                        clientConfig.getObject().addProperty("port", data.getPort());
                        clientConfig.getObject().addProperty("host", pack.getAddress().getHostAddress());
                        clientConfig.getObject().addProperty("exitOnFail", true);
                        if (clientConfig.getString("type", null) == null) {
                            clientConfig.getObject().addProperty("type", "defaultSocketsClient");
                        }
                        clients.add(Factory.instantiate(Connection.class, clientConfig, AutoDiscoveryConnection.this));
                    }
                } catch (Exception e) {
                    if (!shutdown) {
                        log.error("Error", e);
                    }
                }
            }
        }
    }

    private class MulticastClientThead extends Thread {

        public MulticastClientThead() {
            super(parent.getNode().getThreadGroup(), parent.getDisplayName() + " - Multicast Client Thread");
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    MulticastSocket socket = new MulticastSocket();
                    byte []buffer = parent.getNode().getSerializer().serialize(new MulticastData(parent.getUuid(), listenPort)).getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupName, multicastPort);
                    socket.send(packet);
                    socket.close();

                    Thread.sleep(30 * 1000);
                } catch (Exception e) {
                    if (!shutdown) {
                        log.error("Error", e);
                    }
                }
            }
        }
    }

    @Override
    public String getUuid() {
        return parent.getUuid();
    }

    @Override
    public String getDisplayName() {
        return parent.getDisplayName();
    }

    @Override
    public void enableConnection(Connection connection, String uuid, String displayName) {
        connectionsByNodeUuid.put(uuid, connection);
        parent.enableConnection(this, uuid, displayName);
    }

    @Override
    public void disableConnection(Connection connection, String uuid, String displayName) {
        parent.disableConnection(this, uuid, displayName);
        connectionsByNodeUuid.remove(uuid);
        if (connection != socketsServer) {
            clients.remove(connection);
        }
    }

    @Override
    public void handleIncomingEnvelope(Envelope e) {
        parent.handleIncomingEnvelope(e);
    }

    @Override
    public Node getNode() {
        return parent.getNode();
    }

    public static class MulticastData {
        private String uuid;
        private int port;

        public MulticastData() {
        }

        public MulticastData(String uuid, int port) {
            this.uuid = uuid;
            this.port = port;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
