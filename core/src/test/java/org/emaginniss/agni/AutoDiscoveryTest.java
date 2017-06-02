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

import com.google.gson.JsonParser;
import org.emaginniss.agni.annotations.Subscribe;
import org.emaginniss.agni.impl.NodeImpl;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class AutoDiscoveryTest {

    private Configuration nodeAConf = new Configuration(new JsonParser().parse("{ 'uuid': 'nodeA', 'connections': { 'autoDiscovery': { 'type': 'autoDiscovery' }}}").getAsJsonObject());
    private Configuration nodeBConf = new Configuration(new JsonParser().parse("{ 'uuid': 'nodeB', 'connections': { 'autoDiscovery': { 'type': 'autoDiscovery' }}}").getAsJsonObject());
    private Configuration nodeCConf = new Configuration(new JsonParser().parse("{ 'uuid': 'nodeC', 'connections': { 'autoDiscovery': { 'type': 'autoDiscovery' }}}").getAsJsonObject());
    private Configuration nodeDConf = new Configuration(new JsonParser().parse("{ 'uuid': 'nodeD', 'connections': { 'autoDiscovery': { 'type': 'autoDiscovery' }}}").getAsJsonObject());
    private Configuration nodeEConf = new Configuration(new JsonParser().parse("{ 'uuid': 'nodeE', 'connections': { 'autoDiscovery': { 'type': 'autoDiscovery' }}}").getAsJsonObject());

    @After
    public void tearDown() throws Exception {
        Thread.sleep(1000);
    }

    @Test
    public void testNodeCreation() throws Exception {
        Node nodeA = null;
        Node nodeB = null;
        try {
            nodeA = new NodeImpl(nodeAConf);
            Thread.sleep(1000);
            nodeB = new NodeImpl(nodeBConf);

            Thread.sleep(1000);
            nodeA.register(new Object() {
                @Subscribe(typeName = "Message1")
                public void handle1() {
                }

                @Subscribe(typeName = "Message2")
                public void handle2() {
                }
            });
            Thread.sleep(1000);
            nodeB.register(new Object() {
                @Subscribe(typeName = "Message1")
                public void handle1() {
                }

                @Subscribe(typeName = "Message3")
                public void handle2() {
                }
            });
            Thread.sleep(1000);

            assertEquals(2, nodeA.getDestinationPaths(new String[]{"Message1"}, new Criteria(), true).size());
            assertEquals(2, nodeB.getDestinationPaths(new String[]{"Message1"}, new Criteria(), true).size());
            assertEquals(1, nodeA.getDestinationPaths(new String[]{"Message2"}, new Criteria(), true).size());
            assertEquals(1, nodeB.getDestinationPaths(new String[]{"Message2"}, new Criteria(), true).size());
            assertEquals(1, nodeA.getDestinationPaths(new String[]{"Message3"}, new Criteria(), true).size());
            assertEquals(1, nodeB.getDestinationPaths(new String[]{"Message3"}, new Criteria(), true).size());

            Object m4 = new Object() {
                @Subscribe(typeName = "Message4")
                public void handle1() {
                }
            };
            nodeA.register(m4);
            Thread.sleep(1000);

            assertEquals(1, nodeA.getDestinationPaths(new String[]{"Message4"}, new Criteria(), true).size());
            assertEquals(1, nodeB.getDestinationPaths(new String[]{"Message4"}, new Criteria(), true).size());

            nodeA.unsubscribe(m4, null);
            Thread.sleep(1000);

            assertEquals(0, nodeA.getDestinationPaths(new String[]{"Message4"}, new Criteria(), true).size());
            assertEquals(0, nodeB.getDestinationPaths(new String[]{"Message4"}, new Criteria(), true).size());
        } finally {
            if (nodeA != null) {
                nodeA.shutdown();
            }
            if (nodeB != null) {
                nodeB.shutdown();
            }
        }
    }

    @Test
    public void testSending() throws Exception {
        final AtomicInteger hitCount = new AtomicInteger(0);

        Node nodeA = null;
        Node nodeB = null;
        Node nodeC = null;
        Node nodeD = null;
        Node nodeE = null;
        try {
            nodeA = new NodeImpl(nodeAConf);
            Thread.sleep(1000);
            nodeB = new NodeImpl(nodeBConf);
            Thread.sleep(1000);
            nodeC = new NodeImpl(nodeCConf);
            Thread.sleep(1000);
            nodeD = new NodeImpl(nodeDConf);
            Thread.sleep(1000);
            nodeE = new NodeImpl(nodeEConf);
            Thread.sleep(1000);

            nodeA.register(new Object() {
                @Subscribe(typeName = "Message1")
                public String handle1() {
                    hitCount.incrementAndGet();
                    return "OK";
                }

                @Subscribe(typeName = "Message2")
                public String handle2() {
                    hitCount.incrementAndGet();
                    return "OK";
                }
            });
            Thread.sleep(1000);
            nodeB.register(new Object() {
                @Subscribe(typeName = "Message1")
                public String handle1() {
                    hitCount.incrementAndGet();
                    return "OK";
                }

                @Subscribe(typeName = "Message3")
                public String handle3() {
                    hitCount.incrementAndGet();
                    return "OK";
                }
            });
            Thread.sleep(1000);

            new AgniBuilder("Test").type("Message1").send(nodeA);
            new AgniBuilder("Test").type("Message1").send(nodeB);
            Thread.sleep(1000);
            assertEquals(2, hitCount.get());

            new AgniBuilder("Test").type("Message1").broadcast(nodeA);
            Thread.sleep(1000);
            assertEquals(4, hitCount.get());

            assertEquals("OK", new AgniBuilder("Test").type("Message1").request(nodeA).getPayload());
            assertEquals("OK", new AgniBuilder("Test").type("Message1").request(nodeB).getPayload());
            assertEquals(6, hitCount.get());

            assertEquals(2, new AgniBuilder("Test").type("Message1").requestAll(nodeA).size());
            assertEquals(2, new AgniBuilder("Test").type("Message1").requestAll(nodeB).size());
            assertEquals(2, new AgniBuilder("Test").type("Message1").requestAll(nodeC).size());
            assertEquals(2, new AgniBuilder("Test").type("Message1").requestAll(nodeD).size());
            assertEquals(2, new AgniBuilder("Test").type("Message1").requestAll(nodeE).size());
            assertEquals(16, hitCount.get());

            hitCount.set(0);
            new AgniBuilder("Test").type("Message2").send(nodeA);
            new AgniBuilder("Test").type("Message2").send(nodeB);
            Thread.sleep(1000);
            assertEquals(2, hitCount.get());

            new AgniBuilder("Test").type("Message2").broadcast(nodeA);
            Thread.sleep(1000);
            assertEquals(3, hitCount.get());

            assertEquals("OK", new AgniBuilder("Test").type("Message2").request(nodeA).getPayload());
            assertEquals("OK", new AgniBuilder("Test").type("Message2").request(nodeB).getPayload());
            assertEquals(5, hitCount.get());

            assertEquals(1, new AgniBuilder("Test").type("Message2").requestAll(nodeA).size());
            assertEquals(1, new AgniBuilder("Test").type("Message2").requestAll(nodeB).size());
            assertEquals(7, hitCount.get());
        } finally {
            if (nodeA != null) {
                nodeA.shutdown();
            }
            if (nodeB != null) {
                nodeB.shutdown();
            }
            if (nodeC != null) {
                nodeC.shutdown();
            }
            if (nodeD != null) {
                nodeD.shutdown();
            }
            if (nodeE != null) {
                nodeE.shutdown();
            }
        }
    }
}
