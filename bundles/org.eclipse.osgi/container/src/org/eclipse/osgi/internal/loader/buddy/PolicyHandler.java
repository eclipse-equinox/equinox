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

import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.internal.loader.BundleLoader;
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
	private volatile Object[] policies = null;

	//Support to cut class / resource loading cycles in the context of one thread. The contained object is a set of classname
	private final ThreadLocal<Set<String>> beingLoaded;
	private final PackageAdmin packageAdmin;

	public PolicyHandler(BundleLoader loader, String buddyList, PackageAdmin packageAdmin) {
		policedLoader = loader;
		policies = getArrayFromList(buddyList);
		beingLoaded = new ThreadLocal<Set<String>>();
		this.packageAdmin = packageAdmin;
	}

	static Object[] getArrayFromList(String stringList) {
		if (stringList == null || stringList.trim().equals("")) //$NON-NLS-1$
			return null;
		List<Object> list = new ArrayList<Object>();
		StringTokenizer tokens = new StringTokenizer(stringList, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.add(token);
		}
		return list.isEmpty() ? new Object[0] : list.toArray(new Object[list.size()]);
	}

	private IBuddyPolicy getPolicyImplementation(Object[] policiesSnapshot, int policyOrder) {
		synchronized (policiesSnapshot) {
			if (policyOrder >= policiesSnapshot.length)
				return null;
			if (policiesSnapshot[policyOrder] instanceof String) {
				String buddyName = (String) policiesSnapshot[policyOrder];

				if (REGISTERED_POLICY.equals(buddyName)) {
					policiesSnapshot[policyOrder] = new RegisteredPolicy(policedLoader);
					return (IBuddyPolicy) policiesSnapshot[policyOrder];
				}
				if (BOOT_POLICY.equals(buddyName)) {
					policiesSnapshot[policyOrder] = SystemPolicy.getInstance(SystemPolicy.BOOT);
					return (IBuddyPolicy) policiesSnapshot[policyOrder];
				}
				if (APP_POLICY.equals(buddyName)) {
					policiesSnapshot[policyOrder] = SystemPolicy.getInstance(SystemPolicy.APP);
					return (IBuddyPolicy) policiesSnapshot[policyOrder];
				}
				if (EXT_POLICY.equals(buddyName)) {
					policiesSnapshot[policyOrder] = SystemPolicy.getInstance(SystemPolicy.EXT);
					return (IBuddyPolicy) policiesSnapshot[policyOrder];
				}
				if (DEPENDENT_POLICY.equals(buddyName)) {
					policiesSnapshot[policyOrder] = new DependentPolicy(policedLoader);
					return (IBuddyPolicy) policiesSnapshot[policyOrder];
				}
				if (GLOBAL_POLICY.equals(buddyName)) {
					policiesSnapshot[policyOrder] = new GlobalPolicy(packageAdmin);
					return (IBuddyPolicy) policiesSnapshot[policyOrder];
				}
				if (PARENT_POLICY.equals(buddyName)) {
					policiesSnapshot[policyOrder] = new SystemPolicy(policedLoader.getParentClassLoader());
					return (IBuddyPolicy) policiesSnapshot[policyOrder];
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
			return (IBuddyPolicy) policiesSnapshot[policyOrder];
		}
	}

	public Class<?> doBuddyClassLoading(String name) {
		if (startLoading(name) == false)
			return null;

		Class<?> result = null;
		Object[] policiesSnapshot = policies;
		int policyCount = (policiesSnapshot == null) ? 0 : policiesSnapshot.length;
		for (int i = 0; i < policyCount && result == null; i++) {
			IBuddyPolicy policy = getPolicyImplementation(policiesSnapshot, i);
			if (policy != null)
				result = policy.loadClass(name);
		}
		stopLoading(name);
		return result;
	}

	public URL doBuddyResourceLoading(String name) {
		if (startLoading(name) == false)
			return null;

		URL result = null;
		Object[] policiesSnapshot = policies;
		int policyCount = (policiesSnapshot == null) ? 0 : policiesSnapshot.length;
		for (int i = 0; i < policyCount && result == null; i++) {
			IBuddyPolicy policy = getPolicyImplementation(policiesSnapshot, i);
			if (policy != null)
				result = policy.loadResource(name);
		}
		stopLoading(name);
		return result;
	}

	public Enumeration<URL> doBuddyResourcesLoading(String name) {
		if (startLoading(name) == false)
			return null;

		List<URL> results = null;
		Object[] policiesSnapshot = policies;
		int policyCount = (policiesSnapshot == null) ? 0 : policiesSnapshot.length;
		for (int i = 0; i < policyCount; i++) {
			IBuddyPolicy policy = getPolicyImplementation(policiesSnapshot, i);
			if (policy == null)
				continue;
			Enumeration<URL> result = policy.loadResources(name);
			if (result != null) {
				if (results == null)
					results = new ArrayList<URL>(policyCount);
				while (result.hasMoreElements()) {
					URL url = result.nextElement();
					if (!results.contains(url)) //only add if not already added 
						results.add(url);
				}
			}
		}
		stopLoading(name);
		return results == null || results.isEmpty() ? null : Collections.enumeration(results);
	}

	private boolean startLoading(String name) {
		Set<String> classesAndResources = beingLoaded.get();
		if (classesAndResources != null && classesAndResources.contains(name))
			return false;

		if (classesAndResources == null) {
			classesAndResources = new HashSet<String>(3);
			beingLoaded.set(classesAndResources);
		}
		classesAndResources.add(name);
		return true;
	}

	private void stopLoading(String name) {
		beingLoaded.get().remove(name);
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
			String list = policedLoader.getBundle().getBundleData().getManifest().get(Constants.BUDDY_LOADER);
			policies = getArrayFromList(list);
		} catch (BundleException e) {
			//Ignore
		}
	}
}
