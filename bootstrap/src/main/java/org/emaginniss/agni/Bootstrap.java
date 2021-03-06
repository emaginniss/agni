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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.emaginniss.agni.rest.RestServer;
import org.emaginniss.agni.scheduler.Scheduler;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Bootstrap {

    public static void main(String... args) throws Exception {
        if (!new Bootstrap().initialize(args)) {
            System.exit(-1);
        }
    }

    private RestServer restServer;

    public boolean initialize(String... args) throws Exception {
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

        Agni.initialize(config.getChild("node"));
        Thread.sleep(1000);
        addShutdownHandler(Agni.getNode());

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

    public void initializeLogging(Configuration config) throws IOException {
        final String logFile = config.getString("logFile", null);
        if (logFile != null) {
            loadConfig(new FileInputStream(logFile));
        }
        final String logResource = config.getString("logResource", null);
        if (logResource != null) {
            loadConfig(Bootstrap.class.getClassLoader().getResourceAsStream(logResource));
        }
    }

    private void loadConfig(final InputStream resourceAsStream) throws IOException {
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(resourceAsStream);
        } catch (JoranException e) {
            e.printStackTrace();
        } finally {
            resourceAsStream.close();
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
            InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(confFile);
            if (in != null) {
                log.debug("Loading config from resource " + confFile);
                return in;
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
