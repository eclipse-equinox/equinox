/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
import java.util.Enumeration;
import java.util.List;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;

/**
 * DependentPolicy is an implementation of a buddy policy.
 * It is responsible for looking up a class in the dependents of the bundle
 * to which this policy is attached to.
 */
public class DependentPolicy implements IBuddyPolicy {
	BundleLoader buddyRequester;
	int lastDependentOfAdded = -1; //remember the index of the bundle for which we last added the dependent
	List<ModuleWiring> allDependents = null; //the list of all dependents known so far

	public DependentPolicy(BundleLoader requester) {
		buddyRequester = requester;

		//Initialize with the first level of dependent the list
		allDependents = new ArrayList<>();
		basicAddImmediateDependents(requester.getWiring());
		//If there is no dependent, reset to null
		if (allDependents.size() == 0)
			allDependents = null;
	}

	@Override
	public Class<?> loadClass(String name) {
		if (allDependents == null) {
			return null;
		}
		//size may change, so we must check it every time
		for (int i = 0; i < allDependents.size(); i++) {
			ModuleWiring searchWiring = allDependents.get(i);
			BundleLoader searchLoader = (BundleLoader) searchWiring.getModuleLoader();
			if (searchLoader != null) {
				Class<?> result = searchLoader.findClassNoParentNoException(name);
				if (result != null) {
					return result;
				}
				addDependent(i, searchWiring);
			}
		}
		return null;
	}

	private synchronized void addDependent(int i, ModuleWiring searchedWiring) {
		if (i > lastDependentOfAdded) {
			lastDependentOfAdded = i;
			basicAddImmediateDependents(searchedWiring);
		}
	}

	@Override
	public URL loadResource(String name) {
		if (allDependents == null)
			return null;

		URL result = null;
		//size may change, so we must check it every time
		for (int i = 0; i < allDependents.size() && result == null; i++) {
			ModuleWiring searchWiring = allDependents.get(i);
			BundleLoader searchLoader = (BundleLoader) searchWiring.getModuleLoader();
			if (searchLoader != null) {
				result = searchLoader.findResource(name);
				if (result == null) {
					addDependent(i, searchWiring);
				}
			}
		}
		return result;
	}

	@Override
	public Enumeration<URL> loadResources(String name) {
		if (allDependents == null)
			return null;

		Enumeration<URL> results = null;
		//size may change, so we must check it every time
		for (int i = 0; i < allDependents.size(); i++) {
			ModuleWiring searchWiring = allDependents.get(i);
			BundleLoader searchLoader = (BundleLoader) searchWiring.getModuleLoader();
			if (searchLoader != null) {
				try {
					results = BundleLoader.compoundEnumerations(results, searchLoader.findResources(name));
					addDependent(i, searchWiring);
				} catch (IOException e) {
					//Ignore and keep looking
				}
			}
		}
		return results;
	}

	private void basicAddImmediateDependents(ModuleWiring wiring) {
		List<ModuleWire> providedWires = wiring.getProvidedModuleWires(null);
		if (providedWires != null) {
			for (ModuleWire wire : providedWires) {
				String namespace = wire.getRequirement().getNamespace();
				if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace) || BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)) {
					ModuleWiring dependent = wire.getRequirerWiring();
					if (!allDependents.contains(dependent)) {
						allDependents.add(dependent);
					}
				}
			}
		}
	}
}
