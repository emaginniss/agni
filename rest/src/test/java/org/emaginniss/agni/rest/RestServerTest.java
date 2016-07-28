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

package org.emaginniss.agni.rest;

import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.emaginniss.agni.Agni;
import org.emaginniss.agni.Configuration;
import org.emaginniss.agni.PayloadAndAttachments;
import org.emaginniss.agni.annotations.Criterion;
import org.emaginniss.agni.annotations.Subscribe;
import org.emaginniss.agni.attachments.Attachments;
import org.junit.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class RestServerTest {

    private static AtomicLong successCount = new AtomicLong(0);
    private static RestServer restServer;

    @BeforeClass
    public static void init() throws Exception {
        Agni.initialize();
        Agni.register(new Object() {
            @Subscribe
            public String handle(Message1 in) {
                successCount.incrementAndGet();
                return "This is my design";
            }

            @Subscribe
            public void handle(Message2 in) {
                if ("Howdy".equals(in.getValue1())) {
                    successCount.incrementAndGet();
                }
            }

            @Subscribe(criteria = {
                    @Criterion(key = "value1", value = "Howdy")
            })
            public void handle(Message3 in) {
                successCount.incrementAndGet();
            }

            @Subscribe
            public String handle(Message4 in, Attachments atts) {
                successCount.incrementAndGet();
                return "This is my design";
            }

            @Subscribe
            public PayloadAndAttachments<String> handle(Message5 in) {
                successCount.incrementAndGet();
                return new PayloadAndAttachments<>("", new Attachments().addByteArrayAttachment("att1", "This is my design".getBytes()));
            }
        });

        restServer = new RestServer(new Configuration(
                new JsonParser().parse("" +
                        "{" +
                        "  'connectors': { 'http': { 'port': 8080 } }," +
                        "  'endpoints': [" +
                        "    { 'path': '/message1A', 'payload': 'new', 'payloadType': '" + Message1.class.getName() + "', 'execute': 'send' }," +
                        "    { 'path': '/message1B', 'payload': 'new', 'payloadType': '" + Message1.class.getName() + "', 'execute': 'request', 'response': 'payload' }," +
                        "    { 'path': '/message2A/${url_param}', 'payload': 'new', 'payloadType': '" + Message2.class.getName() + "', 'inject': { 'value1': '${url_param}' }, 'execute': 'send' }," +
                        "    { 'path': '/message3A/${url_param}', 'payload': 'new', 'payloadType': '" + Message3.class.getName() + "', 'criteria': { 'value1': '${url_param}' }, 'execute': 'send' }," +
                        "    { 'path': '/message4A', 'method': 'POST', 'payload': 'new', 'payloadType': '" + Message4.class.getName() + "', 'attachments': { 'att1': 'incoming' }, 'execute': 'request', 'response': 'payload' }," +
                        "    { 'path': '/message5A', 'payload': 'new', 'payloadType': '" + Message5.class.getName() + "', 'execute': 'request', 'response': 'attachment', 'responseAttachmentName': 'att1', 'responseAttachmentFilename': 'file1.txt', 'responseAttachmentContentType': 'text/plain' }" +
                        "  ]" +
                        "}").getAsJsonObject()));

        Thread.sleep(1000);
    }

    @Before
    public void reset() {
        successCount.set(0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        restServer.shutdown();
        Agni.shutdown();
    }

    @Test
    public void testSimpleSend() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/message1A").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        Thread.sleep(300);
        assertEquals(1, successCount.get());
    }

    @Test
    public void testSimpleRequest() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/message1B").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        assertEquals("\"This is my design\"", IOUtils.readLines(conn.getInputStream()).get(0));
        assertEquals(1, successCount.get());
    }

    @Test
    public void testPayloadInjection() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/message2A/Howdy").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        Thread.sleep(300);
        assertEquals(1, successCount.get());
    }

    @Test
    public void testCriteriaInjection() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/message3A/Howdy").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        Thread.sleep(300);
        assertEquals(1, successCount.get());
    }

    @Test
    public void testSendAttachment() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/message4A").openConnection();
        conn.setRequestMethod("POST");
        conn.setUseCaches(false);
        conn.setDoOutput(true);

        String boundary = "*****";

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        DataOutputStream request = new DataOutputStream(conn.getOutputStream());

        request.writeBytes("--" + boundary + "\r\n");
        request.writeBytes("Content-Disposition: form-data; name=\"incoming\"\r\n\r\n");
        request.writeBytes("This is my design\r\n");
        request.writeBytes("--" + boundary + "--\r\n");
        request.flush();
        request.close();

        assertEquals(200, conn.getResponseCode());
        assertEquals("\"This is my design\"", IOUtils.readLines(conn.getInputStream()).get(0));
        assertEquals(1, successCount.get());
    }

    @Test
    public void testReceiveAttachment() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/message5A").openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        assertEquals("This is my design", IOUtils.readLines(conn.getInputStream()).get(0).trim());
        assertEquals(1, successCount.get());
    }

    public static class Message1 {
    }

    public static class Message2 {
        private String value1;

        public String getValue1() {
            return value1;
        }

        public void setValue1(String value1) {
            this.value1 = value1;
        }
    }

    public static class Message3 {
    }

    public static class Message4 {
    }

    public static class Message5 {
    }

}
