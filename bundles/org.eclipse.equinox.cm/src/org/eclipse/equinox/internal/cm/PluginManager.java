/*******************************************************************************
 * Copyright (c) 2005, 2008 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * PluginManager tracks and allows customization via ConfigurationPlugin  
 */
public class PluginManager {
	private final PluginTracker pluginTracker;

	public PluginManager(BundleContext context) {
		pluginTracker = new PluginTracker(context);
	}

	public void start() {
		pluginTracker.open();
	}

	public void stop() {
		pluginTracker.close();
	}

	public void modifyConfiguration(ServiceReference managedReference, Dictionary properties) {
		if (properties == null)
			return;

		ServiceReference[] references = pluginTracker.getServiceReferences();
		for (int i = 0; i < references.length; ++i) {
			String[] pids = (String[]) references[i].getProperty(ConfigurationPlugin.CM_TARGET);
			if (pids != null) {
				String pid = (String) properties.get(Constants.SERVICE_PID);
				if (!Arrays.asList(pids).contains(pid))
					continue;
			}
			ConfigurationPlugin plugin = (ConfigurationPlugin) pluginTracker.getService(references[i]);
			if (plugin != null)
				plugin.modifyConfiguration(managedReference, properties);
		}
	}

	private static class PluginTracker extends ServiceTracker {
		final Integer ZERO = new Integer(0);
		private TreeSet serviceReferences = new TreeSet(new Comparator() {
			public int compare(Object o1, Object o2) {
				return getRank((ServiceReference) o1).compareTo(getRank((ServiceReference) o2));
			}

			private Integer getRank(ServiceReference ref) {
				Object ranking = ref.getProperty(ConfigurationPlugin.CM_RANKING);
				if (ranking == null || !(ranking instanceof Integer))
					return ZERO;
				return ((Integer) ranking);
			}
		});

		public PluginTracker(BundleContext context) {
			super(context, ConfigurationPlugin.class.getName(), null);
		}

		/* NOTE: this method alters the contract of the overriden method.
		 * Rather than returning null if no references are present, it
		 * returns an empty array.
		 */
		public ServiceReference[] getServiceReferences() {
			synchronized (serviceReferences) {
				return (ServiceReference[]) serviceReferences.toArray(new ServiceReference[0]);
			}
		}

		public Object addingService(ServiceReference reference) {
			synchronized (serviceReferences) {
				serviceReferences.add(reference);
			}
			return context.getService(reference);
		}

		public void modifiedService(ServiceReference reference, Object service) {
			// nothing to do
		}

		public void removedService(ServiceReference reference, Object service) {
			synchronized (serviceReferences) {
				serviceReferences.remove(reference);
			}
			context.ungetService(reference);
		}
	}
}
