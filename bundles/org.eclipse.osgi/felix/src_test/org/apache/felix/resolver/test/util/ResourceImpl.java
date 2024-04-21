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
package org.apache.felix.resolver.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.felix.resolver.test.util.IdentityCapability;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ResourceImpl implements Resource
{
    private final List<Capability> m_caps;
    private final List<Requirement> m_reqs;

    public ResourceImpl() {
        m_caps = new ArrayList<Capability>();
        m_reqs = new ArrayList<Requirement>();
    }
    public ResourceImpl(String name) {
        this(name, IdentityNamespace.TYPE_BUNDLE, Version.emptyVersion);
    }
    public ResourceImpl(String name, String type, Version v)
    {
        m_caps = new ArrayList<Capability>();
        m_caps.add(0, new IdentityCapability(this, name, type, v));
        m_reqs = new ArrayList<Requirement>();
    }

    public void addCapability(Capability cap)
    {
        m_caps.add(cap);
    }

    public List<Capability> getCapabilities(String namespace)
    {
        List<Capability> result = m_caps;
        if (namespace != null)
        {
            result = new ArrayList<Capability>();
            for (Capability cap : m_caps)
            {
                if (cap.getNamespace().equals(namespace))
                {
                    result.add(cap);
                }
            }
        }
        return result;
    }

    public void addRequirement(Requirement req)
    {
        m_reqs.add(req);
    }

    public List<Requirement> getRequirements(String namespace)
    {
        List<Requirement> result = m_reqs;
        if (namespace != null)
        {
            result = new ArrayList<Requirement>();
            for (Requirement req : m_reqs)
            {
                if (req.getNamespace().equals(namespace))
                {
                    result.add(req);
                }
            }
        }
        return result;
    }

    @Override
    public String toString()
    {
        return getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).toString();
    }

    public void addCapabilities(Collection<Capability> caps) {
        for (Capability cap : caps) {
            addCapability(cap);
        }
    }

    public void addRequirements(Collection<Requirement> reqs) {
        for (Requirement req : reqs) {
            addRequirement(req);
        }
    }
}