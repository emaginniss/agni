/*
 * Copyright (c) 2015-2016, Eric A Maginniss
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

import com.hazelcast.config.Config;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.core.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emaginniss.agni.Configuration;
import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.annotations.Component;

/**
 * Created by emagi on 10/30/2017.
 */
@Component(value = "hazelcast")
public class HazelcastConnection implements Connection {

    private static final Logger log = LogManager.getLogger(HazelcastConnection.class);

    private final Configuration configuration;
    private final ConnectionParent parent;
    private HazelcastInstance instance;
    private boolean shutdown = false;
    private ListenerThead listenerThead;

    public HazelcastConnection(Configuration configuration, ConnectionParent parent) {
        this.configuration = configuration;
        this.parent = parent;

        Config config = new Config();
        MemberAttributeConfig memberAttributeConfig = new MemberAttributeConfig();
        memberAttributeConfig.setStringAttribute("uuid", parent.getUuid());
        memberAttributeConfig.setStringAttribute("display", parent.getDisplayName());
        config.setMemberAttributeConfig(memberAttributeConfig);
        instance = Hazelcast.newHazelcastInstance(config);
        instance.getCluster().addMembershipListener(new MembershipAdapter() {
            @Override
            public void memberAdded(MembershipEvent membershipEvent) {
                super.memberAdded(membershipEvent);
                log.error("Connection established with " + membershipEvent.getMember().getStringAttribute("uuid"));
                parent.enableConnection(HazelcastConnection.this, membershipEvent.getMember().getStringAttribute("uuid"), membershipEvent.getMember().getStringAttribute("display"));
            }

            @Override
            public void memberRemoved(MembershipEvent membershipEvent) {
                super.memberRemoved(membershipEvent);
                log.error("Connection lost with " + membershipEvent.getMember().getStringAttribute("uuid"));
                parent.disableConnection(HazelcastConnection.this, membershipEvent.getMember().getStringAttribute("uuid"), membershipEvent.getMember().getStringAttribute("display"));
            }
        });

        listenerThead = new ListenerThead();
        listenerThead.start();
    }

    @Override
    public void shutdown() {
        shutdown = true;
        listenerThead.interrupt();
        instance.shutdown();
        try {
            listenerThead.join(5000);
        } catch (Exception e) {
            //do nothing
        }
    }

    @Override
    public boolean forwardMessage(Envelope envelope, String targetNodeUuid) {
        IQueue<Envelope> queue = instance.getQueue(targetNodeUuid);
        log.error("Push envelope to " + targetNodeUuid);
        queue.offer(envelope);
        return true;
    }

    private class ListenerThead extends Thread {

        public ListenerThead() {
            super(parent.getNode().getThreadGroup(), parent.getDisplayName() + " - Multicast Server Thread");
        }

        @Override
        public void run() {
            IQueue<Envelope> queue = instance.getQueue(parent.getUuid());
            while (!shutdown) {
                try {
                    Envelope envelope = queue.take();
                    log.error("Got envelope from " + envelope.getPath()[envelope.getPath().length - 1]);
                    parent.handleIncomingEnvelope(envelope);
                } catch (Exception e) {
                    if (!shutdown) {
                        log.error("Error", e);
                    }
                }
            }
        }
    }
}
