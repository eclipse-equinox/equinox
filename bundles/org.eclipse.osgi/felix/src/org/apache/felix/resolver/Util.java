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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.osgi.internal.container.Capabilities;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class Util
{
    public static String getSymbolicName(Resource resource)
    {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE))
            {
                return cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString();
            }
        }
        return null;
    }

    public static Version getVersion(Resource resource)
    {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE))
            {
                return (Version)
                    cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
            }
        }
        return null;
    }

	public static String getResourceName(Resource resource) {
		String symbolicName = getSymbolicName(resource);
		if (symbolicName != null) {
			return symbolicName + " " + getVersion(resource);
		}
		return resource.toString();
	}

    public static boolean isFragment(Resource resource)
    {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE))
            {
                String type = (String)
                    cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
                return (type != null) && type.equals(IdentityNamespace.TYPE_FRAGMENT);
            }
        }
        return false;
    }

    public static boolean isOptional(Requirement req)
    {
        String resolution = req.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
        return Namespace.RESOLUTION_OPTIONAL.equalsIgnoreCase(resolution);
    }

    public static boolean isMultiple(Requirement req)
    {
    	return Namespace.CARDINALITY_MULTIPLE.equals(req.getDirectives()
            .get(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE)) && !isDynamic(req);
    }

    public static boolean isDynamic(Requirement req)
    {
    	return PackageNamespace.RESOLUTION_DYNAMIC.equals(req.getDirectives()
            .get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
    }

    public static boolean isReexport(Requirement req)
    {
        return BundleNamespace.VISIBILITY_REEXPORT.equals(req.getDirectives()
            .get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE));
    }

    public static List<Requirement> getDynamicRequirements(List<Requirement> reqs)
    {
        List<Requirement> result = new ArrayList<Requirement>();
        if (reqs != null)
        {
            for (Requirement req : reqs)
            {
                String resolution = req.getDirectives()
                    .get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
                if ((resolution != null)
                    && resolution.equals(PackageNamespace.RESOLUTION_DYNAMIC))
                {
                    result.add(req);
                }
            }
        }
        return result;
    }

	public static boolean matches(Capability capability, Requirement requirement) {
		String namespace = requirement.getNamespace();
		if (!namespace.equals(capability.getNamespace())) {
			return false;
		}
		String filterSpec = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		boolean matchMandatory = PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)
				|| BundleNamespace.BUNDLE_NAMESPACE.equals(namespace) || HostNamespace.HOST_NAMESPACE.equals(namespace);
		try {
			return Capabilities.matches(FrameworkUtil.createFilter(filterSpec), capability, matchMandatory);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	public static String getPackageName(Capability capability) {
		if (capability != null && PackageNamespace.PACKAGE_NAMESPACE.equals(capability.getNamespace())) {
			Object object = capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
			if (object instanceof String) {
				return (String) object;
			}
		}
		return "";
	}

	public static boolean isExportPackage(Capability capability) {
		return capability != null && PackageNamespace.PACKAGE_NAMESPACE.equals(capability.getNamespace());
	}

	public static Set<String> getUses(Capability capability) {
		if (capability != null && PackageNamespace.PACKAGE_NAMESPACE.equals(capability.getNamespace())) {
			String uses = capability.getDirectives().get(PackageNamespace.CAPABILITY_USES_DIRECTIVE);
			if (uses != null && !uses.isEmpty()) {
				return Arrays.stream(uses.split(",")).map(String::trim).collect(Collectors.toSet());
			}
		}
		return Collections.emptySet();
	}
}