/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.internal.adaptor;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class ConsoleManager {

	public static final String PROP_CONSOLE = "osgi.console"; //$NON-NLS-1$
	public static final String CONSOLE_BUNDLE = "org.eclipse.equinox.console"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_ENABLED = "osgi.console.enable.builtin"; //$NON-NLS-1$

	private final BundleContext context;
	private final String consoleBundle;
	private final String consolePort;

	public ConsoleManager(BundleContext context, EquinoxConfiguration equinoxConfig) {
		String port = null;
		String consolePropValue = equinoxConfig.getConfiguration(PROP_CONSOLE);
		if (consolePropValue != null) {
			int index = consolePropValue.lastIndexOf(":"); //$NON-NLS-1$
			port = consolePropValue.substring(index + 1);
		}
		this.consolePort = port != null ? port.trim() : port;
		String enabled = equinoxConfig.getConfiguration(PROP_CONSOLE_ENABLED, CONSOLE_BUNDLE).trim();
		if ("true".equals(enabled) || "false".equals(enabled)) { //$NON-NLS-1$ //$NON-NLS-2$
			// Note the osgi.console.enable.builtin property is a legacy setting that would
			// enable the internal built-in console if set to true.
			// There no longer is any built-in console in the framework.
			// It makes no sense to support values true or false.
			// If "true" or "false" is used then we just default to the
			// equinox.console bundle.
			enabled = CONSOLE_BUNDLE;
		}
		this.consoleBundle = enabled;
		this.context = context;
	}

	public static ConsoleManager startConsole(BundleContext context, EquinoxConfiguration equinoxConfig) {
		ConsoleManager consoleManager = new ConsoleManager(context, equinoxConfig);
		return consoleManager;
	}

	@SuppressWarnings("deprecation")
	public void checkForConsoleBundle() throws BundleException {
		if ("none".equals(consolePort)) //$NON-NLS-1$
			return;
		// otherwise we need to check for the equinox console bundle and start it
		ServiceReference<org.osgi.service.packageadmin.PackageAdmin> paRef = context
				.getServiceReference(org.osgi.service.packageadmin.PackageAdmin.class);
		org.osgi.service.packageadmin.PackageAdmin pa = paRef == null ? null : context.getService(paRef);
		Bundle[] consoles = pa.getBundles(consoleBundle, null);
		if (consoles == null || consoles.length == 0) {
			if (consolePort != null)
				throw new BundleException("Could not find bundle: " + consoleBundle, //$NON-NLS-1$
						BundleException.UNSUPPORTED_OPERATION);
			return;
		}
		try {
			consoles[0].start(Bundle.START_TRANSIENT);
		} catch (BundleException e) {
			throw new BundleException("Could not start bundle: " + consoleBundle, BundleException.UNSUPPORTED_OPERATION, //$NON-NLS-1$
					e);
		}
	}

	/**
	 * Stops the OSGi Command console
	 */
	public void stopConsole() {
		// nothing
	}

}
