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
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by Eric on 7/14/2015.
 */
public abstract class Agni {

    private static Node node;

    public static void initialize(Configuration configuration) {
        if (node != null) {
            node.shutdown();
            node = null;
        }
        node = new Node(configuration);
    }

    public static void initialize() {
        initialize(new Configuration());
    }

    public static void shutdown() {
        node.shutdown();
        node = null;
    }

    public static void send(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority) {
        node.send(payload, type, criteria, attachments, priority);
    }

    public static void broadcast(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority) {
        node.broadcast(payload, type, criteria, attachments, priority);
    }

    public static PayloadAndAttachments request(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority, Long timeout) {
        return node.request(payload, type, criteria, attachments, priority, timeout);
    }

    public static Map<Destination, PayloadAndAttachments> requestAll(@NotNull Object payload, String type, Criteria criteria, Attachments attachments, Priority priority, Long timeout) {
        return node.requestAll(payload, type, criteria, attachments, priority, timeout);
    }

    public static void register(@NotNull Object object) {
        node.register(object);
    }

    @NotNull
    public static String subscribe(@NotNull Object object, Method method, String type, Criteria criteria, String displayName, String uuid) {
        return node.subscribe(object, method, type, criteria, displayName, uuid);
    }

    public static void unsubscribe(@NotNull Object object, Method method) {
        node.unsubscribe(object, method);
    }

    public static AgniBuilder build(Object payload) {
        return new AgniBuilder(payload);
    }

    public static Node getNode() {
        return node;
    }
}
