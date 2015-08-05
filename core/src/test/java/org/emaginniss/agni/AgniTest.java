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
import org.emaginniss.agni.annotations.Subscribe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Eric on 7/14/2015.
 */
public class AgniTest {

    @Before
    public void setup() throws Exception {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(1000);
    }

    @Test
    public void testNodeCreation() throws Exception {
        final AtomicInteger hitCount = new AtomicInteger(0);

        Agni.initialize();
        Agni.register(new Object() {
            @Subscribe(typeName = "Message1")
            public void handle() {
                hitCount.incrementAndGet();
            }
        });

        Agni.build("").type("Message1").send();
        Thread.sleep(100);

        Assert.assertEquals(1, hitCount.get());

        Agni.shutdown();
    }
}
