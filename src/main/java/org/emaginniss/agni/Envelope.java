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
import org.emaginniss.agni.util.ExtendedDataInputStream;
import org.emaginniss.agni.util.ExtendedDataOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class Envelope {

    private String uuid = UUID.randomUUID().toString();
    private String destinationUuid;
    private String nodeUuid;
    private String responseToUuid;
    private String []path = new String[0];
    private String type;
    private Criteria criteria = new Criteria();
    private String className;
    private String payload;
    private Priority priority;
    private boolean responseExpected;
    private Attachments attachments = new Attachments();

    public Envelope() {
    }

    public Envelope(String type, String className, String payload, Priority priority, Attachments attachments, Criteria criteria, boolean responseExpected) {
        this.type = type;
        this.className = className;
        this.payload = payload;
        this.priority = priority;
        this.attachments = attachments;
        this.criteria = criteria;
        this.responseExpected = responseExpected;
    }

    public String getUuid() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    public String getPayload() {
        return payload;
    }

    public Criteria getCriteria() {
        return criteria;
    }

    public Attachments getAttachments() {
        return attachments;
    }

    public String getDestinationUuid() {
        return destinationUuid;
    }

    public void setDestinationUuid(String destinationUuid) {
        this.destinationUuid = destinationUuid;
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }

    public String getResponseToUuid() {
        return responseToUuid;
    }

    public void setResponseToUuid(String responseToUuid) {
        this.responseToUuid = responseToUuid;
    }

    public String[] getPath() {
        return path;
    }

    public void setPath(String[] path) {
        this.path = path;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean isResponseExpected() {
        return responseExpected;
    }

    public void setResponseExpected(boolean responseExpected) {
        this.responseExpected = responseExpected;
    }

    public void write(ExtendedDataOutputStream dos) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ExtendedDataOutputStream local = new ExtendedDataOutputStream(baos);
        local.write(uuid);
        local.write(nodeUuid);
        local.write(destinationUuid);
        local.write(responseToUuid);
        local.write(path);
        local.write(type);
        local.write(criteria);
        local.write(className);
        local.write(payload);
        local.write(priority);
        local.writeBoolean(responseExpected);
        local.writeInt(attachments.size());
        local.flush();

        dos.write(baos.toByteArray());
        for (String key : attachments.keySet()) {
            dos.write(key, attachments.get(key));
        }
    }

    public static Envelope read(ExtendedDataInputStream dis) throws IOException {
        Envelope envelope = new Envelope();
        envelope.uuid = dis.readString();
        envelope.nodeUuid = dis.readString();
        envelope.destinationUuid = dis.readString();
        envelope.responseToUuid = dis.readString();
        envelope.path = dis.readStringArray();
        envelope.type = dis.readString();
        envelope.criteria = dis.readCriteria();
        envelope.className = dis.readString();
        envelope.payload = dis.readString();
        envelope.priority = dis.readPriority();
        envelope.responseExpected = dis.readBoolean();
        envelope.attachments = dis.readAttachments();

        return envelope;
    }

    public boolean hasVisited(String uuid) {
        for (String pathEl : path) {
            if (pathEl.equals(uuid)) {
                return true;
            }
        }
        return false;
    }
}
