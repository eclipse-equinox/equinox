/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.runtime.internal.adaptor;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Properties;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class EclipseErrorHandler implements AdaptorHook, HookConfigurator {
	// System property used to prevent VM exit when unexpected errors occur
	private static final String PROP_EXITONERROR = "eclipse.exitOnError"; //$NON-NLS-1$
	private BaseAdaptor adaptor;

	/**
	 * @throws BundleException  
	 */
	public void frameworkStart(BundleContext context) throws BundleException {
		// do nothing
	}

	/**
	 * @throws BundleException  
	 */
	public void frameworkStop(BundleContext context) throws BundleException {
		// do nothing
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

	private boolean isFatalException(Throwable error) {
		if (error instanceof VirtualMachineError) {
			return true;
		}
		if (error instanceof ThreadDeath) {
			return true;
		}
		return false;
	}

	public void handleRuntimeError(Throwable error) {
		// this is the important method to handle errors
		boolean exitOnError = false;
		try {
			// check the prop each time this happens (should NEVER happen!)
			exitOnError = Boolean.valueOf(FrameworkProperties.getProperty(EclipseErrorHandler.PROP_EXITONERROR, "true")).booleanValue(); //$NON-NLS-1$
			String message = EclipseAdaptorMsg.ECLIPSE_ADAPTOR_RUNTIME_ERROR;
			if (exitOnError && isFatalException(error))
				message += ' ' + EclipseAdaptorMsg.ECLIPSE_ADAPTOR_EXITING;
			FrameworkLogEntry logEntry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, error, null);
			adaptor.getFrameworkLog().log(logEntry);
		} catch (Throwable t) {
			// we may be in a currupted state and must be able to handle any
			// errors (ie OutOfMemoryError)
			// that may occur when handling the first error; this is REALLY the
			// last resort.
			try {
				error.printStackTrace();
				t.printStackTrace();
			} catch (Throwable t1) {
				// if we fail that then we are beyond help.
			}
		} finally {
			// do the exit outside the try block just incase another runtime
			// error was thrown while logging
			if (exitOnError && isFatalException(error))
				System.exit(13);
		}
	}

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addAdaptorHook(this);
	}

	public FrameworkLog createFrameworkLog() {
		// do nothing
		return null;
	}

	public void initialize(BaseAdaptor initAdaptor) {
		this.adaptor = initAdaptor;
	}
}
