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
import org.emaginniss.agni.attachments.Attachments;
import org.emaginniss.agni.impl.NodeImpl;
import org.junit.Before;
import org.junit.Test;
import sun.misc.IOUtils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class AttachmentsTest {

    @Test
    public void singleNodeAttachmentSendTest() throws Exception {
        try (Node node = new NodeImpl()) {
            final AtomicLong counter = new AtomicLong(0);
            node.register(new Object() {
                @Subscribe(typeName = "Message1")
                public void handle(Attachments atts) throws Exception {
                    if (Arrays.equals("This is a test".getBytes(), IOUtils.readFully(atts.get("att1").open(), -1, true))) {
                        counter.incrementAndGet();
                    }
                }
            });
            new AgniBuilder("").type("Message1").attachment("att1", "This is a test".getBytes()).send(node);
            Thread.sleep(100);
            assertEquals(1, counter.get());
        }
    }

    @Test
    public void singleNodeAttachmentRequestTest() throws Exception {
        try (Node node = new NodeImpl()) {
            final AtomicLong counter = new AtomicLong(0);
            node.register(new Object() {
                @Subscribe(typeName = "Message1")
                public String handle(Attachments atts) throws Exception {
                    if (Arrays.equals("This is a test".getBytes(), IOUtils.readFully(atts.get("att1").open(), -1, true))) {
                        counter.incrementAndGet();
                    }
                    return "Howdy";
                }
            });
            assertEquals("Howdy", new AgniBuilder("").type("Message1").attachment("att1", "This is a test".getBytes()).request(node).getPayload());
            assertEquals(1, counter.get());
        }
    }

    @Test
    public void singleNodeAttachmentRequestResponseTest() throws Exception {
        try (Node node = new NodeImpl()) {
            final AtomicLong counter = new AtomicLong(0);
            node.register(new Object() {
                @Subscribe(typeName = "Message1")
                public PayloadAndAttachments<String> handle(Attachments atts) throws Exception {
                    if (Arrays.equals("This is a test".getBytes(), IOUtils.readFully(atts.get("att1").open(), -1, true))) {
                        counter.incrementAndGet();
                    }
                    return new PayloadAndAttachments<>("Howdy", new Attachments().addByteArrayAttachment("att2", "So is this".getBytes()));
                }
            });
            PayloadAndAttachments<String> resp = new AgniBuilder("").type("Message1").attachment("att1", "This is a test".getBytes()).request(node);
            assertEquals("Howdy", resp.getPayload());
            assertArrayEquals("So is this".getBytes(), IOUtils.readFully(resp.getAttachments().get("att2").open(), -1, true));
            assertEquals(1, counter.get());
        }
    }

    private Configuration nodeAConf = new Configuration(new JsonParser().parse("{ 'uuid': 'nodeA', 'connections': { 'autoDiscovery': { 'type': 'autoDiscovery' }}}").getAsJsonObject());
    private Configuration nodeBConf = new Configuration(new JsonParser().parse("{ 'uuid': 'nodeB', 'connections': { 'autoDiscovery': { 'type': 'autoDiscovery' }}}").getAsJsonObject());

    @Test
    public void multipleNodeAttachmentSendTest() throws Exception {
        Node nodeA = null;
        Node nodeB = null;
        try {
            final AtomicLong counter = new AtomicLong(0);
            nodeA = new NodeImpl(nodeAConf);
            nodeA.register(new Object() {
                @Subscribe(typeName = "Message1")
                public void handle(Attachments atts) throws Exception {
                    if (Arrays.equals("This is a test".getBytes(), IOUtils.readFully(atts.get("att1").open(), -1, true))) {
                        counter.incrementAndGet();
                    }
                }
            });
            Thread.sleep(1000);
            nodeB = new NodeImpl(nodeBConf);
            Thread.sleep(1000);

            new AgniBuilder("").type("Message1").attachment("att1", "This is a test".getBytes()).send(nodeB);
            Thread.sleep(100);
            assertEquals(1, counter.get());
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
    public void multipleNodeAttachmentRequestTest() throws Exception {
        Node nodeA = null;
        Node nodeB = null;
        try {
            nodeA = new NodeImpl(nodeAConf);
            final AtomicLong counter = new AtomicLong(0);
            nodeA.register(new Object() {
                @Subscribe(typeName = "Message1")
                public String handle(Attachments atts) throws Exception {
                    if (Arrays.equals("This is a test".getBytes(), IOUtils.readFully(atts.get("att1").open(), -1, true))) {
                        counter.incrementAndGet();
                    }
                    return "Howdy";
                }
            });
            Thread.sleep(1000);
            nodeB = new NodeImpl(nodeBConf);
            Thread.sleep(1000);

            assertEquals("Howdy", new AgniBuilder("").type("Message1").attachment("att1", "This is a test".getBytes()).request(nodeB).getPayload());
            assertEquals(1, counter.get());
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
    public void multipleNodeAttachmentRequestResponseTest() throws Exception {
        Node nodeA = null;
        Node nodeB = null;
        try {
            nodeA = new NodeImpl(nodeAConf);
            final AtomicLong counter = new AtomicLong(0);
            nodeA.register(new Object() {
                @Subscribe(typeName = "Message1")
                public PayloadAndAttachments<String> handle(Attachments atts) throws Exception {
                    if (Arrays.equals("This is a test".getBytes(), IOUtils.readFully(atts.get("att1").open(), -1, true))) {
                        counter.incrementAndGet();
                    }
                    return new PayloadAndAttachments<>("Howdy", new Attachments().addByteArrayAttachment("att2", "So is this".getBytes()));
                }
            });
            Thread.sleep(1000);
            nodeB = new NodeImpl(nodeBConf);
            Thread.sleep(1000);

            PayloadAndAttachments<String> resp = new AgniBuilder("").type("Message1").attachment("att1", "This is a test".getBytes()).request(nodeB);
            assertEquals("Howdy", resp.getPayload());
            assertArrayEquals("So is this".getBytes(), IOUtils.readFully(resp.getAttachments().get("att2").open(), -1, true));
            assertEquals(1, counter.get());
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
    public void multipleNodeLargeAttachmentSendTest() throws Exception {
        Node nodeA = null;
        Node nodeB = null;
        try {
            nodeA = new NodeImpl(nodeAConf);
            final AtomicLong counter = new AtomicLong(0);
            nodeA.register(new Object() {
                @Subscribe(typeName = "Message1")
                public void handle(Attachments atts) throws Exception {
                    byte []hold = IOUtils.readFully(atts.get("att1").open(), -1, true);
                    boolean correct = hold.length == 455 * 1024;
                    byte test = 0;
                    for (byte b : hold) {
                        if (b != test) {
                            correct = false;
                        }
                        test++;
                    }
                    if (correct) {
                        counter.incrementAndGet();
                    }
                }
            });
            Thread.sleep(1000);
            nodeB = new NodeImpl(nodeBConf);
            Thread.sleep(1000);

            byte []send = new byte[455 * 1024];
            byte b = 0;
            for (int i = 0; i < send.length; i++) {
                send[i] = b++;
            }

            new AgniBuilder("").type("Message1").attachment("att1", send).send(nodeB);
            Thread.sleep(1000);
            assertEquals(1, counter.get());
        } finally {
            if (nodeA != null) {
                nodeA.shutdown();
            }
            if (nodeB != null) {
                nodeB.shutdown();
            }
        }
    }
}
