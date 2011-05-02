/*******************************************************************************
 * Copyright (c) 1997, 2010 by ProSyst Software GmbH
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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.felix.scr.ScrService;
import org.eclipse.equinox.internal.util.ref.Log;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This is the main starting class for the Service Component Runtime.
 * The SCR is not fully initialized until it detects at least one bundle providing DS components. 
 * Thus it has considerably small startup time and does improve a little the runtime performance 
 * since it does not listen for service events.
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */

public class Activator implements BundleActivator, SynchronousBundleListener, ServiceListener {

	public static BundleContext bc = null;
	public static ConfigurationAdmin configAdmin = null;
	public static boolean security = false;

	private ServiceRegistration configListenerReg;
	private SCRManager scrManager = null;
	public ScrServiceImpl scrService = null;
	private ServiceRegistration scrServiceReg;
	private ServiceRegistration scrCommandProviderReg;
	private static FrameworkLog fwLog;
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

		try {
			bc.addServiceListener(this, "(objectClass=" + ConfigurationAdmin.class.getName() + ')'); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			//should never happen
		}
		//get config admin service if available
		ServiceReference caRef = bc.getServiceReference(ConfigurationAdmin.class.getName());
		if (caRef != null) {
			configAdmin = (ConfigurationAdmin) bc.getService(caRef);
		}
		if (startup)
			timeLog("ConfigurationAdmin service getting took "); //$NON-NLS-1$

		scrManager = new SCRManager();
		if (startup)
			timeLog("SCRManager instantiation took "); //$NON-NLS-1$

		// add the configuration listener - we to receive CM events to restart
		// components
		configListenerReg = bc.registerService(ConfigurationListener.class.getName(), scrManager, null);
		if (startup)
			timeLog("ConfigurationListener service registered for "); //$NON-NLS-1$
		bc.addServiceListener(scrManager);

		scrManager.startIt();
		if (Activator.startup)
			Activator.timeLog("startIt() method took "); //$NON-NLS-1$

