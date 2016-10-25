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

package org.emaginniss.agni.scheduler.util;

import org.emaginniss.agni.Node;
import org.emaginniss.agni.scheduler.messages.ScheduledTask;

public class ScheduledTaskBuilder {

    private ScheduledTask scheduledTask = new ScheduledTask();

    public ScheduledTaskBuilder() {
    }

    public ScheduledTaskBuilder group(String group) {
        scheduledTask.setGroup(group);
        return this;
    }

    public ScheduledTaskBuilder name(String name) {
        scheduledTask.setName(name);
        return this;
    }

    public ScheduledTaskBuilder description(String description) {
        scheduledTask.setDescription(description);
        return this;
    }

    public ScheduledTaskBuilder payload(Object payload, Node node) {
        scheduledTask.setPayloadSerialized(node.getSerializer().serialize(payload));
        scheduledTask.setPayloadClassName(payload.getClass().getName());
        return this;
    }

    public ScheduledTaskBuilder payloadType(String payloadType) {
        scheduledTask.setPayloadType(payloadType);
        return this;
    }

    public ScheduledTaskBuilder startInMs(long startInMs) {
        scheduledTask.setStartInMs(startInMs);
        return this;
    }

    public ScheduledTaskBuilder startInS(long startInS) {
        scheduledTask.setStartInMs(startInS * 1000);
        return this;
    }

    public ScheduledTaskBuilder startInM(long startInM) {
        scheduledTask.setStartInMs(startInM * 60 * 1000);
        return this;
    }

    public ScheduledTaskBuilder startInH(long startInH) {
        scheduledTask.setStartInMs(startInH * 60 * 60 * 1000);
        return this;
    }

    public ScheduledTaskBuilder startInD(long startInD) {
        scheduledTask.setStartInMs(startInD * 24 * 60 * 60 * 1000);
        return this;
    }

    public ScheduledTaskBuilder intervallMs(long intervallMs) {
        scheduledTask.setIntervallMs(intervallMs);
        return this;
    }

    public ScheduledTaskBuilder intervallS(long intervallS) {
        scheduledTask.setIntervallMs(intervallS * 1000);
        return this;
    }

    public ScheduledTaskBuilder intervallM(long intervallM) {
        scheduledTask.setIntervallMs(intervallM * 60 * 1000);
        return this;
    }

    public ScheduledTaskBuilder intervallH(long intervallH) {
        scheduledTask.setIntervallMs(intervallH * 60 * 60 * 1000);
        return this;
    }

    public ScheduledTaskBuilder intervallD(long intervallD) {
        scheduledTask.setIntervallMs(intervallD * 24 * 60 * 60 * 1000);
        return this;
    }

    public ScheduledTaskBuilder cronString(String cronString) {
        scheduledTask.setCronString(cronString);
        return this;
    }

    public ScheduledTaskBuilder execute(String execute) {
        scheduledTask.setExecute(execute);
        return this;
    }

    public ScheduledTaskBuilder criteria(String key, String value) {
        scheduledTask.getCriteria().put(key, value);
        return this;
    }

    public ScheduledTask build() {
        return scheduledTask;
    }
}
