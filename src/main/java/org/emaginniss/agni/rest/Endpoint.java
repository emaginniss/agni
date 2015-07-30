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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.emaginniss.agni.Agni;
import org.emaginniss.agni.AgniBuilder;
import org.emaginniss.agni.Configuration;
import org.emaginniss.agni.PayloadAndAttachments;
import org.emaginniss.agni.attachments.Attachment;
import org.emaginniss.agni.attachments.InputStreamAttachment;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by Eric on 7/25/2015.
 */
public class Endpoint {

    private String path;
    private String []pathParts;
    private String method;
    private String execute;
    private String payload;
    private String payloadType;
    private String payloadAttachment;
    private String response;
    private String responseAttachmentName;
    private String responseAttachmentFilename;
    private String responseAttachmentContentType;
    private Map<String, String> criteria = new HashMap<>();
    private Map<String, String> inject = new HashMap<>();
    private Map<String, String> attachments = new HashMap<>();

    public Endpoint(Configuration config) {
        path = config.getString("path", null);
        payloadType = config.getString("payloadType", null);

        if (path == null || path.isEmpty()) {
            throw new RuntimeException("Endpoint is missing path");
        }
        if (payloadType == null || payloadType.isEmpty()) {
            throw new RuntimeException("Payload Type for endpoint " + path + " is missing");
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        pathParts = path.split(Pattern.quote("/"));
        method = config.getString("method", "GET");
        execute = config.getString("execute", "request");
        payload = config.getString("payload", "null");
        payloadAttachment = config.getString("payloadAttachment", null);
        response = config.getString("response", "payload");
        responseAttachmentName = config.getString("responseAttachmentName", null);
        responseAttachmentFilename = config.getString("responseAttachmentFilename", null);
        responseAttachmentContentType = config.getString("responseAttachmentContentType", null);
        criteria.putAll(config.getStringMap("criteria"));
        inject.putAll(config.getStringMap("inject"));
        attachments.putAll(config.getStringMap("attachment"));
    }

    public String getPath() {
        return path;
    }

    public String[] getPathParts() {
        return pathParts;
    }

    public String getMethod() {
        return method;
    }

    public void handle(Map<String, String> variables, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object payload = buildPayload(request);

        injectPayload(payload, variables);

        AgniBuilder builder = Agni.build(payload).type(payloadType);

        setCriteria(builder, variables);

        loadAttachments(builder, request);

        if ("send".equals(execute)) {
            builder.send();
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().flush();
        } else if ("request".equals(execute)) {
            PayloadAndAttachments payloadAndAttachments = builder.request();
            if (payloadAndAttachments == null) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                response.getWriter().flush();
            } else {
                if ("payload".equals(this.response)) {
                    sendPayload(response, payloadAndAttachments);
                } else if ("attachment".equals(this.response)) {
                    sendAttachment(response, payloadAndAttachments, variables);
                }
            }
        }

    }

    private void sendAttachment(HttpServletResponse response, PayloadAndAttachments resp, Map<String, String> variables) throws IOException {
        Attachment att = resp.getAttachments().get(responseAttachmentName);
        if (att == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            if (responseAttachmentContentType != null && responseAttachmentContentType.startsWith("${")) {
                responseAttachmentContentType = variables.get(responseAttachmentContentType);
            }
            if (responseAttachmentContentType != null) {
                response.setContentType(responseAttachmentContentType);
            }
            response.setContentLength(att.size());
            if (responseAttachmentFilename != null && responseAttachmentFilename.startsWith("${")) {
                responseAttachmentFilename = variables.get(responseAttachmentFilename);
            }
            if (responseAttachmentFilename != null) {
                response.setHeader("Content-Disposition", "attachment; filename=\"" + responseAttachmentFilename + "\";");
            } else {
                response.setHeader("Content-Disposition", "attachment;");
            }
            InputStream in = att.open();
            IOUtils.copy(in, response.getOutputStream());
            att.release();
        }
    }

    private void sendPayload(HttpServletResponse response, PayloadAndAttachments resp) throws IOException {
        if (resp.getPayload() == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            Writer writer = response.getWriter();
            writer.write(Agni.getNode().getSerializer().serialize(resp.getPayload()));
            writer.flush();
        }
    }

    private void loadAttachments(AgniBuilder builder, HttpServletRequest request) throws IOException, ServletException {
        for (String attName : attachments.keySet()) {
            Part part = request.getPart(attachments.get(attName));
            builder.attachment(attName, new InputStreamAttachment((int) part.getSize(), part.getInputStream()));
        }
    }

    private void setCriteria(AgniBuilder builder, Map<String, String> variables) {
        for (String criteriaName : criteria.keySet()) {
            String value = criteria.get(criteriaName);
            if (value.startsWith("${")) {
                value = variables.get(value);
            }
            builder.criteria(criteriaName, value);
        }
    }

    private void injectPayload(Object p, Map<String, String> variables) throws IllegalAccessException, InvocationTargetException {
        for (String beanName : inject.keySet()) {
            String value = inject.get(beanName);
            if (value.startsWith("${")) {
                value = variables.get(value);
            }
            BeanUtils.setProperty(p, beanName, value);
        }
    }

    private Object buildPayload(HttpServletRequest request) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, ServletException {
        Object p;
        if ("null".equals(payload)) {
            p = "";
        } else if ("new".equals(payload)) {
            p = Class.forName(payloadType).newInstance();
        } else if ("body".equals(payload)) {
            StringBuilder body = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            p = Agni.getNode().getSerializer().deserialize(body.toString(), payloadType);
        } else if ("attachment".equals(payload)) {
            Part part = request.getPart(payloadAttachment);
            StringBuilder body = new StringBuilder();
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(part.getInputStream()));
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            p = Agni.getNode().getSerializer().deserialize(body.toString(), payloadType);
        } else {
            p = Agni.getNode().getSerializer().deserialize(payload, payloadType);
        }
        return p;
    }
}
