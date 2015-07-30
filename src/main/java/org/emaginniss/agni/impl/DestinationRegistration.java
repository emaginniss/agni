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

import org.emaginniss.agni.AgniBuilder;
import org.emaginniss.agni.Criteria;
import org.emaginniss.agni.Destination;
import org.emaginniss.agni.Node;
import org.emaginniss.agni.messages.AddDestination;
import org.emaginniss.agni.messages.RemoveDestination;
import org.emaginniss.agni.messages.StatsResponse;
import org.emaginniss.agni.messages.SubscriptionInfo;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DestinationRegistration {

    private Node node;
    private Map<String, Destination> destinationLookupByGuid = new TreeMap<>();
    private Map<String, Set<Destination>> destinationLookupByType = new TreeMap<>();
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
            new AgniBuilder(new AddDestination(ld)).broadcast(node);
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    private void addDestinationInternal(Destination destination) {
        if (!destinationLookupByGuid.containsKey(destination.getUuid())) {
            if (!destinationLookupByType.containsKey(destination.getType())) {
                destinationLookupByType.put(destination.getType(), new HashSet<Destination>());
            }
            destinationLookupByType.get(destination.getType()).add(destination);
            destinationLookupByGuid.put(destination.getUuid(), destination);
        }
    }

    public void unsubscribe(Object object, Method method) {
        try {
            destinationPathLookupWriteLock.lock();
            Set<String> affected = new HashSet<>();
            for (String type : destinationLookupByType.keySet()) {
                Set<Destination> remove = new HashSet<>();
                for (Destination destination : destinationLookupByType.get(type)) {
                    if (destination instanceof LocalDestination) {
                        LocalDestination ld = (LocalDestination) destination;
                        if (ld.is(object, method)) {
                            remove.add(destination);
                            destinationLookupByGuid.remove(ld.getUuid());
                            new AgniBuilder(new RemoveDestination(ld.getUuid())).broadcast(node);
                        }
                    }
                }
                if (remove.size() > 0) {
                    destinationLookupByType.get(type).removeAll(remove);
                    affected.add(type);
                }
            }

            for (String type : affected) {
                if (destinationLookupByType.get(type).size() == 0) {
                    destinationLookupByType.remove(type);
                }
            }
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
                    for (Destination destination : destinationLookupByType.get(type)) {
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
            return destinationLookupByGuid.get(uuid);
        } finally {
            destinationPathLookupReadLock.unlock();
        }
    }

    public void handle(SubscriptionInfo subscriptionInfo) {
        try {
            destinationPathLookupWriteLock.lock();
            for (Destination destination : subscriptionInfo.getDestinations()) {
                addDestinationInternal(destination);
            }
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    public Collection<Destination> getAll() {
        try {
            destinationPathLookupReadLock.lock();
            return destinationLookupByGuid.values();
        } finally {
            destinationPathLookupReadLock.unlock();
        }
    }

    public void handle(AddDestination addDestination) {
        try {
            Destination destination = addDestination.getDestination();
            destinationPathLookupWriteLock.lock();
            addDestinationInternal(destination);
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    public void handle(RemoveDestination removeDestination) {
        try {
            destinationPathLookupWriteLock.lock();
            destinationLookupByGuid.remove(removeDestination.getDestinationUuid());
            for (String type : destinationLookupByType.keySet()) {
                for (Iterator<Destination> iter = destinationLookupByType.get(type).iterator(); iter.hasNext(); ) {
                    if (iter.next().getUuid().equals(removeDestination.getDestinationUuid())) {
                        iter.remove();
                    }
                }
            }
        } finally {
            destinationPathLookupWriteLock.unlock();
        }
    }

    public void populate(StatsResponse resp) {
        try {
            destinationPathLookupReadLock.lock();
            for (Destination d : destinationLookupByGuid.values()) {
                if (d instanceof LocalDestination) {
                    LocalDestination ld = (LocalDestination) d;
                    resp.getDestinationInfos().add(new StatsResponse.DestinationInfo(ld.getUuid(), ld.getDisplayName(), ld.getNodeUuid(), ld.getType(), ld.getCriteria(), true, ld.getTimesCalled(), ld.getTimesFailed(), ld.getTotalTimeSpent(), ld.getCurrent()));
                } else {
                    resp.getDestinationInfos().add(new StatsResponse.DestinationInfo(d.getUuid(), d.getDisplayName(), d.getNodeUuid(), d.getType(), d.getCriteria()));
                }
            }
        } finally {
            destinationPathLookupReadLock.unlock();
        }
    }
}
