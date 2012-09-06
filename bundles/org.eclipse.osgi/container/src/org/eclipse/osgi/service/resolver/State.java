/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import java.util.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

/**
 * The state of a system as reported by a resolver. This includes all bundles
 * presented to the resolver relative to this state (i.e., both resolved and
 * unresolved).
 * <p>
 * This interface is not intended to be implemented by clients.  The
 * {@link StateObjectFactory} should be used to construct instances.
 * </p>
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface State {
	/**
	 * Adds the given bundle to this state.
	 * <p>
	 * If the bundle already exists in another state then an <code>IllegalStateException</code>
	 * will be thrown.  Note that even if you remove a <code>BundleDescription</code> from
	 * one <code>State</code> object using {@link State#removeBundle(BundleDescription)} it 
	 * may still be considered as removing pending if other bundles in that state depend on the
	 * bundle you removed.  To complete a pending removal a call must be done to 
	 * {@link State#resolve(BundleDescription[])} with the removed bundle.
	 * </p>
	 * 
	 * @param description the description to add
	 * @return a boolean indicating whether the bundle was successfully added
	 * @throws IllegalStateException if the bundle already exists in another state
	 */
	public boolean addBundle(BundleDescription description);

	/**
	 * Returns a delta describing the differences between this state and the
	 * given state. The given state is taken as the base so the absence of a bundle
	 * in this state is reported as a deletion, etc.
	 *<p>Note that the generated StateDelta will contain BundleDeltas with one
	 *of the following types: BundleDelta.ADDED, BundleDelta.REMOVED and 
	 *BundleDelta.UPDATED</p>
	 * 
	 * @param baseState the base state
	 * @return a delta describing differences between this and the base state state 
	 */
	public StateDelta compare(State baseState) throws BundleException;

	/**
	 * Removes a bundle description with the given bundle id.
	 * 
	 * @param bundleId the id of the bundle description to be removed
	 * @return the removed bundle description, or <code>null</code>, if a bundle
	 * 	with the given id does not exist in this state
	 */
	public BundleDescription removeBundle(long bundleId);

	/**
	 * Removes the given bundle description.
	 * 
	 * @param bundle the bundle description to be removed
	 * @return <code>true</code>, if if the bundle description was removed, 
	 * 	<code>false</code> otherwise 	
	 */
	public boolean removeBundle(BundleDescription bundle);

	/**
	 * Updates an existing bundle description with the given description. 
	 * 
	 * @param newDescription the bundle description to replace an existing one
	 * @return <code>true</code>, if if the bundle description was updated, 
	 * 	<code>false</code> otherwise 	
	 */
	public boolean updateBundle(BundleDescription newDescription);

	/**
	 * Returns the delta representing the changes from the time this state was
	 * first captured until now.
	 * 
	 * @return the state delta
	 */
	public StateDelta getChanges();

	/**
	 * Returns descriptions for all bundles known to this state.
	 * 
	 * @return the descriptions for all bundles known to this state.
	 */
	public BundleDescription[] getBundles();

	/**
	 * Returns the bundle descriptor for the bundle with the given id. 
	 * <code>null</code> is returned if no such bundle is found in 
	 * this state. 
	 * 
	 * @return the descriptor for the identified bundle
	 * @see BundleDescription#getBundleId()
	 */
	public BundleDescription getBundle(long id);

	/**
	 * Returns the bundle descriptor for the bundle with the given name and
	 * version. A null value is returned if no such bundle is found in this state.
	 * A resolved bundle is always preferably returned over an unresolved bundle.
	 * If multiple bundles with the same resolution state are available, the bundle
	 * with the highest version number is returned if the <code>version<code> is
	 * null.
	 * 
	 * @param symbolicName symbolic name of the bundle to query
	 * @param version version of the bundle to query. null matches any bundle
	 * @return the descriptor for the identified bundle
	 */
	public BundleDescription getBundle(String symbolicName, Version version);

	/**
	 * Returns the bundle descriptor for the bundle with the given location
	 * identifier. null is returned if no such bundle is found in this state. 
	 * 
	 * @param location location identifier of the bundle to query
	 * @return the descriptor for the identified bundle
	 */
	public BundleDescription getBundleByLocation(String location);

	/**
	 * Returns the timestamp for this state. This
	 * correlates this timestamp to the system state. For example, if
	 * the system state timestamp is 4 but then some bundles are installed,
	 * the system state timestamp is updated. By comparing 4 to the current system
	 * state timestamp it is possible to detect if the states are out of sync.
	 * 
	 * @return the timestamp of this state
	 */
	public long getTimeStamp();

	/**
	 * Sets the timestamp for this state
	 * @param newTimeStamp the new timestamp for this state
	 */
	public void setTimeStamp(long newTimeStamp);

	/**
	 * Returns true if there have been no modifications to this state since the
	 * last time resolve() was called.
	 * 
	 * @return whether or not this state has changed since last resolved.
	 */
	public boolean isResolved();

	/**
	 * Resolves the given version constraint with the given supplier. The given
	 * constraint object is destructively modified to reflect its new resolved
	 * state. Note that a constraint can be unresolved by passing null for 
	 * the supplier.
	 * <p>
	 * This method is intended to be used by resolvers in the process of
	 * determining which constraints are satisfied by which components.
	 * </p>
	 * 
	 * @param constraint the version constraint to update
	 * @param supplier the supplier which satisfies the constraint. May be null if 
	 * the constraint is to be unresolved.
	 * @throws IllegalStateException if this is not done during a call to
	 * <code>resolve</code>
	 */
	public void resolveConstraint(VersionConstraint constraint, BaseDescription supplier);

	/**
	 * Sets whether or not the given bundle is selected in this state.
	 * <p>
	 * This method is intended to be used by resolvers in the process of
	 * determining which constraints are satisfied by which components.
	 * </p>
	 * 
	 * @param bundle the bundle to update
	 * @param status whether or not the given bundle is resolved, if false the other parameters are ignored
	 * @param hosts the host for the resolve fragment, can be <code>null</code>
	 * @param selectedExports the selected exported packages for this resolved bundle, can be <code>null</code>
	 * @param resolvedRequires the {@link BundleDescription}s that resolve the required bundles for this bundle, can be <code>null</code>
	 * @param resolvedImports the exported packages that resolve the imports for this bundle, can be <code>null</code>
	 * @throws IllegalStateException if this is not done during a call to <code>resolve</code>
	 * @deprecated use {@link #resolveBundle(BundleDescription, boolean, BundleDescription[], ExportPackageDescription[], ExportPackageDescription[], GenericDescription[], BundleDescription[], ExportPackageDescription[], GenericDescription[], Map)}
	 */
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports);

	/**
	 * Sets whether or not the given bundle is selected in this state.
	 * <p>
	 * This method is intended to be used by resolvers in the process of
	 * determining which constraints are satisfied by which components.
	 * </p>
	 * 
	 * @param bundle the bundle to update
	 * @param status whether or not the given bundle is resolved, if false the other parameters are ignored
	 * @param hosts the host for the resolve fragment, can be <code>null</code>
	 * @param selectedExports the selected exported packages for this resolved bundle, can be <code>null</code>
	 * @param substitutedExports the exported packages that resolve imports for this bundle and substitute exports, can be <code>null</code>
	 * @param resolvedRequires the {@link BundleDescription}s that resolve the required bundles for this bundle, can be <code>null</code>
	 * @param resolvedImports the exported packages that resolve the imports for this bundle, can be <code>null</code>
	 * @throws IllegalStateException if this is not done during a call to <code>resolve</code>
	 * @since 3.4
	 * @deprecated use {@link #resolveBundle(BundleDescription, boolean, BundleDescription[], ExportPackageDescription[], ExportPackageDescription[], GenericDescription[], BundleDescription[], ExportPackageDescription[], GenericDescription[], Map)}
	 */
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports);

	/**
	 * Sets whether or not the given bundle is selected in this state.
	 * <p>
	 * This method is intended to be used by resolvers in the process of
	 * determining which constraints are satisfied by which components.
	 * </p>
	 * 
	 * @param bundle the bundle to update
	 * @param status whether or not the given bundle is resolved, if false the other parameters are ignored
	 * @param hosts the host for the resolve fragment, can be <code>null</code>
	 * @param selectedExports the selected exported packages for this resolved bundle, can be <code>null</code>
	 * @param substitutedExports the exported packages that resolve imports for this bundle and substitute exports, can be <code>null</code>
	 * @param selectedCapabilities the selected capabilities for this resolved bundle, can be <code>null</code>
	 * @param resolvedRequires the {@link BundleDescription}s that resolve the required bundles for this bundle, can be <code>null</code>
	 * @param resolvedImports the exported packages that resolve the imports for this bundle, can be <code>null</code>
	 * @param resolvedCapabilities the capabilities that resolve the required capabilities for this bundle, can be <code>null</code>
	 * @param resolvedWires the map of state wires for the resolved requirements of the given bundle.  The key is the name space of the requirement.
	 * @throws IllegalStateException if this is not done during a call to <code>resolve</code>
	 * @since 3.7
	 */
	public void resolveBundle(BundleDescription bundle, boolean status, BundleDescription[] hosts, ExportPackageDescription[] selectedExports, ExportPackageDescription[] substitutedExports, GenericDescription[] selectedCapabilities, BundleDescription[] resolvedRequires, ExportPackageDescription[] resolvedImports, GenericDescription[] resolvedCapabilities, Map<String, List<StateWire>> resolvedWires);

	/**
	 * Sets the given removal pending bundle to removal complete for this state.
	 * <p>
	 * This method is intended to be used by resolvers in the process of 
	 * resolving bundles.
	 * </p>
	 * @param bundle the bundle to set a removal complete.
	 * @throws IllegalStateException if this is not done during a call to
	 * <code>resolve</code>
	 */
	public void removeBundleComplete(BundleDescription bundle);

	/**
	 * Adds a new <code>ResolverError</code> for the specified bundle.
	 * <p>
	 * This method is intended to be used by resolvers in the process of
	 * resolving.
	 * </p>
	 * 
	 * @param bundle the bundle to add a new <code>ResolverError</code> for
	 * @param type the type of <code>ResolverError</code> to add
	 * @param data the data for the <code>ResolverError</code>
	 * @param unsatisfied the unsatisfied constraint or null if the resolver error was not caused
	 * by an unsatisfied constraint.
	 * @throws IllegalStateException if this is not done during a call to <code>resolve</code>
	 * @since 3.2
	 */
	public void addResolverError(BundleDescription bundle, int type, String data, VersionConstraint unsatisfied);

	/**
	 * Removes all <code>ResolverError</code>s for the specified bundle.
	 * <p>
	 * This method is intended to be used by resolvers in the process of
	 * resolving.
	 * </p>
	 * 
	 * @param bundle the bundle to remove all <code>ResolverError</code>s for
	 * @throws IllegalStateException if this is not done during a call to <code>resolve</code>
	 * @since 3.2
	 */
	public void removeResolverErrors(BundleDescription bundle);

	/**
	 * Returns all <code>ResolverError</code>s for the given bundle
	 * @param bundle the bundle to get all <code>ResolverError</code>s for
	 * @return all <code>ResolverError</code>s for the given bundle
	 * @since 3.2
	 */
	public ResolverError[] getResolverErrors(BundleDescription bundle);

	/**
	 * Returns the resolver associated with this state. A state can work with
	 * at most one resolver at any given time. Similarly, a resolver can work
	 * with at most one state at a time.
	 * 
	 * @return the resolver for this state. null is returned if the state does
	 * not have a resolver
	 */
	public Resolver getResolver();

	/**
	 * Sets the resolver associated with this state. A state can work with at
	 * most one resolver at any given time. Similarly, a resolver can work with
	 * at most one state at a time.
	 * <p>
	 * To ensure that this state and the given resovler are properly linked,
	 * the following expression must be included in this method if the given
	 * resolver (value) is not identical to the result of this.getResolver().
	 * 
	 * <pre>
	 *  if (this.getResolver() != value) value.setState(this);
	 * </pre>
	 * 
	 * </p>
	 */
	// TODO what happens if you set the Resolver after some bundles have
	// been added to the state but it is not resolved?  Should setting
	// the resolver force a state to be unresolved?
	public void setResolver(Resolver value);

	/**
	 * Resolves the constraints contained in this state using the resolver
	 * currently associated with the state and returns a delta describing the
	 * changes in resolved states and dependencies in the state.
	 * <p>
	 * Note that this method is typically implemented using
	 * 
	 * <pre>
	 *  this.getResolver().resolve();
	 * </pre>
	 * 
	 * and is the preferred path for invoking resolution. In particular, states
	 * should refuse to perform updates (@see #select() and
	 * #resolveConstraint()) if they are not currently involved in a resolution
	 * cycle.
	 * <p>
	 * Note the given state is destructively modified to reflect the results of
	 * resolution.
	 * </p>
	 * 
	 * @param incremental a flag controlling whether resolution should be incremental
	 * @return a delta describing the changes in resolved state and 
	 * interconnections
	 */
	public StateDelta resolve(boolean incremental);

	/**
	 * Same as State.resolve(true);
	 */
	public StateDelta resolve();

	/**
	 * Resolves the constraints contained in this state using the resolver
	 * currently associated with the state in an incremental, "least-perturbing" 
	 * mode, and returns a delta describing the changes in resolved states and 
	 * dependencies in the state.
	 * 
	 * @param discard an array containing descriptions for bundles whose 
	 * 	current resolution state should be forgotten.  If <code>null</code>
	 *  then all the current removal pending BundleDescriptions are refreshed.
	 * @return a delta describing the changes in resolved state and 
	 * 	interconnections
	 */
	public StateDelta resolve(BundleDescription[] discard);

	/**
	 * Resolves the constraints contained in this state using the resolver
	 * currently associated with the state in an incremental, "least-perturbing"
	 * mode, and returns a delta describing the changes in resolved states and
	 * dependencies in the state.  If discard is set to true the 
	 * the descriptions contained in the resolve array will have their 
	 * current resolution state discarded and will be re-resolved.
	 * This method will attempt to resolve the supplied descriptions
	 * and may attempt to resolve any other unresolved descriptions contained
	 * in this state.
	 * 
	 * @param resolve an array containing descriptions for bundles to resolve.
	 * @param discard a value of true indicates the resolve descriptions
	 * should have their current resolution state discarded and re-resolved.
	 * @return a delta describing the changes in the resolved state and
	 * interconnections.
	 * @since 3.7
	 */
	public StateDelta resolve(BundleDescription[] resolve, boolean discard);

	/**
	 * Sets the version overrides which are to be applied during the resolutoin
	 * of this state. Version overrides allow external forces to
	 * refine/override the version constraints setup by the components in the
	 * state.
	 * 
	 * @param value
	 * @deprecated The exact form of this has never been defined.  There is
	 * no alternative method available.
	 */
	public void setOverrides(Object value);

	/**
	 * Returns descriptions for all bundles currently resolved in this state.
	 * 
	 * @return the descriptions for all bundles currently resolved in this
	 * state.
	 */
	public BundleDescription[] getResolvedBundles();

	/**
	 * Returns descriptions for all bundles in a removal pending state.
	 * @return the descriptions for all bundles in a removal pending state.
	 * @since 3.7
	 */
	public BundleDescription[] getRemovalPending();

	/**
	 * Returns the dependency closure for the specified bundles.
	 * 
	 * <p>
	 * A graph of bundles is computed starting with the specified bundles. The
	 * graph is expanded by adding any bundle that is either wired to a package
	 * that is currently exported by a bundle in the graph or requires a bundle
	 * in the graph. The graph is fully constructed when there is no bundle
	 * outside the graph that is wired to a bundle in the graph. The graph may
	 * contain removal pending bundles.
	 * 
	 * @param bundles The initial bundles for which to generate the dependency
	 *        closure.
	 * @return A collection containing a snapshot of the dependency closure of
	 *         the specified bundles, or an empty collection if there were no
	 *         specified bundles.
	 * @since 3.7
	 */
	public Collection<BundleDescription> getDependencyClosure(Collection<BundleDescription> bundles);

	/**
	 * Returns whether this state is empty.
	 * @return <code>true</code> if this state is empty, <code>false</code> 
	 * 	otherwise
	 */
	public boolean isEmpty();

	/**
	 * Returns all exported packages in this state, according to the OSGi rules for resolution. 
	 * @see org.osgi.service.packageadmin.PackageAdmin#getExportedPackages(org.osgi.framework.Bundle)
	 */
	public ExportPackageDescription[] getExportedPackages();

	/**
	 * Returns all bundle descriptions with the given bundle symbolic name.
	 * @param symbolicName symbolic name of the bundles to query
	 * @return the descriptors for all bundles known to this state with the
	 * specified symbolic name.
	 */
	public BundleDescription[] getBundles(String symbolicName);

	/**
	 * Returns the factory that created this state.
	 * @return the state object factory that created this state 
	 */
	public StateObjectFactory getFactory();

	/**
	 * Attempts to find an ExportPackageDescription that will satisfy a dynamic import
	 * for the specified requestedPackage for the specified importingBundle.  If no
	 * ExportPackageDescription is available that satisfies a dynamic import for the 
	 * importingBundle then <code>null</code> is returned.
	 * @param importingBundle the BundleDescription that is requesting a dynamic package
	 * @param requestedPackage the name of the package that is being requested
	 * @return the ExportPackageDescription that satisfies the dynamic import request; 
	 * a value of <code>null</code> is returned if none is available.
	 */
	public ExportPackageDescription linkDynamicImport(BundleDescription importingBundle, String requestedPackage);

	/**
	 * Adds the specified dynamic imports to the specified importingBundle.  The added 
	 * dynamic imports are only valid for the instance of this state and will be 
	 * forgotten if this state is read from a persistent cache.
	 * @param importingBundle the bundle to add the imports to.
	 * @param dynamicImports the dynamic imports to add.
	 * @since 3.7
	 * @see BundleDescription#getAddedDynamicImportPackages()
	 */
	public void addDynamicImportPackages(BundleDescription importingBundle, ImportPackageSpecification[] dynamicImports);

	/**
	 * Sets the platform properties of the state.  The platform properties
	 * are used to resolve the following constraints:
	 * <ul>
	 * <li> The execution environment requirements (i.e. Bundle-RequiredExecutionEnvironment).</li>
	 * <li>The platform filter requirements (i.e. Eclipse-PlatformFilter).</li>
	 * <li>The native code requirements (i.e. Bundle-NativeCode).</li>
	 * </ul>
	 * Arbitrary keys  may be used in the platform properties but the following keys have a specified meaning:
	 * <ul>
	 * <li>osgi.nl - the platform language setting.</li>
	 * <li>osgi.os - the platform operating system.</li>
	 * <li>osgi.arch - the platform architecture.</li>
	 * <li>osgi.ws - the platform windowing system.</li>
	 * <li>osgi.resolverMode - the resolver mode.  A value of "strict" will set the resolver mode to strict.</li>
	 * <li>org.osgi.framework.system.packages - the packages exported by the system bundle.</li>
	 * <li>org.osgi.framework.executionenvironment - the comma separated list of supported execution environments.  
	 * This property is then used to resolve the required execution environment the bundles in a state.</li>
	 * <li>org.osgi.framework.os.name - the name of the operating system.  This property is used to resolve the osname attribute of 
	 * bundle native code (i.e. Bundle-NativeCode).</li>
	 * <li>org.osgi.framework.os.version - the version of the operating system.  This property is used to resolve the osversion attribute 
	 * of bundle native code (i.e. Bundle-NativeCode).</li>
	 * <li>org.osgi.framework.processor - the processor name.  This property is used to resolve the processor attribute 
	 * of bundle native code (i.e. Bundle-NativeCode).</li>
	 * <li>org.osgi.framework.language - the language being used.  This property is used to resolve the language attribute 
	 * of bundle native code (i.e. Bundle-NativeCode).</li>
	 * </ul>
	 * <p>
	 * The values used for the supported properties can be <tt>String</tt> type
	 * to specify a single value for the property or they can by <tt>String[]</tt>
	 * to specify a list of values for the property. 
	 * @param platformProperties the platform properties of the state
	 * @return false if the platformProperties specified do not change any of the
	 * supported properties already set.  If any of the supported property values 
	 * are changed as a result of calling this method then true is returned.
	 */
	public boolean setPlatformProperties(Dictionary<?, ?> platformProperties);

	/**
	 * Sets the platform properties of the state to a list of platform properties.  
	 * @see #setPlatformProperties(Dictionary)
	 * 
	 * @param platformProperties a set of platform properties for the state
	 * @return false if the platformProperties specified do not change any of the
	 * supported properties already set.  If any of the supported property values 
	 * are changed as a result of calling this method then true is returned.
	 */
	public boolean setPlatformProperties(Dictionary<?, ?>[] platformProperties);

	/**
	 * Returns the list of platform properties currently set for this state.
	 * @return the list of platform properties currently set for this state.
	 */
	@SuppressWarnings("rawtypes")
	public Dictionary[] getPlatformProperties();

	/**
	 * Returns the list of system packages which are exported by the system bundle.  
	 * The list of system packages is set by the org.osgi.framework.system.packages
	 * value in the platform properties for this state.
	 * @see #setPlatformProperties(Dictionary)
	 * @return the list of system packages
	 */
	public ExportPackageDescription[] getSystemPackages();

	/**
	 * Returns a state helper object. State helpers provide convenience methods 
	 * for manipulating states. 
	 * <p>
	 * A possible implementation for this
	 * method would provide the same single StateHelper instance to all clients.
	 * </p>
	 * 
	 * @return a state helper
	 * @see StateHelper
	 * @since 3.2
	 */
	public StateHelper getStateHelper();

	/**
	 * Returns the highest bundle ID.  The value -1 is returned if no 
	 * bundles exist in this state.
	 * <p>
	 * Note that this method returns the highest bundle ID the ever existed in this 
	 * this state object.  This bundle may have been removed from the state.
	 * @return the highest bundle ID.
	 * @since 3.3
	 */
	public long getHighestBundleId();

	/**
	 * Sets the native code paths of a native code description as invalid.  Native
	 * code paths are invalid if they can not be found in the bundle content.
	 * <p>
	 * The framework, or some other entity which has access to bundle content,
	 * will call this method to validate or invalidate native code paths.
	 * </p>
	 * @param nativeCodeDescription the native code description.
	 * @param hasInvalidNativePaths true if the native code paths are invalid; false otherwise.
	 * @since 3.4
	 */
	public void setNativePathsInvalid(NativeCodeDescription nativeCodeDescription, boolean hasInvalidNativePaths);

	/**
	 * Returns an array of BundleDescriptions for the bundles that are disabled
	 * in the system. Use {@link #getDisabledInfos(BundleDescription)} to interrogate the reason that
	 * each bundle is disabled.
	 * @return the array of disabled bundles.  An empty array is returned if no bundles are disabled.
	 * @see DisabledInfo
	 * @since 3.4
	 */
	public BundleDescription[] getDisabledBundles();

	/**
	 * Adds the disabled info to this state.  If a disable info already exists
	 * for the specified policy and the specified bundle then it is replaced with 
	 * the given disabled info.
	 * @param disabledInfo the disabled info to add.
	 * @throws IllegalArgumentException if the <code>BundleDescription</code> for
	 * the specified disabled info does not exist in this state.
	 * @since 3.4
	 */
	public void addDisabledInfo(DisabledInfo disabledInfo);

	/**
	 * Removes the disabled info from the state.
	 * @param disabledInfo the disabled info to remove
	 * @since 3.4
	 */
	public void removeDisabledInfo(DisabledInfo disabledInfo);

	/**
	 * Returns an array of disabled info for the specified bundle.  If no disabled info exist
	 * then an empty array is returned.
	 * @param bundle the bundle to get the disabled info for.
	 * @return the array of disabled info.
	 * @since 3.4
	 */
	public DisabledInfo[] getDisabledInfos(BundleDescription bundle);

	/**
	 * Returns the disabled info for the specified bundle with the specified policy name.
	 * If no disabled info exists then <code>null</code> is returned.
	 * @param bundle the bundle to get the disabled info for
	 * @return the disabled info.
	 * @since 3.4
	 */
	public DisabledInfo getDisabledInfo(BundleDescription bundle, String policyName);

	/**
	 * Sets the resolver hook factory for this state.  The resolver hook factory is 
	 * used during resolve operations according to the OSGi specification for the
	 * resolver hook factory.
	 * @param hookFactory the resolver hook factory
	 * @since 3.7
	 * @throws IllegalStateException if the resolver hook factory is already set
	 */
	public void setResolverHookFactory(ResolverHookFactory hookFactory);
}
