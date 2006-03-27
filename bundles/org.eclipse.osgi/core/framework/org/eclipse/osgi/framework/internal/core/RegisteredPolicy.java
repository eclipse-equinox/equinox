/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.net.URL;
import java.util.*;

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

		for (Iterator iter = allDependents.iterator(); iter.hasNext();) {
			BundleLoaderProxy proxy = buddyRequester.getLoaderProxy((BundleDescription) iter.next());			
			if (proxy == null)
				iter.remove();

			try {
				String[] allContributions = ManifestElement.getArrayFromList((String) ((AbstractBundle) proxy.getBundle()).getBundleData().getManifest().get(Constants.REGISTERED_POLICY));
				if (allContributions == null) {
					iter.remove();
					continue;
				}
				boolean contributes = false;
				for (int j = 0; j < allContributions.length && contributes == false; j++) {
					if (allContributions[j].equals(buddyRequester.bundle.getSymbolicName()))
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
    
	public Class loadClass(String name) {
		if (allDependents == null)
			return null;

		Class result = null;
        int size = allDependents.size();
		for (int i = 0; i < size && result == null; i++) {
			try {
				BundleLoaderProxy proxy = buddyRequester.getLoaderProxy((BundleDescription) allDependents.get(i));
				if (proxy == null)
					continue;
				result = proxy.getBundleLoader().findClass(name, true);
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
			BundleLoaderProxy proxy = buddyRequester.getLoaderProxy((BundleDescription) allDependents.get(i));
			if (proxy == null)
				continue;
			result = proxy.getBundleLoader().findResource(name, true);
		}
		return result;
	}

	public Enumeration loadResources(String name) {
		if (allDependents == null)
			return null;

		Vector resources = null; //use a Vector to maintain order
        int size = allDependents.size();
		for (int i = 0; i < size; i++) {
			try {
				BundleLoaderProxy proxy = buddyRequester.getLoaderProxy((BundleDescription) allDependents.get(i));
				if (proxy == null)
					continue;
				Enumeration result = proxy.getBundleLoader().findResources(name);
                if (result != null) {
                    if (resources == null)
                        resources = new Vector();
                    while (result.hasMoreElements()) {
                        Object url = result.nextElement();
                        if (!resources.contains(url)) //only add if not already added 
                            resources.add(url);
                    }
                }
			} catch (IOException e) {
				//Ignore and keep looking
			}
		}
        return (resources == null) ? null : resources.elements();
	}
}
