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

package org.eclipse.osgi.internal.serviceregistry;

import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.BundleContextImpl;
import org.eclipse.osgi.framework.internal.core.FilterImpl;
import org.osgi.framework.*;
import org.osgi.framework.hooks.service.ListenerHook;

/**
 * Service Listener delegate.
 */
class FilteredServiceListener implements ServiceListener, ListenerHook.ListenerInfo {
	/** Filter for listener. */
	private final FilterImpl filter;
	/** Real listener. */
	private final ServiceListener listener;
	/** The bundle context */
	private final BundleContextImpl context;
	/** is this an AllServiceListener */
	private final boolean allservices;
	/** an objectClass required by the filter */
	private final String objectClass;
	/** Indicates if the last event was delivered because of a filter match */
	/* @GuardedBy this */
	private boolean matched;

	/**
	 * Constructor.
	 *
	 * @param context The bundle context of the bundle which added the specified service listener.
	 * @param filterstring The filter string specified when this service listener was added.
	 * @param listener The service listener object.
	 * @exception InvalidSyntaxException if the filter is invalid.
	 */
	FilteredServiceListener(final BundleContextImpl context, final ServiceListener listener, final String filterstring) throws InvalidSyntaxException {
		if (filterstring == null) {
			this.filter = null;
			this.objectClass = null;
		} else {
			FilterImpl filterImpl = FilterImpl.newInstance(filterstring);
			String clazz = filterImpl.getRequiredObjectClass();
			if (clazz == null) {
				this.objectClass = null;
				this.filter = filterImpl;
			} else {
				this.objectClass = clazz.intern(); /*intern the name for future identity comparison */
				this.filter = filterstring.equals(getObjectClassFilterString(this.objectClass)) ? null : filterImpl;
			}
		}
		this.matched = false;
		this.listener = listener;
		this.context = context;
		this.allservices = (listener instanceof AllServiceListener);
	}

	/**
	 * Receives notification that a service has had a lifecycle change.
	 * 
	 * @param event The <code>ServiceEvent</code> object.
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

		if (!ServiceRegistry.hasListenServicePermission(event, context))
			return;

		if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
			String listenerName = this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this)); //$NON-NLS-1$
			Debug.println("filterServiceEvent(" + listenerName + ", \"" + getFilter() + "\", " + reference.getRegistration().getProperties() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		checkfilter: {
			if (filter == null) {
				break checkfilter; // no filter, so deliver event
			}
			final boolean match = filter.match(reference);
			synchronized (this) {
				if (match) { // if the filter matches now
					matched = true; // remember that the filter matched
					break checkfilter; // filter matched, so deliver event
				}
				if (matched) { // if the filter does not match now, but it previously matched
					matched = false; // remember that the filter no longer matches
					if (event.getType() == ServiceEvent.MODIFIED) {
						event = new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH, reference);
						break checkfilter; // deliver a MODIFIED_ENDMATCH event
					}
				}
			}
			return; // there is a filter and it does not match, so do NOT deliver the event
		}

		if (allservices || ServiceRegistry.isAssignableTo(context, reference)) {
			if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
				String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(listener)); //$NON-NLS-1$
				Debug.println("dispatchFilteredServiceEvent(" + listenerName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			listener.serviceChanged(event);
		}
	}

	/**
	 * The string representation of this Filtered listener.
	 *
	 * @return The string representation of this listener.
	 */
	public String toString() {
		String filterString = getFilter();
		if (filterString == null) {
			filterString = ""; //$NON-NLS-1$
		}
		return listener.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(listener)) + filterString; //$NON-NLS-1$
	}

	/** 
	 * Return the bundle context for the ListenerHook.
	 * @return The context of the bundle which added the service listener.
	 * @see org.osgi.framework.hooks.service.ListenerHook.ListenerInfo#getBundleContext()
	 */
	public BundleContext getBundleContext() {
		return context;
	}

	/** 
	 * Return the filter string for the ListenerHook.
	 * @return The filter string with which the listener was added. This may
	 * be <code>null</code> if the listener was added without a filter.
	 * @see org.osgi.framework.hooks.service.ListenerHook.ListenerInfo#getFilter()
	 */
	public String getFilter() {
		if (filter != null) {
			return filter.toString();
		}
		return getObjectClassFilterString(objectClass);
	}

	/**
	 * Returns an objectClass filter string for the specified class name.
	 * @return A filter string for the specified class name or <code>null</code> if the 
	 * specified class name is <code>null</code>.
	 */
	private static String getObjectClassFilterString(String className) {
		if (className == null) {
			return null;
		}
		return "(" + Constants.OBJECTCLASS + "=" + className + ")"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}
}
