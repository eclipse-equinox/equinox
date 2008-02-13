/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
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
import org.eclipse.equinox.internal.util.ref.Log;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This is the main starting class for the Service Component Runtime.
 * 
 * It also acts as a "Bundle Manager" - a listener for bundle events. Whenever a
 * bundle is stopped or started it will invoke the resolver to respectively
 * enable or disable neccessary service components.
 * 
 * Notice, the SynchronousBundleListener bundle listeners are called prior
 * bundle event is completed. This will make the stuff a little bit faster ;)
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.1
 */

public class Activator implements BundleActivator, SynchronousBundleListener {

	public static BundleContext bc = null;

	private ServiceTracker cmTracker;
	private ServiceRegistration cmTrackerReg;

	private SCRManager scrManager = null;

	private boolean inited = false;

	public static Log log;
	public static boolean DEBUG;
	public static boolean PERF;
	public static boolean DBSTORE;
	public static boolean startup;

	static long time[] = null;

	public static void timeLog(int id) {
		time[1] = time[0];
		log.debug(0x0100, id, String.valueOf((time[0] = System.currentTimeMillis()) - time[1]), null, false, true);
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
				timeLog(114); /* 114 = "[BEGIN - lazy SCR init] " */
		}

		WorkThread.IDLE_TIMEOUT = getInteger("equinox.ds.idle_timeout", 1000);
		WorkThread.BLOCK_TIMEOUT = getInteger("equinox.ds.block_timeout", 30000);

		// start the config tracker
		cmTracker = new ServiceTracker(bc, ConfigurationAdmin.class.getName(), null);

		if (startup)
			timeLog(102);
		/*102 = "ConfigurationAdmin ServiceTracker instantiation took "*/

		ConfigurationManager.cmTracker = cmTracker;
		cmTracker.open();
		if (startup)
			timeLog(103); /* 103 = "ServiceTracker starting took " */

		scrManager = new SCRManager(bc, log);
		if (startup)
			timeLog(104); /* 104 = "SCRManager instantiation took " */

		// add the configuration listener - we to receive CM events to restart
		// components
		cmTrackerReg = bc.registerService(ConfigurationListener.class.getName(), scrManager, null);
		if (startup)
			timeLog(106); /*106 = "ConfigurationListener service registered for "*/
		bc.addServiceListener(scrManager);
		if (startup)
			timeLog(107); /* 107 = "addServiceListener() method took " */

		scrManager.startIt();
		if (Activator.startup)
			Activator.timeLog(113); /* 113 = "startIt() method took " */

		if (startup && lazyIniting) {
			/* 115 = "[END - lazy SCR init] Activator.initSCR() method executed for "*/
			log.debug(0x0100, 115, String.valueOf(time[0] - time[2]), null, false);
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
		startup = getBoolean("equinox.measurements.full") || getBoolean("equinox.measurements.bundles");
		if (startup) {
			long tmp = System.currentTimeMillis();
			time = new long[] {tmp, 0, tmp};
		}
		// initialize the logging routines
		log = new Log(bc, false);
		DEBUG = getBoolean("equinox.ds.debug");
		PERF = getBoolean("equinox.ds.perf");
		DBSTORE = getBoolean("equinox.ds.dbstore");
		log.setDebug(DEBUG);
		boolean print = getBoolean("equinox.ds.print");
		log.setPrintOnConsole(print);
		if (DEBUG) {
			log.setMaps(TracerMap.getMap(), TracerMap.getStarts());
		}

		if (startup)
			timeLog(100);
		/*
		 * 100 = "[BEGIN - start method] Creating Log
		 * instance and initializing log system took "
		 */

		boolean hasHeaders = false;
		Bundle[] allBundles = bc.getBundles();
		for (int i = 0; i < allBundles.length; i++) {
			Dictionary allHeaders = allBundles[i].getHeaders();
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
			log.debug(0x0100, 108, String.valueOf(time[0] - time[2]), null, false);
			time = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) throws Exception {
		log.info("Shutting down service component runtime!");
		if (scrManager != null) {
			bc.removeServiceListener(scrManager);
		}
		// dispose the CM Listener
		if (cmTrackerReg != null) {
			cmTrackerReg.unregister();
		}
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

		if (scrManager != null) {
			scrManager.stopIt();
		}
		log.close();
		log = null;
		bc = null;
	}

	public static Filter createFilter(String filter) throws InvalidSyntaxException {
		return bc.createFilter(filter);
	}

	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.STARTING) {
			Dictionary allHeaders = event.getBundle().getHeaders();
			if ((allHeaders.get(ComponentConstants.SERVICE_COMPONENT)) != null) {
				// The bundle is holding components - activate scr
				bc.removeBundleListener(this);
				initSCR();
			}
		}
	}

	public static boolean getBoolean(String property) {
		String prop = (bc != null) ? bc.getProperty(property) : System.getProperty(property);
		return ((prop != null) && prop.equalsIgnoreCase("true"));
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
}
