//TODO Need to add the header
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
 */
public interface NamedClassSpace {
    /**
     * Returns the bundle providing the package associated with this 
     * <tt>NamedClassSpace</tt> object.
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
     * Returns <tt>true</tt> if the bundle associated with this <tt>NamedClassSpace</tt>
     * object has been provided by a bundle that has been updated or uninstalled.
     *
     * @return <tt>true</tt> if the associated bundle is being
     * provided by a bundle that has been updated or uninstalled, or if this
     * <tt>NamedClassSpace</tt> object has become stale; <tt>false</tt> otherwise.
     */
	public boolean isRemovalPending();
}
