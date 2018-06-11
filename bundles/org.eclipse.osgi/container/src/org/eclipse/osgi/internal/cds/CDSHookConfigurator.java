/***********************************************************************
 * IBM Confidential 
 * OCO Source Materials
 *
 * (C) Copyright IBM Corp. 2006
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 ************************************************************************/

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
