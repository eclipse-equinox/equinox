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

import java.util.ArrayList;
import org.eclipse.core.runtime.adaptor.EclipseAdaptor;

/**
 * Contains information about activated bundles and acts as the main 
 * entry point for logging plugin activity.
 */

public class BundleStats {
	public String pluginId;
	public int activationOrder;
	private long timestamp; //timeStamp at which this plugin has been activated
	private boolean duringStartup; // indicate if the plugin has been activated during startup
	private long startupTime; // the time took by the plugin to startup
	private long startupMethodTime; // the time took to run the startup method

	// Indicate the position of the activation trace in the file
	private long traceStart = -1;
	private long traceEnd = -1;

	//To keep plugins parentage
	private ArrayList pluginsActivated = new ArrayList(3); // TODO create lazily
	private BundleStats activatedBy = null;

	//	static {
	//		// activate the boot plugin manually since we do not control the classloader
	////		activateBootPlugin();
	//	}

	// hard code the starting of the boot plugin as it does not go through the normal sequence
	//	private static void activateBootPlugin() {
	//		BundleStats plugin = findPlugin(BootLoader.PI_BOOT);
	//		plugin.setTimestamp(System.currentTimeMillis());
	//		plugin.setActivationOrder(plugins.size());
	//	}

	// Get the pluginInfo if available, or create it.
	public BundleStats(String pluginId) {
		this.pluginId = pluginId;
		//		duringStartup = booting;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getActivationOrder() {
		return activationOrder;
	}

	protected void activated(BundleStats plugin) {
		pluginsActivated.add(plugin);
	}

	public BundleStats getActivatedBy() {
		return activatedBy;
	}

	public String getPluginId() {
		return pluginId;
	}

	public long getStartupTime() {
		return startupTime;
	}

	public long getStartupMethodTime() {
		return startupMethodTime;
	}

	public boolean isStartupPlugin() {
		return duringStartup;
	}

	public int getClassLoadCount() {
		if (!EclipseAdaptor.MONITOR_CLASSES)
			return 0;
		ClassloaderStats loader = ClassloaderStats.getLoader(pluginId);
		return loader == null ? 0 : loader.getClassLoadCount();
	}

	public long getClassLoadTime() {
		if (!EclipseAdaptor.MONITOR_CLASSES)
			return 0;
		ClassloaderStats loader = ClassloaderStats.getLoader(pluginId);
		return loader == null ? 0 : loader.getClassLoadTime();
	}

	public ArrayList getPluginsActivated() {
		return pluginsActivated;
	}

	public long getTraceStart() {
		return traceStart;
	}

	public long getTraceEnd() {
		return traceEnd;
	}

	protected void setTimestamp(long value) {
		timestamp = value;
	}

	protected void setActivationOrder(int value) {
		activationOrder = value;
	}

	protected void setTraceStart(long time) {
		traceStart = time;
	}

	protected static void setBooting(boolean boot) {
		//		booting = boot;
	}

	protected void endActivation() {
		startupTime = System.currentTimeMillis() - timestamp;
	}

	protected void setTraceEnd(long position) {
		traceEnd = position;
	}

	protected void setActivatedBy(BundleStats value) {
		activatedBy = value;
	}
}