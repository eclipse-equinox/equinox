/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package org.eclipse.equinox.region.tests;

import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.util.*;
import org.eclipse.equinox.region.Region;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.*;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.ServiceTracker;

public class BundleInstaller {
	private final BundleContext context;
	private final Bundle testBundle;
	private String rootLocation;
	private Map<String, Bundle> bundles = new HashMap<String, Bundle>();
	private final ServiceTracker<URLConverter, URLConverter> converter;
	private final FrameworkWiring frameworkWiring;

	public BundleInstaller(String bundlesRoot, Bundle testBundle) throws InvalidSyntaxException {
		BundleContext bc = testBundle.getBundleContext();
		Bundle systemBundle = bc.getBundle(0);
		context = systemBundle.getBundleContext();
		frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
		rootLocation = bundlesRoot;
		converter = new ServiceTracker<URLConverter, URLConverter>(context, context.createFilter("(&(objectClass=" + URLConverter.class.getName() + ")(protocol=bundleentry))"), null);
		converter.open();
		this.testBundle = testBundle;
	}

	synchronized public Bundle installBundle(String name) throws BundleException {
		return installBundle(name, null);
	}

	synchronized public Bundle installBundle(String name, Region region) throws BundleException {
		if (bundles == null)
			return null;
		String location = getBundleLocation(name);
		Bundle bundle = region != null ? region.installBundle(location) : context.installBundle(location);
		if (bundles.containsKey(name)) {
			int offset = 0;
			while (bundles.containsKey(name + '_' + offset)) {
				offset++;
			}
			bundles.put(name + '_' + offset, bundle);
		} else {
			bundles.put(name, bundle);
		}
		return bundle;
	}

	public String getBundleLocation(final String name) throws BundleException {
		if (System.getSecurityManager() == null)
			return getBundleLocation0(name);
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
				@Override
				public String run() throws Exception {
					return getBundleLocation0(name);
				}
			});
		} catch (PrivilegedActionException e) {
			throw (BundleException) e.getException();
		}
	}

	String getBundleLocation0(String name) throws BundleException {
		String bundleFileName = rootLocation + "/" + name;
		URL bundleURL = testBundle.getEntry(bundleFileName);
		if (bundleURL == null)
			bundleURL = testBundle.getEntry(bundleFileName + ".jar");
		if (bundleURL == null)
			throw new BundleException("Could not find bundle to install at: " + name);
		try {
			bundleURL = (converter.getService()).resolve(bundleURL);
		} catch (IOException e) {
			throw new BundleException("Converter error", e);
		}
		String location = bundleURL.toExternalForm();
		if ("file".equals(bundleURL.getProtocol()))
			location = "reference:" + location;
		return location;
	}

	synchronized public Bundle updateBundle(String fromName, String toName) throws BundleException {
		if (bundles == null)
			return null;
		Bundle fromBundle = bundles.get(fromName);
		if (fromBundle == null)
			throw new BundleException("The bundle to update does not exist!! " + fromName);
		String bundleFileName = rootLocation + "/" + toName;
		URL bundleURL = testBundle.getEntry(bundleFileName);
		if (bundleURL == null)
			bundleURL = testBundle.getEntry(bundleFileName + ".jar");
		try {
			bundleURL = converter.getService().resolve(bundleURL);
		} catch (IOException e) {
			throw new BundleException("Converter error", e);
		}
		String location = bundleURL.toExternalForm();
		if ("file".equals(bundleURL.getProtocol()))
			location = "reference:" + location;
		try {
			fromBundle.update(new URL(location).openStream());
		} catch (Exception e) {
			throw new BundleException("Errors when updating bundle " + fromBundle, e);
		}
		bundles.remove(fromName);
		bundles.put(toName, fromBundle);
		return fromBundle;
	}

	synchronized public Bundle uninstallBundle(String name) throws BundleException {
		if (bundles == null)
			return null;
		Bundle bundle = bundles.remove(name);
		if (bundle == null)
			return null;
		bundle.uninstall();
		return bundle;
	}

	synchronized public Bundle[] uninstallAllBundles() throws BundleException {
		if (bundles == null)
			return null;
		List<Bundle> result = new ArrayList<Bundle>(bundles.size());
		for (Iterator<Bundle> iter = bundles.values().iterator(); iter.hasNext();) {
			Bundle bundle = iter.next();
			try {
				bundle.uninstall();
			} catch (IllegalStateException e) {
				// ignore; bundle probably already uninstalled
			}
			result.add(bundle);
		}
		bundles.clear();
		return result.toArray(new Bundle[result.size()]);
	}

	synchronized public Bundle[] shutdown() throws BundleException {
		if (bundles == null)
			return null;
		Bundle[] result = uninstallAllBundles();
		refreshPackages(result);
		converter.close();
		bundles = null;
		return result;
	}

	synchronized public Bundle[] refreshPackages(Bundle[] refresh) {
		if (refresh == null)
			return null;
		final boolean[] flag = new boolean[] {false};
		FrameworkListener listener = new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED)
					synchronized (flag) {
						flag[0] = true;
						flag.notifyAll();
					}
			}
		};
		final Set<Bundle> refreshed = new HashSet<Bundle>();
		BundleListener refreshBundleListener = new SynchronousBundleListener() {
			public void bundleChanged(BundleEvent event) {
				refreshed.add(event.getBundle());
			}
		};
		context.addBundleListener(refreshBundleListener);
		try {
			frameworkWiring.refreshBundles(Arrays.asList(refresh), listener);
			synchronized (flag) {
				while (!flag[0]) {
					try {
						flag.wait(5000);
					} catch (InterruptedException e) {
						// do nothing
					}
				}
			}
		} finally {
			context.removeBundleListener(refreshBundleListener);
		}
		return refreshed.toArray(new Bundle[refreshed.size()]);
	}

	synchronized public boolean resolveBundles(Bundle[] resolve) {
		if (resolve == null)
			return false;
		return frameworkWiring.resolveBundles(Arrays.asList(resolve));
	}

	synchronized public Bundle getBundle(String name) {
		if (bundles == null)
			return null;
		return bundles.get(name);
	}
}
