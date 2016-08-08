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

package org.emaginniss.agni;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.emaginniss.agni.rest.RestServer;
import org.emaginniss.agni.scheduler.Scheduler;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class Bootstrap {

    private static final Logger log = Logger.getLogger(Bootstrap.class);

    public static void main(String... args) throws Exception {
        if (!new Bootstrap().initialize(args)) {
            System.exit(-1);
        }
    }

    private RestServer restServer;

    public boolean initialize(String... args) throws Exception {
        BasicConfigurator.configure();
        Thread.currentThread().setName("Agni Bootstrap");

        InputStream configIn = findConfig(args);
        if (configIn == null) {
            log.error("Unable to find agni configuration.  You can set it in three ways:");
            log.error("  Pass it as the first parameter to the Bootstrap class");
            log.error("  Define it as -Dagni.conf=<XXX>");
            log.error("  Place a file called agni.json in the classpath");
            return false;
        }

        JsonElement configEl;
        try {
            configEl = new JsonParser().parse(new InputStreamReader(configIn));
        } catch (Exception e) {
            log.error("Config is not valid json", e);
            return false;
        }

        if (!configEl.isJsonObject()) {
            log.error("Config is not a json object");
            return false;
        }

        Configuration config = new Configuration(configEl.getAsJsonObject());
        initializeLogging(config);

        if (config.getBoolean("showLog", false)) {
            log.info("Log file:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(config));
        }

        addShutdownHandler(Agni.getNode());

        Agni.initialize(config.getChild("node"));
        Thread.sleep(1000);

        buildHandlers(Agni.getNode(), config);
        log.info("Bootstrap initialization complete");
        return true;
    }

    public void addShutdownHandler(final Node node) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown(node);
            }
        });
    }

    public void buildHandlers(Node node, Configuration config) throws Exception {
        if (config.has("scheduler")) {
            log.info("Creating Scheduler...");
            new Scheduler(node, config.getChild("scheduler"));
        }

        if (config.has("subscribers")) {
            config.getObject().getAsJsonArray("subscribers").forEach(s -> instantiateSubscriber(node, s));
        }

        if (config.has("rest")) {
            log.info("Creating REST system...");
            restServer = new RestServer(node, config.getChild("rest"));
        }
    }

    public void initializeLogging(Configuration config) {
        if (config.getString("logFile", null) != null) {
            BasicConfigurator.resetConfiguration();
            PropertyConfigurator.configure(config.getString("logFile", null));
        }
    }

    private void instantiateSubscriber(Node node, JsonElement subscriber) {
        String className = null;
        Configuration config = null;
        if (subscriber.isJsonPrimitive()) {
            className = subscriber.getAsString();
            config = new Configuration();
        } else if (subscriber.isJsonObject()) {
            config = new Configuration(subscriber.getAsJsonObject());
            className = config.getString("class", null);
        }

        log.info("Loading subscriber " + className);
        try {
            Class clazz = Class.forName(className);
            Constructor ctor = null;
            for (Constructor test : clazz.getConstructors()) {
                if (test.isAccessible()) {
                    boolean canHandle = true;
                    for (Class paramClass : test.getParameterTypes()) {
                        if (!paramClass.equals(Configuration.class) && !paramClass.equals(Node.class)) {
                            canHandle = false;
                        }
                    }
                    if (canHandle && (ctor == null || ctor.getParameterTypes().length < test.getParameterTypes().length)) {
                        ctor = test;
                    }
                }
            }
            if (ctor == null) {
                node.register(clazz.newInstance());
            } else {
                List<Object> params = new ArrayList<>();
                for (Class paramClass : ctor.getParameterTypes()) {
                    if (paramClass.equals(Configuration.class)) {
                        params.add(config);
                    } else if (paramClass.equals(Node.class)) {
                        params.add(node);
                    }
                }
                node.register(ctor.newInstance(params.toArray()));
            }
        } catch (Exception e) {
            log.error("Unable to instantiate subscriber " + className, e);
        }
    }

    public InputStream findConfig(String... args) throws IOException {
        if (args != null && args.length > 0) {
            File f = new File(args[0]);
            if (f.exists() && f.isFile() && f.canRead()) {
                log.debug("Loading config from file " + f.getAbsolutePath());
                return new FileInputStream(f);
            }
        }
        String confFile = System.getProperty("agni.conf");
        if (confFile != null) {
            File f = new File(confFile);
            if (f.exists() && f.isFile() && f.canRead()) {
                log.debug("Loading config from file " + f.getAbsolutePath());
                return new FileInputStream(f);
            }
        }
        log.debug("Attempting to load agni.json from classpath");
        return Bootstrap.class.getResourceAsStream("/agni.json");
    }

    public void shutdown(Node node) {
        if (restServer != null) {
            restServer.shutdown();
        }
        node.shutdown();
    }
}
