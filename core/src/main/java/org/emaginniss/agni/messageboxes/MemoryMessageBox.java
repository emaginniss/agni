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

package org.emaginniss.agni.messageboxes;

import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.Priority;
import org.emaginniss.agni.annotations.Component;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

@Component(value = "memory", isDefault = true)
public class MemoryMessageBox implements MessageBox {

    private AtomicInteger currentSize = new AtomicInteger(0);

    private LinkedList<Envelope> queue = new LinkedList<>();

    @Override
    public void enqueue(@NotNull Envelope envelope) {
        synchronized (this) {
            queue.addLast(envelope);
            currentSize.incrementAndGet();

            this.notifyAll();
        }
    }

    @Override
    public Envelope dequeue(boolean wait, Priority priority) throws InterruptedException {
        synchronized (this) {
            while (true) {
                if (queue.size() == 0) {
                    if (wait) {
                        this.wait();
                    } else {
                        return null;
                    }
                }

                if (queue.size() > 0) {
                    currentSize.decrementAndGet();
                    return queue.removeFirst();
                }
            }
        }
    }

    public int getMaximumSize() {
        return -1;
    }

    public int getCurrentSize() {
        return currentSize.get();
    }

    public int getCurrentMemorySize() {
        return currentSize.get();
    }

    @Override
    public void shutdown() {
    }
}
