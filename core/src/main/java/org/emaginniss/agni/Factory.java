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

import eu.infomas.annotation.AnnotationDetector;
import org.apache.log4j.Logger;
import org.emaginniss.agni.annotations.Component;
import org.emaginniss.agni.annotations.ComponentType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.*;

public abstract class Factory {

    private static final Logger log = Logger.getLogger(Factory.class);

    private static Map<Class, Map<String, Class>> componentLookup = new HashMap<>();
    private static Map<Class, String> defaultComponents = new HashMap<>();

    static {
        log.info("Starting component search");

        try {
            new AnnotationDetector(new AnnotationDetector.TypeReporter() {
                @Override
                public void reportTypeAnnotation(Class<? extends Annotation> annotationClass, String className) {
                    Class clazz;
                    try {
                        clazz = Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    Component component = (Component) clazz.getAnnotation(Component.class);
                    for (Class i : clazz.getInterfaces()) {
                        if (i.getAnnotation(ComponentType.class) != null) {
                            log.info("Found component " + component.value() + " of type " + i.getSimpleName());
                            if (!componentLookup.containsKey(i)) {
                                componentLookup.put(i, new HashMap<String, Class>());
                            }
                            componentLookup.get(i).put(component.value(), clazz);
                            if (component.isDefault()) {
                                defaultComponents.put(i, component.value());
                            }
                        }
                    }
                }

                @Override
                public Class<? extends Annotation>[] annotations() {
                    return new Class[]{Component.class};
                }
            }).detect();
        } catch (Exception e) {
            log.error("Error during component search", e);
        }
        log.info("Component search complete");
    }

    public static <T> T instantiate(Class<T> compType, Configuration configuration, Object ...parametersPassed) {
        configuration = configuration == null ? new Configuration() : configuration;
        String type = configuration.getString("type", defaultComponents.get(compType));

        if (!componentLookup.containsKey(compType)) {
            throw new RuntimeException(compType.getSimpleName() + " is not a valid component type");
        }

        if (type == null) {
            throw new RuntimeException("Unable to find default type for component " + compType.getSimpleName());
        }

        Class<T> clazz = componentLookup.get(compType).get(type);

        if (clazz == null) {
            throw new RuntimeException("Unable to find " + compType.getSimpleName() + " of type " + type);
        }

        if (clazz.getConstructors().length > 1) {
            throw new RuntimeException("Found multiple constructors on " + clazz.getName());
        }
        log.debug("Creating instance of component type " + clazz.getSimpleName());
        if (clazz.getConstructors().length == 0) {
            try {
                return (T)clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            List<Object> parametersIn = new ArrayList(Arrays.asList(parametersPassed));
            parametersIn.add(configuration);

            Constructor ctor = clazz.getConstructors()[0];
            Object []parameters = new Object[ctor.getParameterTypes().length];
            for (int i = 0; i < parameters.length; i++) {
                for (Object obj : parametersIn) {
                    if (ctor.getParameterTypes()[i].isAssignableFrom(obj.getClass())) {
                        parameters[i] = obj;
                    }
                }
            }
            try {
                return (T)ctor.newInstance(parameters);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
