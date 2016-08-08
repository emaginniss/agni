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

package org.emaginniss.agni;

import org.apache.log4j.BasicConfigurator;
import org.emaginniss.agni.annotations.Criterion;
import org.emaginniss.agni.annotations.Subscribe;
import org.emaginniss.agni.impl.NodeImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class LocalNodeTest {

    @Before
    public void setup() throws Exception {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
    }

    @Test
    public void testNodeCreation() throws Exception {
        Node node = new NodeImpl();
        node.shutdown();
    }

    @Test
    public void testSimpleRegister1() throws Exception {
        try (Node node = new NodeImpl()) {
            node.register(new Object() {
                @Subscribe(typeClass = String.class)
                public void handleString() {
                    //Do nothing
                }
            });
            assertEquals(1, node.getDestinations(new String[]{String.class.getName()}, null, true).size());
        }
    }

    @Test
    public void testSimpleRegister2() throws Exception {
        try (Node node = new NodeImpl()) {
            node.register(new Object() {
                @Subscribe
                public void handleString(String in) {
                    //Do nothing
                }
            });
            assertEquals(1, node.getDestinations(new String[]{String.class.getName()}, null, true).size());
        }
    }

    @Test
    public void testSimpleRegister3() throws Exception {
        try (Node node = new NodeImpl()) {
            node.register(new Object() {
                @Subscribe(typeName = "java.lang.String")
                public void handleString() {
                    //Do nothing
                }
            });
            assertEquals(1, node.getDestinations(new String[]{String.class.getName()}, null, true).size());
        }
    }

    @Test
    public void testCriteriaRegister1() throws Exception {
        try (Node node = new NodeImpl()) {
            node.register(new Object() {
                @Subscribe(typeClass = String.class, criteria = { @Criterion(key = "x", value = "y")})
                public void handleString() {
                    //Do nothing
                }
            });
            assertEquals(0, node.getDestinations(new String[]{String.class.getName()}, null, true).size());
            assertEquals(0, node.getDestinations(new String[]{String.class.getName()}, new Criteria().add("x", "z"), true).size());
            assertEquals(1, node.getDestinations(new String[]{String.class.getName()}, new Criteria().add("x", "y"), true).size());
            assertEquals(1, node.getDestinations(new String[]{String.class.getName()}, new Criteria().add("x", "*"), true).size());
        }
    }

    @Test
    public void testCriteriaRegister2() throws Exception {
        try (Node node = new NodeImpl()) {
            node.register(new Object() {
                @Subscribe(typeClass = String.class, criteria = { @Criterion(key = "x", value = "y")})
                public void handleString1() {
                    //Do nothing
                }

                @Subscribe(typeClass = String.class, criteria = { @Criterion(key = "x", value = "z")})
                public void handleString2() {
                    //Do nothing
                }
            });
            assertEquals(0, node.getDestinations(new String[]{String.class.getName()}, null, true).size());
            assertEquals(1, node.getDestinations(new String[]{String.class.getName()}, new Criteria().add("x", "z"), true).size());
            assertEquals(1, node.getDestinations(new String[]{String.class.getName()}, new Criteria().add("x", "y"), true).size());
            assertEquals(2, node.getDestinations(new String[]{String.class.getName()}, new Criteria().add("x", "*"), true).size());
        }
    }

    @Test
    public void testUnregister() throws Exception {
        try (Node node = new NodeImpl()) {
            Object obj = new Object() {
                @Subscribe(typeClass = String.class)
                public void handleString1() {
                    //Do nothing
                }
            };
            node.register(obj);
            assertEquals(1, node.getDestinations(new String[]{String.class.getName()}, null, true).size());
            Thread.sleep(300);
            node.unsubscribe(obj, null);
            assertEquals(0, node.getDestinations(new String[]{String.class.getName()}, null, true).size());
        }
    }

    @Test
    public void testSpecificUnregister() throws Exception {
        try (Node node = new NodeImpl()) {
            Object obj = new Object() {
                @Subscribe(typeClass = String.class)
                public void handleString() {
                    //Do nothing
                }

                @Subscribe(typeClass = Integer.class)
                public void handleInteger() {
                    //Do nothing
                }
            };
            node.register(obj);
            assertEquals(1, node.getDestinations(new String[]{String.class.getName()}, null, true).size());
            assertEquals(1, node.getDestinations(new String[]{Integer.class.getName()}, null, true).size());
            Thread.sleep(300);
            new SubscriptionBuilder(obj).method("handleString").unsubscribe(node);
            assertEquals(0, node.getDestinations(new String[]{String.class.getName()}, null, true).size());
            assertEquals(1, node.getDestinations(new String[]{Integer.class.getName()}, null, true).size());
        }
    }

    @Test
    public void testSend() throws Exception {
        try (Node node = new NodeImpl()) {
            final AtomicInteger receivedCount = new AtomicInteger(0);
            Object obj = new Object() {
                @Subscribe(typeClass = Integer.class)
                public void handleInt() {
                    receivedCount.incrementAndGet();
                }
            };
            node.register(obj);
            for (int i = 0; i < 100; i++) {
                node.send(i, null, null, null, Priority.MEDIUM);
            }
            Thread.sleep(1000);
            assertEquals(100, receivedCount.get());
        }
    }

    @Test
    public void testRequest() throws Exception {
        try (Node node = new NodeImpl()) {
            Object obj = new Object() {
                @Subscribe(typeClass = Integer.class)
                public int handleInt(int in) {
                    return in;
                }
            };
            node.register(obj);
            for (int i = 0; i < 100; i++) {
                assertEquals(i, node.request(i, null, null, null, Priority.MEDIUM, 30000l).getPayload());
            }
        }
    }

    @Test
    public void testBroadcast() throws Exception {
        try (Node node = new NodeImpl()) {
            final AtomicInteger receivedCount = new AtomicInteger(0);
            Object obj = new Object() {
                @Subscribe(typeClass = Integer.class)
                public void handleInt1() {
                    receivedCount.incrementAndGet();
                }

                @Subscribe(typeClass = Integer.class)
                public void handleInt2() {
                    receivedCount.incrementAndGet();
                }

                @Subscribe(typeClass = Integer.class)
                public void handleInt3() {
                    receivedCount.incrementAndGet();
                }
            };
            node.register(obj);
            for (int i = 0; i < 100; i++) {
                node.broadcast(i, null, null, null, Priority.MEDIUM);
            }
            Thread.sleep(1000);
            assertEquals(300, receivedCount.get());
        }
    }

    @Test
    public void testRequestAll() throws Exception {
        try (Node node = new NodeImpl()) {
            Object obj = new Object() {
                @Subscribe(typeClass = Integer.class)
                public int handleInt1(int in) {
                    return in;
                }

                @Subscribe(typeClass = Integer.class)
                public int handleInt2(int in) {
                    return in;
                }

                @Subscribe(typeClass = Integer.class)
                public int handleInt3(int in) {
                    return in;
                }
            };
            node.register(obj);
            for (int i = 0; i < 100; i++) {
                Map<Destination, PayloadAndAttachments> out = node.requestAll(i, null, null, null, Priority.MEDIUM, 30000l);
                assertEquals(3, out.size());
                for (PayloadAndAttachments p : out.values()) {
                    assertEquals(i, p.getPayload());
                }
            }
        }
    }
}
