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
package org.apache.felix.resolver;

import java.util.Map;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

/** Keeps the type of a {@link BundleRequirement} as resolve contexts rely on it. */
class RelaxedBundleRequirement extends RelaxedRequirement implements BundleRequirement
{
    private final BundleRequirement m_wrapped;

    RelaxedBundleRequirement(BundleRequirement wrapped, Map<String, String> directives)
    {
        super(wrapped, directives);
        m_wrapped = wrapped;
    }

    public BundleRevision getRevision()
    {
        return m_wrapped.getRevision();
    }

    @Override
    public BundleRevision getResource()
    {
        return m_wrapped.getResource();
    }

    public boolean matches(BundleCapability capability)
    {
        return m_wrapped.matches(capability);
    }
}
