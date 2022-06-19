/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
package org.eclipse.core.internal.preferences;

import static java.util.function.Predicate.not;
import static org.eclipse.osgi.framework.util.Wirings.inState;

import org.eclipse.core.internal.preferences.exchange.ILegacyPreferences;
import org.eclipse.osgi.framework.util.Wirings;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class contains a set of helper OSGI methods for the Preferences plugin.
 * The closeServices() method should be called before the plugin is stopped.
 *
 * @since org.eclipse.equinox.preferences 3.2
 */
public class PreferencesOSGiUtils {
	private ServiceTracker<?, ILegacyPreferences> initTracker;
	private ServiceTracker<?, DebugOptions> debugTracker;
	private ServiceTracker<?, ?> configurationLocationTracker;
	private ServiceTracker<?, ?> instanceLocationTracker;

	private static final PreferencesOSGiUtils singleton = new PreferencesOSGiUtils();

	public static PreferencesOSGiUtils getDefault() {
		return singleton;
	}

	/**
	 * Private constructor to block instance creation.
	 */
	private PreferencesOSGiUtils() {
		super();
	}

	void openServices() {
		BundleContext context = Activator.getContext();
		if (context == null) {
			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL)
				PrefsMessages.message("PreferencesOSGiUtils called before plugin started"); //$NON-NLS-1$
			return;
		}

		initTracker = new ServiceTracker<>(context, ILegacyPreferences.class, null);
		initTracker.open(true);

		debugTracker = new ServiceTracker<>(context, DebugOptions.class, null);
		debugTracker.open();

		// locations

		Filter filter = null;
		try {
			filter = context.createFilter(Location.CONFIGURATION_FILTER);
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		configurationLocationTracker = new ServiceTracker<>(context, filter, null);
		configurationLocationTracker.open();

		try {
			filter = context.createFilter(Location.INSTANCE_FILTER);
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		instanceLocationTracker = new ServiceTracker<>(context, filter, null);
		instanceLocationTracker.open();
	}

	void closeServices() {
		if (initTracker != null) {
			initTracker.close();
			initTracker = null;
		}
		if (debugTracker != null) {
			debugTracker.close();
			debugTracker = null;
		}
		if (configurationLocationTracker != null) {
			configurationLocationTracker.close();
			configurationLocationTracker = null;
		}
		if (instanceLocationTracker != null) {
			instanceLocationTracker.close();
			instanceLocationTracker = null;
		}
	}

	public ILegacyPreferences getLegacyPreferences() {
		if (initTracker != null)
			return initTracker.getService();
		if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL)
			PrefsMessages.message("Legacy preference tracker is not set"); //$NON-NLS-1$
		return null;
	}

	public boolean getBooleanDebugOption(String option, boolean defaultValue) {
		if (debugTracker == null) {
			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL)
				PrefsMessages.message("Debug tracker is not set"); //$NON-NLS-1$
			return defaultValue;
		}
		DebugOptions options = debugTracker.getService();
		if (options != null) {
			String value = options.getOption(option);
			if (value != null)
				return value.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
		return defaultValue;
	}

	public Bundle getBundle(String bundleName) {
		return Wirings.getBundles(bundleName) //
				.filter(not(inState(Bundle.INSTALLED, Bundle.UNINSTALLED))) //
				.findFirst().orElse(null);
	}

	public Location getConfigurationLocation() {
		if (configurationLocationTracker != null)
			return (Location) configurationLocationTracker.getService();
		return null;
	}

	public Location getInstanceLocation() {
		if (instanceLocationTracker != null)
			return (Location) instanceLocationTracker.getService();
		return null;
	}
}
