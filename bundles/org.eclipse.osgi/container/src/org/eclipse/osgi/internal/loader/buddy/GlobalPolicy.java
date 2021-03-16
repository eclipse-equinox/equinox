/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.loader.buddy;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.internal.container.Capabilities;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Namespace;

/**
 * Global policy is an implementation of a buddy policy. It is responsible
 * for looking up a class within the global set of exported classes. If multiple
 * version of the same package are exported in the system, the exported package
 * with the highest version will be returned.
 */
public class GlobalPolicy implements IBuddyPolicy {
	private FrameworkWiring frameworkWiring;

	public GlobalPolicy(FrameworkWiring frameworkWiring) {
		this.frameworkWiring = frameworkWiring;
	}

	@Override
	public Class<?> loadClass(String name) {
		return getExportingBundles(BundleLoader.getPackageName(name)) //
				.stream().findFirst().map(b -> {
					try {
						return b.loadClass(name);
					} catch (ClassNotFoundException e) {
						return null;
					}
				}).orElse(null);
	}

	@Override
	public URL loadResource(String name) {
		return getExportingBundles(BundleLoader.getResourcePackageName(name)) //
				.stream().findFirst().map(b -> {
					return b.getResource(name);
				}).orElse(null);
	}

	@Override
	public Enumeration<URL> loadResources(String name) {
		Enumeration<URL> results = null;
		Collection<Bundle> exporters = getExportingBundles(name);
		for (Bundle exporter : exporters) {
			try {
				results = BundleLoader.compoundEnumerations(results, exporter.getResources(name));
			}catch (IOException e) {
				//ignore IO problems and try next package
			}
		}
		return results;
	}

	private Collection<Bundle> getExportingBundles(String pkgName) {
		Collection<Bundle> result = new ArrayList<>();
		String filter = "(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + pkgName + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Map<String, String> directives = Collections
				.<String, String>singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
		Map<String, Boolean> attributes = Collections.singletonMap(Capabilities.SYNTHETIC_REQUIREMENT, Boolean.TRUE);
		Collection<BundleCapability> packages = frameworkWiring.findProviders(
				ModuleContainer.createRequirement(PackageNamespace.PACKAGE_NAMESPACE, directives, attributes));
		for (BundleCapability pkg : packages) {
			if ((pkg.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				// use the hosts
				BundleWiring wiring = pkg.getRevision().getWiring();
				for (BundleWire hostWire : wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE)) {
					result.add(hostWire.getProvider().getBundle());
				}
			} else {
				result.add(pkg.getRevision().getBundle());
			}
		}
		return result;
	}
}
