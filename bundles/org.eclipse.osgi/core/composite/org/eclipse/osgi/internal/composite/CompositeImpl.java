/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.composite;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import org.eclipse.osgi.framework.adaptor.BundleClassLoader;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.BundleLoaderProxy;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.service.internal.composite.CompositeModule;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.service.framework.*;

public class CompositeImpl extends CompositeBase implements CompositeBundle {
	private static String COMPOSITE_STORAGE = "store"; //$NON-NLS-1$
	public static String COMPOSITE_CONFIGURATION = "compositeConfig.properties"; //$NON-NLS-1$

	private final ServiceTrackerManager trackerManager = new ServiceTrackerManager();

	public CompositeImpl(BundleData bundledata, org.eclipse.osgi.framework.internal.core.Framework framework) throws BundleException {
		super(bundledata, framework);
	}

	protected Framework findCompanionFramework(org.eclipse.osgi.framework.internal.core.Framework thisFramework, BundleData thisData) throws BundleException {
		// allocate storage area for the composite framework
		File compositeStorage = thisData.getDataFile(COMPOSITE_STORAGE);
		boolean firstTime = false;
		if (!compositeStorage.exists())
			// the child storage area has not been allocated; this is the first time
			firstTime = true;
		// find the configuration properties
		URL childConfig = bundledata.getEntry(COMPOSITE_CONFIGURATION);
		Properties props = new Properties();
		try {
			props.load(childConfig.openStream());
		} catch (IOException e) {
			throw new BundleException("Could not load child configuration", e); //$NON-NLS-1$
		}
		props.put(Constants.FRAMEWORK_STORAGE, compositeStorage.getAbsolutePath());
		// save the parent framework so the parent companion bundle can find it
		props.put(PROP_PARENTFRAMEWORK, thisFramework.getSystemBundleContext().getBundle());
		// TODO leaks "this" out of the constructor
		props.put(PROP_COMPOSITE, this);
		Equinox equinox = new Equinox((Map) props);
		if (!firstTime)
			// if not the first time then we are done
			return equinox;
		equinox.init();
		installSurrogate(equinox.getBundleContext(), thisData);
		return equinox;
	}

	private void installSurrogate(BundleContext companionContext, BundleData thisData) throws BundleException {
		Bundle surrogate;
		try {
			InputStream surrogateContent = CompositeHelper.getSurrogateInput(thisData.getManifest(), null, null);
			surrogate = companionContext.installBundle(thisData.getLocation(), surrogateContent);
		} catch (IOException e) {
			throw new BundleException("Error installing parent companion composite bundle", e); //$NON-NLS-1$
		}
		// disable the surrogate initially since we know we have not resolved the composite yet.
		CompositeHelper.setDisabled(true, surrogate, companionContext);
		// set the permissions of the surrogate bundle
		CompositeHelper.setCompositePermissions(thisData.getLocation(), companionContext);
	}

	private boolean updateSurrogate(BundleData thisData, BundleDescription child, ExportPackageDescription[] matchingExports) throws BundleException {
		// update the surrogate content with the matching exports provided by the composite
		InputStream surrogateContent;
		try {
			surrogateContent = CompositeHelper.getSurrogateInput(thisData.getManifest(), child, matchingExports);
		} catch (IOException e) {
			throw new BundleException("Error updating surrogate bundle.", e); //$NON-NLS-1$
		}
		CompositeModule surrogateComposite = (CompositeModule) getSurrogateBundle();
		surrogateComposite.updateContent(surrogateContent);
		// enable/disable the surrogate composite based on if we have exports handed to us
		boolean disable = matchingExports == null ? true : false;
		CompositeHelper.setDisabled(disable, getSurrogateBundle(), getCompositeFramework().getBundleContext());
		// return true if we can resolve the surrogate bundle
		return disable ? false : surrogateComposite.resolveContent();
	}

	public Framework getCompositeFramework() {
		if ((getState() & Bundle.UNINSTALLED) == 0) {
			if ((companionFramework.getState() & (Bundle.INSTALLED | Bundle.RESOLVED)) != 0)
				try {
					companionFramework.init();
				} catch (BundleException e) {
					throw new RuntimeException("Cannot initialize child framework.", e);
				}
		}
		return companionFramework;
	}

	public SurrogateBundle getSurrogateBundle() {
		return (SurrogateBundle) getCompanionBundle();
	}

	protected Bundle getCompanionBundle() {
		return getCompositeFramework().getBundleContext().getBundle(1);
	}

	public void update(Map compositeManifest) throws BundleException {
		// validate the composite manifest
		CompositeHelper.validateCompositeManifest(compositeManifest);
		if (isResolved()) {
			// force the class loader creation before updating the surrogate to cache the current state
			// this is to allow for lazy updates of composite bundles
			BundleLoader loader = getBundleLoader();
			if (loader != null)
				loader.createClassLoader();
		}
		try {
			Map frameworkConfig = getFrameworkConfig();
			// first update the parent companion and disable it
			updateSurrogate(getBundleData(), null, null);
			// update the content with the new manifest
			updateContent(CompositeHelper.getCompositeInput(frameworkConfig, compositeManifest));
		} catch (IOException e) {
			throw new BundleException("Error updating composite.", e); //$NON-NLS-1$
		}
	}

	private Map getFrameworkConfig() throws IOException {
		Properties result = new Properties();
		URL config = getEntry(COMPOSITE_CONFIGURATION);
		result.load(config.openStream());
		return result;
	}

