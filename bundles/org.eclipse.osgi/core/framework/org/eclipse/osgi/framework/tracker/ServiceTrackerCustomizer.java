/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.tracker;

import org.osgi.framework.ServiceReference;

/**
 * The <tt>ServiceTrackerCustomizer</tt> interface allows a <tt>ServiceTracker</tt> object to customize
 * the service objects that are tracked. The <tt>ServiceTrackerCustomizer</tt> object
 * is called when a service is being added to the <tt>ServiceTracker</tt> object. The <tt>ServiceTrackerCustomizer</tt> can
 * then return an object for the tracked service. The <tt>ServiceTrackerCustomizer</tt> object is also
 * called when a tracked service is modified or has been removed from the
 * <tt>ServiceTracker</tt> object.
 *
 * <p>The methods in this interface may be called as the result of a <tt>ServiceEvent</tt>
 * being received by a <tt>ServiceTracker</tt> object. Since <tt>ServiceEvent</tt>s are
 * synchronously delivered by the Framework, it is highly recommended that implementations
 * of these methods do not
 * register (<tt>BundleContext.registerService</tt>), modify
 * (<tt>ServiceRegistration.setProperties</tt>) or unregister
 * (<tt>ServiceRegistration.unregister</tt>)
 * a service while being synchronized on any object.
 */
public interface ServiceTrackerCustomizer {
	/**
	 * A service is being added to the <tt>ServiceTracker</tt> object.
	 *
	 * <p>This method is called before a service which matched
	 * the search parameters of the <tt>ServiceTracker</tt> object is
	 * added to it. This method should return the
	 * service object to be tracked for this <tt>ServiceReference</tt> object.
	 * The returned service object is stored in the <tt>ServiceTracker</tt> object
	 * and is available from the <tt>getService</tt> and <tt>getServices</tt>
	 * methods.
	 *
	 * @param reference Reference to service being added to the <tt>ServiceTracker</tt> object.
	 * @return The service object to be tracked for the
	 * <tt>ServiceReference</tt> object or <tt>null</tt> if the <tt>ServiceReference</tt> object should not
	 * be tracked.
	 */
	public abstract Object addingService(ServiceReference reference);

	/**
	 * A service tracked by the <tt>ServiceTracker</tt> object has been modified.
	 *
	 * <p>This method is called when a service being tracked
	 * by the <tt>ServiceTracker</tt> object has had it properties modified.
	 *
	 * @param reference Reference to service that has been modified.
	 * @param service The service object for the modified service.
	 */
	public abstract void modifiedService(ServiceReference reference, Object service);

	/**
	 * A service tracked by the <tt>ServiceTracker</tt> object has been removed.
	 *
	 * <p>This method is called after a service is no longer being tracked
	 * by the <tt>ServiceTracker</tt> object.
	 *
	 * @param reference Reference to service that has been removed.
	 * @param service The service object for the removed service.
	 */
	public abstract void removedService(ServiceReference reference, Object service);
}
