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

import java.lang.reflect.Method;

public class SubscriptionBuilder {

    private Object object;
    private Method method;
    private String type;
    private Criteria criteria = new Criteria();
    private String displayName;
    private String uuid;

    public SubscriptionBuilder(Object object) {
        this.object = object;
    }

    public SubscriptionBuilder method(Method method) {
        this.method = method;
        return this;
    }

    public SubscriptionBuilder method(String methodName) {
        Method method = null;
        for (Method test : object.getClass().getMethods()) {
            if (test.getName().equals(methodName)) {
                if (method != null) {
                    throw new RuntimeException("Too many methods with name '" + methodName + "'");
                }
                method = test;
            }
        }

        if (method == null) {
            throw new RuntimeException("Unable to find method '" + methodName + "'");
        }

        this.method = method;
        return this;
    }

    public SubscriptionBuilder type(String type) {
        this.type = type;
        return this;
    }

    public SubscriptionBuilder criteria(Criteria criteria) {
        this.criteria = criteria;
        return this;
    }

    public SubscriptionBuilder criteria(String key, String value) {
        criteria.add(key, value);
        return this;
    }

    public SubscriptionBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public SubscriptionBuilder uuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String subscribe(Node node) {
        return node.subscribe(object, method, type, criteria, displayName, uuid);
    }

    public String subscribe() {
        return Agni.subscribe(object, method, type, criteria, displayName, uuid);
    }

    public void unsubscribe(Node node) {
        node.unsubscribe(object, method);
    }

    public void unsubscribe() {
        Agni.unsubscribe(object, method);
    }
}
