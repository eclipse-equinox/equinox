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
package org.eclipse.equinox.internal.security.ui;

import java.util.Arrays;
import java.util.Hashtable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.security.ui.AuthorizationManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.internal.provisional.service.security.AuthorizationEngine;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

// TBD change to use standard NLS mechanism and then switch to implement BundleActivator
// TBD most of the strings in the message file are not used
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.equinox.security.ui"; //$NON-NLS-1$

	// constants for services
	private static final String PROP_TRUST_ENGINE = "osgi.signedcontent.trust.engine"; //$NON-NLS-1$
	private static final String PROP_AUTHZ_ENGINE = "osgi.signedcontent.authorization.engine"; //$NON-NLS-1$
	private static final String PROP_AUTHZ_MANAGER = "osgi.signedcontent.authorization.manager"; //$NON-NLS-1$

	private static final String PROP_DEFAULT_SERVICE = "org.eclipse.osgi"; //$NON-NLS-1$

	//service trackers
	private static ServiceTracker trustEngineTracker;
	private static ServiceTracker authzEngineTracker;
	private static ServiceTracker authzManagerTracker;
	private static ServiceTracker platformAdminTracker;
	private static ServiceTracker debugTracker;

	// The shared plug-in instance
	private static Activator plugin;

	// The bundle context
	private static BundleContext bundleContext;
	private ServiceRegistration defaultAuthzManagerReg;

	// debug tracing
	private static final String OPTION_DEBUG = "org.eclipse.equinox.security.ui/debug"; //$NON-NLS-1$;
	private static final String OPTION_DEBUG_STORAGE = OPTION_DEBUG + "/storage"; //$NON-NLS-1$;

	public Activator() {
		super();
	}

	/**
	 * Returns the bundle context.
	 */
	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns the symbolic name for this bundle.
	 */
	public static String getSymbolicName() {
		return plugin.getBundle().getSymbolicName();
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		bundleContext = context;
		plugin = this;

		// Register the default authorization manager
		Hashtable properties = new Hashtable(7);
		properties.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
		properties.put(PROP_AUTHZ_MANAGER, PROP_DEFAULT_SERVICE);
		defaultAuthzManagerReg = bundleContext.registerService(AuthorizationManager.class.getName(), new DefaultAuthorizationManager(), properties);
	}

	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		bundleContext = null;
		plugin = null;

		if (defaultAuthzManagerReg != null) {
			defaultAuthzManagerReg.unregister();
			defaultAuthzManagerReg = null;
		}

		if (authzEngineTracker != null) {
			authzEngineTracker.close();
			authzEngineTracker = null;
		}

		if (authzManagerTracker != null) {
			authzManagerTracker.close();
			authzManagerTracker = null;
		}

		if (platformAdminTracker != null) {
			platformAdminTracker.close();
			platformAdminTracker = null;
		}
		if (debugTracker != null) {
			debugTracker.close();
			debugTracker = null;
		}
	}

	public static TrustEngine[] getTrustEngines() {
		if (trustEngineTracker == null) {
			String trustAuthorityProp = Activator.getBundleContext().getProperty(PROP_TRUST_ENGINE);
			Filter filter = null;
			if (trustAuthorityProp != null)
				try {
					filter = FrameworkUtil.createFilter("(&(" + Constants.OBJECTCLASS + "=" + TrustEngine.class.getName() + ")(" + PROP_TRUST_ENGINE + "=" + trustAuthorityProp + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
				} catch (InvalidSyntaxException e) {
					e.printStackTrace();
					// do nothing just use no filter TODO we may want to log something
				}
			if (filter != null) {
				trustEngineTracker = new ServiceTracker(bundleContext, filter, null);
			} else
				trustEngineTracker = new ServiceTracker(bundleContext, TrustEngine.class.getName(), null);
			trustEngineTracker.open();
		}
		Object[] services = trustEngineTracker.getServices();
		if (services != null) {
			return (TrustEngine[]) Arrays.asList(services).toArray(new TrustEngine[] {});
		}
		return new TrustEngine[0];
	}

	public static AuthorizationEngine getAuthorizationEngine() {
		if (authzEngineTracker == null) {
			String implProp = Activator.getBundleContext().getProperty(PROP_AUTHZ_ENGINE);
			Filter filter = null;
			if (implProp != null)
				try {
					filter = FrameworkUtil.createFilter("(&(" + Constants.OBJECTCLASS + "=" + AuthorizationEngine.class.getName() + ")(" + PROP_AUTHZ_ENGINE + "=" + implProp + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
				} catch (InvalidSyntaxException e) {
					//TODO:log the error
				}
			if (filter != null) {
				authzEngineTracker = new ServiceTracker(Activator.getBundleContext(), filter, null);
			} else {
				authzEngineTracker = new ServiceTracker(Activator.getBundleContext(), AuthorizationEngine.class.getName(), null);
			}
			authzEngineTracker.open();
		}
		return (AuthorizationEngine) authzEngineTracker.getService();
	}

	public static AuthorizationManager getAuthorizationManager() {
		if (authzManagerTracker == null) {
			String implProp = Activator.getBundleContext().getProperty(PROP_AUTHZ_MANAGER);
			Filter filter = null;
			if (implProp != null)
				try {
					filter = FrameworkUtil.createFilter("(&(" + Constants.OBJECTCLASS + "=" + AuthorizationManager.class.getName() + ")(" + PROP_AUTHZ_MANAGER + "=" + implProp + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
				} catch (InvalidSyntaxException e) {
					//TODO:log the error
				}
			if (filter != null) {
				authzManagerTracker = new ServiceTracker(Activator.getBundleContext(), filter, null);
			} else {
				authzManagerTracker = new ServiceTracker(Activator.getBundleContext(), AuthorizationManager.class.getName(), null);
			}
			authzManagerTracker.open();
		}
		return (AuthorizationManager) authzManagerTracker.getService();
	}

	public static PlatformAdmin getPlatformAdmin() {
		if (platformAdminTracker == null) {
			platformAdminTracker = new ServiceTracker(Activator.getBundleContext(), PlatformAdmin.class.getName(), null);
			platformAdminTracker.open();
		}
		return (PlatformAdmin) platformAdminTracker.getService();
	}

	/**
	 * Get the workbench image with the given path relative to ICON_PATH.
	 * @param relativePath
	 * @return ImageDescriptor
	 */
	public static ImageDescriptor getImageDescriptor(String relativePath) {
		return imageDescriptorFromPlugin("org.eclipse.equinox.security.ui", "icons" + relativePath); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Logs a message.
	 * @param severity  Either IStatus.INFO, IStatus.WARNING or IStatus.ERROR.
	 * @param key       The key of the translated message in the resource bundle.
	 * @param args      The arguments to pass to <code>MessageFormat.format</code>
	 *                  or <code>null</code> if the message has no arguments.
	 * @param throwable exception associated with this message or <code>null</code>.
	 */
	public static void log(int severity, String key, Object args[], Throwable throwable) {
		plugin.getLog().log(new Status(severity, getSymbolicName(), IStatus.OK, NLS.bind(key, args), throwable));
	}

	public DebugOptions getDebugOptions() {
		if (debugTracker == null) {
			debugTracker = new ServiceTracker(bundleContext, DebugOptions.class.getName(), null);
			debugTracker.open();
		}
		return (DebugOptions) debugTracker.getService();
	}

	public boolean debugStorageContents() {
		DebugOptions debugOptions = getDebugOptions();
		if (debugOptions == null)
			return false;
		return debugOptions.getBooleanOption(OPTION_DEBUG, false) && //
				debugOptions.getBooleanOption(OPTION_DEBUG_STORAGE, false);
	}

}
