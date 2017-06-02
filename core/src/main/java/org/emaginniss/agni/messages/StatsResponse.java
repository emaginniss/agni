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

package org.emaginniss.agni.messages;

import org.emaginniss.agni.Criteria;

import java.util.*;

public class StatsResponse {

    private String uuid;
    private String displayName;
    private long runningFor;
    private int messageBoxMaximumSize;
    private int messageBoxCurrentSize;
    private int messageBoxCurrentMemorySize;
    private DestinationInfo[] destinationInfos = new DestinationInfo[0];
    private Map<String, String> connections = new HashMap<>();
    private String[][] knownPaths = new String[0][];
    private ProcessorThreadInfo[] processorThreadInfos = new ProcessorThreadInfo[0];

    public StatsResponse() {
    }

    public StatsResponse(String uuid, String displayName, long runningFor, int messageBoxMaximumSize, int messageBoxCurrentSize, int messageBoxCurrentMemorySize) {
        this.uuid = uuid;
        this.displayName = displayName;
        this.runningFor = runningFor;
        this.messageBoxMaximumSize = messageBoxMaximumSize;
        this.messageBoxCurrentSize = messageBoxCurrentSize;
        this.messageBoxCurrentMemorySize = messageBoxCurrentMemorySize;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getRunningFor() {
        return runningFor;
    }

    public void setRunningFor(long runningFor) {
        this.runningFor = runningFor;
    }

    public int getMessageBoxMaximumSize() {
        return messageBoxMaximumSize;
    }

    public void setMessageBoxMaximumSize(int messageBoxMaximumSize) {
        this.messageBoxMaximumSize = messageBoxMaximumSize;
    }

    public int getMessageBoxCurrentSize() {
        return messageBoxCurrentSize;
    }

    public void setMessageBoxCurrentSize(int messageBoxCurrentSize) {
        this.messageBoxCurrentSize = messageBoxCurrentSize;
    }

    public int getMessageBoxCurrentMemorySize() {
        return messageBoxCurrentMemorySize;
    }

    public void setMessageBoxCurrentMemorySize(int messageBoxCurrentMemorySize) {
        this.messageBoxCurrentMemorySize = messageBoxCurrentMemorySize;
    }

    public DestinationInfo[] getDestinationInfos() {
        return destinationInfos;
    }

    public void setDestinationInfos(DestinationInfo[] destinationInfos) {
        this.destinationInfos = destinationInfos;
    }

    public Map<String, String> getConnections() {
        return connections;
    }

    public void setConnections(Map<String, String> connections) {
        this.connections = connections;
    }

    public String[][] getKnownPaths() {
        return knownPaths;
    }

    public void setKnownPaths(String[][] knownPaths) {
        this.knownPaths = knownPaths;
    }

    public ProcessorThreadInfo[] getProcessorThreadInfos() {
        return processorThreadInfos;
    }

    public void setProcessorThreadInfos(ProcessorThreadInfo[] processorThreadInfos) {
        this.processorThreadInfos = processorThreadInfos;
    }

    public static class DestinationInfo {
        private String uuid;
        private String displayName;
        private String nodeUuid;
        private String type;
        private Criteria criteria;
        private boolean local;
        private long timesCalled;
        private long timesFailed;
        private long totalTimeSpent;
        private long current;

        public DestinationInfo() {
        }

        public DestinationInfo(String uuid, String displayName, String nodeUuid, String type, Criteria criteria, boolean local, long timesCalled, long timesFailed, long totalTimeSpent, long current) {
            this.uuid = uuid;
            this.displayName = displayName;
            this.nodeUuid = nodeUuid;
            this.type = type;
            this.criteria = criteria;
            this.local = local;
            this.timesCalled = timesCalled;
            this.timesFailed = timesFailed;
            this.totalTimeSpent = totalTimeSpent;
            this.current = current;
        }

        public DestinationInfo(String uuid, String displayName, String nodeUuid, String type, Criteria criteria) {
            this(uuid, displayName, nodeUuid, type, criteria, false, 0, 0, 0, 0);
        }

        public String getUuid() {
            return uuid;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getNodeUuid() {
            return nodeUuid;
        }

        public String getType() {
            return type;
        }

        public Criteria getCriteria() {
            return criteria;
        }

        public boolean isLocal() {
            return local;
        }

        public long getTimesCalled() {
            return timesCalled;
        }

        public long getTimesFailed() {
            return timesFailed;
        }

        public long getTotalTimeSpent() {
            return totalTimeSpent;
        }

        public long getCurrent() {
            return current;
        }
    }

    public static class ProcessorThreadInfo {
        private String name;
        private boolean waiting;
        private String envelopeType;
        private StackTraceElement[] stackTrace = new StackTraceElement[0];

        public ProcessorThreadInfo() {
        }

        public ProcessorThreadInfo(String name, boolean waiting) {
            this.name = name;
            this.waiting = waiting;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isWaiting() {
            return waiting;
        }

        public void setWaiting(boolean waiting) {
            this.waiting = waiting;
        }

        public String getEnvelopeType() {
            return envelopeType;
        }

        public void setEnvelopeType(String envelopeType) {
            this.envelopeType = envelopeType;
        }

        public StackTraceElement[] getStackTrace() {
            return stackTrace;
        }

        public void setStackTrace(StackTraceElement[] stackTrace) {
            this.stackTrace = stackTrace;
        }
    }


}