	public void uninstall() throws BundleException {
		// ensure class loader is created if needed
		checkClassLoader();
		// stop first before stopping the child to let the service listener clean up
		stop(Bundle.STOP_TRANSIENT);
		stopChildFramework();
		super.uninstall();
	}

	private void checkClassLoader() {
		BundleLoaderProxy proxy = getLoaderProxy();
		if (proxy != null && proxy.inUse() && proxy.getBundleLoader() != null) {
			BundleClassLoader loader = proxy.getBundleLoader().createClassLoader();
			loader.getResource("dummy"); //$NON-NLS-1$
		}
	}

	protected void startHook() throws BundleException {
		// always start the child framework
		companionFramework.start();
		trackerManager.startedComposite();
	}

	protected void stopHook() throws BundleException {
		// bug 363561; need to make sure the class loader is created
		// before stopping the composite framework
		try {
			checkClassLoader();
		} catch (Throwable t) {
			framework.publishFrameworkEvent(FrameworkEvent.ERROR, this, t);
		}
		trackerManager.stoppedComposite();
		// do not stop the framework unless we are persistently stopped 
		if ((bundledata.getStatus() & Constants.BUNDLE_STARTED) == 0)
			stopChildFramework();
	}

	public void started(CompositeModule surrogate) {
		if (surrogate == getSurrogateBundle())
			trackerManager.startedSurrogate();
	}

	public void stopped(CompositeModule surrogate) {
		if (surrogate == getSurrogateBundle())
			trackerManager.stoppedSurrogate();
	}

	private void stopChildFramework() throws BundleException {
		companionFramework.stop();
		try {
			FrameworkEvent stopped = companionFramework.waitForStop(30000);
			switch (stopped.getType()) {
				case FrameworkEvent.ERROR :
					throw new BundleException("Error stopping the child framework.", stopped.getThrowable()); //$NON-NLS-1$
				case FrameworkEvent.WAIT_TIMEDOUT :
					throw new BundleException("Timed out waiting for the child framework to stop."); //$NON-NLS-1$
				case FrameworkEvent.STOPPED :
					// normal stop, just return
					return;
				default :
					throw new BundleException("Unexpected code returned when stopping the child framework:" + stopped.getType()); //$NON-NLS-1$
			}
		} catch (InterruptedException e) {
			throw new BundleException("Error stopping child framework", e); //$NON-NLS-1$
		}
	}

	public boolean giveExports(ExportPackageDescription[] matchingExports) {
		if (matchingExports == null) {
			SurrogateBundle surrogate = getSurrogateBundle();
			// disable the surrogate
			CompositeHelper.setDisabled(true, getSurrogateBundle(), getCompositeFramework().getBundleContext());
			// refresh the surrogate synchronously
			((CompositeModule) surrogate).refreshContent();
			return true;
		}
		try {
			return updateSurrogate(getBundleData(), getBundleDescription(), matchingExports);
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	/*
	 * Listens to source and target bundles and source and target framework changes
	 */
	class ServiceTrackerManager {
		static final int COMPOSITE_ACTIVE = 0x01;
		static final int SURROGATE_ACTIVE = 0x02;
		// @GuardedBy(this)
		private int bundlesActive = 0;
		// @GuardedBy(this)
		private CompositeServiceTracker shareToChildServices;
		// @GuardedBy(this)
		private CompositeServiceTracker shareToParentServices;

		void startedComposite() throws BundleException {
			open(COMPOSITE_ACTIVE);
			getSurrogateBundle().start(Bundle.START_TRANSIENT);
		}

		void startedSurrogate() {
			open(SURROGATE_ACTIVE);
		}

		void stoppedComposite() {
			try {
				getSurrogateBundle().stop(Bundle.STOP_TRANSIENT);
			} catch (BundleException e) {
				// nothing
			} catch (IllegalStateException e) {
				// child framework must have been stoped
			}
			close(COMPOSITE_ACTIVE);
		}

		void stoppedSurrogate() {
			close(SURROGATE_ACTIVE);
		}

		private synchronized void open(int bundleActive) {
			bundlesActive |= bundleActive;
			if ((bundlesActive & (COMPOSITE_ACTIVE | SURROGATE_ACTIVE)) != (COMPOSITE_ACTIVE | SURROGATE_ACTIVE))
				return;
			// create a service tracker to track and share services from the parent framework
			shareToChildServices = new CompositeServiceTracker(getBundleContext(), getSurrogateBundle().getBundleContext(), (String) getBundleContext().getBundle().getHeaders("").get(CompositeBundleFactory.COMPOSITE_SERVICE_FILTER_IMPORT)); //$NON-NLS-1$
			shareToChildServices.open();
			// create a service tracker to track and share services from the child framework
			shareToParentServices = new CompositeServiceTracker(getSurrogateBundle().getBundleContext(), getBundleContext(), (String) getBundleContext().getBundle().getHeaders("").get(CompositeBundleFactory.COMPOSITE_SERVICE_FILTER_EXPORT)); //$NON-NLS-1$
			shareToParentServices.open();

		}

		private synchronized void close(int bundleStopped) {
			bundlesActive ^= bundleStopped;
			// close the service tracker to stop tracking and sharing services
			if (shareToChildServices != null)
				shareToChildServices.close();
			if (shareToParentServices != null)
				shareToParentServices.close();
		}
	}
}
