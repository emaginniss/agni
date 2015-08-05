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

import org.emaginniss.agni.*;
import org.emaginniss.agni.annotations.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component("priority")
public class PriorityMessageBox implements MessageBox {

    private Map<Priority, MessageBox> messageBoxes = new TreeMap<>();

    public PriorityMessageBox(Configuration configuration, Node node) {
        Map<String, Configuration> children = configuration.getMap("children");
        for (Priority priority : Priority.values()) {
            messageBoxes.put(priority, Factory.instantiate(MessageBox.class, children.get(priority.name()), node));
        }
    }

    @Override
    public void enqueue(@NotNull Envelope envelope) {
        synchronized (this) {
            messageBoxes.get(envelope.getPriority()).enqueue(envelope);
            this.notify();
        }
    }

    @Override
    public List<Envelope> dequeue(int max, boolean wait) throws InterruptedException {
        while (true) {
            synchronized (this) {
                List<Envelope> out = pull(max);
                if (out != null) {
                    return out;
                }
                if (wait) {
                    this.wait();
                } else {
                    return null;
                }
                out = pull(max);
                if (out != null) {
                    return out;
                }
            }
        }
    }

    private List<Envelope> pull(int max) throws InterruptedException {
        for (MessageBox mb : messageBoxes.values()) {
            List<Envelope> out = mb.dequeue(max, false);
            if (out != null) {
                return out;
            }
        }
        return null;
    }

    @Override
    public int getMaximumSize() {
        int out = 0;
        for (MessageBox mb : messageBoxes.values()) {
            out += mb.getMaximumSize();
        }
        return out;
    }

    @Override
    public int getCurrentSize() {
        int out = 0;
        for (MessageBox mb : messageBoxes.values()) {
            out += mb.getCurrentSize();
        }
        return out;
    }

    @Override
    public int getCurrentMemorySize() {
        int out = 0;
        for (MessageBox mb : messageBoxes.values()) {
            out += mb.getCurrentMemorySize();
        }
        return out;
    }

    @Override
    public void shutdown() {
        for (MessageBox mb : messageBoxes.values()) {
            mb.shutdown();
        }
    }
}