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
import org.emaginniss.agni.Envelope;
import org.emaginniss.agni.attachments.Attachment;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class EnvelopeOutputStream implements Closeable {

    private OutputStream out;

    public EnvelopeOutputStream(OutputStream out) {
        this.out = out;
    }

    public synchronized void write(Envelope e) throws IOException {
        write(e.getUuid());
        write(e.getNodeUuid());
        write(e.getDestinationUuid());
        write(e.getResponseToUuid());
        write(e.getPath());
        write(e.getType());
        write(e.getCriteria().size());
        for (Map.Entry<String, String> entry : e.getCriteria().entrySet()) {
            write(entry.getKey());
            write(entry.getValue());
        }
        write(e.getClassName());
        write(e.getPayload());
        write(e.getPriority().name());
        write(e.isResponseExpected());
        write(e.getAttachments().size());

        for (Map.Entry<String, Attachment> entry : e.getAttachments().entrySet()) {
            write(entry.getKey());
            write(entry.getValue().size());
            InputStream inputStream = entry.getValue().open();
            IOUtils.copy(inputStream, out);
            inputStream.close();
        }
    }

    private void write(boolean s) throws IOException {
        out.write(s ? 1 : 0);
    }

    private void write(int s) throws IOException {
        out.write((s >> 24) & 0xFF);
        out.write((s >> 16) & 0xFF);
        out.write((s >>  8) & 0xFF);
        out.write(s & 0xFF);
    }

    private void write(String[] s) throws IOException {
        if (s == null) {
            write(-1);
        } else {
            write(s.length);
            for (String str : s) {
                write(str);
            }
        }
    }

    public void write(String s) throws IOException {
        if (s == null) {
            write(-1);
        } else {
            byte []bytes = s.getBytes();
            write(bytes.length);
            IOUtils.write(bytes, out);
        }
    }

    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
