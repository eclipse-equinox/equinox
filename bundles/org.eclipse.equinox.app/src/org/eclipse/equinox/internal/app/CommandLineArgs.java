/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.app;

import org.eclipse.osgi.service.environment.EnvironmentInfo;

public class CommandLineArgs {
	// obsolete command line args
	private static final String NO_PACKAGE_PREFIXES = "-noPackagePrefixes"; //$NON-NLS-1$
	private static final String NO_UPDATE = "-noUpdate"; //$NON-NLS-1$
	private static final String BOOT = "-boot"; //$NON-NLS-1$
	private static final String CLASSLOADER_PROPERTIES = "-classloaderProperties"; //$NON-NLS-1$	
	private static final String PLUGINS = "-plugins"; //$NON-NLS-1$
	private static final String FIRST_USE = "-firstUse"; //$NON-NLS-1$
	private static final String NEW_UPDATES = "-newUpdates"; //$NON-NLS-1$
	private static final String UPDATE = "-update"; //$NON-NLS-1$
	private static final String PASSWORD = "-password"; //$NON-NLS-1$
	private static final String KEYRING = "-keyring"; //$NON-NLS-1$
	private static final String PLUGIN_CUSTOMIZATION = "-pluginCustomization"; //$NON-NLS-1$

	// supported command line args
	private static final String PRODUCT = "-product"; //$NON-NLS-1$
	private static final String FEATURE = "-feature"; //$NON-NLS-1$
	private static final String APPLICATION = "-application"; //$NON-NLS-1$	

	// Command line args as seen by the Eclipse runtime. allArgs does NOT
	// include args consumed by the underlying framework (e.g., OSGi)
	private static String[] appArgs = new String[0];
	private static String[] allArgs = new String[0];
	private static String product;
	private static String application;

	static String[] processCommandLine(EnvironmentInfo envInfo) {
		String[] args = envInfo.getNonFrameworkArgs();
		if (args == null)
			return args;
		if (args.length == 0)
			return args;
		allArgs = args;
		int[] configArgs = new int[args.length];
		//need to initialize the first element to something that could not be an index.
		configArgs[0] = -1;
		int configArgIndex = 0;
		for (int i = 0; i < args.length; i++) {
			boolean found = false;
			// check for args without parameters (i.e., a flag arg)

			// consume obsolete args for compatibility
			if (args[i].equalsIgnoreCase(CLASSLOADER_PROPERTIES))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(NO_PACKAGE_PREFIXES))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(PLUGINS))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(FIRST_USE))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(NO_UPDATE))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(NEW_UPDATES))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(UPDATE))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(BOOT))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(KEYRING))
				found = true; // ignored  
			if (args[i].equalsIgnoreCase(PASSWORD))
				found = true; // ignored
			if (args[i].equalsIgnoreCase(PLUGIN_CUSTOMIZATION))
				found = true; // ignored

			// done checking obsolete for args.  Remember where an arg was found 
			if (found) {
				configArgs[configArgIndex++] = i;
				// check if the obsolete arg had a second param
				if (i < (args.length - 1) && !args[i + 1].startsWith("-")) //$NON-NLS-1$
					configArgs[configArgIndex++] = ++i;
				continue;
			}

			// check for args with parameters
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			// look for the product to run
			// treat -feature as a synonym for -product for compatibility.
			if (args[i - 1].equalsIgnoreCase(PRODUCT) || args[i - 1].equalsIgnoreCase(FEATURE)) {
				product = arg;
				envInfo.setProperty(EclipseAppContainer.PROP_PRODUCT, product);
				found = true;
			}

			// look for the application to run.  
			if (args[i - 1].equalsIgnoreCase(APPLICATION)) {
				application = arg;
				envInfo.setProperty(EclipseAppContainer.PROP_ECLIPSE_APPLICATION, application);
				found = true;
			}

			// done checking for args.  Remember where an arg was found 
			if (found) {
				configArgs[configArgIndex++] = i - 1;
				configArgs[configArgIndex++] = i;
			}
		}

		// remove all the arguments consumed by this argument parsing
		if (configArgIndex == 0) {
			appArgs = args;
			return args;
		}
		appArgs = new String[args.length - configArgIndex];
		configArgIndex = 0;
		int j = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == configArgs[configArgIndex])
				configArgIndex++;
			else
				appArgs[j++] = args[i];
		}
		return appArgs;
	}

	static String getApplication() {
		return application;
	}

	static String getProduct() {
		return product;
	}

	public static String[] getApplicationArgs() {
		return appArgs;
	}

	public static String[] getAllArgs() {
		return allArgs;
	}
}
