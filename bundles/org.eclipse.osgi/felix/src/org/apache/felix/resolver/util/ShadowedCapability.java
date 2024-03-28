/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.resolver.util;

import java.util.Map;
import java.util.Objects;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.HostedCapability;

/**
 * A {@link Capability} that is shadowed by a {@link HostedCapability}, this is
 * done when we merge the fragments capabilities with its host, but to insert
 * {@link HostedCapability} we need to make sure we always pass the original so
 * this class keeps track of the fact that we have replaced it with something
 * else.
 */
class ShadowedCapability implements HostedCapability {

    private HostedCapability hosted;
    private int hashCode;
    private Capability shadowed;

    public ShadowedCapability(HostedCapability hosted, Capability shadowed) {
        this.hosted = hosted;
        this.shadowed = shadowed;
    }

    @Override
    public String getNamespace() {
        return hosted.getNamespace();
    }

    @Override
    public Map<String, String> getDirectives() {
        return hosted.getDirectives();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return hosted.getAttributes();
    }

    @Override
    public Resource getResource() {
        return hosted.getResource();
    }

    @Override
    public Capability getDeclaredCapability() {
        return hosted.getDeclaredCapability();
    }

    public Capability getShadowed() {
        return shadowed;
    }

    @Override
    public int hashCode() {
        if (hashCode != 0) {
            return hashCode;
        }
        return hashCode = Objects.hash(getNamespace(), getDirectives(), getAttributes(), getResource());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Capability) {
            Capability other = (Capability) obj;
            return Objects.equals(getNamespace(), other.getNamespace())
                    && Objects.equals(getDirectives(), other.getDirectives())
                    && Objects.equals(getAttributes(), other.getAttributes())
                    && Objects.equals(getResource(), other.getResource());
        }
        return false;
    }

}
