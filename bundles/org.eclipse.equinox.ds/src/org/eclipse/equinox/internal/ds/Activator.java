/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.internal.util.ref.Log;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This is the main starting class for the Service Component Runtime.
 * 
 * It also acts as a "Bundle Manager" - a listener for bundle events. Whenever a
 * bundle is stopped or started it will invoke the resolver to respectively
 * enable or disable necessary service components.
 * 
 * Notice, the SynchronousBundleListener bundle listeners are called prior
 * bundle event is completed. This will make the stuff a little bit faster ;)
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */

public class Activator implements BundleActivator, SynchronousBundleListener {

	public static BundleContext bc = null;

	private ServiceTracker cmTracker;
	private ServiceRegistration cmTrackerReg;

	private ServiceTracker debugTracker = null;

	private SCRManager scrManager = null;

	private ServiceRegistration scrCommandProviderReg;
	private SCRCommandProvider scrCommandProvider;

	private boolean inited = false;

	public static Log log;
	public static boolean DEBUG;
	public static boolean PERF;
	public static boolean DBSTORE;
	public static boolean INSTANTIATE_ALL;
	public static boolean startup;

	static long time[] = null;

	public static void timeLog(String message) {
		time[1] = time[0];
		log.debug(message + String.valueOf((time[0] = System.currentTimeMillis()) - time[1]), null);
	}

	private void initSCR() {
		synchronized (this) {
			if (inited)
				return;
			inited = true;
		}

		boolean lazyIniting = false;
		if (startup && time == null) {
			long tmp = System.currentTimeMillis();
			time = new long[] {tmp, 0, tmp};
			lazyIniting = true;
			if (startup)
				timeLog("[BEGIN - lazy SCR init]"); //$NON-NLS-1$
		}

		WorkThread.IDLE_TIMEOUT = getInteger("equinox.ds.idle_timeout", 1000); //$NON-NLS-1$
		WorkThread.BLOCK_TIMEOUT = getInteger("equinox.ds.block_timeout", 30000); //$NON-NLS-1$

		// start the config tracker
		cmTracker = new ServiceTracker(bc, ConfigurationAdmin.class.getName(), null);

		ConfigurationManager.cmTracker = cmTracker;
		cmTracker.open();
		if (startup)
			timeLog("ServiceTracker starting took "); //$NON-NLS-1$

		scrManager = new SCRManager(bc, log);
		if (startup)
			timeLog("SCRManager instantiation took "); //$NON-NLS-1$

		// add the configuration listener - we to receive CM events to restart
		// components
		cmTrackerReg = bc.registerService(ConfigurationListener.class.getName(), scrManager, null);
		if (startup)
			timeLog("ConfigurationListener service registered for "); //$NON-NLS-1$
		bc.addServiceListener(scrManager);

		scrManager.startIt();
		if (Activator.startup)
			Activator.timeLog("startIt() method took "); //$NON-NLS-1$

		if (scrCommandProvider == null) {
			scrCommandProvider = new SCRCommandProvider(scrManager);
			Hashtable reg_props = new Hashtable(3, 1);
			scrCommandProviderReg = bc.registerService(CommandProvider.class.getName(), scrCommandProvider, reg_props);
		}

		if (startup && lazyIniting) {
			log.debug("[END - lazy SCR init] Activator.initSCR() method executed for " + String.valueOf(time[0] - time[2]), null); //$NON-NLS-1$
			time = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		Activator.bc = bc;
		startup = getBoolean("equinox.measurements.bundles", false); //$NON-NLS-1$
		if (startup) {
			long tmp = System.currentTimeMillis();
			time = new long[] {tmp, 0, tmp};
		}
		// initialize the logging routines
		debugTracker = new ServiceTracker(bc, DebugOptions.class.getName(), null);
		debugTracker.open();
		log = new Log(bc, false);
		DEBUG = getBooleanDebugOption("org.eclipse.equinox.ds/debug", false) || getBoolean("equinox.ds.debug", false); //$NON-NLS-1$ //$NON-NLS-2$
		PERF = getBooleanDebugOption("org.eclipse.equinox.ds/performance", false) || getBoolean("equinox.ds.perf", false); //$NON-NLS-1$ //$NON-NLS-2$
		INSTANTIATE_ALL = getBooleanDebugOption("org.eclipse.equinox.ds/instantiate_all", false) || getBoolean("equinox.ds.instantiate_all", false); //$NON-NLS-1$ //$NON-NLS-2$

		DBSTORE = getBooleanDebugOption("org.eclipse.equinox.ds/cache_descriptions", true) || getBoolean("equinox.ds.dbstore", true); //$NON-NLS-1$ //$NON-NLS-2$
		log.setDebug(DEBUG);
		boolean print = getBooleanDebugOption("org.eclipse.equinox.ds/print_on_console", false) || getBoolean("equinox.ds.print", false); //$NON-NLS-1$ //$NON-NLS-2$
		log.setPrintOnConsole(print);

		if (startup)
			timeLog("[BEGIN - start method] Creating Log instance and initializing log system took "); //$NON-NLS-1$

		boolean hasHeaders = false;
		Bundle[] allBundles = bc.getBundles();
		for (int i = 0; i < allBundles.length; i++) {
			Dictionary allHeaders = allBundles[i].getHeaders(""); //$NON-NLS-1$
			if (allHeaders.get(ComponentConstants.SERVICE_COMPONENT) != null) {
				hasHeaders = true;
				break;
			}
		}

		if (hasHeaders) {
			initSCR();
		} else {
			// there are no bundles holding components - SCR will not be
			// initialized yet
			bc.addBundleListener(this);
		}
		if (startup) {
			log.debug("[END - start method] Activator.start() method executed for " + String.valueOf(time[0] - time[2]), null); //$NON-NLS-1$
			time = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) throws Exception {
		if (scrManager != null) {
			scrManager.stopIt();
			bc.removeServiceListener(scrManager);
		}
		// dispose the CM Listener
		if (cmTrackerReg != null) {
			cmTrackerReg.unregister();
		}

		if (scrCommandProviderReg != null)
			scrCommandProviderReg.unregister();
		scrCommandProvider = null;

		if (scrManager != null) {
			bc.removeBundleListener(scrManager);
		} else {
			bc.removeBundleListener(this);
		}

		// untrack the cm!
		if (cmTracker != null) {
			ConfigurationManager.cmTracker = null;
			cmTracker.close();
			cmTracker = null;
		}

		if (debugTracker != null) {
			debugTracker.close();
			debugTracker = null;
		}

		log.close();
		log = null;
		bc = null;
	}

	public static Filter createFilter(String filter) throws InvalidSyntaxException {
		return bc.createFilter(filter);
	}

	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.STARTED || event.getType() == BundleEvent.LAZY_ACTIVATION) {
			Dictionary allHeaders = event.getBundle().getHeaders(""); //$NON-NLS-1$ 
			if ((allHeaders.get(ComponentConstants.SERVICE_COMPONENT)) != null) {
				// The bundle is holding components - activate scr
				bc.removeBundleListener(this);
				initSCR();
			}
		}
	}

