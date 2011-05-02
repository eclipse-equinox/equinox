/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util;

import org.eclipse.equinox.internal.util.impl.tpt.threadpool.ThreadPoolFactoryImpl;
import org.eclipse.equinox.internal.util.impl.tpt.timer.TimerFactory;
import org.eclipse.equinox.internal.util.ref.Log;
import org.eclipse.equinox.internal.util.ref.TimerRef;
import org.eclipse.equinox.internal.util.threadpool.ThreadPoolFactory;
import org.eclipse.equinox.internal.util.threadpool.ThreadPoolManager;
import org.eclipse.equinox.internal.util.timer.Timer;
import org.osgi.framework.*;

/**
 * Bundle activator for the Utility Bundle. This class is responsible to the
 * registration of ProSyst utility services as Parser Service, Thread Pool
 * Service, etc.
 * 
 * @author Maria Ivanova
 * @author Daniel Yanev
 * @author Plamen K. Kosseff
 * @author Pavlin Dobrev
 * @version 1.0
 */
public class UtilActivator implements BundleActivator {

	public static ThreadPoolFactoryImpl thMan;
	private ServiceRegistration thManReg;

	public static TimerFactory timer;
	private ServiceRegistration timerReg;
	public static Log log;
	public static int debugLevel = 1;
	public static BundleContext bc;
	public static boolean LOG_DEBUG;
	public static boolean startup;

	static long time[] = null;

	static void timeLog(int id) {
		time[1] = time[0];
		log.debug(0, id, String.valueOf((time[0] = System.currentTimeMillis()) - time[1]), null, false, true);
	}

	public static long[] points = null;

	/**
	 * This is implementation of BundleActivator start method. Thsi method is
	 * responsible to the registering of the thread pool, system, pasrser and
	 * timer services. Also adds the framework and system pluggable commands to
	 * the parser service pluggable commands.
	 * 
	 * @param bc
	 *            The execution context of the bundle being started.
	 * @exception BundleException
	 *                If this method throws an exception, the bundle is marked
	 *                as stopped and the framework will remove the bundle's
	 *                listeners, unregister all service's registered by the
	 *                bundle, release all services used by the bundle.
	 */
	public void start(BundleContext bc) throws BundleException {
		UtilActivator.bc = bc;
		startup = getBoolean("equinox.measurements.bundles");
		if (startup) {
			long tmp = System.currentTimeMillis();
			time = new long[] {tmp, 0, tmp};
			points = new long[3];
		}

		UtilActivator.bc = bc;
		try {
			log = new Log(bc, false);
			LOG_DEBUG = getBoolean("equinox.putil.debug");
			log.setDebug(LOG_DEBUG);
			log.setPrintOnConsole(getBoolean("equinox.putil.console"));
			debugLevel = getInteger("equinox.putil.debug.level", 1);

			if (startup) {
				if (LOG_DEBUG)
					log.setMaps(TracerMap.getMap(), TracerMap.getStarts());

				/* 1 = "[BEGIN - start method]" */
				log.debug(0, 1, null, null, false, true);
				/* 1001 = "[BEGIN - Log instance] Loading referent classes took" */
				log.debug(0, 1001, String.valueOf(points[0] - time[0]), null, false, true);
				/* 101 = "Getting system props, bundle id and log service took " */
				log.debug(0, 101, String.valueOf(points[1] - points[0]), null, false, true);
				/* 102 = "Getting Trace Service took " */
				log.debug(0, 102, String.valueOf(points[2] - points[1]), null, false, true);
				time[1] = time[0];
				/* 3001 = "Loading tracer map took " */
				log.debug(0, 3001, String.valueOf((time[0] = System.currentTimeMillis()) - points[2]), null, false, true);
				/* 2001 = "[END - Log instance] Creating log instance took " */
				log.debug(0, 2001, String.valueOf(time[0] - time[1]), null, false, true);
			}

			String bundleName = ThreadPoolFactoryImpl.getName(bc.getBundle());
			thMan = new ThreadPoolFactoryImpl(bundleName, log);
			if (startup)
				timeLog(3); /* 3 = "Creating Thread Pool service took " */

			thManReg = bc.registerService(new String[] {ThreadPoolManager.class.getName(), ThreadPoolFactory.class.getName()}, thMan, null);
			if (startup)
				timeLog(4); /* 4 = "Registering Thread Pool service took " */

			timer = new TimerFactory(bundleName, thMan, log);
			if (startup)
				timeLog(33); /* 33 = "Creating Timer service took " */

			int i = getInteger("equinox.util.threadpool.inactiveTime", 30);
			timerReg = bc.registerService(Timer.class.getName(), timer, null);
			timer.addNotifyListener(ThreadPoolFactoryImpl.threadPool, Thread.NORM_PRIORITY, Timer.PERIODICAL_TIMER, (i * 1000L), 0);

			TimerRef.timer = timer;

			if (startup)
				timeLog(5); /* 5 = "Registering Timer service took " */

			if (startup) {

				/*
				 * 16 = "[END - start method] PutilActivator.start() method
				 * executed for "
				 */
				log.debug(0, 16, String.valueOf(time[0] - time[2]), null, false, true);
				time = points = null;
			}

		} catch (Throwable ee) {
			ee.printStackTrace();
			System.out.println("log1: " + log);
			log.error("[UtilActivator] An error has occurred while starting ProSyst Utility Bundle.", ee);
			throw new BundleException("Error while starting ProSyst Utililty Bundle!", ee);
		}
	}

	/**
	 * This is implementation of BundleActivator stop method. This method
	 * unregisteres the thread pool, system, parser and timer services.
	 * 
	 * @param bc
	 *            The execution context of the bundle being stopped.
	 * @exception BundleException
	 *                If this method throws an exception, the bundle is still
	 *                marked as stopped and the framework will remove the
	 *                bundle's listeners, unregister all service's registered by
	 *                the bundle, release all service's used by the bundle.
	 */
	public void stop(BundleContext bc) throws BundleException {
		try {
			thManReg.unregister();

			timerReg.unregister();
			TimerFactory.stopTimer();
			TimerRef.timer = timer = null;

			ThreadPoolFactoryImpl.stopThreadPool();
			thMan = null;
		} catch (Throwable e) {
			log.error("[UtilActivator] An error has occurred while stopping ProSyst Utility Bundle.", e);
			throw new BundleException("Error while stopping ProSyst Utililty Bundle!", e);
		} finally {
			log.close();
			log = null;
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
