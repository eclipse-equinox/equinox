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
package org.eclipse.osgi.internal.composite;

import java.util.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

class CompositeServiceTracker implements ServiceTrackerCustomizer {
	final BundleContext sourceContext;
	final BundleContext targetContext;
	final ServiceTracker[] trackers;
	final String[] filters;
	/* @GuardedBy("serviceComposites") */
	final HashMap serviceComposites = new HashMap();

	public CompositeServiceTracker(BundleContext sourceContext, BundleContext targetContext, String serviceFilters) {
		this.sourceContext = sourceContext;
		this.targetContext = targetContext;
		filters = ManifestElement.getArrayFromList(serviceFilters, ","); //$NON-NLS-1$
		trackers = new ServiceTracker[filters.length];
	}

	synchronized void open() {
		for (int i = 0; i < trackers.length; i++) {
			try {
				trackers[i] = new ServiceTracker(sourceContext, sourceContext.createFilter(filters[i]), this);
				trackers[i].open();
			} catch (InvalidSyntaxException e) {
				// TODO log
				// we will skip this filter; note that trackers may have null entries
			}
		}
	}

	synchronized void close() {
		for (int i = 0; i < trackers.length; i++) {
			if (trackers[i] != null)
				trackers[i].close();
		}
	}

	public Object addingService(ServiceReference reference) {
		ServiceLink serviceLink;
		int useCount;
		synchronized (serviceComposites) {
			serviceLink = (ServiceLink) serviceComposites.get(reference);
			if (serviceLink == null) {
				serviceLink = new ServiceLink(reference);
				serviceComposites.put(reference, serviceLink);
			}
			useCount = serviceLink.incrementUse();
		}
		// register service outside of the sync block
		if (useCount == 1)
			serviceLink.register();
		return serviceLink;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		ServiceLink serviceLink = (ServiceLink) service;
		Dictionary serviceProps = null;
		synchronized (serviceComposites) {
			serviceProps = serviceLink.getRefreshProperties();
		}
		// set service properties out side the sync block
		if (serviceProps != null)
			((ServiceLink) service).setServiceProperties(serviceProps);
	}

	public void removedService(ServiceReference reference, Object service) {
		int useCount;
		synchronized (serviceComposites) {
			useCount = ((ServiceLink) service).decrementUse();
			if (useCount == 0)
				serviceComposites.remove(reference);
		}
		// unregister outside the sync block
		if (useCount == 0)
			((ServiceLink) service).unregister();
	}

	class ServiceLink implements ServiceFactory {
		private final ServiceReference reference;
		private volatile ServiceRegistration registration;
		/* @GuardedBy("this") */
		private Object service;
		/* @GuardedBy("serviceLinks") */
		private int useCount;

		ServiceLink(ServiceReference reference) {
			this.reference = reference;
		}

		/* @GuardedBy("serviceLinks") */
		Dictionary getRefreshProperties() {
			Dictionary result = getServiceProperties();
			if (useCount <= 1)
				return result;
			// need to do an expensive properties check to avoid multiple registration property changes
			String[] originalKeys = registration.getReference().getPropertyKeys();
			for (int i = 0; i < originalKeys.length; i++) {
				if (!Constants.OBJECTCLASS.equals(originalKeys[i]) && !Constants.SERVICE_ID.equals(originalKeys[i]))
					// identity compare is done on purpose here to catch any kind of change
					if (registration.getReference().getProperty(originalKeys[i]) != result.get(originalKeys[i]))
						return result;
			}
			for (Enumeration eKeys = result.keys(); eKeys.hasMoreElements();) {
				String key = (String) eKeys.nextElement();
				if (!Constants.OBJECTCLASS.equals(key) && !Constants.SERVICE_ID.equals(key))
					// identity compare is done on purpose here to catch any kind of change
					if (result.get(key) != registration.getReference().getProperty(key))
						return result;
			}
			return null;
		}

		/* @GuardedBy("serviceLinks") */
		int decrementUse() {
			return --useCount;
		}

		/* @GuardedBy("serviceLinks") */
		int incrementUse() {
			return ++useCount;
		}

		/* @GuardedBy("serviceLinks") */
		int getUse() {
			return useCount;
		}

		void setServiceProperties(Dictionary props) {
			ServiceRegistration current = registration;
			if (current != null)
				current.setProperties(props);
		}

		void register() {
			Dictionary props = getServiceProperties();
			registration = targetContext.registerService((String[]) props.get(Constants.OBJECTCLASS), this, props);
		}

		void unregister() {
			ServiceRegistration current = registration;
			if (current != null)
				current.unregister();
		}

		private Dictionary getServiceProperties() {
			String[] keys = reference.getPropertyKeys();
			Hashtable serviceProps = new Hashtable(keys.length);
			for (int i = 0; i < keys.length; i++)
				serviceProps.put(keys[i], reference.getProperty(keys[i]));
			return serviceProps;
		}

		public synchronized Object getService(Bundle bundle, ServiceRegistration reg) {
			if (service == null)
				service = sourceContext.getService(reference);
			return service;
		}

		public void ungetService(Bundle bundle, ServiceRegistration reg, Object serv) {
			// nothing
		}
	}
}