	public static boolean getBoolean(String property, boolean defaultValue) {
		String prop = (bc != null) ? bc.getProperty(property) : System.getProperty(property);
		if (prop != null) {
			return prop.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
		return defaultValue;
	}

	public static boolean getBoolean(String property) {
		return getBoolean(property, false);
	}

	public static int getInteger(String property, int defaultValue) {
		String prop = (bc != null) ? bc.getProperty(property) : System.getProperty(property);
		if (prop != null) {
			try {
				return Integer.decode(prop).intValue();
			} catch (NumberFormatException e) {
				//do nothing
			}
		}
		return defaultValue;
	}

	public boolean getBooleanDebugOption(String option, boolean defaultValue) {
		if (debugTracker == null) {
			return defaultValue;
		}
		DebugOptions options = (DebugOptions) debugTracker.getService();
		if (options != null) {
			String value = options.getOption(option);
			if (value != null)
				return value.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
		return defaultValue;
	}

	public static void log(BundleContext bundleContext, int level, String message, Throwable t) {
		LogService logService = null;
		ServiceReference logRef = null;
		try {
			logRef = bundleContext.getServiceReference(LogService.class.getName());
			logService = (LogService) bundleContext.getService(logRef);
		} catch (Exception e) {
			if (Activator.DEBUG) {
				log.debug(NLS.bind(Messages.CANNOT_GET_LOGSERVICE, bundleContext.getBundle().getSymbolicName()), e);
			}
		}
		if (logService != null) {
			logService.log(level, message, t);
			bundleContext.ungetService(logRef);
			if (log.getPrintOnConsole()) {
				String prefix = ""; //$NON-NLS-1$
				switch (level) {
					case LogService.LOG_ERROR :
						prefix = "ERROR "; //$NON-NLS-1$
						break;
					case LogService.LOG_WARNING :
						prefix = "WARNING "; //$NON-NLS-1$
						break;
					case LogService.LOG_INFO :
						prefix = "INFO "; //$NON-NLS-1$
						break;
				}
				dumpOnConsole(prefix, bundleContext, message, t);
			}
		} else {
			//using the SCR log
			switch (level) {
				case LogService.LOG_ERROR :
					log.error(message, t);
					break;
				case LogService.LOG_WARNING :
					log.warning(message, t);
					break;
				default :
					log.error(message, t);
					break;
			}
		}
	}

	private static void dumpOnConsole(String prefix, BundleContext bundleContext, String msg, Throwable t) {
		System.out.println(prefix + bundleContext.getBundle().getBundleId() + " " + msg); //$NON-NLS-1$
		if (t != null) {
			t.printStackTrace();
		}
	}

}