		installCommandProvider();

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
	public void start(BundleContext bundleContext) throws Exception {
		Activator.bc = bundleContext;
		startup = getBoolean("equinox.measurements.bundles", false); //$NON-NLS-1$
		if (startup) {
			long tmp = System.currentTimeMillis();
			time = new long[] {tmp, 0, tmp};
		}
		// initialize the logging routines
		log = new Log(bundleContext, false);
		ServiceTracker debugTracker = new ServiceTracker(bundleContext, DebugOptions.class.getName(), null);
		debugTracker.open();
		DebugOptions debugOptions = (DebugOptions) debugTracker.getService();
		DEBUG = getBooleanDebugOption(debugOptions, "org.eclipse.equinox.ds/debug", false) || getBoolean("equinox.ds.debug", false); //$NON-NLS-1$ //$NON-NLS-2$
		PERF = getBooleanDebugOption(debugOptions, "org.eclipse.equinox.ds/performance", false) || getBoolean("equinox.ds.perf", false); //$NON-NLS-1$ //$NON-NLS-2$
		INSTANTIATE_ALL = getBooleanDebugOption(debugOptions, "org.eclipse.equinox.ds/instantiate_all", false) || getBoolean("equinox.ds.instantiate_all", false); //$NON-NLS-1$ //$NON-NLS-2$

		DBSTORE = getBooleanDebugOption(debugOptions, "org.eclipse.equinox.ds/cache_descriptions", true) || getBoolean("equinox.ds.dbstore", true); //$NON-NLS-1$ //$NON-NLS-2$
		boolean print = getBooleanDebugOption(debugOptions, "org.eclipse.equinox.ds/print_on_console", false) || getBoolean("equinox.ds.print", false); //$NON-NLS-1$ //$NON-NLS-2$
		log.setDebug(DEBUG);
		log.setPrintOnConsole(print);
		//DebugOptions no longer needed
		debugTracker.close();
		ServiceReference fwRef = bc.getServiceReference(FrameworkLog.class.getName());
		if (fwRef != null) {
			fwLog = (FrameworkLog) bc.getService(fwRef);
		}

		if (startup)
			timeLog("[BEGIN - start method] Creating Log instance and initializing log system took "); //$NON-NLS-1$

		security = Log.security();
		boolean hasHeaders = false;
		Bundle[] allBundles = bundleContext.getBundles();
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
			bundleContext.addBundleListener(this);
		}
		ServiceReference envInfoRef = bc.getServiceReference(EnvironmentInfo.class.getName());
		EnvironmentInfo envInfo = null;
		if (envInfoRef != null) {
			envInfo = (EnvironmentInfo) bc.getService(envInfoRef);
		}
		if (envInfo != null) {
			envInfo.setProperty("equinox.use.ds", "true"); //$NON-NLS-1$//$NON-NLS-2$
			bc.ungetService(envInfoRef);
		} else {
			System.setProperty("equinox.use.ds", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		scrService = new ScrServiceImpl();
		scrServiceReg = bc.registerService(ScrService.class.getName(), scrService, null);

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
	public void stop(BundleContext bundleContext) throws Exception {
		if (scrManager != null) {
			scrManager.stopIt();
			bundleContext.removeServiceListener(scrManager);
		}
		// dispose the CM Listener
		if (configListenerReg != null) {
			configListenerReg.unregister();
		}
		if (scrService != null) {
			scrService.dispose();
			scrServiceReg.unregister();
		}

		if (scrCommandProviderReg != null)
			scrCommandProviderReg.unregister();

		if (scrManager != null) {
			bundleContext.removeBundleListener(scrManager);
		} else {
			bundleContext.removeBundleListener(this);
		}
		ServiceReference envInfoRef = bc.getServiceReference(EnvironmentInfo.class.getName());
		EnvironmentInfo envInfo = null;
		if (envInfoRef != null) {
			envInfo = (EnvironmentInfo) bc.getService(envInfoRef);
		}
		if (envInfo != null) {
			envInfo.setProperty("equinox.use.ds", "false"); //$NON-NLS-1$//$NON-NLS-2$
			bc.ungetService(envInfoRef);
		} else {
			System.setProperty("equinox.use.ds", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		log.close();
		log = null;
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

	public static Configuration getConfiguration(String pid) throws IOException {
		if (configAdmin != null) {
			return configAdmin.getConfiguration(pid);
		}
		return null;
	}

	public static Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
		if (configAdmin != null) {
			return configAdmin.listConfigurations(filter);
		}
		return null;
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

	public boolean getBooleanDebugOption(DebugOptions optionsService, String option, boolean defaultValue) {
		if (optionsService != null) {
			String value = optionsService.getOption(option);
			if (value != null)
				return value.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
		return defaultValue;
	}

	private void installCommandProvider() {
		try {
			SCRCommandProvider scrCommandProvider = new SCRCommandProvider(scrManager);
			Hashtable reg_props = new Hashtable(1, 1);
			reg_props.put(org.osgi.framework.Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
			scrCommandProviderReg = bc.registerService(org.eclipse.osgi.framework.console.CommandProvider.class.getName(), scrCommandProvider, reg_props);
		} catch (NoClassDefFoundError e) {
			//the org.eclipse.osgi.framework.console package is optional 
			if (Activator.DEBUG) {
				log.debug("Cannot register SCR CommandProvider!", e); //$NON-NLS-1$
			}
		}
	}

	public static void log(BundleContext bundleContext, int level, String message, Throwable t) {
		LogService logService = null;
		ServiceReference logRef = null;
		if (bundleContext != null) {
			try {
				logRef = bundleContext.getServiceReference(LogService.class.getName());
				if (logRef != null) {
					logService = (LogService) bundleContext.getService(logRef);
				}
			} catch (Exception e) {
				if (Activator.DEBUG) {
					log.debug("Cannot get LogService for bundle " + bundleContext.getBundle().getSymbolicName(), e); //$NON-NLS-1$
				}
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
				dumpOnConsole(prefix, bundleContext, message, t, level == LogService.LOG_ERROR);
			}
		} else {
			logRef = bc.getServiceReference(LogService.class.getName());
			if (logRef == null) {
				//log service is not available
				if (!log.getPrintOnConsole() && !log.autoPrintOnConsole && fwLog == null) {
					//The log will not print the message on the console and the FrameworkLog service is not available
					//Will print errors on the console as last resort
					if (level == LogService.LOG_ERROR) {
						dumpOnConsole("ERROR ", bundleContext, message, t, true); //$NON-NLS-1$
					}
				}
			}

			//using the SCR log
			switch (level) {
				case LogService.LOG_ERROR :
					log.error(message, t);
					break;
				case LogService.LOG_WARNING :
					log.warning(message, t);
					break;
				case LogService.LOG_INFO :
					log.info(message);
					break;
				default :
					log.debug(message, t);
					break;
			}
		}
		if (fwLog != null) {
			logToFWLog(bundleContext != null ? bundleContext.getBundle().getSymbolicName() : bc.getBundle().getSymbolicName(), level, message, t);
		}
	}

	private static void dumpOnConsole(String prefix, BundleContext bundleContext, String msg, Throwable t, boolean printInErr) {
		String message = prefix + bundleContext.getBundle().getBundleId() + " " + msg; //$NON-NLS-1$
		if (printInErr) {
			System.err.println(message);
		} else {
			System.out.println(message);
		}
		if (t != null) {
			t.printStackTrace();
		}
	}

	private static void logToFWLog(String bsn, int level, String message, Throwable t) {
		int severity = FrameworkLogEntry.INFO;
		switch (level) {
			case LogService.LOG_ERROR :
				severity = FrameworkLogEntry.ERROR;
				break;
			case LogService.LOG_WARNING :
				severity = FrameworkLogEntry.WARNING;
				break;
			case LogService.LOG_INFO :
				severity = FrameworkLogEntry.INFO;
				break;
			case LogService.LOG_DEBUG :
				severity = FrameworkLogEntry.INFO;
				break;
		}
		fwLog.log(new FrameworkLogEntry(bsn, severity, 0, message, 0, t, null));
	}

	public void serviceChanged(ServiceEvent event) {
		switch (event.getType()) {
			case ServiceEvent.REGISTERED :
				Object caService = bc.getService(event.getServiceReference());
				configAdmin = (ConfigurationAdmin) caService;
				if (caService != null) {
					// Config Admin registered
					if (scrManager != null) {
						scrManager.configAdminRegistered((ConfigurationAdmin) caService, event.getServiceReference());
					}
				}
				break;
			case ServiceEvent.UNREGISTERING :
				//get replacement config admin service if available
				ServiceReference caRef = bc.getServiceReference(ConfigurationAdmin.class.getName());
				if (caRef != null) {
					configAdmin = (ConfigurationAdmin) bc.getService(caRef);
				} else {
					configAdmin = null;
				}
				break;
		}
	}
}
