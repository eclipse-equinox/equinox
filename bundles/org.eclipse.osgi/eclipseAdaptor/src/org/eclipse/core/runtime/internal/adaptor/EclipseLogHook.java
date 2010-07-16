/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
import org.eclipse.core.runtime.adaptor.*;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class EclipseLogHook implements HookConfigurator, AdaptorHook {
	// The eclipse log file extension */
	private static final String LOG_EXT = ".log"; //$NON-NLS-1$
	BaseAdaptor adaptor;

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addAdaptorHook(this);
	}

	public void initialize(BaseAdaptor initAdaptor) {
		this.adaptor = initAdaptor;
	}

	/**
	 * @throws BundleException  
	 */
	public void frameworkStart(BundleContext context) throws BundleException {
		AdaptorUtil.register(FrameworkLog.class.getName(), adaptor.getFrameworkLog(), context);
		registerPerformanceLog(context);
	}

	/**
	 * @throws BundleException  
	 */
	public void frameworkStop(BundleContext context) throws BundleException {
		// TODO should unregister service registered a frameworkStart
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
		// TODO Auto-generated method stub

	}

	public FrameworkLog createFrameworkLog() {
		FrameworkLog frameworkLog;
		String logFileProp = FrameworkProperties.getProperty(EclipseStarter.PROP_LOGFILE);
		if (logFileProp != null) {
			frameworkLog = new EclipseLog(new File(logFileProp));
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
				frameworkLog = new EclipseLog(logFile);
			} else
				frameworkLog = new EclipseLog();
		}
		if ("true".equals(FrameworkProperties.getProperty(EclipseStarter.PROP_CONSOLE_LOG))) //$NON-NLS-1$
			frameworkLog.setConsoleLog(true);
		return frameworkLog;
	}

	private void registerPerformanceLog(BundleContext context) {
		Object service = createPerformanceLog();
		String serviceName = FrameworkLog.class.getName();
		Hashtable serviceProperties = new Hashtable(7);
		Dictionary headers = context.getBundle().getHeaders();

		serviceProperties.put(Constants.SERVICE_VENDOR, headers.get(Constants.BUNDLE_VENDOR));
		serviceProperties.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
		serviceProperties.put(Constants.SERVICE_PID, context.getBundle().getBundleId() + '.' + service.getClass().getName());
		serviceProperties.put(FrameworkLog.SERVICE_PERFORMANCE, Boolean.TRUE.toString());

		context.registerService(serviceName, service, serviceProperties);
	}

	private FrameworkLog createPerformanceLog() {
		String logFileProp = FrameworkProperties.getProperty(EclipseStarter.PROP_LOGFILE);
		if (logFileProp != null) {
			int lastSlash = logFileProp.lastIndexOf(File.separatorChar);
			if (lastSlash > 0) {
				String logFile = logFileProp.substring(0, lastSlash + 1) + "performance.log"; //$NON-NLS-1$
				return new EclipseLog(new File(logFile));
			}
		}
		//if all else fails, write to std err
		return new EclipseLog(new PrintWriter(System.err));
	}
}
