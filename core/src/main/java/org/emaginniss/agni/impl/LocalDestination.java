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

import org.emaginniss.agni.Criteria;
import org.emaginniss.agni.Destination;
import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.PayloadAndAttachments;
import org.emaginniss.agni.attachments.Attachments;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class LocalDestination extends Destination {

    private static transient double currentEPS = 0;
    private static transient AtomicLong eventsSinceLastTimeCheck = new AtomicLong(0);
    private static transient long lastTimeCheck;

    static {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                currentEPS = 0;
                while (true) {
                    lastTimeCheck = System.currentTimeMillis();
                    eventsSinceLastTimeCheck.set(0);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    currentEPS = (eventsSinceLastTimeCheck.get() * 1000.0) / (double)(System.currentTimeMillis() - lastTimeCheck);
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private transient Object object;
    private transient Method method;
    private transient AtomicLong timesCalled = new AtomicLong(0);
    private transient AtomicLong timesFailed = new AtomicLong(0);
    private transient AtomicLong totalTimeSpent = new AtomicLong(0);
    private transient AtomicLong current = new AtomicLong(0);

    public LocalDestination(String uuid, String nodeUuid, String displayName, String type, Criteria criteria, Object object, Method method) {
        super(uuid == null ? UUID.randomUUID().toString() : uuid, nodeUuid, displayName, type, criteria);
        this.object = object;
        this.method = method;

        method.setAccessible(true);
    }

    public PayloadAndAttachments invoke(Envelope envelope, Object payload) {
        eventsSinceLastTimeCheck.incrementAndGet();
        timesCalled.incrementAndGet();
        current.incrementAndGet();
        long start = System.currentTimeMillis();
        List<Object> paramArray = new ArrayList<>();
        for (Class paramType : method.getParameterTypes()) {
            if (paramType == Attachments.class) {
                paramArray.add(envelope.getAttachments());
            } else if (paramType == Envelope.class) {
                paramArray.add(envelope);
            } else if (paramType == Criteria.class) {
                paramArray.add(envelope.getCriteria());
            } else {
                paramArray.add(payload);
            }
        }
        try {
            Object response = method.invoke(object, paramArray.toArray(new Object[paramArray.size()]));

            if (response == null) {
                return null;
            } else if (response instanceof PayloadAndAttachments) {
                return (PayloadAndAttachments) response;
            } else {
                return new PayloadAndAttachments(response);
            }
        } catch (InvocationTargetException e) {
            timesFailed.incrementAndGet();
            return new PayloadAndAttachments(e.getCause());
        } catch (Exception e) {
            timesFailed.incrementAndGet();
            return new PayloadAndAttachments(e);
        } finally {
            current.decrementAndGet();
            totalTimeSpent.addAndGet(System.currentTimeMillis() - start);
        }
    }

    public boolean is(Object object, Method method) {
        return this.object == object && (method == null || this.method.equals(method));
    }

    public long getTimesCalled() {
        return timesCalled.get();
    }

    public long getTimesFailed() {
        return timesFailed.get();
    }

    public long getTotalTimeSpent() {
        return totalTimeSpent.get();
    }

    public long getCurrent() {
        return current.get();
    }

    public static double getCurrentEPS() {
        return currentEPS;
    }
}
