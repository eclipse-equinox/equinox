/*
 * $Header: /home/eclipse/org.eclipse.osgi/osgi/src/org/osgi/service/packageadmin/PackageAdmin.java,v 1.6 2004/04/05 19:35:12 twatson Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2001, 2002).
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
 * Framework service which allows bundle programmers to inspect the packages
 * exported in the Framework and eagerly update or uninstall bundles.
 *
 * If present, there will only be a single instance of this service
 * registered with the Framework.
 *
 * <p>The term <i>exported package</i> (and the corresponding interface
 * {@link ExportedPackage})refers to a package that has actually been
 * exported (as opposed to one that is available for export).
 *
 * <p>The information about exported packages returned by this
 * service is valid only until the next time {@link #refreshPackages}is
 * called.
 * If an <tt>ExportedPackage</tt> object becomes stale, (that is, the package it references
 * has been updated or removed as a result of calling
 * <tt>PackageAdmin.refreshPackages()</tt>),
 * its <tt>getName()</tt> and <tt>getSpecificationVersion()</tt> continue to return their
 * old values, <tt>isRemovalPending()</tt> returns <tt>true</tt>, and <tt>getExportingBundle()</tt>
 * and <tt>getImportingBundles()</tt> return <tt>null</tt>.
 *
 * @version $Revision: 1.6 $
 * @author Open Services Gateway Initiative
 */
public interface PackageAdmin {
    /**
     * Gets the packages exported by the specified bundle.
     *
     * @param bundle The bundle whose exported packages are to be returned,
     *               or <tt>null</tt> if all the packages currently
     *               exported in the Framework are to be returned.  If the
     *               specified bundle is the system bundle (that is, the
     *               bundle with id zero), this method returns all the packages
     *               on the system classpath whose name does not start with
     *               "java.".  In an environment where the exhaustive list
     *               of packages on the system classpath is not known in
     *               advance, this method will return all currently known
     *               packages on the system classpath, that is, all packages
     *               on the system classpath that contains one or more classes
     *               that have been loaded.
     *
     * @return The array of packages exported by the specified bundle,
     * or <tt>null</tt> if the specified bundle has not exported any packages.
     */
    public ExportedPackage[] getExportedPackages(Bundle bundle);

    /**
     * Gets the <tt>ExportedPackage</tt> object with the specified package name.  All exported
     * packages
     * will be checked for the specified name.  In an environment where the
     * exhaustive list of packages on the system classpath is not known in
     * advance, this method attempts to see if the named package is on the
     * system classpath.
     * This
     * means that this method may discover an <tt>ExportedPackage</tt> object that was
     * not present in the list returned by
     * a prior call to <tt>getExportedPackages()</tt>.
     *
     * @param name The name of the exported package to be returned.
     *
     * @return The exported package with the specified name, or <tt>null</tt>
     *         if no expored package with that name exists.
     */
    public ExportedPackage getExportedPackage(String name);

    /**
     * Forces the update (replacement) or removal of packages exported by
     * the specified bundles.
     *
     * <p> If no bundles are specified, this method will update or remove any
     * packages exported by any bundles that were previously updated or
     * uninstalled since the last call to this method.
     * The technique by which this is accomplished
     * may vary among different Framework implementations. One permissible
     * implementation is to stop and restart the Framework.
     *
     * <p> This method returns to the caller immediately and then performs the
     * following steps in its own thread:
     *
     * <ol>
     * <li> Compute a graph of bundles starting with the specified bundles. If no
     * bundles are specified, compute a graph of bundles starting with
     * previously updated or uninstalled ones.
     * Add to the graph any bundle that imports a package that is currently exported
     * by a bundle in the graph. The graph is fully
     * constructed when there is no bundle outside the graph that imports a
     * package from a bundle in the graph. The graph may contain
     * <tt>UNINSTALLED</tt> bundles that are currently still
     * exporting packages.
     *
     * <li> Each bundle in the graph that is in the <tt>ACTIVE</tt> state
     * will be stopped as described in the <tt>Bundle.stop</tt> method.
     *
     * <li> Each bundle in the graph that is in the
     * <tt>RESOLVED</tt> state is moved
     * to the <tt>INSTALLED</tt> state.
     * The effect of this step is that bundles in the graph are no longer
     * <tt>RESOLVED</tt>.
     *
     * <li> Each bundle in the graph that is in the <tt>UNINSTALLED</tt> state is
     * removed from the graph and is now completely removed from the Framework.
     *
     * <li> Each bundle in the graph that was in the
     * <tt>ACTIVE</tt> state prior to Step 2 is started as
     * described in the <tt>Bundle.start</tt> method, causing all
     * bundles required for the restart to be resolved.
     * It is possible that, as a
     * result of the previous steps, packages that were
     * previously exported no longer are. Therefore, some bundles
     * may be unresolvable until another bundle
     * offering a compatible package for export has been installed in the
     * Framework.
     * <li>A framework event of type <tt>FrameworkEvent.PACKAGES_REFRESHED</tt> is broadcast.
     * </ol>
     *
     * <p>For any exceptions that are thrown during any of these steps, a
     * <tt>FrameworkEvent</tt> of type <tt>ERROR</tt> is
     * broadcast, containing the exception.
     * The source bundle for these events should be the specific bundle
     * to which the exception is related. If no specific bundle can be
     * associated with the exception then the System Bundle must be used
     * as the source bundle for the event.
     *
     * @param bundles the bundles whose exported packages are to be updated or
     * removed, or <tt>null</tt> for all previously updated or uninstalled bundles.
     *
     * @exception SecurityException if the caller does not have the
     * <tt>AdminPermission</tt> and the Java runtime environment supports
     * permissions.
     */
    public void refreshPackages(Bundle[] bundles);

