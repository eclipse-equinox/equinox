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
import org.eclipse.core.runtime.adaptor.EclipseAdaptor;
import org.eclipse.osgi.framework.adaptor.IBundleStats;

public class StatsManager implements IBundleStats {
	// This connect plugins and their info, and so allows to access the info without running through
	// the plugin registry. This map only contains activated plugins. The key is the plugin Id
	private static Map plugins = new HashMap(20);
	private static Stack activationStack = new Stack(); 		// a stack of the plugins being activated
	private static boolean booting = true; 						// the state of the platform. This value is changed by the InternalPlatform itself.
	
	private static StatsManager defaultInstance;
	
	public static boolean MONITOR_BUNDLES = false;
	public static boolean MONITOR_CLASSES = false;
	public static String TRACE_FILENAME = null;
	
	public static StatsManager getDefault() {
		if (defaultInstance == null)
			defaultInstance = new StatsManager();
		return defaultInstance;
	}
	
	public void startActivation(String bundle) {
		// should be called from a synchronized location to protect against concurrent updates
		BundleStats plugin = findPlugin(bundle);
		plugin.setTimestamp(System.currentTimeMillis());
		plugin.setActivationOrder(plugins.size());

		// set the parentage of activation
		if (activationStack.size() != 0) {
			BundleStats activatedBy = (BundleStats) activationStack.peek();
			activatedBy.activated(plugin);
			plugin.setActivatedBy(activatedBy);
		}
		activationStack.push(plugin);

		if (EclipseAdaptor.TRACE_BUNDLES = true) {
			traceActivate(bundle, plugin);
		}
	}

	public void endActivation(String pluginId) {
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
				for (int i = activationStack.size() - 1; i >= 0 ; i--)
					output.println("\t" + ((BundleStats)activationStack.get(i)).getPluginId()); //$NON-NLS-1$
				output.println("Class loading stack:"); //$NON-NLS-1$
				Stack classStack = ClassloaderStats.getClassStack();
				for (int i = classStack.size() - 1; i >= 0 ; i--)
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
	
	private static BundleStats findPlugin(String id) {
		BundleStats result = (BundleStats)plugins.get(id);
		ClassLoader cl = BundleStats.class.getClassLoader();
		System.out.println(cl);
		System.out.println(StatsManager.class.getClassLoader());
		try {
		if (result == null) {
			result = new BundleStats(id);
			plugins.put(id, result);
		}} catch(IllegalAccessError e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public BundleStats[] getPlugins() {
		return (BundleStats[])plugins.values().toArray(new BundleStats[plugins.size()]);
	}

	public BundleStats getPlugin(String id) {
		return (BundleStats) plugins.get(id);
	}

}
