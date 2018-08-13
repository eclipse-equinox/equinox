/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corp. and others
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] http://openjdk.java.net/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *******************************************************************************/

package org.eclipse.osgi.internal.cds;

import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;

public class CDSHookConfigurator implements HookConfigurator {

	private static final String SUPPRESS_ERRORS = "j9.cds.suppresserrors"; //$NON-NLS-1$
	private static final String DISABLE_CDS = "j9.cds.disable"; //$NON-NLS-1$
	private static final String OLD_CDS_CONFIGURATOR = "com.ibm.cds.CDSHookConfigurator"; //$NON-NLS-1$
	private static final String J9_SHARED_CLASS_HELPER_CLASS = "com.ibm.oti.shared.SharedClassHelperFactory"; //$NON-NLS-1$

	public void addHooks(HookRegistry hookRegistry) {
		boolean disableCDS = "true".equals(hookRegistry.getConfiguration().getProperty(DISABLE_CDS)); //$NON-NLS-1$
		if (disableCDS) {
			return;
		}
		// check for the external com.ibm.cds system.bundle fragment
		try {
			Class.forName(OLD_CDS_CONFIGURATOR);
			// the old com.ibm.cds fragment is installed; disable build-in one
			return;
		} catch (ClassNotFoundException e) {
			// expected
		}
		try {
			Class.forName(J9_SHARED_CLASS_HELPER_CLASS);
		} catch (ClassNotFoundException e) {
			boolean reportErrors = "false".equals(hookRegistry.getConfiguration().getProperty(SUPPRESS_ERRORS)); //$NON-NLS-1$
			// not running on J9
			if (reportErrors) {
				System.err.println("The J9 Class Sharing Adaptor will not work in this configuration."); //$NON-NLS-1$
				System.err.println("You are not running on a J9 Java VM."); //$NON-NLS-1$
			}
			return;
		}

		new CDSHookImpls().registerHooks(hookRegistry);
	}

}
