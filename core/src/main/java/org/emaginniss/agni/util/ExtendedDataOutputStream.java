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
import org.emaginniss.agni.attachments.Attachment;
import org.emaginniss.agni.attachments.Attachments;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ExtendedDataOutputStream extends DataOutputStream {

    public ExtendedDataOutputStream(OutputStream out) {
        super(out);
    }

    public void write(Priority priority) throws IOException {
        write(priority.name());
    }

    public void write(String in) throws IOException {
        if (in == null) {
            writeInt(0);
        } else {
            writeInt(in.length());
            writeChars(in);
        }
    }

    public void write(String []in) throws IOException {
        if (in == null) {
            writeInt(0);
        } else {
            writeInt(in.length);
            for (String s : in) {
                write(s);
            }
        }
    }

    public void write(String key, Attachment attachment) throws IOException {
        write(key);
        writeInt(attachment.size());
        InputStream inputStream = attachment.open();
        byte[] buffer = new byte[100 * 1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            write(buffer, 0, len);
        }
        inputStream.close();
    }

    public void write(Criteria criteria) throws IOException {
        writeInt(criteria.size());
        for (String key : criteria.keySet()) {
            write(key);
            write(criteria.get(key));
        }
    }
}
