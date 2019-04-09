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

package org.emaginniss.agni.scheduler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.emaginniss.agni.Agni;
import org.emaginniss.agni.Configuration;
import org.emaginniss.agni.annotations.Subscribe;
import org.emaginniss.agni.scheduler.messages.ScheduledTask;
import org.emaginniss.agni.scheduler.util.ScheduledTaskBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class SchedulerTest {

    public static class Message1 {
        private String field1;
        private int field2;

        public Message1() {
        }

        public Message1(String field1, int field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public int getField2() {
            return field2;
        }

        public void setField2(int field2) {
            this.field2 = field2;
        }
    }

    @Before
    public void setup() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        Agni.initialize();
        new Scheduler(Agni.getNode(), new Configuration());
    }

    @After
    public void tearDown() {
        Agni.shutdown();
    }

    @Test
    public void testScheduler() throws Exception {
        final AtomicBoolean handlerHit = new AtomicBoolean(false);

        SchedulerManager sm = Agni.createManager(SchedulerManager.class);

        ScheduledTask []tasks = sm.list();
        assertNotNull(tasks);
        assertEquals(0, tasks.length);

        Agni.register(new Object() {
            @Subscribe
            public void handle(Message1 m1) {
                if ("f1".equals(m1.getField1()) && 45 == m1.getField2()) {
                    handlerHit.set(true);
                }
            }
        });

        sm.add(new ScheduledTaskBuilder()
                .name("st1")
                .group("g1")
                .payload(new Message1("f1", 45), Agni.getNode())
                .intervallMs(1000)
                .build());
        Thread.sleep(2000);
        assertTrue(handlerHit.get());

        tasks = sm.list();
        assertNotNull(tasks);
        assertEquals(1, tasks.length);
        assertEquals("st1", tasks[0].getName());

        sm.delete("st1", "g1");

        tasks = sm.list();
        assertNotNull(tasks);
        assertEquals(0, tasks.length);

        handlerHit.set(false);
        Thread.sleep(2000);
        assertFalse(handlerHit.get());
    }

}
