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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.emaginniss.agni.*;
import org.emaginniss.agni.attachments.Attachments;
import org.emaginniss.agni.messages.*;
import org.emaginniss.agni.util.LimitedHashSet;

import java.util.Arrays;
import java.util.Set;

public class WhisperHandler {

    private static final Logger log = LogManager.getLogger(WhisperHandler.class);

    private Set<String> seenWhispers = new LimitedHashSet<>(100);
    private Node node;

    public WhisperHandler(Node node) {
        this.node = node;
        new SubscriptionBuilder(this).method("handleSubscriptionInfo").uuid("WHISPER_" + node.getUuid() + "_SUBSCRIPTION_INFO").subscribe(node);
        new SubscriptionBuilder(this).method("handleAddDestination").uuid("WHISPER_" + node.getUuid() + "_ADD_DESTINATION").subscribe(node);
        new SubscriptionBuilder(this).method("handleRemoveDestination").uuid("WHISPER_" + node.getUuid() + "_REMOVE_DESTINATION").subscribe(node);
        new SubscriptionBuilder(this).method("handleRemoveLink").uuid("WHISPER_" + node.getUuid() + "_REMOVE_LINK").subscribe(node);
        new SubscriptionBuilder(this).method("handlePing").uuid("WHISPER_" + node.getUuid() + "_PING").subscribe(node);
        new SubscriptionBuilder(this).method("handleStatsRequest").uuid("WHISPER_" + node.getUuid() + "_STATS_REQUEST").type(StatsRequest.class.getName()).subscribe(node);
        new SubscriptionBuilder(this).method("handleStopRouting").uuid("WHISPER_" + node.getUuid() + "_STOP_ROUTING").subscribe(node);
    }

    public Envelope buildSubscriptionInfoEnvelope(String previousMessageUuid, String []pathIn, String targetUuid) {
        SubscriptionInfo info = new SubscriptionInfo();
        if (previousMessageUuid != null) {
            info.setMessageUuid(previousMessageUuid);
        }
        info.getDestinations().addAll(node.getDestinationRegistration().getAll());
        info.getPaths().addAll(Arrays.asList(node.getPathFinder().getKnownPaths()));
        Envelope envelope = new Envelope(SubscriptionInfo.class.getName(), SubscriptionInfo.class.getName(), node.getSerializer().serialize(info), Priority.HIGHEST, new Attachments(), new Criteria(), false);
        envelope.setDestinationUuid("WHISPER_" + targetUuid + "_SUBSCRIPTION_INFO");
        envelope.setNodeUuid(targetUuid);
        envelope.setPath(pathIn);

        return envelope;
    }

    public void handleSubscriptionInfo(SubscriptionInfo subscriptionInfo, Envelope envelope) {
        if (checkMessage(subscriptionInfo)) {
            log.debug("Incoming subscriptionInfo from " + envelope.getPath()[envelope.getPath().length - 2]);
            node.getDestinationRegistration().handle(subscriptionInfo);
            node.getPathFinder().handle(subscriptionInfo, envelope.getPath()[envelope.getPath().length - 2]);
            String[] path = new String[envelope.getPath().length - 1];
            System.arraycopy(envelope.getPath(), 0, path, 0, path.length);
            for (String connectedNodeUuid : node.getConnectedNodeUuids()) {
                if (!envelope.hasVisited(connectedNodeUuid)) {
                    node.getInbox().enqueue(buildSubscriptionInfoEnvelope(subscriptionInfo.getMessageUuid(), path, connectedNodeUuid));
                }
            }
        }
    }

    private boolean checkMessage(WhisperMessage message) {
        if (seenWhispers.contains(message.getMessageUuid())) {
            return false;
        }
        seenWhispers.add(message.getMessageUuid());
        return true;
    }

    public void handleAddDestination(AddDestination addDestination, Envelope envelope) {
        if (checkMessage(addDestination)) {
            if (envelope.getPath().length > 1) {
                log.debug("Incoming addDestination from " + envelope.getPath()[envelope.getPath().length - 2]);
                node.getDestinationRegistration().handle(addDestination);
            }
        }
    }

    public void handleRemoveDestination(RemoveDestination removeDestination, Envelope envelope) {
        if (checkMessage(removeDestination)) {
            if (envelope.getPath().length > 1) {
                log.debug("Incoming removeDestination from " + envelope.getPath()[envelope.getPath().length - 2]);
                node.getDestinationRegistration().handle(removeDestination);
            }
        }
    }

    public void handleRemoveLink(RemoveLink removeLink, Envelope envelope) {
        if (checkMessage(removeLink)) {
            if (envelope.getPath().length > 1) {
                log.debug("Incoming removeLink from " + envelope.getPath()[envelope.getPath().length - 2]);
                node.getPathFinder().handle(removeLink);
            }
        }
    }

    public Ping handlePing(Ping in) {
        return in;
    }

    public StatsResponse handleStatsRequest() {
        return node.buildStatsResponse();
    }

    public void handleStopRouting(StopRouting msg) {
        if (checkMessage(msg)) {
            if (!msg.getNodeUuid().equals(node.getUuid())) {
                log.debug("Incoming stopRouting from " + msg.getNodeUuid());
                Set<String> lostNodeUuids = node.getPathFinder().handle(msg);
                node.getDestinationRegistration().handleLostNodes(lostNodeUuids);
            }
        }
    }
}
