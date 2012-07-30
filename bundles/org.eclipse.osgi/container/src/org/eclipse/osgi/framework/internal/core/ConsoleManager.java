/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class ConsoleManager {

	public static final String PROP_CONSOLE = "osgi.console"; //$NON-NLS-1$
	public static final String CONSOLE_BUNDLE = "org.eclipse.equinox.console"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_ENABLED = "osgi.console.enable.builtin"; //$NON-NLS-1$

	private final BundleContext context;
	private final String consoleBundle;
	private final String consolePort;

	public ConsoleManager(BundleContext context, String consolePropValue) {
		String port = null;
		if (consolePropValue != null) {
			int index = consolePropValue.lastIndexOf(":"); //$NON-NLS-1$
			port = consolePropValue.substring(index + 1);
		}
		this.consolePort = port != null ? port.trim() : port;
		String enabled = FrameworkProperties.getProperty(PROP_CONSOLE_ENABLED, CONSOLE_BUNDLE);
		this.context = context;
		if (!"true".equals(enabled) || "none".equals(consolePort)) { //$NON-NLS-1$ //$NON-NLS-2$
			this.consoleBundle = "false".equals(enabled) ? CONSOLE_BUNDLE : enabled; //$NON-NLS-1$
			if (consolePort == null || consolePort.length() > 0) {
				// no -console was specified or it has specified none or a port for telnet;
				// need to make sure the gogo shell does not create an interactive console on standard in/out
				FrameworkProperties.setProperty("gosh.args", "--nointeractive"); //$NON-NLS-1$//$NON-NLS-2$
			} else {
				// Need to make sure we don't shutdown the framework if no console is around (bug 362412)
				FrameworkProperties.setProperty("gosh.args", "--noshutdown"); //$NON-NLS-1$//$NON-NLS-2$
			}
			return;
		}
		this.consoleBundle = "unknown"; //$NON-NLS-1$
	}

	public static ConsoleManager startConsole(BundleContext context) {
		ConsoleManager consoleManager = new ConsoleManager(context, FrameworkProperties.getProperty(PROP_CONSOLE));
		return consoleManager;
	}

	@SuppressWarnings("deprecation")
	public void checkForConsoleBundle() throws BundleException {
		if ("none".equals(consolePort)) //$NON-NLS-1$
			return;
		// otherwise we need to check for the equinox console bundle and start it
		ServiceReference<PackageAdmin> paRef = context.getServiceReference(PackageAdmin.class);
		PackageAdmin pa = paRef == null ? null : context.getService(paRef);
		Bundle[] consoles = pa.getBundles(consoleBundle, null);
		if (consoles == null || consoles.length == 0) {
			if (consolePort != null)
				throw new BundleException("Could not find bundle: " + consoleBundle, BundleException.UNSUPPORTED_OPERATION); //$NON-NLS-1$
			return;
		}
		try {
			consoles[0].start(Bundle.START_TRANSIENT);
		} catch (BundleException e) {
			throw new BundleException("Could not start bundle: " + consoleBundle, BundleException.UNSUPPORTED_OPERATION, e); //$NON-NLS-1$
		}
	}

	/**
	 *  Stops the OSGi Command console
	 *
	 */
	public void stopConsole() {
		// nothing
	}

}
