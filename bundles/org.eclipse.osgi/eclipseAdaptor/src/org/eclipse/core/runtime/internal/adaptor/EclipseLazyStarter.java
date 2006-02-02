/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.runtime.internal.adaptor;

import java.net.URL;
import org.eclipse.core.runtime.internal.stats.StatsManager;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingStatsHook;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class EclipseLazyStarter implements ClassLoadingStatsHook, HookConfigurator {

	public void preFindLocalClass(String name, ClasspathManager manager) throws ClassNotFoundException {
		AbstractBundle bundle = (AbstractBundle) manager.getBaseData().getBundle();
		// If the bundle is active, uninstalled or stopping then the bundle has already
		// been initialized (though it may have been destroyed) so just return the class.
		if ((bundle.getState() & (Bundle.ACTIVE | Bundle.UNINSTALLED | Bundle.STOPPING)) != 0)
			return;

		// The bundle is not active and does not require activation, just return the class
		if (!shouldActivateFor(name, manager.getBaseData()))
			return;

		// The bundle is starting.  Note that if the state changed between the tests 
		// above and this test (e.g., it was not ACTIVE but now is), that's ok, we will 
		// just try to start it again (else case).
		// TODO need an explanation here of why we duplicated the mechanism 
		// from the framework rather than just calling start() and letting it sort it out.
		if (bundle.getState() == Bundle.STARTING) {
			// If the thread trying to load the class is the one trying to activate the bundle, then return the class 
			if (bundle.testStateChanging(Thread.currentThread()) || bundle.testStateChanging(null))
				return;

			// If it's another thread, we wait and try again. In any case the class is returned. 
			// The difference is that an exception can be logged.
			// TODO do we really need this test?  We just did it on the previous line?
			if (!bundle.testStateChanging(Thread.currentThread())) {
				Thread threadChangingState = bundle.getStateChanging();
				if (StatsManager.TRACE_BUNDLES && threadChangingState != null)
					System.out.println("Concurrent startup of bundle " + bundle.getSymbolicName() + " by " + Thread.currentThread() + " and " + threadChangingState.getName() + ". Waiting up to 5000ms for " + threadChangingState + " to finish the initialization."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				long start = System.currentTimeMillis();
				long delay = 5000;
				long timeLeft = delay;
				while (true) {
					try {
						Thread.sleep(100); // do not release the classloader lock (bug 86713)
						if (bundle.testStateChanging(null) || timeLeft <= 0)
							break;
					} catch (InterruptedException e) {
						//Ignore and keep waiting
					}
					timeLeft = start + delay - System.currentTimeMillis();
				}
				if (timeLeft <= 0 || bundle.getState() != Bundle.ACTIVE) {
					String bundleName = bundle.getSymbolicName() == null ? Long.toString(bundle.getBundleId()) : bundle.getSymbolicName();
					String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_CONCURRENT_STARTUP, new Object[] {Thread.currentThread().getName(), name, threadChangingState.getName(), bundleName, Long.toString(delay)});
					manager.getBaseData().getAdaptor().getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, new Exception(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_GENERATED_EXCEPTION), null));
				}
				return;
			}
		}

		//The bundle must be started.
		try {
			bundle.start();
		} catch (BundleException e) {
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_ACTIVATION, bundle.getSymbolicName(), Long.toString(bundle.getBundleId()));
			manager.getBaseData().getAdaptor().getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null));
			throw new ClassNotFoundException(name, e);
		}
		return;
	}

	public void postFindLocalClass(String name, Class clazz, ClasspathManager manager) {
		// do nothing
	}

	public void preFindLocalResource(String name, ClasspathManager manager) {
		// do nothing
	}

	public void postFindLocalResource(String name, URL resource, ClasspathManager manager) {
		// do nothing
	}

	public void recordClassDefine(String name, Class clazz, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) {
		// do nothing
	}

	private boolean shouldActivateFor(String className, BaseData bundledata) throws ClassNotFoundException {
		if (!isAutoStartable(className, bundledata))
			return false;
		//Don't reactivate on shut down
		if (bundledata.getAdaptor().isStopping()) {
			BundleStopper stopper = getBundleStopper(bundledata);
			if (stopper != null && stopper.isStopped(bundledata.getBundle())) {
				String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_ALREADY_STOPPED, className, bundledata.getSymbolicName());
				throw new ClassNotFoundException(message);
			}
		}
		return true;
	}

	private boolean isAutoStartable(String className, BaseData bundledata) {
		EclipseStorageHook storageHook = (EclipseStorageHook) bundledata.getStorageHook(EclipseStorageHook.KEY);
		if (storageHook == null)
			return false;
		boolean autoStart = storageHook.isAutoStart();
		String[] autoStartExceptions = storageHook.getAutoStartExceptions();
		// no exceptions, it is easy to figure it out
		if (autoStartExceptions == null)
			return autoStart;
		// otherwise, we need to check if the package is in the exceptions list
		int dotPosition = className.lastIndexOf('.');
		// the class has no package name... no exceptions apply
		if (dotPosition == -1)
			return autoStart;
		String packageName = className.substring(0, dotPosition);
		// should activate if autoStart and package is not an exception, or if !autoStart and package is exception
		return autoStart ^ contains(autoStartExceptions, packageName);
	}

	private boolean contains(String[] array, String element) {
		for (int i = 0; i < array.length; i++)
			if (array[i].equals(element))
				return true;
		return false;
	}

	private BundleStopper getBundleStopper(BaseData bundledata) {
		AdaptorHook[] adaptorhooks = bundledata.getAdaptor().getHookRegistry().getAdaptorHooks();
		for (int i = 0; i < adaptorhooks.length; i++)
			if (adaptorhooks[i] instanceof EclipseAdaptorHook)
				return ((EclipseAdaptorHook) adaptorhooks[i]).getBundleStopper();
		return null;
	}

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addClassLoadingStatsHook(this);
	}
}
