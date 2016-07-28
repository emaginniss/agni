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

import org.emaginniss.agni.annotations.Criterion;
import org.emaginniss.agni.annotations.Subscribe;
import org.emaginniss.agni.managers.ManagerCriterion;
import org.emaginniss.agni.managers.Inject;
import org.emaginniss.agni.managers.ManagerMethod;
import org.emaginniss.agni.managers.Payload;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ManagersTest {

    public static class Message1 {
        private String field1;
        private int field2;

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

    public static class Message2 {
    }

    public interface Manager1 {

        @ManagerMethod(payloadClass = Message1.class)
        String send(@Inject("field1") String field1, @Inject("field2") int field2, @ManagerCriterion("crit1") String target);

        @ManagerMethod(execute = Execute.broadcast)
        void send(@Payload Message2 message2);
    }

    @Test
    public void testManager() throws Exception {
        final AtomicInteger hitCountA = new AtomicInteger(0);
        final AtomicInteger hitCountB = new AtomicInteger(0);
        final AtomicInteger hitCountC = new AtomicInteger(0);

        Agni.initialize();
        Agni.register(new Object() {
            @Subscribe(criteria = {
                    @Criterion(key = "crit1", value = "A")
            })
            public String handle1(Message1 message1) {
                hitCountA.incrementAndGet();
                return "Howdy1";
            }

            @Subscribe(criteria = {
                    @Criterion(key = "crit1", value = "B")
            })
            public String handle2(Message1 message1) {
                hitCountB.incrementAndGet();
                return "Howdy2";
            }

            @Subscribe
            public void handle3(Message2 message2) {
                hitCountC.incrementAndGet();
            }

            @Subscribe
            public void handle4(Message2 message2) {
                hitCountC.incrementAndGet();
            }
        });

        Manager1 m1 = Agni.createManager(Manager1.class);
        assertEquals("Howdy1", m1.send("f1", 7, "A"));
        assertEquals(1, hitCountA.get());
        assertEquals(0, hitCountB.get());
        assertEquals(0, hitCountC.get());

        assertEquals("Howdy2", m1.send("f1", 7, "B"));
        assertEquals(1, hitCountA.get());
        assertEquals(1, hitCountB.get());
        assertEquals(0, hitCountC.get());

        m1.send(new Message2());
        Thread.sleep(1000);
        assertEquals(1, hitCountA.get());
        assertEquals(1, hitCountB.get());
        assertEquals(2, hitCountC.get());
    }

}
