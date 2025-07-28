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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import org.apache.felix.resolver.reason.ReasonException;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;

final class UseConstraintError extends ResolutionError {

    private final ResolveContext m_context;
    private final Candidates m_allCandidates;
    private final Resource m_resource;
    private final String m_pkgName;
    private final Blame m_blame1;
    private final Blame m_blame2;

    public UseConstraintError(ResolveContext context, Candidates allCandidates, Resource resource, String pkgName, Blame blame) {
        this(context, allCandidates, resource, pkgName, blame, null);
    }

    public UseConstraintError(ResolveContext context, Candidates allCandidates, Resource resource, String pkgName, Blame blame1, Blame blame2) {
        this.m_context = context;
        this.m_allCandidates = allCandidates;
        this.m_resource = resource;
        this.m_pkgName = pkgName;
        if (blame1 == null)
        {
            throw new NullPointerException("First blame cannot be null.");
        }
        this.m_blame1 = blame1;
        this.m_blame2 = blame2;
    }

    public String getMessage() {
        if (m_blame2 == null)
        {
            return "Uses constraint violation. Unable to resolve resource "
                    + Util.getSymbolicName(m_resource)
                    + " [" + m_resource
                    + "] because it exports package '"
                    + m_pkgName
                    + "' and is also exposed to it from resource "
                    + Util.getSymbolicName(m_blame1.m_cap.getResource())
                    + " [" + m_blame1.m_cap.getResource()
                    + "] via the following dependency chain:\n\n"
                    + toStringBlame(m_blame1);
        }
        else
        {
            return  "Uses constraint violation. Unable to resolve resource "
                    + Util.getSymbolicName(m_resource)
                    + " [" + m_resource
                    + "] because it is exposed to package '"
                    + m_pkgName
                    + "' from resources "
                    + Util.getSymbolicName(m_blame1.m_cap.getResource())
                    + " [" + m_blame1.m_cap.getResource()
                    + "] and "
                    + Util.getSymbolicName(m_blame2.m_cap.getResource())
                    + " [" + m_blame2.m_cap.getResource()
                    + "] via two dependency chains.\n\nChain 1:\n"
                    + toStringBlame(m_blame1)
                    + "\n\nChain 2:\n"
                    + toStringBlame(m_blame2);
        }
    }

    public Collection<Requirement> getUnresolvedRequirements() {
        if (m_blame2 == null)
        {
            // This is an export conflict so there is only the first blame;
            // use its requirement.
            return Collections.singleton(m_blame1.m_reqs.get(0));
        }
        else
        {
            return Collections.singleton(m_blame2.m_reqs.get(0));
        }
    }

    private String toStringBlame(Blame blame)
    {
        StringBuilder sb = new StringBuilder();
        if ((blame.m_reqs != null) && !blame.m_reqs.isEmpty())
        {
            for (int i = 0; i < blame.m_reqs.size(); i++)
            {
                Requirement req = blame.m_reqs.get(i);
                sb.append("  ");
                sb.append(Util.getSymbolicName(req.getResource()));
                sb.append(" [");
                sb.append(req.getResource().toString());
                sb.append("]\n");
                if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    sb.append("    import: ");
                }
                else
                {
                    sb.append("    require: ");
                }
                sb.append(req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
                sb.append("\n     |");
                if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    sb.append("\n    export: ");
                }
                else
                {
                    sb.append("\n    provide: ");
                }
                if ((i + 1) < blame.m_reqs.size())
                {
                    Capability cap = getSatisfyingCapability(blame.m_reqs.get(i));
                    if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                    {
                        sb.append(PackageNamespace.PACKAGE_NAMESPACE);
                        sb.append("=");
                        sb.append(cap.getAttributes()
                                .get(PackageNamespace.PACKAGE_NAMESPACE));
                        Capability usedCap =
                                getSatisfyingCapability(blame.m_reqs.get(i + 1));
                        sb.append("; uses:=");
                        sb.append(usedCap.getAttributes()
                                .get(PackageNamespace.PACKAGE_NAMESPACE));
                    }
                    else
                    {
                        sb.append(cap);
                    }
                    sb.append("\n");
                }
                else
                {
                    Capability export = getSatisfyingCapability(blame.m_reqs.get(i));
                    sb.append(export.getNamespace());
                    sb.append(": ");
                    Object namespaceVal = export.getAttributes().get(export.getNamespace());
                    if (namespaceVal != null)
                    {
                        sb.append(namespaceVal.toString());
                    }
                    else
                    {
                        for (Entry<String, Object> attrEntry : export.getAttributes().entrySet())
                        {
                            sb.append(attrEntry.getKey()).append('=')
                                    .append(attrEntry.getValue()).append(';');
                        }
                    }
                    if (export.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)
                            && !export.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)
                            .equals(blame.m_cap.getAttributes().get(
                                    PackageNamespace.PACKAGE_NAMESPACE)))
                    {
                        sb.append("; uses:=");
                        sb.append(blame.m_cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
                        sb.append("\n    export: ");
                        sb.append(PackageNamespace.PACKAGE_NAMESPACE);
                        sb.append("=");
                        sb.append(blame.m_cap.getAttributes()
                                .get(PackageNamespace.PACKAGE_NAMESPACE));
                    }
                    sb.append("\n  ");
                    sb.append(Util.getSymbolicName(blame.m_cap.getResource()));
                    sb.append(" [");
                    sb.append(blame.m_cap.getResource().toString());
                    sb.append("]");
                }
            }
        }
        else
        {
            sb.append(blame.m_cap.getResource().toString());
        }
        return sb.toString();
    }

    private Capability getSatisfyingCapability(Requirement req)
    {
        // If the requiring revision is not resolved, then check in the
        // candidate map for its matching candidate.
        Capability cap = m_allCandidates.getFirstCandidate(req);
        // Otherwise, if the requiring revision is resolved then check
        // in its wires for the capability satisfying the requirement.
        if (cap == null && m_context.getWirings().containsKey(req.getResource()))
        {
            List<Wire> wires =
                    m_context.getWirings().get(req.getResource()).getRequiredResourceWires(null);
            req = ResolverImpl.getDeclaredRequirement(req);
            for (Wire w : wires)
            {
                if (w.getRequirement().equals(req))
                {
                    // TODO: RESOLVER - This is not 100% correct, since requirements for
                    //       dynamic imports with wildcards will reside on many wires and
                    //       this code only finds the first one, not necessarily the correct
                    //       one. This is only used for the diagnostic message, but it still
                    //       could confuse the user.
                    cap = w.getCapability();
                    break;
                }
            }
        }

        return cap;
    }

    @Override
    public ResolutionException toException()
    {
        return new ReasonException(ReasonException.Reason.UseConstraint, getMessage(), null, getUnresolvedRequirements());
    }
}