/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.loader.buddy;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.BundleLoaderProxy;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * DependentPolicy is an implementation of a buddy policy. 
 * It is responsible for looking up a class in the dependents of the bundle
 * to which this policy is attached to.
 */
public class DependentPolicy implements IBuddyPolicy {
	BundleLoader buddyRequester;
	int lastDependentOfAdded = -1; //remember the index of the bundle for which we last added the dependent
	List<BundleDescription> allDependents = null; //the list of all dependents known so far

	public DependentPolicy(BundleLoader requester) {
		buddyRequester = requester;

		//Initialize with the first level of dependent the list
		allDependents = new ArrayList<BundleDescription>();
		basicAddImmediateDependents(buddyRequester.getBundle().getBundleDescription());
		//If there is no dependent, reset to null
		if (allDependents.size() == 0)
			allDependents = null;
	}

	public Class<?> loadClass(String name) {
		if (allDependents == null)
			return null;

		Class<?> result = null;
		//size may change, so we must check it every time
		for (int i = 0; i < allDependents.size() && result == null; i++) {
			BundleDescription searchedBundle = allDependents.get(i);
			try {
				BundleLoaderProxy proxy = buddyRequester.getLoaderProxy(searchedBundle);
				if (proxy == null)
					continue;
				result = proxy.getBundleLoader().findClass(name);
			} catch (ClassNotFoundException e) {
				if (result == null)
					addDependent(i, searchedBundle);
			}
		}
		return result;
	}

	private synchronized void addDependent(int i, BundleDescription searchedBundle) {
		if (i > lastDependentOfAdded) {
			lastDependentOfAdded = i;
			basicAddImmediateDependents(searchedBundle);
		}
	}

	public URL loadResource(String name) {
		if (allDependents == null)
			return null;

		URL result = null;
		//size may change, so we must check it every time
		for (int i = 0; i < allDependents.size() && result == null; i++) {
			BundleDescription searchedBundle = allDependents.get(i);
			BundleLoaderProxy proxy = buddyRequester.getLoaderProxy(searchedBundle);
			if (proxy == null)
				continue;
			result = proxy.getBundleLoader().findResource(name);
			if (result == null) {
				addDependent(i, searchedBundle);
			}
		}
		return result;
	}

	public Enumeration<URL> loadResources(String name) {
		if (allDependents == null)
			return null;

		Enumeration<URL> results = null;
		//size may change, so we must check it every time
		for (int i = 0; i < allDependents.size(); i++) {
			BundleDescription searchedBundle = allDependents.get(i);
			try {
				BundleLoaderProxy proxy = buddyRequester.getLoaderProxy(searchedBundle);
				if (proxy == null)
					continue;
				results = BundleLoader.compoundEnumerations(results, proxy.getBundleLoader().findResources(name));
				addDependent(i, searchedBundle);
			} catch (IOException e) {
				//Ignore and keep looking
			}
		}
		return results;
	}

	private void basicAddImmediateDependents(BundleDescription root) {
		BundleDescription[] dependents = root.getDependents();
		for (int i = 0; i < dependents.length; i++) {
			BundleDescription toAdd = dependents[i];
			if (toAdd.getHost() == null && !allDependents.contains(toAdd)) {
				allDependents.add(toAdd);
			}
		}
	}
}
