/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import org.eclipse.osgi.framework.debug.Debug;
import org.osgi.framework.*;

public class FilteredServiceListener implements ServiceListener {
	/** Filter for listener. */
	private final FilterImpl filter;
	/** Real listener. */
	private final ServiceListener listener;
	// The bundle context
	private final BundleContextImpl context;
	// is this an AllServiceListener
	private final boolean allservices;
	// an objectClass required by the filter
	private final String objectClass;

	/**
	 * Constructor.
	 *
	 * @param filterstring filter for this listener.
	 * @param listener real listener.
	 * @exception InvalidSyntaxException if the filter is invalid.
	 */
	protected FilteredServiceListener(String filterstring, ServiceListener listener, BundleContextImpl context) throws InvalidSyntaxException {
		if (filterstring == null) {
			this.filter = null;
			this.objectClass = null;
		} else {
			FilterImpl filterImpl = new FilterImpl(filterstring);
			String clazz = filterImpl.getRequiredObjectClass();
			if (clazz == null) {
				this.objectClass = null;
				this.filter = filterImpl;
			} else {
				this.objectClass = clazz.intern(); /*intern the name for future identity comparison */
				String objectClassFilter = FilterImpl.getObjectClassFilterString(this.objectClass);
				this.filter = (objectClassFilter.equals(filterstring)) ? null : filterImpl;
			}
		}
		this.listener = listener;
		this.context = context;
		this.allservices = (listener instanceof AllServiceListener);
	}

	/**
	 * Receive notification that a service has had a
	 * change occur in it's lifecycle.
	 *
	 * @param event The ServiceEvent.
	 */
	public void serviceChanged(ServiceEvent event) {
		ServiceReferenceImpl reference = (ServiceReferenceImpl) event.getServiceReference();

		// first check if we can short circuit the filter match if the required objectClass does not match the event
		objectClassCheck: if (objectClass != null) {
			String[] classes = reference.getClasses();
			int size = classes.length;
			for (int i = 0; i < size; i++) {
				if (classes[i] == objectClass) // objectClass strings have previously been interned for identity comparison 
					break objectClassCheck;
			}
			return; // no class in this event matches a required part of the filter; we do not need to deliver this event
		}

		if (!context.hasListenServicePermission(event))
			return;

		if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
			String listenerName = this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this)); //$NON-NLS-1$
			Debug.println("filterServiceEvent(" + listenerName + ", \"" + filter + "\", " + reference.registration.getProperties() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		if ((filter == null || filter.match(reference)) && (allservices || context.isAssignableTo(reference))) {
			if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
				String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(listener)); //$NON-NLS-1$
				Debug.println("dispatchFilteredServiceEvent(" + listenerName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			listener.serviceChanged(event);
		}
	}

	/**
	 * Get the filter string used by this Filtered listener.
	 *
	 * @return The filter string used by this listener.
	 */
	public String toString() {
		return filter == null ? listener.toString() : filter.toString();
	}

}