    /**
     * Attempts to resolve the specified bundles.  The Framework must 
     * attempt to resolve the unresolved bundles that are included in 
     * the specified bundles.  Additional bundles that are not included 
     * in the specified bundles may be resolved as a result of calling 
     * this method.  A permissible implementation of this method is to 
     * attempt to resolve all unresolved bundles installed in the framework.
     * 
     * <p>If <tt>null</tt> is specified then the Framework will attempt to 
     * resolve all unresolved bundles installed in the Framework.  Calling 
     * this method must not cause any bundle to be refreshed, stopped, or 
     * started.  This method will not return until the operation has completed.
     * 
     * @param bundles the bundles to attempt to resolve, or <tt>null</tt>
     * to attempt to resolve all unresolved bundles installed in the Framework. 
     */
    public void resolveBundles(Bundle[] bundles);
    
    public NamedClassSpace[] getNamedClassSpaces(String symbolicName);

    /**
     * Gets the Bundles with the specified symbolic name.  If no bundles are 
     * installed that have the specified symbolic name then null is returned.  
     * If the version argument is not null then only the Bundles that have 
     * the specified symbolic name and a version greater than or equal to the 
     * specified version are returned.  If the version and match arguments are 
     * not null then only the Bundles that have the specified symbolic name 
     * and a version that matches the specified version using the specified 
     * match rule are returned.  The returned bundles are ordered in 
     * descending bundle version order.
     * 
     * @param symbolicName the symbolic name of the bundles that are to be returned.
     * @param version the version that the return bundle versions must match, 
     * or <tt>null</tt> if no version matching is to be done. 
     * @param match the match rule that the returned bundle versions must match,
     * or <tt>null</tt> if no version is specified or if the greater than or equal
     * match rule is to be used.
     * @return the array of Bundles with the specified name that match the 
     * specified version and match rule, or <tt>null</tt> if no bundles are found.
     */
    public Bundle[] getBundles(String symbolicName, String version, String match);

	/**
	 * Gets an array of attached fragment bundles for the specified bundle.  If the 
	 * specified bundle is a fragment then <tt>null</tt> is returned.  If no fragments are 
	 * attached to the specified bundle then <tt>null</tt> is returned.
	 * 
	 * @param bundle the bundle to get the attached fragment bundles for.
	 * @return an array of fragment bundles or <tt>null</tt> if the bundle does not 
	 * have any attached fragment bundles. 
	 */
    public Bundle[] getFragments(Bundle bundle);

	/**
	 * Gets an array of host bundles that the specified fragment bundle is 
	 * attached to or <tt>null</tt> if the specified bundle is not attached to a host.  
	 * If the bundle is not a fragment bundle then <tt>null</tt> is returned.
	 * 
	 * @param bundle the bundle to get the host bundles for.
	 * @return an array of host bundles or null if the bundle does not have any
	 * bundles host.
	 */
    public Bundle[] getHosts(Bundle bundle);

    /**
     * A bundle of this type is a fragment bundle.
     */
    public static final int BUNDLE_TYPE_FRAGMENT = 0x00000001; 

    /**
	 * Gets the type of the specified bundle.  Currently the only defined bundle 
	 * type is BUNDLE_TYPE_FRAGMENT.  Additional bundle types may be defined in the 
	 * future.  If a bundle is not one or more of the defined types then 0 
	 * is returned.
	 * <p>A bundle may be more than one type at a time.
	 * @return the type of the bundle.
	 */
    public int getBundleType(Bundle bundle);
}
