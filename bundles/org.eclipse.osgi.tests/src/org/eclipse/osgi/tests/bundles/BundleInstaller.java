/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings({ "deprecation", "removal" }) // AccessController
public class BundleInstaller {
	private final BundleContext context;
	private final String rootLocation;
	private Map<String, Bundle> bundles = new HashMap<>();
	private final ServiceTracker<?, PackageAdmin> packageAdmin;
	private final ServiceTracker<?, StartLevel> startlevel;
	private final ServiceTracker<?, URLConverter> converter;
	private final ServiceTracker<?, PlatformAdmin> platformAdmin;

	public BundleInstaller(String bundlesRoot, BundleContext context) throws InvalidSyntaxException {
		this.context = context;
		rootLocation = bundlesRoot;
		converter = new ServiceTracker<>(context,
				context.createFilter("(&(objectClass=" + URLConverter.class.getName() + ")(protocol=bundleentry))"),
				null);
		converter.open();
		startlevel = new ServiceTracker<>(context, StartLevel.class.getName(), null);
		startlevel.open();
		packageAdmin = new ServiceTracker<>(context, PackageAdmin.class.getName(), null);
		packageAdmin.open();
		platformAdmin = new ServiceTracker<>(context, PlatformAdmin.class.getName(), null);
		platformAdmin.open();
	}

	synchronized public Bundle installBundle(String name) throws BundleException {
		return installBundle(name, true);
	}

	synchronized public Bundle installBundle(String name, boolean track) throws BundleException {
		String location = getBundleLocation(name);
		return install(location, null, name, track);
	}

	public synchronized Bundle installBundleAtLocation(String location) throws BundleException {
		return install(location, null, location, true);
	}

	public synchronized Bundle installBundleAtLocation(String location, InputStream input) throws BundleException {
		return install(location, input, location, true);
	}

	private Bundle install(String location, InputStream input, String name, boolean track) throws BundleException {
		if (bundles == null && track)
			return null;
		try (InputStream in = input) {
			Bundle bundle = context.installBundle(location, input);
			if (track)
				bundles.put(name, bundle);
			return bundle;
		} catch (IOException e) { // ignore
			throw new BundleException("Failed to close bundle's input stream", e);
		}
	}

	public String getBundleLocation(final String name) throws BundleException {
		if (System.getSecurityManager() == null)
			return getBundleLocation0(name);
		try {
			return (String) AccessController.doPrivileged((PrivilegedExceptionAction) () -> getBundleLocation0(name));
		} catch (PrivilegedActionException e) {
			throw (BundleException) e.getException();
		}
	}

	String getBundleLocation0(String name) throws BundleException {
		String bundleFileName = rootLocation + "/" + name;
		URL bundleURL = context.getBundle().getEntry(bundleFileName + ".jar");
		if (bundleURL == null)
			bundleURL = context.getBundle().getEntry(bundleFileName);
		if (bundleURL == null)
			throw new BundleException("Could not find bundle to install at: " + name);
		try {
			bundleURL = converter.getService().resolve(bundleURL);
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
		URL bundleURL = context.getBundle().getEntry(bundleFileName + ".jar");
		if (bundleURL == null)
			bundleURL = context.getBundle().getEntry(bundleFileName);
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

	synchronized public Bundle[] uninstallAllBundles() {
		if (bundles == null)
			return new Bundle[0];
		List<Bundle> result = new ArrayList<>(bundles.size());
		for (Bundle bundle : bundles.values()) {
			try {
				bundle.uninstall();
			} catch (IllegalStateException e) {
				// ignore; bundle probably already uninstalled
			} catch (BundleException e) {
				// ignore and move on, but print stacktrace for logs
				e.printStackTrace();
			}
			result.add(bundle);
		}
		bundles.clear();
		return result.toArray(new Bundle[result.size()]);
	}

	synchronized public void shutdown() {
		if (bundles == null)
			return;
		Bundle[] result = uninstallAllBundles();
		refreshPackages(result);
		packageAdmin.close();
		startlevel.close();
		converter.close();
		platformAdmin.close();
		bundles = null;
	}

	synchronized public Bundle[] refreshPackages(Bundle[] refresh) {
		if (bundles == null)
			return null;
		PackageAdmin pa = packageAdmin.getService();
		CountDownLatch flag = new CountDownLatch(1);
		FrameworkListener listener = event -> {
			if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
				flag.countDown();
			}
		};
		context.addFrameworkListener(listener);
		final Set<Bundle> refreshed = new HashSet<>();
		SynchronousBundleListener refreshBundleListener = event -> refreshed.add(event.getBundle());
		context.addBundleListener(refreshBundleListener);
		try {
			pa.refreshPackages(refresh);
			assertTrue("refreshPackages timed out", flag.await(30, TimeUnit.SECONDS));
		} catch (InterruptedException e) { // do nothing
		} finally {
			context.removeFrameworkListener(listener);
			context.removeBundleListener(refreshBundleListener);
		}
		return refreshed.toArray(new Bundle[refreshed.size()]);
	}

	synchronized public boolean resolveBundles(Bundle[] resolve) {
		if (bundles == null)
			return false;
		PackageAdmin pa = packageAdmin.getService();
		return pa.resolveBundles(resolve);
	}

	synchronized public Bundle getBundle(String name) {
		if (bundles == null)
			return null;
		return bundles.get(name);
	}

	public StartLevel getStartLevel() {
		return startlevel.getService();
	}

	public PackageAdmin getPackageAdmin() {
		return packageAdmin.getService();
	}

	public PlatformAdmin getPlatformAdmin() {
		return platformAdmin.getService();
	}
}
