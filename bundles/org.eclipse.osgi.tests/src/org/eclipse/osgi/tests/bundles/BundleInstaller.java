/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.util.*;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

public class BundleInstaller {
	private BundleContext context;
	private String rootLocation;
	private HashMap bundles = new HashMap();
	private ServiceTracker packageAdmin;
	private ServiceTracker startlevel;
	private ServiceTracker converter;
	private ServiceTracker platformAdmin;

	public BundleInstaller(String bundlesRoot, BundleContext context) throws InvalidSyntaxException {
		this.context = context;
		rootLocation = bundlesRoot;
		converter = new ServiceTracker(context, context.createFilter("(&(objectClass=" + URLConverter.class.getName() + ")(protocol=bundleentry))"), null);
		converter.open();
		startlevel = new ServiceTracker(context, StartLevel.class.getName(), null);
		startlevel.open();
		packageAdmin = new ServiceTracker(context, PackageAdmin.class.getName(), null);
		packageAdmin.open();
		platformAdmin = new ServiceTracker(context, PlatformAdmin.class.getName(), null);
		platformAdmin.open();
	}

	synchronized public Bundle installBundle(String name) throws BundleException {
		return installBundle(name, true);
	}

	synchronized public Bundle installBundle(String name, boolean track) throws BundleException {
		if (bundles == null && track)
			return null;
		String location = getBundleLocation(name);
		Bundle bundle = context.installBundle(location);
		if (track)
			bundles.put(name, bundle);
		return bundle;
	}

	public String getBundleLocation(final String name) throws BundleException {
		if (System.getSecurityManager() == null)
			return getBundleLocation0(name);
		try {
			return (String) AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					return getBundleLocation0(name);
				}
			});
		} catch (PrivilegedActionException e) {
			throw (BundleException) e.getException();
		}
	}

	String getBundleLocation0(String name) throws BundleException {
		String bundleFileName = rootLocation + "/" + name;
		URL bundleURL = context.getBundle().getEntry(bundleFileName);
		if (bundleURL == null)
			bundleURL = context.getBundle().getEntry(bundleFileName + ".jar");
		if (bundleURL == null)
			throw new BundleException("Could not find bundle to install at: " + name);
		try {
			bundleURL = ((URLConverter) converter.getService()).resolve(bundleURL);
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
		Bundle fromBundle = (Bundle) bundles.get(fromName);
		if (fromBundle == null)
			throw new BundleException("The bundle to update does not exist!! " + fromName);
		String bundleFileName = rootLocation + "/" + toName;
		URL bundleURL = context.getBundle().getEntry(bundleFileName);
		if (bundleURL == null)
			bundleURL = context.getBundle().getEntry(bundleFileName + ".jar");
		try {
			bundleURL = ((URLConverter) converter.getService()).resolve(bundleURL);
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
		Bundle bundle = (Bundle) bundles.remove(name);
		if (bundle == null)
			return null;
		bundle.uninstall();
		return bundle;
	}

	synchronized public Bundle[] uninstallAllBundles() throws BundleException {
		if (bundles == null)
			return null;
		ArrayList result = new ArrayList(bundles.size());
		for (Iterator iter = bundles.values().iterator(); iter.hasNext();) {
			Bundle bundle = (Bundle) iter.next();
			try {
				bundle.uninstall();
			} catch (IllegalStateException e) {
				// ignore; bundle probably already uninstalled
			}
			result.add(bundle);
		}
		bundles.clear();
		return (Bundle[]) result.toArray(new Bundle[result.size()]);
	}

	synchronized public Bundle[] shutdown() throws BundleException {
		if (bundles == null)
			return null;
		Bundle[] result = uninstallAllBundles();
		refreshPackages(result);
		packageAdmin.close();
		startlevel.close();
		converter.close();
		platformAdmin.close();
		bundles = null;
		return result;
	}

	synchronized public Bundle[] refreshPackages(Bundle[] refresh) {
		if (bundles == null)
			return null;
		PackageAdmin pa = (PackageAdmin) packageAdmin.getService();
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
		context.addFrameworkListener(listener);
		final HashSet refreshed = new HashSet();
		BundleListener refreshBundleListener = new SynchronousBundleListener() {
			public void bundleChanged(BundleEvent event) {
				refreshed.add(event.getBundle());
			}
		};
		context.addBundleListener(refreshBundleListener);
		try {
			pa.refreshPackages(refresh);
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
			context.removeFrameworkListener(listener);
			context.removeBundleListener(refreshBundleListener);
		}
		return (Bundle[]) refreshed.toArray(new Bundle[refreshed.size()]);
	}

	synchronized public boolean resolveBundles(Bundle[] resolve) {
		if (bundles == null)
			return false;
		PackageAdmin pa = (PackageAdmin) packageAdmin.getService();
		return pa.resolveBundles(resolve);
	}

	synchronized public Bundle getBundle(String name) {
		if (bundles == null)
			return null;
		return (Bundle) bundles.get(name);
	}

	public StartLevel getStartLevel() {
		return (StartLevel) startlevel.getService();
	}

	public PackageAdmin getPackageAdmin() {
		return (PackageAdmin) packageAdmin.getService();
	}

	public PlatformAdmin getPlatformAdmin() {
		return (PlatformAdmin) platformAdmin.getService();
	}
}
