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

import org.emaginniss.agni.annotations.Subscribe;
import org.emaginniss.agni.impl.NodeImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class LocalNodePerformanceTest {

    @Test
    public void singleThreadSendTest() throws Exception {
        final int messageTotal = 10000;
        try (Node node = new NodeImpl()) {
            final AtomicLong counter = new AtomicLong(0);
            node.register(new Object() {
                @Subscribe(typeName = "Message1")
                public void handle() {
                    counter.incrementAndGet();
                }
            });
            long start = System.currentTimeMillis();
            for (int i = 0; i < messageTotal; i++) {
                new AgniBuilder("").type("Message1").send(node);
            }
            while (counter.get() < messageTotal) {
                Thread.sleep(10);
            }
            long end = System.currentTimeMillis();
            System.out.println("singleThreadSendTest MPS = " + ((double)messageTotal / (end - start)) * 1000.0);
        }
    }


    @Test
    public void multiThreadSendTest() throws Exception {
        final int messageTotal = 10000;
        final int threadTotal = 100;
        Assert.assertEquals(0, messageTotal % threadTotal);
        try (Node node = new NodeImpl()) {
            final AtomicLong counter = new AtomicLong(0);
            node.register(new Object() {
                @Subscribe(typeName = "Message1")
                public void handle() {
                    counter.incrementAndGet();
                }
            });
            final Thread []threads = new Thread[threadTotal];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread() {
                    @Override
                    public void run() {
                        for (int i = 0; i < (messageTotal / threads.length); i++) {
                            new AgniBuilder("").type("Message1").send(node);
                        }
                    }
                };
            }
            long start = System.currentTimeMillis();
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
            while (counter.get() < messageTotal) {
                Thread.sleep(10);
            }
            long end = System.currentTimeMillis();
            System.out.println("multiThreadSendTest MPS = " + ((double)messageTotal / (end - start)) * 1000.0);
        }
    }

    @Test
    public void singleThreadRequestTest() throws Exception {
        final int messageTotal = 10000;
        try (Node node = new NodeImpl()) {
            final AtomicLong counter = new AtomicLong(0);
            node.register(new Object() {
                @Subscribe(typeName = "Message1")
                public String handle() {
                    counter.incrementAndGet();
                    return "";
                }
            });
            long start = System.currentTimeMillis();
            for (int i = 0; i < messageTotal; i++) {
                new AgniBuilder("").type("Message1").request(node);
            }
            while (counter.get() < messageTotal) {
                Thread.sleep(10);
            }
            long end = System.currentTimeMillis();
            System.out.println("singleThreadRequestTest MPS = " + ((double)messageTotal / (end - start)) * 1000.0);
        }
    }


    @Test
    public void multiThreadRequestTest() throws Exception {
        final int messageTotal = 10000;
        final int threadTotal = 100;
        Assert.assertEquals(0, messageTotal % threadTotal);
        try (Node node = new NodeImpl()) {
            final AtomicLong counter = new AtomicLong(0);
            node.register(new Object() {
                @Subscribe(typeName = "Message1")
                public String handle() {
                    counter.incrementAndGet();
                    return "";
                }
            });
            final Thread []threads = new Thread[threadTotal];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread() {
                    @Override
                    public void run() {
                        for (int i = 0; i < (messageTotal / threads.length); i++) {
                            new AgniBuilder("").type("Message1").request(node);
                        }
                    }
                };
            }
            long start = System.currentTimeMillis();
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
            while (counter.get() < messageTotal) {
                Thread.sleep(10);
            }
            long end = System.currentTimeMillis();
            System.out.println("multiThreadRequestTest MPS = " + ((double)messageTotal / (end - start)) * 1000.0);
        }
    }
}
