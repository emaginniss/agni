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

package org.emaginniss.agni.util;

import org.emaginniss.agni.Criteria;
import org.emaginniss.agni.Priority;
import org.emaginniss.agni.attachments.Attachments;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ExtendedDataInputStream extends DataInputStream {

    public ExtendedDataInputStream(InputStream in) {
        super(in);
    }

    public Priority readPriority() throws IOException {
        return Priority.valueOf(readString());
    }

    public String readString() throws IOException {
        int length = readInt();
        if (length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(readChar());
        }
        return sb.toString();
    }

    public String []readStringArray() throws IOException {
        int length = readInt();
        if (length == 0) {
            return new String[0];
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            out.add(readString());
        }
        return out.toArray(new String[length]);
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
                int len = read(buffer, 0, Math.min(buffer.length, length - total));
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

    public Criteria readCriteria() throws IOException {
        Criteria out = new Criteria();

        int count = readInt();
        for (int i = 0; i < count; i++) {
            out.put(readString(), readString());
        }

        return out;
    }
}
