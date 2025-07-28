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

import java.util.List;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

class Blame
{
    public final Capability m_cap;
    public final List<Requirement> m_reqs;

    public Blame(Capability cap, List<Requirement> reqs)
    {
        m_cap = cap;
        m_reqs = reqs;
    }

    @Override
    public String toString()
    {
        return m_cap.getResource()
            + "." + m_cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)
            + (((m_reqs == null) || m_reqs.isEmpty())
            ? " NO BLAME"
            : " BLAMED ON " + m_reqs);
    }

    @Override
    public boolean equals(Object o)
    {
        return (o instanceof Blame) && m_reqs.equals(((Blame) o).m_reqs)
            && m_cap.equals(((Blame) o).m_cap);
    }
}