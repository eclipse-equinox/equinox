/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.stats;

import java.io.*;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.BundleWatcher;
import org.eclipse.osgi.framework.debug.DebugOptions;
import org.osgi.framework.Bundle;

public class StatsManager implements BundleWatcher {
	// This connect plugins and their info, and so allows to access the info without running through
	// the plugin registry. This map only contains activated plugins. The key is the plugin Id
	private Map plugins = new HashMap(20);
	private Stack activationStack = new Stack(); // a stack of the plugins being activated
	private static boolean booting = true; // the state of the platform. This value is changed by the InternalPlatform itself.

	private static StatsManager defaultInstance;

	public static boolean MONITOR_ACTIVATION = false;
	public static boolean MONITOR_CLASSES = false;
	public static boolean MONITOR_RESOURCES = false;
	public static String TRACE_FILENAME = "runtime.traces"; //$NON-NLS-1$
	public static String TRACE_FILTERS = "trace.properties"; //$NON-NLS-1$
	public static boolean TRACE_CLASSES = false;
	public static boolean TRACE_BUNDLES = false;
	public static final String FRAMEWORK_SYMBOLICNAME = "org.eclipse.osgi"; //$NON-NLS-1$

	//Option names for spies
	private static final String OPTION_MONITOR_ACTIVATION = FRAMEWORK_SYMBOLICNAME + "/monitor/activation"; //$NON-NLS-1$
	private static final String OPTION_MONITOR_CLASSES = FRAMEWORK_SYMBOLICNAME + "/monitor/classes"; //$NON-NLS-1$
	private static final String OPTION_MONITOR_RESOURCES = FRAMEWORK_SYMBOLICNAME + "/monitor/resources"; //$NON-NLS-1$
	private static final String OPTION_TRACE_BUNDLES = FRAMEWORK_SYMBOLICNAME + "/trace/activation"; //$NON-NLS-1$
	private static final String OPTION_TRACE_CLASSES = FRAMEWORK_SYMBOLICNAME + "/trace/classLoading"; //$NON-NLS-1$
	private static final String OPTION_TRACE_FILENAME = FRAMEWORK_SYMBOLICNAME + "/trace/filename"; //$NON-NLS-1$
	private static final String OPTION_TRACE_FILTERS = FRAMEWORK_SYMBOLICNAME + "/trace/filters"; //$NON-NLS-1$

	static {
		setDebugOptions();
	}

	public static StatsManager getDefault() {
		if (defaultInstance == null) {
			defaultInstance = new StatsManager();
			defaultInstance.initialize();
		}
		return defaultInstance;
	}

	public static void setDebugOptions() {
		DebugOptions options = DebugOptions.getDefault();
		// may be null if debugging is not enabled
		if (options == null)
			return;
		MONITOR_ACTIVATION = options.getBooleanOption(OPTION_MONITOR_ACTIVATION, false);
		MONITOR_CLASSES = options.getBooleanOption(OPTION_MONITOR_CLASSES, false);
		MONITOR_RESOURCES = options.getBooleanOption(OPTION_MONITOR_RESOURCES, false);
		TRACE_CLASSES = options.getBooleanOption(OPTION_TRACE_CLASSES, false);
		TRACE_BUNDLES = options.getBooleanOption(OPTION_TRACE_BUNDLES, false);
		TRACE_FILENAME = options.getOption(OPTION_TRACE_FILENAME, TRACE_FILENAME);
		TRACE_FILTERS = options.getOption(OPTION_TRACE_FILTERS, TRACE_FILTERS);
	}

	public static void doneBooting() {
		booting = false;
	}

	public static boolean isBooting() {
		return booting;
	}

	/**
	 * Returns the result of converting a list of comma-separated tokens into an array
	 * 
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
	public static String[] getArrayFromList(String prop) {
		if (prop == null || prop.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		Vector list = new Vector();
		StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.addElement(token);
		}
		return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[list.size()]);
	}

	private void initialize() {
		// add the system bundle
		BundleStats plugin = findPlugin("org.eclipse.osgi");
		plugin.setTimestamp(System.currentTimeMillis());
		plugin.setActivationOrder(plugins.size());
		plugin.setDuringStartup(booting);
	}

	public void startActivation(Bundle bundle) {
		// should be called from a synchronized location to protect against concurrent updates
		BundleStats plugin = findPlugin(bundle.getSymbolicName());
		plugin.setTimestamp(System.currentTimeMillis());
		plugin.setActivationOrder(plugins.size());
		plugin.setDuringStartup(booting);

		// set the parentage of activation
		if (activationStack.size() != 0) {
			BundleStats activatedBy = (BundleStats) activationStack.peek();
			activatedBy.activated(plugin);
			plugin.setActivatedBy(activatedBy);
		}
		activationStack.push(plugin);

		if (TRACE_BUNDLES == true) {
			traceActivate(bundle.getSymbolicName(), plugin);
		}
	}

	public void endActivation(Bundle pluginId) {
		// should be called from a synchronized location to protect against concurrent updates
		BundleStats plugin = (BundleStats) activationStack.pop();
		plugin.endActivation();
	}

	private void traceActivate(String id, BundleStats plugin) {
		try {
			PrintWriter output = new PrintWriter(new FileOutputStream(ClassloaderStats.traceFile.getAbsolutePath(), true));
			try {
				long startPosition = ClassloaderStats.traceFile.length();
				output.println("Activating plugin: " + id); //$NON-NLS-1$
				output.println("Plugin activation stack:"); //$NON-NLS-1$
				for (int i = activationStack.size() - 1; i >= 0; i--)
					output.println("\t" + ((BundleStats) activationStack.get(i)).getPluginId()); //$NON-NLS-1$
				output.println("Class loading stack:"); //$NON-NLS-1$
				Stack classStack = ClassloaderStats.getClassStack();
				for (int i = classStack.size() - 1; i >= 0; i--)
					output.println("\t" + ((ClassStats) classStack.get(i)).getClassName()); //$NON-NLS-1$
				output.println("Stack trace:"); //$NON-NLS-1$
				new Throwable().printStackTrace(output);
				plugin.setTraceStart(startPosition);
			} finally {
				output.close();
				plugin.setTraceEnd(ClassloaderStats.traceFile.length());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BundleStats findPlugin(String id) {
		BundleStats result = (BundleStats) plugins.get(id);
		try {
			if (result == null) {
				result = new BundleStats(id);
				plugins.put(id, result);
			}
		} catch (IllegalAccessError e) {
			e.printStackTrace();
		}
		return result;
	}

	public BundleStats[] getPlugins() {
		return (BundleStats[]) plugins.values().toArray(new BundleStats[plugins.size()]);
	}

	public BundleStats getPlugin(String id) {
		return (BundleStats) plugins.get(id);
	}

}