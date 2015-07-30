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

import org.apache.log4j.Logger;
import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.Node;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ProcessorThread extends Thread {

    private static final Logger log = Logger.getLogger(ProcessorThread.class);

    private Node node;
    private boolean shuttingDown = false;
    private boolean waiting = false;
    private final int max;
    private List<Envelope> envelopes = new ArrayList<>();

    public ProcessorThread(ThreadGroup group, String name, Node node, int max) {
        super(group, name);
        this.node = node;
        this.max = max;
    }

    @Override
    public void run() {
        while (true) {
            try {
                waiting = true;
                envelopes = node.getInbox().dequeue(max, true);
            } catch (Throwable t) {
                if (t instanceof InterruptedException && shuttingDown) {
                    return;
                }
                log.error("Error while retrieving envelopes", t);
            } finally {
                waiting = false;
            }

            if (envelopes != null && envelopes.size() > 0) {
                for (Envelope envelope : envelopes) {
                    try {
                        node.process(envelope);
                    } catch (Throwable t) {
                        log.error("Error while retrieving envelope (" + envelope + ")", t);
                    }
                }
            }
            envelopes = null;
        }
    }

    public void shutdown() {
        shuttingDown = true;
        this.interrupt();
    }

    public boolean isWaiting() {
        return waiting;
    }

    /**
     * Return a list of the envelopes that this processor thread is currently handling
     * This method is slightly complex because we want to ensure that we don't send back the list of envelopes and have
     * the receiver modify it while the processor thread is in flight.
     * @return A list of envelopes or null if there aren't any
     */
    @Nullable
    public List<Envelope> getEnvelopes() {
        List<Envelope> hold = envelopes;
        if (hold != null) {
            return new ArrayList<>(hold);
        }
        return null;
    }
}
