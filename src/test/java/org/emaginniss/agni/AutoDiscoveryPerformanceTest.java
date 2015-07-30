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
import org.apache.log4j.BasicConfigurator;
import org.emaginniss.agni.annotations.Subscribe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Eric on 7/18/2015.
 */
public class AutoDiscoveryPerformanceTest {

    private Configuration nodeAConf = new Configuration(new JsonParser().parse("{ 'uuid': 'nodeA', 'connections': { 'autoDiscovery': { 'type': 'autoDiscovery' }}}").getAsJsonObject());
    private Configuration nodeBConf = new Configuration(new JsonParser().parse("{ 'uuid': 'nodeB', 'connections': { 'autoDiscovery': { 'type': 'autoDiscovery' }}}").getAsJsonObject());

    private Node nodeA = null;
    private Node nodeB = null;

    @Before
    public void setup() throws Exception {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();

        nodeA = new Node(nodeAConf);
        Thread.sleep(1000);
        nodeB = new Node(nodeBConf);
        Thread.sleep(1000);
    }

    @After
    public void teardown() throws Exception {
        nodeA.shutdown();
        Thread.sleep(1000);
        nodeB.shutdown();
        Thread.sleep(1000);
    }

    @Test
    public void singleThreadSendTest() throws Exception {
        final int messageTotal = 10000;
        final AtomicLong counter = new AtomicLong(0);
        nodeA.register(new Object() {
            @Subscribe(typeName = "Message1")
            public void handle() {
                counter.incrementAndGet();
            }
        });
        Thread.sleep(1000);
        long start = System.currentTimeMillis();
        for (int i = 0; i < messageTotal; i++) {
            new AgniBuilder("").type("Message1").send(nodeB);
        }
        while (counter.get() < messageTotal) {
            Thread.sleep(10);
        }
        long end = System.currentTimeMillis();
        System.out.println("singleThreadSendTest MPS = " + ((double)messageTotal / (end - start)) * 1000.0);
    }


    @Test
    public void multiThreadSendTest() throws Exception {
        final int messageTotal = 10000;
        final int threadTotal = 100;
        Assert.assertEquals(0, messageTotal % threadTotal);
        final AtomicLong counter = new AtomicLong(0);
        nodeA.register(new Object() {
            @Subscribe(typeName = "Message1")
            public void handle() {
                counter.incrementAndGet();
            }
        });
        Thread.sleep(1000);
        final Thread []threads = new Thread[threadTotal];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < (messageTotal / threads.length); i++) {
                        new AgniBuilder("").type("Message1").send(nodeB);
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

    @Test
    public void singleThreadRequestTest() throws Exception {
        final int messageTotal = 10000;
        final AtomicLong counter = new AtomicLong(0);
        nodeA.register(new Object() {
            @Subscribe(typeName = "Message1")
            public String handle() {
                counter.incrementAndGet();
                return "";
            }
        });
        Thread.sleep(1000);
        long start = System.currentTimeMillis();
        for (int i = 0; i < messageTotal; i++) {
            new AgniBuilder("").type("Message1").request(nodeB);
        }
        while (counter.get() < messageTotal) {
            Thread.sleep(10);
        }
        long end = System.currentTimeMillis();
        System.out.println("singleThreadRequestTest MPS = " + ((double)messageTotal / (end - start)) * 1000.0);
    }

    @Test
    public void multiThreadRequestTest() throws Exception {
        final int messageTotal = 10000;
        final int threadTotal = 100;
        Assert.assertEquals(0, messageTotal % threadTotal);
        final AtomicLong counter = new AtomicLong(0);
        nodeA.register(new Object() {
            @Subscribe(typeName = "Message1")
            public String handle() {
                counter.incrementAndGet();
                return "";
            }
        });
        Thread.sleep(1000);
        final Thread []threads = new Thread[threadTotal];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < (messageTotal / threads.length); i++) {
                        new AgniBuilder("").type("Message1").request(nodeB);
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
