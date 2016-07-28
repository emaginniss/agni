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

package org.emaginniss.agni.scheduler.messages;

import org.quartz.JobDataMap;

import java.util.HashMap;
import java.util.Map;

public class ScheduledTask {

    private String group;
    private String name;
    private String description;
    private String payloadClassName;
    private String payloadSerialized;
    private String payloadType;
    private String execute = "send";
    private long startInMs = 0;
    private long intervallMs = 60000;
    private String cronString;
    private Map<String, String> criteria = new HashMap<>();
    private long previousRunTime = 0;
    private long nextRunTime = 0;

    public ScheduledTask() {
    }

    public ScheduledTask(String group, String name, String description, String payloadClassName, String payloadSerialized, String payloadType, String execute, long startInMs, long intervallMs, String cronString) {
        this.group = group;
        this.name = name;
        this.description = description;
        this.payloadClassName = payloadClassName;
        this.payloadSerialized = payloadSerialized;
        this.payloadType = payloadType;
        this.execute = execute;
        this.startInMs = startInMs;
        this.intervallMs = intervallMs;
        this.cronString = cronString;
    }

    public ScheduledTask(JobDataMap map) {
        this(map.getString("group"), map.getString("name"), map.getString("description"), map.getString("payloadClassName"), map.getString("payloadSerialized"), map.getString("payloadType"), map.getString("execute"), map.getLong("startInMs"), map.getLong("intervalInMs"), map.getString("cronString"));
        map.entrySet().stream().filter(e -> e.getKey().startsWith("criteria_")).forEach(e -> criteria.put(e.getKey().substring(9), String.valueOf(e.getValue())));
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPayloadClassName() {
        return payloadClassName;
    }

    public void setPayloadClassName(String payloadClassName) {
        this.payloadClassName = payloadClassName;
    }

    public String getPayloadSerialized() {
        return payloadSerialized;
    }

    public void setPayloadSerialized(String payloadSerialized) {
        this.payloadSerialized = payloadSerialized;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    public String getExecute() {
        return execute;
    }

    public void setExecute(String execute) {
        this.execute = execute;
    }

    public long getStartInMs() {
        return startInMs;
    }

    public void setStartInMs(long startInMs) {
        this.startInMs = startInMs;
    }

    public long getIntervallMs() {
        return intervallMs;
    }

    public void setIntervallMs(long intervallMs) {
        this.intervallMs = intervallMs;
    }

    public String getCronString() {
        return cronString;
    }

    public void setCronString(String cronString) {
        this.cronString = cronString;
    }

    public Map<String, String> getCriteria() {
        return criteria;
    }

    public void setCriteria(Map<String, String> criteria) {
        this.criteria = criteria;
    }

    public long getPreviousRunTime() {
        return previousRunTime;
    }

    public void setPreviousRunTime(long previousRunTime) {
        this.previousRunTime = previousRunTime;
    }

    public long getNextRunTime() {
        return nextRunTime;
    }

    public void setNextRunTime(long nextRunTime) {
        this.nextRunTime = nextRunTime;
    }
}
