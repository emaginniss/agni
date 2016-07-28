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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.emaginniss.agni.Configuration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by Eric on 7/25/2015.
 */
public class NodeHandler extends AbstractHandler {

    private Map<String, PathSegment> rootPathSegmentByMethod = new HashMap<>();

    public NodeHandler(Configuration[] endpointConfigs) {
        for (Configuration endpointConfig : endpointConfigs) {
            Endpoint endpoint;
            String path = endpointConfig.getString("path", null);
            if (path == null || path.isEmpty()) {
                throw new RuntimeException("Endpoint is missing path");
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            String []pathParts = path.split(Pattern.quote("/"));
            String method = endpointConfig.getString("method", "GET");
            String customClass = endpointConfig.getString("custom", null);
            if (customClass == null) {
                endpoint = new DefaultEndpoint(endpointConfig, path);
            } else {
                try {
                    endpoint = (Endpoint) Class.forName(customClass).getConstructor(Configuration.class).newInstance(endpointConfig);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to find or instantiate custom endpoint '" + customClass + "'", e);
                }
            }

            addEndpoint(path, pathParts, method, endpoint);
        }
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (target.startsWith("/")) {
            target = target.substring(1);
        }
        if (target.endsWith("/")) {
            target = target.substring(0, target.length() - 1);
        }

        if (!rootPathSegmentByMethod.containsKey(request.getMethod().toLowerCase())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String []pathParts = target.split(Pattern.quote("/"));
        for (int i = 0; i < pathParts.length; i++) {
            pathParts[i] = URLDecoder.decode(pathParts[i], "UTF-8");
        }
        List<String> variableValues = new ArrayList<>();
        PathSegment current = rootPathSegmentByMethod.get(request.getMethod().toLowerCase());
        for (String p : pathParts) {
            if (current.getChildren().containsKey(p)) {
                current = current.getChildren().get(p);
            } else if (current.getVariable() != null) {
                variableValues.add(p);
                current = current.getVariable();
            }
        }
        if (current.getTerminal() == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Endpoint ep = current.getTerminal();
        Map<String, String> variables = new HashMap<>();
        for (String p : current.getPathParts()) {
            if (p.startsWith("${")) {
                variables.put(p.substring(2, p.length() - 1), variableValues.remove(0));
            }
        }
        try {
            ep.handle(variables, request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addEndpoint(String path, String []pathParts, String method, Endpoint endpoint) {
        if (!rootPathSegmentByMethod.containsKey(method.toLowerCase())) {
            rootPathSegmentByMethod.put(method.toLowerCase(), new PathSegment());
        }
        PathSegment current = rootPathSegmentByMethod.get(method.toLowerCase());
        for (String p : pathParts) {
            if (p.startsWith("${")) {
                if (current.getVariable() == null) {
                    current.setVariable(new PathSegment());
                }
                current = current.getVariable();
            } else {
                if (!current.getChildren().containsKey(p)) {
                    current.getChildren().put(p, new PathSegment());
                }
                current = current.getChildren().get(p);
            }
        }
        if (current.getTerminal() != null) {
            throw new RuntimeException("Multiple endpoints registered for same path " + path);
        }
        current.setTerminal(endpoint);
        current.setPathParts(pathParts);
    }
}
