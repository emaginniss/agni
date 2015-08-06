package org.emaginniss.agni.rest;

import java.util.regex.Pattern;

/**
 * Created by Eric on 8/5/2015.
 */
public class EndpointBuilder {

    private RestServer restServer;
    private String path;
    private String []pathParts;
    private String method = "GET";
    private DefaultEndpoint endpoint = new DefaultEndpoint();

    public EndpointBuilder(RestServer restServer, String path) {
        this.restServer = restServer;
        this.path = path;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        pathParts = path.split(Pattern.quote("/"));
    }

    public EndpointBuilder method(String method) {
        this.method = method;
        return this;
    }

    public EndpointBuilder execute(String execute) {
        endpoint.setExecute(execute);
        return this;
    }

    public EndpointBuilder payload(String payload) {
        endpoint.setPayload(payload);
        return this;
    }

    public EndpointBuilder payloadType(String payloadType) {
        endpoint.setPayloadType(payloadType);
        return this;
    }

    public EndpointBuilder payloadAttachment(String payloadAttachment) {
        endpoint.setPayloadAttachment(payloadAttachment);
        return this;
    }

    public EndpointBuilder response(String response) {
        endpoint.setResponse(response);
        return this;
    }

    public EndpointBuilder responseAttachmentName(String responseAttachmentName) {
        endpoint.setResponseAttachmentName(responseAttachmentName);
        return this;
    }

    public EndpointBuilder responseAttachmentFilename(String responseAttachmentFilename) {
        endpoint.setResponseAttachmentFilename(responseAttachmentFilename);
        return this;
    }

    public EndpointBuilder responseAttachmentContentType(String responseAttachmentContentType) {
        endpoint.setResponseAttachmentContentType(responseAttachmentContentType);
        return this;
    }

    public EndpointBuilder criteria(String key, String value) {
        endpoint.getCriteria().put(key, value);
        return this;
    }

    public EndpointBuilder inject(String key, String value) {
        endpoint.getInject().put(key, value);
        return this;
    }

    public EndpointBuilder attachments(String targetName, String inputName) {
        endpoint.getAttachments().put(targetName, inputName);
        return this;
    }

    public void create() {
        restServer.getNodeHandler().addEndpoint(path, pathParts, method, endpoint);
    }
}
