/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.loader.buddy;

import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.BundleLoader;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class PolicyHandler implements SynchronousBundleListener {
	//Key for the framework buddies
	private final static String DEPENDENT_POLICY = "dependent"; //$NON-NLS-1$
	private final static String GLOBAL_POLICY = "global"; //$NON-NLS-1$
	private final static String REGISTERED_POLICY = "registered"; //$NON-NLS-1$
	private final static String APP_POLICY = "app"; //$NON-NLS-1$
	private final static String EXT_POLICY = "ext"; //$NON-NLS-1$
	private final static String BOOT_POLICY = "boot"; //$NON-NLS-1$
	private final static String PARENT_POLICY = "parent"; //$NON-NLS-1$

	//The loader to which this policy is attached.
	private final BundleLoader policedLoader;
	//List of the policies as well as cache for the one that have been created. The size of this array never changes over time. This is why the synchronization is not done when iterating over it.
	// @GuardedBy (this)
	private Object[] policies = null;

	//Support to cut class / resource loading cycles in the context of one thread. The contained object is a set of classname
	private final ThreadLocal beingLoaded;
	private final PackageAdmin packageAdmin;

	public PolicyHandler(BundleLoader loader, String buddyList, PackageAdmin packageAdmin) {
		policedLoader = loader;
		policies = getArrayFromList(buddyList);
		beingLoaded = new ThreadLocal();
		this.packageAdmin = packageAdmin;
	}

	static Object[] getArrayFromList(String stringList) {
		if (stringList == null || stringList.trim().equals("")) //$NON-NLS-1$
			return null;
		Vector list = new Vector();
		StringTokenizer tokens = new StringTokenizer(stringList, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.addElement(token);
		}
		return list.isEmpty() ? new Object[0] : (Object[]) list.toArray(new Object[list.size()]);
	}

	private synchronized IBuddyPolicy getPolicyImplementation(int policyOrder) {
		if (policyOrder >= policies.length)
			return null;
		if (policies[policyOrder] instanceof String) {
			String buddyName = (String) policies[policyOrder];

			if (REGISTERED_POLICY.equals(buddyName)) {
				policies[policyOrder] = new RegisteredPolicy(policedLoader);
				return (IBuddyPolicy) policies[policyOrder];
			}
			if (BOOT_POLICY.equals(buddyName)) {
				policies[policyOrder] = SystemPolicy.getInstance(SystemPolicy.BOOT);
				return (IBuddyPolicy) policies[policyOrder];
			}
			if (APP_POLICY.equals(buddyName)) {
				policies[policyOrder] = SystemPolicy.getInstance(SystemPolicy.APP);
				return (IBuddyPolicy) policies[policyOrder];
			}
			if (EXT_POLICY.equals(buddyName)) {
				policies[policyOrder] = SystemPolicy.getInstance(SystemPolicy.EXT);
				return (IBuddyPolicy) policies[policyOrder];
			}
			if (DEPENDENT_POLICY.equals(buddyName)) {
				policies[policyOrder] = new DependentPolicy(policedLoader);
				return (IBuddyPolicy) policies[policyOrder];
			}
			if (GLOBAL_POLICY.equals(buddyName)) {
				policies[policyOrder] = new GlobalPolicy(packageAdmin);
				return (IBuddyPolicy) policies[policyOrder];
			}
			if (PARENT_POLICY.equals(buddyName)) {
				policies[policyOrder] = new SystemPolicy(policedLoader.getParentClassLoader());
				return (IBuddyPolicy) policies[policyOrder];
			}

			//			//Buddy policy can be provided by service implementations
			//			BundleContext fwkCtx = policedLoader.bundle.framework.systemBundle.context;
			//			ServiceReference[] matchingBuddies = null;
			//			try {
			//				matchingBuddies = fwkCtx.getAllServiceReferences(IBuddyPolicy.class.getName(), "buddyName=" + buddyName);
			//			} catch (InvalidSyntaxException e) {
			//				//The filter is valid
			//			}
			//			if (matchingBuddies == null)
			//				return new IBuddyPolicy() {
			//					public Class loadClass(String name) {
			//						return null;
			//					}
			//
			//					public URL loadResource(String name) {
			//						return null;
			//					}
			//
			//					public Enumeration loadResources(String name) {
			//						return null;
			//					}
			//				};
			//
			//			//The policies loaded through service are not cached
			//			return ((IBuddyPolicy) fwkCtx.getService(matchingBuddies[0]));
		}
		return (IBuddyPolicy) policies[policyOrder];
	}

	public Class doBuddyClassLoading(String name) {
		if (startLoading(name) == false)
			return null;

		Class result = null;
		int policyCount = getPolicyCount();
		for (int i = 0; i < policyCount && result == null; i++) {
			IBuddyPolicy policy = getPolicyImplementation(i);
			if (policy != null)
				result = policy.loadClass(name);
		}
		stopLoading(name);
		return result;
	}

	public URL doBuddyResourceLoading(String name) {
		if (startLoading(name) == false)
			return null;

		int policyCount = getPolicyCount();
		URL result = null;
		for (int i = 0; i < policyCount && result == null; i++) {
			IBuddyPolicy policy = getPolicyImplementation(i);
			if (policy != null)
				result = policy.loadResource(name);
		}
		stopLoading(name);
		return result;
	}

	public Enumeration doBuddyResourcesLoading(String name) {
		if (startLoading(name) == false)
			return null;

		int policyCount = getPolicyCount();;
		Vector results = null;
		for (int i = 0; i < policyCount; i++) {
			IBuddyPolicy policy = getPolicyImplementation(i);
			if (policy == null)
				continue;
			Enumeration result = policy.loadResources(name);
			if (result != null) {
				if (results == null)
					results = new Vector(policyCount);
				while (result.hasMoreElements()) {
					Object url = result.nextElement();
					if (!results.contains(url)) //only add if not already added 
						results.add(url);
				}
			}
		}
		stopLoading(name);
		return results == null || results.isEmpty() ? null : results.elements();
	}

	private boolean startLoading(String name) {
		Set classesAndResources = (Set) beingLoaded.get();
		if (classesAndResources != null && classesAndResources.contains(name))
			return false;

		if (classesAndResources == null) {
			classesAndResources = new HashSet(3);
			beingLoaded.set(classesAndResources);
		}
		classesAndResources.add(name);
		return true;
	}

	private void stopLoading(String name) {
		((Set) beingLoaded.get()).remove(name);
	}

	private synchronized int getPolicyCount() {
		return policies == null ? 0 : policies.length;
	}

	public void open(BundleContext context) {
		context.addBundleListener(this);
	}

	public void close(BundleContext context) {
		context.removeBundleListener(this);
	}

	public void bundleChanged(BundleEvent event) {
		if ((event.getType() & (BundleEvent.RESOLVED | BundleEvent.UNRESOLVED)) == 0)
			return;
		// reinitialize the policies
		try {
			String list = (String) policedLoader.getBundle().getBundleData().getManifest().get(Constants.BUDDY_LOADER);
			synchronized (this) {
				policies = getArrayFromList(list);
			}
		} catch (BundleException e) {
			//Ignore
		}
	}
}
