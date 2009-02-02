/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.osgi.framework.debug.FrameworkDebugOptions;
import org.osgi.framework.*;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;

/**
 * This class activates the System Bundle.
 */

public class SystemBundleActivator implements BundleActivator {
	private BundleContext context;
	private InternalSystemBundle bundle;
	private Framework framework;
	private ServiceRegistration packageAdmin;
	private ServiceRegistration securityAdmin;
	private ServiceRegistration startLevel;
	private ServiceRegistration debugOptions;
	private ServiceRegistration contextFinder;

	public SystemBundleActivator() {
	}

	public void start(BundleContext context) throws Exception {
		this.context = context;
		bundle = (InternalSystemBundle) context.getBundle();
		framework = bundle.framework;

		if (framework.packageAdmin != null)
			packageAdmin = register(new String[] {Constants.OSGI_PACKAGEADMIN_NAME}, framework.packageAdmin, null);
		if (framework.securityAdmin != null)
			securityAdmin = register(new String[] {Constants.OSGI_PERMISSIONADMIN_NAME, ConditionalPermissionAdmin.class.getName()}, framework.securityAdmin, null);
		if (framework.startLevelManager != null)
			startLevel = register(new String[] {Constants.OSGI_STARTLEVEL_NAME}, framework.startLevelManager, null);
		FrameworkDebugOptions dbgOptions = null;
		if ((dbgOptions = FrameworkDebugOptions.getDefault()) != null)
			debugOptions = register(new String[] {org.eclipse.osgi.service.debug.DebugOptions.class.getName()}, dbgOptions, null);
		ClassLoader tccl = framework.getContextFinder();
		if (tccl != null) {
			Hashtable props = new Hashtable(7);
			props.put("equinox.classloader.type", "contextClassLoader"); //$NON-NLS-1$ //$NON-NLS-2$
			contextFinder = register(new String[] {ClassLoader.class.getName()}, tccl, props);
		}

		// Always call the adaptor.frameworkStart() at the end of this method.
		framework.adaptor.frameworkStart(context);
		// attempt to resolve all bundles
		// this is done after the adaptor.frameworkStart has been called
		// this should be the first time the resolver State is accessed
		framework.packageAdmin.setResolvedBundles(bundle);
		// reinitialize the system bundles localization to take into account system bundle fragments
		framework.systemBundle.manifestLocalization = null;
	}

	public void stop(BundleContext context) throws Exception {
		// Always call the adaptor.frameworkStop() at the begining of this method.
		framework.adaptor.frameworkStop(context);

		if (packageAdmin != null)
			packageAdmin.unregister();
		if (securityAdmin != null)
			securityAdmin.unregister();
		if (startLevel != null)
			startLevel.unregister();
		if (debugOptions != null)
			debugOptions.unregister();
		if (contextFinder != null)
			contextFinder.unregister();

		framework = null;
		bundle = null;
		this.context = null;
	}

	/**
	 * Register a service object.
	 *
	 */
	private ServiceRegistration register(String[] names, Object service, Hashtable properties) {
		if (properties == null)
			properties = new Hashtable(7);
		Dictionary headers = bundle.getHeaders();
		properties.put(Constants.SERVICE_VENDOR, headers.get(Constants.BUNDLE_VENDOR));
		properties.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		properties.put(Constants.SERVICE_PID, bundle.getBundleId() + "." + service.getClass().getName()); //$NON-NLS-1$
		return context.registerService(names, service, properties);
	}

}
