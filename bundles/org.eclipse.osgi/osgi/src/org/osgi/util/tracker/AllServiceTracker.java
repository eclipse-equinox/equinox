/*
 * $Header: $
 * 
 * Copyright (c) OSGi Alliance (2000, 2004). All Rights Reserved.
 * 
 * Implementation of certain elements of the OSGi Specification may be subject
 * to third party intellectual property rights, including without limitation,
 * patent rights (such a third party may or may not be a member of the OSGi
 * Alliance). The OSGi Alliance is not responsible and shall not be held
 * responsible in any manner for identifying or failing to identify any or all
 * such third party intellectual property rights.
 * 
 * This document and the information contained herein are provided on an "AS IS"
 * basis and THE OSGI ALLIANCE DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION
 * HEREIN WILL NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL THE
 * OSGI ALLIANCE BE LIABLE FOR ANY LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF
 * USE OF DATA, INTERRUPTION OF BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR
 * EXEMPLARY, INCIDENTIAL, PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN
 * CONNECTION WITH THIS DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH LOSS OR DAMAGE.
 * 
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.util.tracker;

import org.osgi.framework.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;

/**
 * The <tt>AllServiceTracker</tt> class simplifies using services from the
 * Framework's service registry.
 * <p>
 * A <tt>AllServiceTracker</tt> object is constructed with search criteria and a
 * <tt>ServiceTrackerCustomizer</tt> object. A <tt>AllServiceTracker</tt>
 * object can use the <tt>ServiceTrackerCustomizer</tt> object to customize
 * the service objects to be tracked. The <tt>ServiceTracker</tt> object can
 * then be opened to begin tracking all services in the Framework's service
 * registry that match the specified search criteria. The
 * <tt>ServiceTracker</tt> object correctly handles all of the details of
 * listening to <tt>ServiceEvent</tt> objects and getting and ungetting
 * services.
 * <p>
 * The <tt>getServiceReferences</tt> method can be called to get references to
 * the services being tracked. The <tt>getService</tt> and
 * <tt>getServices</tt> methods can be called to get the service objects for
 * the tracked service.
 * 
 * @version $ $
 */
public class AllServiceTracker extends ServiceTracker {
	
	/**
	 * @param context
	 * @param filter
	 * @param customizer
	 */
	public AllServiceTracker(BundleContext context, Filter filter, ServiceTrackerCustomizer customizer) {
		super(context, filter, customizer);
	}
	/**
	 * @param context
	 * @param reference
	 * @param customizer
	 */
	public AllServiceTracker(BundleContext context, ServiceReference reference, ServiceTrackerCustomizer customizer) {
		super(context, reference, customizer);
	}
	/**
	 * @param context
	 * @param clazz
	 * @param customizer
	 */
	public AllServiceTracker(BundleContext context, String clazz, ServiceTrackerCustomizer customizer) {
		super(context, clazz, customizer);
	}

	protected Tracked createTracked() {
		return new AllTracked();
	}

	protected ServiceReference[] getInitialReferences(String initTrackClass, Filter initFilter) throws InvalidSyntaxException {
		return context.getAllServiceReferences(initTrackClass, initFilter == null ? null : initFilter.toString()); 
	}

	class AllTracked extends Tracked implements AllServiceListener{
		private static final long serialVersionUID = 4050764875305137716L;
	}
}