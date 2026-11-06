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
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/** A requirement that has other directives than the one it wraps. */
class RelaxedRequirement implements Requirement
{
    private final Requirement m_wrapped;
    private final Map<String, String> m_directives;

    RelaxedRequirement(Requirement wrapped, Map<String, String> directives)
    {
        m_wrapped = wrapped;
        m_directives = directives;
    }

    public String getNamespace()
    {
        return m_wrapped.getNamespace();
    }

    public Map<String, String> getDirectives()
    {
        return m_directives;
    }

    public Map<String, Object> getAttributes()
    {
        return m_wrapped.getAttributes();
    }

    public Resource getResource()
    {
        return m_wrapped.getResource();
    }

    @Override
    public String toString()
    {
        return m_wrapped.toString();
    }
}
