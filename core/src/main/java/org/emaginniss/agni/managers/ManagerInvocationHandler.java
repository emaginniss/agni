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

package org.emaginniss.agni.managers;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.emaginniss.agni.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ManagerInvocationHandler implements InvocationHandler {

    private Node node;

    public ManagerInvocationHandler(Node node) {
        this.node = node;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Annotation [][]paramAnnotationsList = method.getParameterAnnotations();
        ManagerMethod mm = method.getAnnotation(ManagerMethod.class);
        if (mm == null) {
            throw new UnsupportedOperationException("Method " + method.toString() + " is not a ManagerMethod");
        }
        Execute execute = mm.execute();
        String payloadType = mm.payloadType();
        if ("[BLANK]".equals(payloadType)) {
            payloadType = null;
        }
        Class payloadClass = mm.payloadClass();
        Object payload = null;
        if (payloadClass.equals(Object.class)) {
            for (int i = 0; i < paramAnnotationsList.length; i++) {
                for (Annotation paramAnnotation : paramAnnotationsList[i]) {
                    if (paramAnnotation instanceof Payload) {
                        payload = args[i];
                    }
                }
            }
            if (payload == null && payloadType == null) {
                throw new UnsupportedOperationException("Method " + method.toString() + " does not define a payload or payloadType");
            }
        } else {
            payload = payloadClass.newInstance();
        }

        if (payload != null) {
            for (int i = 0; i < paramAnnotationsList.length; i++) {
                for (Annotation paramAnnotation : paramAnnotationsList[i]) {
                    if (paramAnnotation instanceof Inject) {
                        Inject inject = (Inject) paramAnnotation;
                        BeanUtils.setProperty(payload, inject.value(), args[i]);
                    }
                }
            }
            for (Inject inject : mm.inject()) {
                BeanUtils.setProperty(payload, inject.key(), inject.value());
            }
        }

        AgniBuilder builder = Agni.build(payload).type(payloadType);

        for (int i = 0; i < paramAnnotationsList.length; i++) {
            for (Annotation paramAnnotation : paramAnnotationsList[i]) {
                if (paramAnnotation instanceof ManagerCriterion) {
                    ManagerCriterion criterion = (ManagerCriterion) paramAnnotation;
                    builder.criteria(criterion.value(), String.valueOf(args[i]));
                }
            }
        }
        for (ManagerCriterion mc : mm.criteria()) {
            builder.criteria(mc.key(), mc.value());
        }

        switch (execute) {
            case send:
                builder.send(node);
                return null;
            case broadcast:
                builder.broadcast(node);
                return null;
            case request:
                PayloadAndAttachments result = builder.request(node);
                if (method.getReturnType().equals(PayloadAndAttachments.class)) {
                    return result;
                }
                if (!"[BLANK]".equals(mm.returnObject())) {
                    return PropertyUtils.getProperty(result.getPayload(), mm.returnObject());
                }
                return result.getPayload();
            case requestAll:
                return builder.requestAll(node);
        }

        return null;
    }
}
