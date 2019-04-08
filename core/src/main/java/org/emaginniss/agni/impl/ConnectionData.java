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

package org.emaginniss.agni.impl;

import lombok.extern.slf4j.Slf4j;
import org.emaginniss.agni.*;
import org.emaginniss.agni.connections.Connection;
import org.emaginniss.agni.connections.ConnectionParent;
import org.emaginniss.agni.messages.RemoveLink;
import org.emaginniss.agni.messages.StatsResponse;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Slf4j
public class ConnectionData implements ConnectionParent {

    private Node node;
    private Map<String, Connection> connections = new HashMap<>();
    private Map<String, Set<Connection>> connectedNodes = new HashMap<>();

    public ConnectionData(Map<String, Configuration> connectionMap, Node node) {
        this.node = node;
        for (String name : connectionMap.keySet()) {
            log.info("Initializing connection " + name);
            connections.put(name, Factory.instantiate(Connection.class, connectionMap.get(name), this));
        }
    }

    public Map<String, Connection> getConnections() {
        return new HashMap<>(connections);
    }

    @NotNull
    public Map<String, Set<Connection>> getConnectedNodes() {
        return connectedNodes;
    }

    @NotNull
    public Set<String> getConnectedNodesUuids() {
        synchronized (this) {
            return new HashSet<>(connectedNodes.keySet());
        }
    }

    public void enableConnection(Connection connection, String uuid, String displayName) {
        log.info("Establishing connection with " + displayName);
        boolean newConnection = false;
        synchronized (this) {
            if (!connectedNodes.containsKey(uuid)) {
                connectedNodes.put(uuid, new HashSet<Connection>());
                newConnection = true;
            }
            connectedNodes.get(uuid).add(connection);
        }
        if (newConnection) {
            node.getPathFinder().addNodePath(new String[]{uuid});

            node.getInbox().enqueue(node.getWhisperHandler().buildSubscriptionInfoEnvelope(null, new String[0], uuid));
        }
    }

    public void disableConnection(Connection connection, String uuid, String displayName) {
        log.info("Connection lost with " + displayName);
        synchronized (this) {
            if (connectedNodes.containsKey(uuid)) {
                connectedNodes.get(uuid).remove(connection);
                if (connectedNodes.get(uuid).size() == 0) {
                    connectedNodes.remove(uuid);
                    node.getPathFinder().removeNodeConnection(null, uuid);
                    node.getDestinationRegistration().handleLostNodes(Collections.singleton(uuid));
                    if (!node.isShuttingDown()) {
                        new AgniBuilder(new RemoveLink(node.getUuid(), uuid)).broadcast(node);
                    }
                }
            }
        }
    }

    public boolean forward(String nodeUuid, Envelope envelope) {
        Set<Connection> connections = new HashSet<>();
        synchronized (this) {
            if (connectedNodes.containsKey(nodeUuid)) {
                connections.addAll(connectedNodes.get(nodeUuid));
            }
        }
        if (connections.size() == 0) {
            log.debug("Unable to forward message - no connections found");
            return false;
        }
        for (Connection connection : connections) {
            if (connection.forwardMessage(envelope, nodeUuid)) {
                return true;
            }
        }
        return false;
    }

    public void shutdown() {
        for (Connection connection : connections.values()) {
            connection.shutdown();
        }
        connections.clear();
        connectedNodes.clear();
    }

    @Override
    public String getUuid() {
        return node.getUuid();
    }

    @Override
    public String getDisplayName() {
        return node.getDisplayName();
    }

    @Override
    public void handleIncomingEnvelope(Envelope e) {
        node.handleIncomingEnvelope(e);
    }

    @Override
    public Node getNode() {
        return node;
    }

    public void populate(StatsResponse resp) {
        for (String name : connections.keySet()) {
            resp.getConnections().put(name, connections.get(name).getClass().getSimpleName());
        }
    }
}
