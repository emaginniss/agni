/*
 * Copyright (c) 2015-2016, Eric A Maginniss
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

import org.apache.log4j.Logger;
import org.emaginniss.agni.*;
import org.emaginniss.agni.annotations.Subscribe;
import org.emaginniss.agni.attachments.Attachments;
import org.emaginniss.agni.managers.ManagerFactory;
import org.emaginniss.agni.messageboxes.MessageBox;
import org.emaginniss.agni.messages.StatsResponse;
import org.emaginniss.agni.messages.StopRouting;
import org.emaginniss.agni.pathfinders.PathFinder;
import org.emaginniss.agni.serializers.Serializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NodeImpl implements Node {

    private static final Logger log = Logger.getLogger(Node.class);

    private final long startupTime = System.currentTimeMillis();
    private final Map<String, ResultContainer> waiting = new ConcurrentHashMap<>();
    private String uuid;
    private String displayName;
    private DestinationRegistration destinationRegistration = new DestinationRegistration(this);
    private ConnectionData connectionData;
    private WhisperHandler whisperHandler;
    private PathFinder pathFinder;
    private Serializer serializer;
    private MessageBox inbox;
    private ManagerFactory managerFactory;
    private ThreadGroup threadGroup;
    private Set<ProcessorThread> processorThreads = new HashSet<>();
    private boolean shuttingDown = false;

    public NodeImpl() {
        this(null);
    }

    public NodeImpl(Configuration configuration) {
        long start = new Date().getTime();

        String threadName = Thread.currentThread().getName();

        configuration = configuration == null ? new Configuration() : configuration;

        uuid = configuration.getString("uuid", UUID.randomUUID().toString());
        displayName = configuration.getString("displayName", uuid);

        Thread.currentThread().setName(displayName + " - Agni Init");

        log.info("Starting node");

        threadGroup = new ThreadGroup("Node[" + displayName + "]");

        log.debug("Creating inbox");
        inbox = Factory.instantiate(MessageBox.class, configuration.getChild("inbox"), this);

        log.debug("Creating serializer");
        serializer = Factory.instantiate(Serializer.class, configuration.getChild("serializer"), this);

        log.debug("Creating manager factory");
        managerFactory = Factory.instantiate(ManagerFactory.class, configuration.getChild("managerFactory"), this);

        int threadCount = configuration.getInt("threadCount", 10);
        log.debug("Starting " + threadCount + " processor threads");
        for (Priority p : Priority.values()) {
            for (int i = 0; i < threadCount; i++) {
                ProcessorThread pt = new ProcessorThread(threadGroup, displayName + " - ProcessorThread[" + i + "]", this, p);
                pt.start();
                processorThreads.add(pt);
            }
        }

        log.debug("Creating path finder");
        pathFinder = Factory.instantiate(PathFinder.class, configuration.getChild("pathFinder"), this);

        log.debug("Creating whisper handler");
        whisperHandler = new WhisperHandler(this);

        log.debug("Creating connection data");
        connectionData = new ConnectionData(configuration.getMap("connections"), this);

        log.debug("Agni initialization completed in " + (new Date().getTime() - start) + "ms");
        Thread.currentThread().setName(threadName);
    }

    public void register(@NotNull Object object) {
        for (Method method : object.getClass().getMethods()) {
            Subscribe subscribe = method.getAnnotation(Subscribe.class);
            if (subscribe != null) {
                String type = subscribe.typeName();
                if ("".equals(type)) {
                    type = null;
                }
                if (type == null && !subscribe.typeClass().equals(void.class)) {
                    type = subscribe.typeClass().getName();
                }
                Criteria criteria = new Criteria();
                Arrays.stream(subscribe.criteria()).forEach(c -> criteria.put(c.key(), c.value()));
                String displayName = subscribe.displayName();
                if ("".equals(displayName)) {
                    displayName = null;
                }

                subscribe(object, method, type, criteria, displayName, null);
            }
        }
    }

    @NotNull
    public String subscribe(@NotNull Object object, Method method, String type, Criteria criteria, String displayName, String uuid) {
        if (type == null) {
            for (Class paramType : method.getParameterTypes()) {
                if (paramType != Attachments.class && paramType != Envelope.class && paramType != Criteria.class) {
                    if (type != null) {
                        throw new RuntimeException("Unable to determine message type.  Multiple types found");
                    }
                    type = paramType.getName();
                }
            }
        }

        if (type == null) {
            throw new RuntimeException("Unable to determine message type.  No type found");
        }

        if (displayName == null) {
            displayName = object.getClass().getSimpleName() + "." + method.getName() + "(" + type + ")";
        }

        LocalDestination ld = new LocalDestination(uuid, this.uuid, displayName, type, criteria, object, method);

        destinationRegistration.register(ld);

        return ld.getUuid();
    }

    public void unsubscribe(@NotNull Object object, Method method) {
        destinationRegistration.unsubscribe(object, method);
    }

    @NotNull
    public Set<Destination> getDestinations(@NotNull String[] types, Criteria criteria, boolean findAll) {
        return destinationRegistration.getDestinations(types, criteria, findAll);
    }

    @NotNull
    public Set<Destination> getDestinationPaths(@NotNull String[] types, Criteria criteria, boolean findAll) {
        criteria = criteria == null ? new Criteria() : criteria;

        Set<Destination> destinations = getDestinations(types, criteria, findAll);
        if (destinations.size() > 0) {
            destinations = pathFinder.sortByShortestPath(destinations);
        }

        return destinations;
    }

    public void send(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority) {
        if (shuttingDown) {
            throw new RuntimeException("Node is shutting down");
        }

        String[] types = type == null ? getClassTypes(payload) : new String[]{type};
        priority = priority == null ? Priority.MEDIUM : priority;
        criteria = criteria == null ? new Criteria() : criteria;
        attachments = attachments == null ? new Attachments() : attachments;

        Set<Destination> destinations = getDestinationPaths(types, criteria, false);
        if (destinations.size() == 0) {
            throw new NoRecipientException();
        }

        Destination destination = destinations.iterator().next();

        Envelope envelope = new Envelope(types[0], payload.getClass().getName(), serializer.serialize(payload), priority, attachments, criteria, false);
        envelope.setDestinationUuid(destination.getUuid());
        envelope.setNodeUuid(destination.getNodeUuid());

        enqueue(envelope);
    }

    private void enqueue(Envelope envelope) {
        log.trace("Enqueuing envelope " + envelope.getUuid());
        inbox.enqueue(envelope);
    }

    public void broadcast(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority) {
        if (shuttingDown) {
            throw new RuntimeException("Node is shutting down");
        }

        String[] types = type == null ? getClassTypes(payload) : new String[]{type};
        priority = priority == null ? Priority.MEDIUM : priority;
        criteria = criteria == null ? new Criteria() : criteria;
        attachments = attachments == null ? new Attachments() : attachments;

        Set<Destination> destinations = getDestinationPaths(types, criteria, false);

        for (Destination destination : destinations) {
            Envelope envelope = new Envelope(types[0], payload.getClass().getName(), serializer.serialize(payload), priority, attachments, criteria, false);
            envelope.setDestinationUuid(destination.getUuid());
            envelope.setNodeUuid(destination.getNodeUuid());

            enqueue(envelope);
        }
    }

    @Nullable
    public PayloadAndAttachments request(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority, Long timeout) {
        if (shuttingDown) {
            throw new RuntimeException("Node is shutting down");
        }

        String[] types = type == null ? getClassTypes(payload) : new String[]{type};
        priority = priority == null ? Priority.HIGH : priority;
        criteria = criteria == null ? new Criteria() : criteria;
        attachments = attachments == null ? new Attachments() : attachments;
        timeout = timeout == null ? 30000 : timeout;

        Set<Destination> destinations = getDestinationPaths(types, criteria, false);
        if (destinations.size() == 0) {
            throw new NoRecipientException();
        }

        Destination destination = destinations.iterator().next();

        Envelope envelope = new Envelope(types[0], payload.getClass().getName(), serializer.serialize(payload), priority, attachments, criteria, true);
        envelope.setDestinationUuid(destination.getUuid());
        envelope.setNodeUuid(destination.getNodeUuid());

        final ResultContainer result = new ResultContainer(1);

        synchronized (result) {
            waiting.put(envelope.getUuid(), result);

            enqueue(envelope);

            try {
                result.wait(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                waiting.remove(envelope.getUuid());
            }
        }

        if (result.getResult().size() > 0) {
            PayloadAndAttachments resp = result.getResult().values().iterator().next();
            if (resp.getPayload() instanceof Throwable) {
                if (resp.getPayload() instanceof RuntimeException) {
                    throw (RuntimeException) resp.getPayload();
                } else {
                    throw new RuntimeException((Throwable) resp.getPayload());
                }
            }
            return resp;
        }

        return null;
    }

    @NotNull
    public Map<Destination, PayloadAndAttachments> requestAll(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority, Long timeout) {
        if (shuttingDown) {
            throw new RuntimeException("Node is shutting down");
        }

        String[] types = type == null ? getClassTypes(payload) : new String[]{type};
        priority = priority == null ? Priority.HIGH : priority;
        criteria = criteria == null ? new Criteria() : criteria;
        attachments = attachments == null ? new Attachments() : attachments;
        timeout = timeout == null ? 30000 : timeout;

        Set<Destination> destinations = getDestinationPaths(types, criteria, false);
        if (destinations.size() == 0) {
            return new HashMap<>();
        }

        ResultContainer result = new ResultContainer(destinations.size());
        Set<Envelope> envelopes = new HashSet<>();
        for (Destination destination : destinations) {
            Envelope envelope = new Envelope(types[0], payload.getClass().getName(), serializer.serialize(payload), priority, attachments, criteria, true);
            envelope.setDestinationUuid(destination.getUuid());
            envelope.setNodeUuid(destination.getNodeUuid());

            waiting.put(envelope.getUuid(), result);

            envelopes.add(envelope);
        }

        synchronized (result) {
            for (Envelope e : envelopes) {
                enqueue(e);
            }

            if (result.getResult().size() < destinations.size()) {
                try {
                    result.wait(timeout);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    for (Envelope e : envelopes) {
                        waiting.remove(e.getUuid());
                    }
                }
            }
        }

        return result.getResult();
    }

    @NotNull
    private String[] getClassTypes(Object payload) {
        List<String> out = new ArrayList<>();
        Class clazz = payload.getClass();
        while (clazz != null) {
            out.add(clazz.getName());
            clazz = clazz.getSuperclass();
        }
        return out.toArray(new String[out.size()]);
    }

    @NotNull
    public MessageBox getInbox() {
        return inbox;
    }

    public void process(@NotNull Envelope envelope) {
        List<String> path = new ArrayList<>(Arrays.asList(envelope.getPath()));
        path.add(uuid);
        envelope.setPath(path.toArray(new String[path.size()]));

        if (envelope.getNodeUuid().equals(uuid)) {
            Object payload = envelope.getPayload() == null ? null : serializer.deserialize(envelope.getPayload(), envelope.getClassName());

            //This node is the final destination
            if (envelope.getResponseToUuid() == null) {
                LocalDestination ld = destinationRegistration.getLocalDestination(envelope.getDestinationUuid());
                if (ld == null) {
                    throw new RuntimeException("Unable to find destination for envelope");
                }
                PayloadAndAttachments response = ld.invoke(envelope, payload);

                if (envelope.isResponseExpected()) {
                    Envelope out = new Envelope(response == null ? null : response.getPayload().getClass().getName(), response == null ? null : response.getPayload().getClass().getName(), response == null ? null : serializer.serialize(response.getPayload()), envelope.getPriority(), response == null ? null : response.getAttachments(), new Criteria(), false);
                    out.setNodeUuid(envelope.getPath()[0]);
                    out.setResponseToUuid(envelope.getUuid());
                    out.setDestinationUuid(ld.getUuid());
                    enqueue(out);
                } else if (response != null && response.getPayload() instanceof Throwable) {
                    throw new RuntimeException((Throwable) response.getPayload());
                }
            } else {
                ResultContainer resultContainer = waiting.get(envelope.getResponseToUuid());
                if (resultContainer != null) {
                    resultContainer.registerResult(destinationRegistration.getDestination(envelope.getDestinationUuid()), new PayloadAndAttachments(payload, envelope.getAttachments()));
                }
            }
        } else {
            //Look for the next node to pass this on to
            String nextNode = pathFinder.findNextNode(envelope);
            if (!connectionData.forward(nextNode, envelope)) {
                log.error("Unable to forward envelope to " + nextNode);
            }
        }
    }

    @NotNull
    public String getUuid() {
        return uuid;
    }

    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public DestinationRegistration getDestinationRegistration() {
        return destinationRegistration;
    }

    @NotNull
    public Set<String> getConnectedNodeUuids() {
        return connectionData.getConnectedNodesUuids();
    }

    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    public void shutdown() {
        long start = new Date().getTime();

        String threadName = Thread.currentThread().getName();
        Thread.currentThread().setName(displayName + " - Agni Shutdown");

        log.info("Shutting down node");

        log.info("Broadcasting stop routing message");
        new AgniBuilder(new StopRouting(uuid)).priority(Priority.HIGHEST).broadcast(this);
        shuttingDown = true;

        log.info("Waiting on processor threads");
        for (ProcessorThread pt : processorThreads) {
            pt.shutdown();
        }
        for (ProcessorThread pt : processorThreads) {
            try {
                pt.join(10);
            } catch (InterruptedException e) {
                //do nothing
            }
        }

        log.info("Stopping connection data");
        connectionData.shutdown();

        log.debug("Agni shutdown completed in " + (new Date().getTime() - start) + "ms");
        Thread.currentThread().setName(threadName);
    }

    @Override
    public void close() throws IOException {
        shutdown();
    }

    public void handleIncomingEnvelope(Envelope e) {
        enqueue(e);
    }

    public PathFinder getPathFinder() {
        return pathFinder;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public WhisperHandler getWhisperHandler() {
        return whisperHandler;
    }

    public StatsResponse buildStatsResponse() {
        StatsResponse resp = new StatsResponse(uuid, displayName, System.currentTimeMillis() - startupTime, inbox.getMaximumSize(), inbox.getCurrentSize(), inbox.getCurrentMemorySize());
        connectionData.populate(resp);
        destinationRegistration.populate(resp);
        resp.getKnownPaths().addAll(pathFinder.getKnownPaths());
        for (ProcessorThread pt : processorThreads) {
            StatsResponse.ProcessorThreadInfo pti = new StatsResponse.ProcessorThreadInfo(pt.getName(), pt.isWaiting());
            if (!pt.isWaiting()) {
                Envelope envelope = pt.getEnvelope();
                if (envelope != null) {
                    pti.setEnvelopeType(envelope.getType());
                }
                pti.getStackTrace().addAll(Arrays.asList(pt.getStackTrace()));
            }
            resp.getProcessorThreadInfos().add(pti);
        }
        return resp;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public <T> T createManager(Class T) {
        return managerFactory.createManager(this, T);
    }
}
