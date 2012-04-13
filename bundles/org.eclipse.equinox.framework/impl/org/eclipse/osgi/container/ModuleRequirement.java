/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.util.Map;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.resource.Namespace;

public class ModuleRequirement implements BundleRequirement {
	private final String namespace;
	private final Map<String, String> directives;
	private final Map<String, Object> attributes;
	private final ModuleRevision revision;
	private volatile Filter filter;

	ModuleRequirement(String namespace, Map<String, String> directives, Map<String, Object> attributes, ModuleRevision revision) {
		this.namespace = namespace;
		this.directives = directives;
		this.attributes = attributes;
		this.revision = revision;
	}

	@Override
	public ModuleRevision getRevision() {
		return revision;
	}

	@Override
	public boolean matches(BundleCapability capability) {
		if (!namespace.equals(capability.getNamespace()))
			return false;
		String filterSpec = directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filterSpec != null) {
			Filter f = getFilter();
			if (f == null) {
				try {
					f = FrameworkUtil.createFilter(filterSpec);
					setFilter(f);
				} catch (InvalidSyntaxException e) {
					return false;
				}
			}
			if (!f.matches(capability.getAttributes()))
				return false;
		}
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace) || BundleNamespace.BUNDLE_NAMESPACE.equals(namespace) || HostNamespace.HOST_NAMESPACE.equals(namespace)) {
			// check for mandatory directive
			String mandatory = capability.getDirectives().get(AbstractWiringNamespace.CAPABILITY_MANDATORY_DIRECTIVE);
			if (mandatory != null) {
				if (filterSpec == null)
					return false;
				String[] mandatoryAttrs = ManifestElement.getArrayFromList(mandatory, ","); //$NON-NLS-1$
				for (String mandatoryAttr : mandatoryAttrs) {
					if (filterSpec.indexOf("(" + mandatoryAttr + "=") < 0) //$NON-NLS-1$ //$NON-NLS-2$
						return false;
				}
			}
		}

		return true;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Map<String, String> getDirectives() {
		return directives;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public ModuleRevision getResource() {
		return revision;
	}

	public Filter getFilter() {
		return filter;
	}

	void setFilter(Filter filter) {
		this.filter = filter;
	}

	public String toString() {
		return namespace + ModuleRevision.toString(attributes, false) + ModuleRevision.toString(directives, true);
	}
}
