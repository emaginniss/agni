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

import org.apache.commons.beanutils.BeanComparator;
import org.emaginniss.agni.AgniBuilder;
import org.emaginniss.agni.Criteria;
import org.emaginniss.agni.Destination;
import org.emaginniss.agni.Node;
import org.emaginniss.agni.messages.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DestinationRegistration {

    private Node node;
    private Map<String, Destination> destinationLookupByUuid = new TreeMap<>();
    private Map<String, Set<String>> destinationLookupByType = new TreeMap<>();
    private Map<String, Set<String>> destinationLookupByNodeUuid = new TreeMap<>();
    private ReadWriteLock destinationPathLookupLock = new ReentrantReadWriteLock(true);
    private Lock destinationPathLookupReadLock = destinationPathLookupLock.readLock();
    private Lock destinationPathLookupWriteLock = destinationPathLookupLock.writeLock();

    public DestinationRegistration(Node node) {
        this.node = node;
    }

    public void register(Destination ld) {
        try {
            destinationPathLookupWriteLock.lock();
            addDestinationInternal(ld);
            rebuildCaches();
            new AgniBuilder(new AddDestination(ld)).broadcast(node);
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    private boolean addDestinationInternal(Destination destination) {
        if (!destinationLookupByUuid.containsKey(destination.getUuid())) {
            destinationLookupByUuid.put(destination.getUuid(), destination);
            return true;
        }
        return false;
    }

    private void rebuildCaches() {
        destinationLookupByType.clear();
        destinationLookupByNodeUuid.clear();
        for (Destination d : destinationLookupByUuid.values()) {
            if (!destinationLookupByType.containsKey(d.getType())) {
                destinationLookupByType.put(d.getType(), new HashSet<String>());
            }
            destinationLookupByType.get(d.getType()).add(d.getUuid());

            if (!destinationLookupByNodeUuid.containsKey(d.getNodeUuid())) {
                destinationLookupByNodeUuid.put(d.getNodeUuid(), new HashSet<String>());
            }
            destinationLookupByNodeUuid.get(d.getNodeUuid()).add(d.getUuid());
        }
    }

    public void unsubscribe(Object object, Method method) {
        try {
            destinationPathLookupWriteLock.lock();
            for (Iterator<String> destIter = destinationLookupByUuid.keySet().iterator(); destIter.hasNext(); ) {
                Destination destination = destinationLookupByUuid.get(destIter.next());
                if (destination instanceof LocalDestination) {
                    LocalDestination ld = (LocalDestination) destination;
                    if (ld.is(object, method)) {
                        destIter.remove();
                        new AgniBuilder(new RemoveDestination(ld.getUuid())).broadcast(node);
                    }
                }
            }
            rebuildCaches();
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    @NotNull
    public Set<Destination> getDestinations(String[] types, Criteria criteria, boolean findAll) {
        Set<Destination> out = new HashSet<>();

        for (String type : types) {
            try {
                destinationPathLookupReadLock.lock();
                if (destinationLookupByType.containsKey(type)) {
                    for (String destinationUuid : destinationLookupByType.get(type)) {
                        Destination destination = destinationLookupByUuid.get(destinationUuid);
                        if (destination.matches(criteria)) {
                            out.add(destination);
                        }
                    }
                }
            } finally {
                destinationPathLookupReadLock.unlock();
            }
            if (!findAll && out.size() > 0) {
                break;
            }
        }

        return out;
    }

    public LocalDestination getLocalDestination(String uuid) {
        return (LocalDestination) getDestination(uuid);
    }

    public Destination getDestination(String uuid) {
        try {
            destinationPathLookupReadLock.lock();
            return destinationLookupByUuid.get(uuid);
        } finally {
            destinationPathLookupReadLock.unlock();
        }
    }

    public void handle(SubscriptionInfo subscriptionInfo) {
        try {
            destinationPathLookupWriteLock.lock();
            boolean changed = false;
            for (Destination destination : subscriptionInfo.getDestinations()) {
                changed = addDestinationInternal(destination) || changed;
            }
            if (changed) {
                rebuildCaches();
            }
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    public Collection<Destination> getAll() {
        try {
            destinationPathLookupReadLock.lock();
            return destinationLookupByUuid.values();
        } finally {
            destinationPathLookupReadLock.unlock();
        }
    }

    public void handle(AddDestination addDestination) {
        try {
            Destination destination = addDestination.getDestination();
            destinationPathLookupWriteLock.lock();
            if (addDestinationInternal(destination)) {
                rebuildCaches();
            }
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    public void handle(RemoveDestination removeDestination) {
        try {
            destinationPathLookupWriteLock.lock();
            destinationLookupByUuid.remove(removeDestination.getDestinationUuid());
            rebuildCaches();
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    public void handleLostNodes(Set<String> lostNodeUuids) {
        try {
            destinationPathLookupWriteLock.lock();
            Set<String> destinationUuids = new HashSet<>();
            for (String nodeUuid : lostNodeUuids) {
                if (!nodeUuid.equals(node.getUuid())) {
                    if (destinationLookupByNodeUuid.containsKey(nodeUuid)) {
                        destinationUuids.addAll(destinationLookupByNodeUuid.get(nodeUuid));
                    }
                }
            }
            for (String destinationUuid : destinationUuids) {
                destinationLookupByUuid.remove(destinationUuid);
            }
            rebuildCaches();
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    public StatsResponse.DestinationInfo[] getDestinationInfos() {
        try {
            destinationPathLookupReadLock.lock();
            Set<StatsResponse.DestinationInfo> out = new TreeSet<>(new BeanComparator<>("displayName"));
            for (Destination d : destinationLookupByUuid.values()) {
                if (d instanceof LocalDestination) {
                    LocalDestination ld = (LocalDestination) d;
                    out.add(new StatsResponse.DestinationInfo(ld.getUuid(), ld.getDisplayName(), ld.getNodeUuid(), ld.getType(), ld.getCriteria(), true, ld.getTimesCalled(), ld.getTimesFailed(), ld.getTotalTimeSpent(), ld.getCurrent()));
                } else {
                    //out.add(new StatsResponse.DestinationInfo(d.getUuid(), d.getDisplayName(), d.getNodeUuid(), d.getType(), d.getCriteria()));
                }
            }
            return out.toArray(new StatsResponse.DestinationInfo[out.size()]);
        } finally {
            destinationPathLookupReadLock.unlock();
        }
    }
}
