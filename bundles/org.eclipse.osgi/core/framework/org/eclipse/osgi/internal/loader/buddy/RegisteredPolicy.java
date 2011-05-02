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
import java.util.Enumeration;
import java.util.Iterator;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.BundleLoaderProxy;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

/**
 *Registered policy is an implementation of a buddy policy. 
 * It is responsible for looking up a class in the bundles (registrant) that declare interest in the bundle that require the buddy loading.
 * Note that the registrants must have a direct dependency on the bundle needing buddy.
 */
public class RegisteredPolicy extends DependentPolicy {

	public RegisteredPolicy(BundleLoader requester) {
		super(requester);

		//Filter the dependents;
		if (allDependents == null)
			return;

		for (Iterator<BundleDescription> iter = allDependents.iterator(); iter.hasNext();) {
			BundleLoaderProxy proxy = buddyRequester.getLoaderProxy(iter.next());
			if (proxy == null)
				iter.remove();

			try {
				String[] allContributions = ManifestElement.getArrayFromList(((AbstractBundle) proxy.getBundle()).getBundleData().getManifest().get(Constants.REGISTERED_POLICY));
				if (allContributions == null) {
					iter.remove();
					continue;
				}
				boolean contributes = false;
				for (int j = 0; j < allContributions.length && contributes == false; j++) {
					if (allContributions[j].equals(buddyRequester.getBundle().getSymbolicName()))
						contributes = true;
				}
				if (!contributes)
					iter.remove();

			} catch (BundleException e) {
				iter.remove();
			}
		}

		//After the filtering, if nothing is left then null out the variable for optimization
		if (allDependents.size() == 0)
			allDependents = null;
	}

	public Class<?> loadClass(String name) {
		if (allDependents == null)
			return null;

		Class<?> result = null;
		int size = allDependents.size();
		for (int i = 0; i < size && result == null; i++) {
			try {
				BundleLoaderProxy proxy = buddyRequester.getLoaderProxy(allDependents.get(i));
				if (proxy == null)
					continue;
				result = proxy.getBundleLoader().findClass(name);
			} catch (ClassNotFoundException e) {
				//Nothing to do, just keep looking
				continue;
			}
		}
		return result;
	}

	public URL loadResource(String name) {
		if (allDependents == null)
			return null;

		URL result = null;
		int size = allDependents.size();
		for (int i = 0; i < size && result == null; i++) {
			BundleLoaderProxy proxy = buddyRequester.getLoaderProxy(allDependents.get(i));
			if (proxy == null)
				continue;
			result = proxy.getBundleLoader().findResource(name);
		}
		return result;
	}

	public Enumeration<URL> loadResources(String name) {
		if (allDependents == null)
			return null;

		Enumeration<URL> results = null;
		int size = allDependents.size();
		for (int i = 0; i < size; i++) {
			try {
				BundleLoaderProxy proxy = buddyRequester.getLoaderProxy(allDependents.get(i));
				if (proxy == null)
					continue;
				results = BundleLoader.compoundEnumerations(results, proxy.getBundleLoader().findResources(name));
			} catch (IOException e) {
				//Ignore and keep looking
			}
		}
		return results;
	}
}
