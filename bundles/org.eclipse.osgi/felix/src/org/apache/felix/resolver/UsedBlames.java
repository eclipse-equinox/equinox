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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/*
 * UsedBlames hold a list of Blame that have a common used capability.
 * The UsedBlames stores sets of capabilities (root causes) that match a
 * root requirement with multiple cardinality.  These causes are the
 * capabilities that pulled in the common used capability.
 * It is assumed that multiple cardinality requirements can only be
 * root requirements of a Blame.
 *
 * This is only true because capabilities can only use a package
 * capability.  They cannot use any other kind of capability so we
 * do not have to worry about transitivity of the uses directive
 * from other capability types.
 */
class UsedBlames
{
    public final Set<Capability> m_caps;
    public final List<Blame> m_blames = new ArrayList<Blame>();
    private Map<Requirement, Set<Capability>> m_rootCauses;

    public UsedBlames(Set<Capability> caps)
    {
        m_caps = caps;
    }

    public void addBlame(Blame blame, Capability matchingRootCause)
    {
        if (!m_caps.contains(blame.m_cap))
        {
            throw new IllegalArgumentException(
                "Attempt to add a blame with a different used capability: "
                + blame.m_cap);
        }
        m_blames.add(blame);
        if (matchingRootCause != null)
        {
            Requirement req = blame.m_reqs.get(0);
            // Assumption made that the root requirement of the chain is the only
            // possible multiple cardinality requirement and that the matching root cause
            // capability is passed down from the beginning of the chain creation.
            if (Util.isMultiple(req))
            {
                // The root requirement is multiple. Need to store the root cause
                // so that we can find it later in case the used capability which the cause
                // capability pulled in is a conflict.
                if (m_rootCauses == null)
                {
                    m_rootCauses = new HashMap<Requirement, Set<Capability>>();
                }
                Set<Capability> rootCauses = m_rootCauses.get(req);
                if (rootCauses == null)
                {
                    rootCauses = new HashSet<Capability>();
                    m_rootCauses.put(req, rootCauses);
                }
                rootCauses.add(matchingRootCause);
            }
        }
    }

    public Set<Capability> getRootCauses(Requirement req)
    {
        if (m_rootCauses == null)
        {
            return Collections.emptySet();
        }
        Set<Capability> result = m_rootCauses.get(req);
        return result == null ? Collections.<Capability>emptySet() : result;
    }

    @Override
    public String toString()
    {
        return m_blames.toString();
    }
}