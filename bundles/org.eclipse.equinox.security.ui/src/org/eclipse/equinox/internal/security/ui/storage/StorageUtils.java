/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.storage;

import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;

public class StorageUtils {

	static private final String JUNIT_APPS = "org.eclipse.pde.junit.runtime"; //$NON-NLS-1$

	/**
	 * Get the shell from an active window. If not found, returns null.
	 */
	static public Shell getShell() {
		if (PlatformUI.isWorkbenchRunning()) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null)
				return window.getShell();
		}
		return null;
	}

	/**
	 * Determines if it is a good idea to show UI prompts
	 */
	static public boolean showUI() {
		if (!PlatformUI.isWorkbenchRunning())
			return false;

		// This is a bit of a strange code that tries to see if we are running in a JUnit
		BundleContext context = Activator.getBundleContext();
		if (context == null)
			return false;
		String app = context.getProperty("eclipse.application"); //$NON-NLS-1$
		if (app != null && app.startsWith(JUNIT_APPS))
			return false;

		return true;
	}

}
