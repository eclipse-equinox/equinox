/*
 * $Header:
 *
 * Copyright (c) The Open Services Gateway Initiative (2004).
 * All Rights Reserved.
 *
 * Implementation of certain elements of the Open Services Gateway Initiative
 * (OSGI) Specification may be subject to third party intellectual property
 * rights, including without limitation, patent rights (such a third party may
 * or may not be a member of OSGi). OSGi is not responsible and shall not be
 * held responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS
 * IS" basis and OSGI DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL
 * NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL OSGI BE LIABLE FOR ANY
 * LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS
 * DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */
package org.osgi.service.packageadmin;

import org.osgi.framework.Bundle;

/**
 * A provided NamedClassSpace.  
 * 
 * Instances implementing this interface are created by the Package 
 * Admin service.
 * 
 * <p>The information about a <tt>NamedClassSpace</tt> provided by this object is 
 * valid only until the next time <tt>PackageAdmin.refreshPackages()</tt> called.   
 * If a <tt>NamedClassSpace</tt> object becomes stale (that is, the bundle it 
 * references has been updated or removed as a result of calling 
 * <tt>PackageAdmin.refreshPackages()</tt>), its <tt>getName()</tt> and 
 * <tt>getVersion()</tt> continue to return their old values, 
 * <tt>isRemovalPending()</tt> returns true, and <tt>getProvidingBundle()</tt> 
 * and <tt>getRequiringBundles()</tt> return <tt>null</tt>.
 * @since <b>1.4 EXPERIMENTAL</b>
 */
public interface NamedClassSpace {
    /**
     * Returns the bundle providing this <tt>NamedClassSpace</tt> object.
     *
     * @return The providing bundle, or <tt>null</tt> if this <tt>NamedClassSpace</tt>
     *         object has become stale.
     */
	public Bundle getProvidingBundle();

	/**
     * Returns the resolved bundles that are currently require the bundle
     * associated with this <tt>NamedClassSpace</tt> object.
     *
     * @return The array of resolved bundles currently requiring the bundle
     * associated with this <tt>Symbolic</tt> object, or <tt>null</tt> if this 
     * <tt>NamedClassSpace</tt> object has become stale.
     */
	public Bundle[] getRequiringBundles();

    /**
     * Returns the symbolic name of the bundle associated with this 
     * <tt>NamedClassSpace</tt> object.
     *
     * @return The symbolic name of this <tt>NamedClassSpace</tt> object.
     */
	public String getName();

    /**
     * Returns the bundle version of this <tt>NamedClassSpace</tt>, as
     * specified in the providing bundle's manifest file.
     *
     * @return The bundle version of this <tt>NamedClassSpace</tt> object, or
     *         "0.0.0" if no bundle version information is available.
     */
	public String getVersion();

    /**
     * Returns <tt>true</tt> if the bundle providing this <tt>NamedClassSpace</tt>
     * object has been updated or uninstalled.
     *
     * @return <tt>true</tt> if the bundle providing this <tt>NamedClassSpace</tt>
     * object has been updated or uninstalled, or if this
     * <tt>NamedClassSpace</tt> object has become stale; <tt>false</tt> otherwise.
     */
	public boolean isRemovalPending();
}
