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

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.emaginniss.agni.Configuration;

import java.util.Map;

/**
 * Created by Eric on 7/24/2015.
 */
public class RestServer {

    private Server server;

    public RestServer(Configuration config) throws Exception {
        server = new Server();

        Map<String, Configuration> connectorConfigs = config.getMap("connectors");
        for (String name : connectorConfigs.keySet()) {
            Configuration connConfig = connectorConfigs.get(name);
            ServerConnector connector;
            if (connConfig.getBoolean("ssl", false)) {
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStorePath(connConfig.getString("keyStorePath", null));
                sslContextFactory.setKeyStorePassword(connConfig.getString("keyStorePassword", null));
                sslContextFactory.setKeyManagerPassword(connConfig.getString("keyManagerPassword", null));
                sslContextFactory.setTrustStorePath(connConfig.getString("trustStorePath", null));
                sslContextFactory.setTrustStorePassword(connConfig.getString("trustStorePassword", null));
                sslContextFactory.setExcludeCipherSuites(connConfig.getStringArray("excludeCipherSuites"));

                // SSL HTTP Configuration
                HttpConfiguration https_config = new HttpConfiguration();
                https_config.addCustomizer(new SecureRequestCustomizer());

                // SSL Connector
                connector = new ServerConnector(server,
                        new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                        new HttpConnectionFactory(https_config));
                connector.setPort(connConfig.getInt("port", 8443));
            } else {
                connector = new ServerConnector(server);
                connector.setPort(connConfig.getInt("port", 8080));
            }
            connector.setName(name);
            if (connConfig.getString("host", null) != null) {
                connector.setHost(connConfig.getString("host", null));
            }

            server.addConnector(connector);
        }

        server.setHandler(new NodeHandler(config.getArray("endpoints")));

        server.start();
    }

    public void shutdown() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            server.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
