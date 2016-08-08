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
import org.emaginniss.agni.*;
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

public class DefaultEndpoint implements Endpoint{

    private Node node;
    private Execute execute = Execute.request;
    private String payload = "null";
    private String payloadType;
    private String payloadAttachment;
    private String response = "payload";
    private String responseAttachmentName;
    private String responseAttachmentFilename;
    private String responseAttachmentContentType;
    private Map<String, String> criteria = new HashMap<>();
    private Map<String, String> inject = new HashMap<>();
    private Map<String, String> attachments = new HashMap<>();

    public DefaultEndpoint(Node node, Configuration config, String path) {
        this.node = node;
        payloadType = config.getString("payloadType", null);

        if (payloadType == null || payloadType.isEmpty()) {
            throw new RuntimeException("Payload Type for endpoint " + path + " is missing");
        }

        execute = Execute.valueOf(config.getString("execute", "request"));
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

    public DefaultEndpoint() {
    }

    public void handle(Map<String, String> pathVariables, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object payload = buildPayload(request);

        injectPayload(payload, pathVariables, request);

        AgniBuilder builder = Agni.build(payload).type(payloadType);

        setCriteria(builder, pathVariables, request);

        loadAttachments(builder, request);

        if (execute == Execute.send) {
            builder.send(node);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().flush();
        } else if (execute == Execute.broadcast) {
            builder.broadcast(node);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().flush();
        } else if (execute == Execute.request) {
            try {
                PayloadAndAttachments payloadAndAttachments = builder.request(node);
                if (payloadAndAttachments == null) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    response.getWriter().flush();
                } else {
                    if ("payload".equals(this.response)) {
                        sendPayload(response, payloadAndAttachments);
                    } else if ("attachment".equals(this.response)) {
                        sendAttachment(response, payloadAndAttachments, pathVariables, request);
                    }
                }
            } catch (Throwable e) {
                while (e.getCause() != null && !e.getCause().equals(e)) {
                    e = e.getCause();
                }
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                Writer writer = response.getWriter();
                writer.write(node.getSerializer().serialize(e));
                writer.flush();
            }
        }
    }

    private void sendAttachment(HttpServletResponse response, PayloadAndAttachments resp, Map<String, String> variables, HttpServletRequest request) throws IOException {
        Attachment att = resp.getAttachments().get(responseAttachmentName);
        if (att == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            String responseAttachmentContentType = (String) resolveVariable(this.responseAttachmentContentType, variables, request);
            if (responseAttachmentContentType != null) {
                response.setContentType(responseAttachmentContentType);
            }
            response.setContentLength(att.size());
            String responseAttachmentFilename = (String) resolveVariable(this.responseAttachmentFilename, variables, request);
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
            writer.write(node.getSerializer().serialize(resp.getPayload()));
            writer.flush();
        }
    }

    private void loadAttachments(AgniBuilder builder, HttpServletRequest request) throws IOException, ServletException {
        for (String attName : attachments.keySet()) {
            Part part = request.getPart(attachments.get(attName));
            builder.attachment(attName, new InputStreamAttachment((int) part.getSize(), part.getInputStream()));
        }
    }

    private void setCriteria(AgniBuilder builder, Map<String, String> variables, HttpServletRequest request) {
        for (String criteriaName : criteria.keySet()) {
            String value = (String) resolveVariable(criteria.get(criteriaName), variables, request);
            builder.criteria(criteriaName, value);
        }
    }

    private void injectPayload(Object p, Map<String, String> variables, HttpServletRequest request) throws IllegalAccessException, InvocationTargetException {
        for (String beanName : inject.keySet()) {
            String value = (String) resolveVariable(inject.get(beanName), variables, request);
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
            p = getRequestBody(request, payloadType);
        } else if ("attachment".equals(payload)) {
            Part part = request.getPart(payloadAttachment);
            StringBuilder body = new StringBuilder();
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(part.getInputStream()));
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            p = node.getSerializer().deserialize(body.toString(), payloadType);
        } else {
            p = node.getSerializer().deserialize(payload, payloadType);
        }
        return p;
    }

    public Object getRequestBody(HttpServletRequest request, String payloadType) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        BufferedReader reader = request.getReader();
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        return node.getSerializer().deserialize(body.toString(), payloadType);
    }

    public Execute getExecute() {
        return execute;
    }

    public void setExecute(Execute execute) {
        this.execute = execute;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    public String getPayloadAttachment() {
        return payloadAttachment;
    }

    public void setPayloadAttachment(String payloadAttachment) {
        this.payloadAttachment = payloadAttachment;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponseAttachmentName() {
        return responseAttachmentName;
    }

    public void setResponseAttachmentName(String responseAttachmentName) {
        this.responseAttachmentName = responseAttachmentName;
    }

    public String getResponseAttachmentFilename() {
        return responseAttachmentFilename;
    }

    public void setResponseAttachmentFilename(String responseAttachmentFilename) {
        this.responseAttachmentFilename = responseAttachmentFilename;
    }

    public String getResponseAttachmentContentType() {
        return responseAttachmentContentType;
    }

    public void setResponseAttachmentContentType(String responseAttachmentContentType) {
        this.responseAttachmentContentType = responseAttachmentContentType;
    }

    public Map<String, String> getCriteria() {
        return criteria;
    }

    public Map<String, String> getInject() {
        return inject;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public Object resolveVariable(String text, Map<String, String> variables, HttpServletRequest request) {
        if (text != null && text.startsWith("${") && text.endsWith("}")) {
            return variables.get(text.substring(2, text.length() - 1));
        }
        return text;
    }
}
