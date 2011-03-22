/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.runtime.internal.adaptor;

import java.io.*;
import java.net.URLConnection;
import java.util.*;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.equinox.log.internal.LogServiceManager;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;

public class EclipseLogHook implements HookConfigurator, AdaptorHook {
	static final String EQUINOX_LOGGER_NAME = "org.eclipse.equinox.logger"; //$NON-NLS-1$
	static final String PERF_LOGGER_NAME = "org.eclipse.performance.logger"; //$NON-NLS-1$
	private static final String PROP_LOG_ENABLED = "eclipse.log.enabled"; //$NON-NLS-1$

	// The eclipse log file extension */
	private static final String LOG_EXT = ".log"; //$NON-NLS-1$
	private final LogServiceManager logServiceManager;
	private final EclipseLogFactory eclipseLogFactory;
	private final EclipseLogWriter logWriter;
	private final EclipseLogWriter perfWriter;

	public EclipseLogHook() {
		String logFileProp = FrameworkProperties.getProperty(EclipseStarter.PROP_LOGFILE);
		boolean enabled = "true".equals(FrameworkProperties.getProperty(PROP_LOG_ENABLED, "true")); //$NON-NLS-1$ //$NON-NLS-2$
		if (logFileProp != null) {
			logWriter = new EclipseLogWriter(new File(logFileProp), EQUINOX_LOGGER_NAME, enabled);
		} else {
			Location location = LocationManager.getConfigurationLocation();
			File configAreaDirectory = null;
			if (location != null)
				// TODO assumes the URL is a file: url
				configAreaDirectory = new File(location.getURL().getFile());

			if (configAreaDirectory != null) {
				String logFileName = Long.toString(System.currentTimeMillis()) + EclipseLogHook.LOG_EXT;
				File logFile = new File(configAreaDirectory, logFileName);
				FrameworkProperties.setProperty(EclipseStarter.PROP_LOGFILE, logFile.getAbsolutePath());
				logWriter = new EclipseLogWriter(logFile, EQUINOX_LOGGER_NAME, enabled);
			} else
				logWriter = new EclipseLogWriter((Writer) null, EQUINOX_LOGGER_NAME, enabled);
		}

		File logFile = logWriter.getFile();
		if (logFile != null) {
			File perfLogFile = new File(logFile.getParentFile(), "performance.log"); //$NON-NLS-1$
			perfWriter = new EclipseLogWriter(perfLogFile, PERF_LOGGER_NAME, true);
		} else {
			perfWriter = new EclipseLogWriter((Writer) null, PERF_LOGGER_NAME, true);
		}
		if ("true".equals(FrameworkProperties.getProperty(EclipseStarter.PROP_CONSOLE_LOG))) //$NON-NLS-1$
			logWriter.setConsoleLog(true);
		logServiceManager = new LogServiceManager(logWriter, perfWriter);
		eclipseLogFactory = new EclipseLogFactory(logWriter, logServiceManager);

	}

	private ServiceRegistration<?> frameworkLogReg;
	private ServiceRegistration<?> perfLogReg;

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addAdaptorHook(this);
	}

	public void initialize(BaseAdaptor initAdaptor) {
		// Nothing
	}

	/**
	 * @throws BundleException  
	 */
	public void frameworkStart(BundleContext context) throws BundleException {
		logServiceManager.start(context);
		frameworkLogReg = AdaptorUtil.register(FrameworkLog.class.getName(), eclipseLogFactory, context);
		perfLogReg = registerPerformanceLog(context);
	}

	/**
	 * @throws BundleException  
	 */
	public void frameworkStop(BundleContext context) throws BundleException {
		frameworkLogReg.unregister();
		perfLogReg.unregister();
		logServiceManager.stop(context);
	}

	public void frameworkStopping(BundleContext context) {
		// do nothing

	}

	public void addProperties(Properties properties) {
		// do nothing
	}

	/**
	 * @throws IOException  
	 */
	public URLConnection mapLocationToURLConnection(String location) throws IOException {
		// do nothing
		return null;
	}

	public void handleRuntimeError(Throwable error) {
		// do nothing
	}

	public FrameworkLog createFrameworkLog() {
		return eclipseLogFactory.createFrameworkLog(null, logWriter);
	}

	private ServiceRegistration<?> registerPerformanceLog(BundleContext context) {
		Object service = createPerformanceLog(context.getBundle());
		String serviceName = FrameworkLog.class.getName();
		Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>(7);
		Dictionary<String, String> headers = context.getBundle().getHeaders();

		serviceProperties.put(Constants.SERVICE_VENDOR, headers.get(Constants.BUNDLE_VENDOR));
		serviceProperties.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
		serviceProperties.put(Constants.SERVICE_PID, context.getBundle().getBundleId() + '.' + service.getClass().getName());
		serviceProperties.put(FrameworkLog.SERVICE_PERFORMANCE, Boolean.TRUE.toString());

		return context.registerService(serviceName, service, serviceProperties);
	}

	private FrameworkLog createPerformanceLog(Bundle systemBundle) {
		return eclipseLogFactory.createFrameworkLog(systemBundle, perfWriter);
	}
}
