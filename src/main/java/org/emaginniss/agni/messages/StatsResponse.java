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

/**
 * Created by Eric on 7/24/2015.
 */
public class StatsResponse {

    private String uuid;
    private String displayName;
    private long runningFor;
    private int messageBoxMaximumSize;
    private int messageBoxCurrentSize;
    private int messageBoxCurrentMemorySize;
    private Set<DestinationInfo> destinationInfos = new HashSet<>();
    private Map<String, String> connections = new HashMap<>();
    private Set<String[]> knownPaths = new HashSet<>();
    private Set<ProcessorThreadInfo> processorThreadInfos = new HashSet<>();

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

    public String getDisplayName() {
        return displayName;
    }

    public long getRunningFor() {
        return runningFor;
    }

    public int getMessageBoxMaximumSize() {
        return messageBoxMaximumSize;
    }

    public int getMessageBoxCurrentSize() {
        return messageBoxCurrentSize;
    }

    public int getMessageBoxCurrentMemorySize() {
        return messageBoxCurrentMemorySize;
    }

    public Set<DestinationInfo> getDestinationInfos() {
        return destinationInfos;
    }

    public Map<String, String> getConnections() {
        return connections;
    }

    public Set<String[]> getKnownPaths() {
        return knownPaths;
    }

    public Set<ProcessorThreadInfo> getProcessorThreadInfos() {
        return processorThreadInfos;
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
        private List<String> envelopeTypes = new ArrayList<>();
        private List<StackTraceElement> stackTrace = new ArrayList<>();

        public ProcessorThreadInfo() {
        }

        public ProcessorThreadInfo(String name, boolean waiting) {
            this.name = name;
            this.waiting = waiting;
        }

        public String getName() {
            return name;
        }

        public boolean isWaiting() {
            return waiting;
        }

        public List<String> getEnvelopeTypes() {
            return envelopeTypes;
        }

        public List<StackTraceElement> getStackTrace() {
            return stackTrace;
        }
    }


}
