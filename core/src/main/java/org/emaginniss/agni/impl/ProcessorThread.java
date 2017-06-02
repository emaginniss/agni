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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.Node;
import org.emaginniss.agni.Priority;
import org.jetbrains.annotations.Nullable;

public class ProcessorThread extends Thread {

    private static final Logger log = LogManager.getLogger(ProcessorThread.class);

    private Node node;
    private Priority priority;
    private boolean shuttingDown = false;
    private boolean waiting = false;
    private Envelope envelope = null;

    public ProcessorThread(ThreadGroup group, String name, Node node, Priority priority) {
        super(group, name);
        this.node = node;
        this.priority = priority;
    }

    @Override
    public void run() {
        while (true) {
            try {
                waiting = true;
                envelope = node.getInbox().dequeue(!shuttingDown, priority);
            } catch (Throwable t) {
                log.error("Error while retrieving envelope", t);
            } finally {
                waiting = false;
            }

            if (envelope != null) {
                try {
                    node.process(envelope);
                } catch (Throwable t) {
                    log.error("Error while processing envelope (" + envelope.getPayload() + " - " + envelope.getDestinationUuid() + ")", t);
                }
            } else if (shuttingDown) {
                return;
            }
            envelope = null;
        }
    }

    public void shutdown() {
        shuttingDown = true;
    }

    public boolean isWaiting() {
        return waiting;
    }

    /**
     * Return the envelope that this processor thread is currently handling
     * @return An envelope or null
     */
    @Nullable
    public Envelope getEnvelope() {
        return envelope;
    }
}
