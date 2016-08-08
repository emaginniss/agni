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

package org.emaginniss.agni.pathfinders;

import org.emaginniss.agni.Destination;
import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.annotations.Component;
import org.emaginniss.agni.messages.RemoveLink;
import org.emaginniss.agni.messages.StopRouting;
import org.emaginniss.agni.messages.SubscriptionInfo;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component(value = "distanceBased", isDefault = true)
public class DistanceBasedPathFinder implements PathFinder {

    private Map<String, Set<String[]>> pathLookup = new HashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();
    private Map<String, Integer> cache = new HashMap<>();

    @Override
    public Set<Destination> sortByShortestPath(Set<Destination> in) {
        if (in.size() < 2) {
            return in;
        }
        try {
            readLock.lock();

            Map<Integer, Set<Destination>> ranking = new TreeMap<>();
            for (Destination destination : in) {
                Integer length = cache.get(destination.getNodeUuid());
                if (length == null) {
                    length = Integer.MAX_VALUE;
                }
                if (!ranking.containsKey(length)) {
                    ranking.put(length, new HashSet<Destination>());
                }
                ranking.get(length).add(destination);
            }

            Set<Destination> out = new LinkedHashSet<>();
            for (Set<Destination> destinations : ranking.values()) {
                out.addAll(destinations);
            }
            return out;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String findNextNode(Envelope envelope) {
        try {
            readLock.lock();

            String targetNodeUuid = envelope.getNodeUuid();
            Set<String[]> paths = pathLookup.get(targetNodeUuid);
            if (paths == null || paths.size() == 0) {
                return null;
            }

            return paths.iterator().next()[0];
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void addNodePath(String[] path) {
        try {
            writeLock.lock();
            addPathInternal(path);
            rebuildCache();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeNodeConnection(String from, String to) {
        try {
            writeLock.lock();
            for (String targetNodeUuid : pathLookup.keySet()) {
                for (Iterator<String[]> iter = pathLookup.get(targetNodeUuid).iterator(); iter.hasNext(); ) {
                    String []path = iter.next();
                    boolean remove = false;
                    if (from == null) {
                        if (path[0].equals(to)) {
                            remove = true;
                        }
                    } else {
                        for (int i = 0; i < path.length - 1 && !remove; i++) {
                            if (path[i].equals(from) && path[i + 1].equals(to)) {
                                remove = true;
                            }
                        }
                    }
                    if (remove) {
                        iter.remove();
                    }
                }
            }

            for (Iterator<String> iter = pathLookup.keySet().iterator(); iter.hasNext(); ) {
                String targetNodeUuid = iter.next();
                if (pathLookup.get(targetNodeUuid).isEmpty()) {
                    iter.remove();
                }
            }
            rebuildCache();
        } finally {
            writeLock.unlock();
        }
    }

    private void rebuildCache() {
        cache.clear();
        for (String targetNodeUuid : pathLookup.keySet()) {
            Set<String[]> paths = pathLookup.get(targetNodeUuid);
            if (paths.size() > 0) {
                cache.put(targetNodeUuid, paths.iterator().next().length);
            }
        }
    }

    private static class LengthBasedComparator implements Comparator<String[]> {
        @Override
        public int compare(String[] o1, String[] o2) {
            if (Arrays.equals(o1, o2)) {
                return 0;
            } else if (o1.length == o2.length) {
                return new Integer(Arrays.hashCode(o1)).compareTo(Arrays.hashCode(o2));
            } else {
                return o1.length < o2.length ? -1 : 1;
            }
        }
    }

    @Override
    public Collection<String[]> getKnownPaths() {
        Set<String[]> out = new HashSet<>();
        try {
            readLock.lock();
            for (Set<String[]> paths : pathLookup.values()) {
                out.addAll(paths);
            }
        } finally {
            readLock.unlock();
        }
        return out;
    }

    @Override
    public void handle(SubscriptionInfo subscriptionInfo, String sourceUuid) {
        try {
            writeLock.lock();

            for (String []pathIn : subscriptionInfo.getPaths()) {
                String []path = new String[pathIn.length + 1];
                path[0] = sourceUuid;
                System.arraycopy(pathIn, 0, path, 1, pathIn.length);

                addPathInternal(path);
            }

            rebuildCache();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void handle(RemoveLink removeLink) {
        removeNodeConnection(removeLink.getFromNodeUuid(), removeLink.getToNodeUuid());
    }

    private void addPathInternal(String[] path) {
        String targetNodeUuid = path[path.length - 1];
        Set<String[]> paths = pathLookup.get(targetNodeUuid);
        if (paths == null) {
            paths = new TreeSet<>(new LengthBasedComparator());
            pathLookup.put(targetNodeUuid, paths);
        }
        paths.add(path);
    }

    @Override
    public Set<String> handle(StopRouting msg) {
        Set<String> lostNodeUuids = new HashSet<>();
        try {
            writeLock.lock();
            for (String targetNodeUuid : pathLookup.keySet()) {
                for (Iterator<String[]> iter = pathLookup.get(targetNodeUuid).iterator(); iter.hasNext(); ) {
                    String []path = iter.next();
                    boolean remove = false;
                    for (String node : path) {
                        if (node.equals(msg.getNodeUuid())) {
                            remove = true;
                        }
                    }
                    if (remove) {
                        iter.remove();
                    }
                }
            }

            for (String node : pathLookup.keySet()) {
                if (pathLookup.get(node).isEmpty()) {
                    lostNodeUuids.add(node);
                }
            }
            for (String node : lostNodeUuids) {
                pathLookup.remove(node);
            }
            rebuildCache();
        } finally {
            writeLock.unlock();
        }
        return lostNodeUuids;
    }
}
