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

package org.emaginniss.agni;

import org.emaginniss.agni.attachments.Attachments;
import org.emaginniss.agni.impl.DestinationRegistration;
import org.emaginniss.agni.impl.WhisperHandler;
import org.emaginniss.agni.messageboxes.MessageBox;
import org.emaginniss.agni.messages.StatsResponse;
import org.emaginniss.agni.pathfinders.PathFinder;
import org.emaginniss.agni.serializers.Serializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public interface Node extends Closeable {

    void register(@NotNull Object object);

    @NotNull
    String subscribe(@NotNull Object object, Method method, String type, Criteria criteria, String displayName, String uuid);

    void unsubscribe(@NotNull Object object, Method method);

    @NotNull
    Set<Destination> getDestinations(@NotNull String[] types, Criteria criteria, boolean findAll);

    @NotNull
    Set<Destination> getDestinationPaths(@NotNull String[] types, Criteria criteria, boolean findAll);

    void send(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority);

    void broadcast(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority);

    @Nullable
    PayloadAndAttachments request(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority, Long timeout);

    @NotNull
    Map<Destination, PayloadAndAttachments> requestAll(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority, Long timeout);

    @NotNull
    MessageBox getInbox();

    void process(@NotNull Envelope envelope);

    @NotNull
    String getUuid();

    String getDisplayName();

    @NotNull
    DestinationRegistration getDestinationRegistration();

    @NotNull
    Set<String> getConnectedNodeUuids();

    ThreadGroup getThreadGroup();

    void shutdown();

    @Override
    void close() throws IOException;

    void handleIncomingEnvelope(Envelope e);

    PathFinder getPathFinder();

    Serializer getSerializer();

    WhisperHandler getWhisperHandler();

    StatsResponse buildStatsResponse();

    boolean isShuttingDown();

    <T> T createManager(Class T);
}
