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
import java.util.List;
import java.util.Set;
import org.apache.felix.resolver.util.ArrayMap;
import org.apache.felix.resolver.util.OpenHashMap;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class Packages
{
    public final OpenHashMap<String, Blame> m_exportedPkgs;
    public final OpenHashMap<String, Blame> m_substitePkgs;
    public final OpenHashMap<String, List<Blame>> m_importedPkgs;
    public final OpenHashMap<String, List<Blame>> m_requiredPkgs;
    public final OpenHashMap<String, ArrayMap<Set<Capability>, UsedBlames>> m_usedPkgs;
    public final OpenHashMap<Capability, Set<Capability>> m_sources;

    @SuppressWarnings("serial")
    public Packages(Resource resource)
    {
        int nbCaps = resource.getCapabilities(null).size();
        int nbReqs = resource.getRequirements(null).size();

        m_exportedPkgs = new OpenHashMap<String, Blame>(nbCaps);
        m_substitePkgs = new OpenHashMap<String, Blame>(nbCaps);
        m_importedPkgs = new OpenHashMap<String, List<Blame>>(nbReqs) {
            public List<Blame> compute(String s) {
                return new ArrayList<Blame>();
            }
        };
        m_requiredPkgs = new OpenHashMap<String, List<Blame>>(nbReqs) {
            public List<Blame> compute(String s) {
                return new ArrayList<Blame>();
            }
        };
        m_usedPkgs = new OpenHashMap<String, ArrayMap<Set<Capability>, UsedBlames>>(128) {
            @Override
            protected ArrayMap<Set<Capability>, UsedBlames> compute(String s) {
                return new ArrayMap<Set<Capability>, UsedBlames>() {
                    @Override
                    protected UsedBlames compute(Set<Capability> key) {
                        return new UsedBlames(key);
                    }
                };
            }
        };
        m_sources = new OpenHashMap<Capability, Set<Capability>>(nbCaps);
    }
}