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

import org.emaginniss.agni.attachments.Attachment;
import org.emaginniss.agni.attachments.Attachments;
import org.emaginniss.agni.attachments.ByteArrayAttachment;
import org.emaginniss.agni.attachments.FileAttachment;

import java.io.File;
import java.util.Map;

public class AgniBuilder {

    private Object payload;
    private String type;
    private Criteria criteria = new Criteria();
    private Attachments attachments = new Attachments();
    private Priority priority;
    private Long timeout;

    public AgniBuilder(Object payload) {
        this.payload = payload;
    }

    public AgniBuilder type(String type) {
        this.type = type;
        return this;
    }

    public AgniBuilder criteria(Criteria criteria) {
        this.criteria = criteria;
        return this;
    }

    public AgniBuilder criteria(String key, String value) {
        criteria.add(key, value);
        return this;
    }

    public AgniBuilder attachments(Attachments attachments) {
        this.attachments = attachments;
        return this;
    }

    public AgniBuilder attachment(String name, File file) {
        attachments.put(name, new FileAttachment(file));
        return this;
    }

    public AgniBuilder attachment(String name, byte []data) {
        attachments.put(name, new ByteArrayAttachment(data));
        return this;
    }

    public AgniBuilder attachment(String name, Attachment att) {
        attachments.put(name, att);
        return this;
    }

    public AgniBuilder priority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public AgniBuilder timeout(Long timeout) {
        this.timeout = timeout;
        return this;
    }

    public void send(Node node) {
        node.send(payload, type, criteria, attachments, priority);
    }

    public void send() {
        Agni.send(payload, type, criteria, attachments, priority);
    }

    public void broadcast(Node node) {
        node.broadcast(payload, type, criteria, attachments, priority);
    }

    public void broadcast() {
        Agni.broadcast(payload, type, criteria, attachments, priority);
    }

    public PayloadAndAttachments request(Node node) {
        return node.request(payload, type, criteria, attachments, priority, timeout);
    }

    public PayloadAndAttachments request() {
        return Agni.request(payload, type, criteria, attachments, priority, timeout);
    }

    public Map<Destination, PayloadAndAttachments> requestAll(Node node) {
        return node.requestAll(payload, type, criteria, attachments, priority, timeout);
    }

    public Map<Destination, PayloadAndAttachments> requestAll() {
        return Agni.requestAll(payload, type, criteria, attachments, priority, timeout);
    }
}
