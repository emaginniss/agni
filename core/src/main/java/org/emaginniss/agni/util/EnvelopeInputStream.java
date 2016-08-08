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

package org.emaginniss.agni.util;

import org.apache.commons.io.IOUtils;
import org.emaginniss.agni.Criteria;
import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.Priority;
import org.emaginniss.agni.attachments.Attachments;

import java.io.*;
import java.nio.file.Files;

public class EnvelopeInputStream implements Closeable {

    private InputStream in;

    public EnvelopeInputStream(InputStream in) {
        this.in = in;
    }

    public Envelope read() throws IOException {
        String uuid = readString();
        String nodeUuid = readString();
        String destinationUuid = readString();
        String responseToUuid = readString();
        String []path = readStringArray();
        String type = readString();

        int criteriaLength = readInt();
        Criteria criteria = new Criteria();
        for (int i = 0; i < criteriaLength; i++) {
            criteria.put(readString(), readString());
        }

        String className = readString();
        String payload = readString();
        Priority priority = Priority.valueOf(readString());
        boolean responseExpected = readBoolean();

        Attachments attachments = readAttachments();

        return new Envelope(uuid, nodeUuid, destinationUuid, responseToUuid, path, type, criteria, className, payload, priority, responseExpected, attachments);
    }

    private boolean readBoolean() throws IOException {
        return in.read() == 1;
    }

    private int readInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
    }

    private String[] readStringArray() throws IOException {
        int length = readInt();
        if (length == -1) {
            return null;
        }
        String []out = new String[length];
        for (int i = 0; i < out.length; i++) {
            out[i] = readString();
        }
        return out;
    }

    public String readString() throws IOException {
        int length = readInt();
        if (length == -1) {
            return null;
        }
        byte []out = new byte[length];
        IOUtils.readFully(in, out);
        return new String(out);
    }

    public Attachments readAttachments() throws IOException {
        Attachments out = new Attachments();

        int count = readInt();
        for (int i = 0; i < count; i++) {
            String name = readString();
            int length = readInt();
            File file = null;
            OutputStream os;
            if (length > 50 * 1024) {
                file = Files.createTempFile("att", ".dat").toFile();
                os = new FileOutputStream(file);
            } else {
                os = new ByteArrayOutputStream(length);
            }

            int total = 0;
            byte[] buffer = new byte[100 * 1024];
            while (total < length) {
                int len = in.read(buffer, 0, Math.min(buffer.length, length - total));
                os.write(buffer, 0, len);
                total += len;
            }
            os.close();
            if (file == null) {
                out.addByteArrayAttachment(name, ((ByteArrayOutputStream)os).toByteArray());
            } else {
                out.addFileAttachment(name, file);
            }
        }

        return out;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
