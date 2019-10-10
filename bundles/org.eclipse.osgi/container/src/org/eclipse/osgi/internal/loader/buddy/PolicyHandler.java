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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
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
	private final List<String> originalBuddyList;
	//List of the policies as well as cache for the one that have been created. The size of this array never changes over time. This is why the synchronization is not done when iterating over it.
	private volatile Object[] policies = null;

	//Support to cut class / resource loading cycles in the context of one thread. The contained object is a set of classname
	private final ThreadLocal<Set<String>> beingLoaded;
	private final PackageAdmin packageAdmin;
	private final ClassLoader bootLoader;

	public PolicyHandler(BundleLoader loader, List<String> buddyList, PackageAdmin packageAdmin, ClassLoader bootLoader) {
		policedLoader = loader;
		this.originalBuddyList = buddyList;
		policies = buddyList.toArray();
		beingLoaded = new ThreadLocal<>();
		this.packageAdmin = packageAdmin;
		this.bootLoader = bootLoader;
	}

	static Object[] getArrayFromList(String stringList) {
		if (stringList == null || stringList.trim().equals("")) //$NON-NLS-1$
			return null;
		List<Object> list = new ArrayList<>();
		StringTokenizer tokens = new StringTokenizer(stringList, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.add(token.toLowerCase());
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
					policiesSnapshot[policyOrder] = SystemPolicy.getInstance(SystemPolicy.BOOT, bootLoader);
					return (IBuddyPolicy) policiesSnapshot[policyOrder];
				}
				if (APP_POLICY.equals(buddyName)) {
					policiesSnapshot[policyOrder] = SystemPolicy.getInstance(SystemPolicy.APP, bootLoader);
					return (IBuddyPolicy) policiesSnapshot[policyOrder];
				}
				if (EXT_POLICY.equals(buddyName)) {
					policiesSnapshot[policyOrder] = SystemPolicy.getInstance(SystemPolicy.EXT, bootLoader);
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
				// Not a valid buddy policy
				EquinoxBundle bundle = (EquinoxBundle) policedLoader.getModuleClassLoader().getBundle();
				bundle.getModule().getContainer().getAdaptor().publishContainerEvent(ContainerEvent.ERROR, bundle.getModule(), new RuntimeException("Invalid buddy policy: " + buddyName)); //$NON-NLS-1$
				policiesSnapshot[policyOrder] = null;
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
					results = new ArrayList<>(policyCount);
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
			classesAndResources = new HashSet<>(3);
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

	@Override
	public void bundleChanged(BundleEvent event) {
		if ((event.getType() & (BundleEvent.RESOLVED | BundleEvent.UNRESOLVED)) == 0)
			return;
		// reinitialize the policies
		policies = originalBuddyList.toArray();
	}
}
