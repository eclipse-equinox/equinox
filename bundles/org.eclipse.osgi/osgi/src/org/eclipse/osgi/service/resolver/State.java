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
package org.eclipse.osgi.service.resolver;

/**
 * The state of a system as reported by a resolver. This includes all bundles
 * presented to the resolver relative to this state (i.e., both resolved and
 * unresolved).
 */
public interface State {

	/**
	 * Adds the given bundle to this state.
	 * 
	 * @param description the description to add
	 */
	public void addBundle(BundleDescription description);

	/**
	 * Returns a delta describing the differences between this state and the
	 * given state. This state is taken as the base so the absence of a bundle
	 * in the given state is reported as a deletion, etc.
	 * 
	 * @param state
	 * @return
	 */
	public StateChangeEvent compare(State state);

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
	 * Returns the delta representing the changes from the time this state was
	 * first captured until now.
	 * 
	 * @return
	 */
	public StateDelta getChanges();

	/**
	 * Returns descriptions for all bundles known to this state.
	 * 
	 * @return the descriptions for all bundles known to this state.
	 */
	public BundleDescription[] getBundles();

	/**
	 * Returns descriptor for all bundles known to this state.
	 * 
	 * @return the descriptors for all bundles known to this state.
	 * @see BundleDescription#getBundleId()
	 */
	public BundleDescription getBundle(long id);

	/**
	 * Returns the bundle descriptor for the bundle with the given name and
	 * version. null is returned if no such bundle is found in this state. If
	 * the version argument is null then the bundle with the given name which
	 * is resolve and/or has the highest version number is returned.
	 * 
	 * @param name name of the bundle to query
	 * @param version version of the bundle to query. null matches any bundle
	 * @return the descriptor for the identified bundle
	 */
	public BundleDescription getBundle(String uniqueId, Version version);

	/**
	 * Returns the id of the state on which this state is based. This
	 * correlates this state to the system state. For example, if
	 * Resolver.getState() returns state 4 but then some bundles are installed,
	 * the system state id is updated. By comparing 4 to the current system
	 * state id it is possible to detect if the the states are out of sync.
	 * 
	 * @return the id of the state on which this state is based
	 */
	public long getTimeStamp();

	/**
	 * Returns true if there have been no modifications to this state since the
	 * last time resolve() was called.
	 * 
	 * @return whether or not this state has changed since last resolved.
	 */
	public boolean isResolved();

	/**
	 * Resolves the given version constraint with the given values. The given
	 * constraint object is destructively modified to reflect its new resolved
	 * state. Note that a constraint can be unresolved by passing null for both
	 * the actual version and the supplier.
	 * <p>
	 * This method is intended to be used by resolvers in the process of
	 * determining which constraints are satisfied by which components.
	 * </p>
	 * 
	 * @param constraint the version constraint to update
	 * @param actualVersion the version to which the constraint will be bound.
	 * May be null if the constraint is to be unresolved.
	 * @param supplier the bundle that supplies the given version which
	 * satisfies the constraint. May be null if the constraint is to be
	 * unresolved.
	 * @throws IllegalStateException if this is not done during a call to
	 * <code>resolve</code>
	 */
	public void resolveConstraint(VersionConstraint constraint, Version actualVersion, BundleDescription supplier);

	/**
	 * Sets whether or not the given bundle is selected in this state..
	 * <p>
	 * This method is intended to be used by resolvers in the process of
	 * determining which constraints are satisfied by which components.
	 * </p>
	 * 
	 * @param bundle the bundle to update
	 * @param status whether or not the given bundle is selected
	 */
	public void resolveBundle(BundleDescription bundle, int status);

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
	 * currently associated with the state in a incremental, "least-perturbing" 
	 * mode, and returns a delta describing the changes in resolved states and 
	 * dependencies in the state.
	 * 
	 * @param discard an array containing descriptions for bundles whose 
	 * 	current resolution state should be forgotten 
	 * @return a delta describing the changes in resolved state and 
	 * 	interconnections
	 */
	public StateDelta resolve(BundleDescription[] discard);

	/**
	 * Sets the version overrides which are to be applied during the resolutoin
	 * of this state. Version overrides allow external forces to
	 * refine/override the version constraints setup by the components in the
	 * state.
	 * 
	 * @param value
	 */
	// TODO the exact form of this is not defined as yet.
	public void setOverrides(Object value);
	/**
	 * Returns descriptions for all bundles currently resolved in this state.
	 * 
	 * @return the descriptions for all bundles currently resolved in this
	 * state.
	 */
	public BundleDescription[] getResolvedBundles();
	/** 
	 * Adds the given listener for change events to this state.
	 * Has no effect if an identical listener is already registered.
	 * <p>
	 * The resolver associated with the state, if implements StateChangeListener, 
	 * receives all notifications for added/removed bundlers for free. 
	 * </p>
	 * 
	 * @param listener the listener
	 * @param flags the bit-wise OR of all event types of interest to the listener 
	 * 
	 * @see BundleDelta for all available event types 
	 */
	public void addStateChangeListener(StateChangeListener listener, int flags);
	/** 
	 * Removes the given listener from the list of listeners.
	 * Has no effect if an identical listener is not registered.
	 * 
	 * @param listener the listener
	 */
	public void removeStateChangeListener(StateChangeListener listener);

	/**
	 * Returns whether this state is empty.
	 * @return <code>true</code> if this state is empty, <code>false</code> 
	 * 	otherwise
	 */
	public boolean isEmpty();

	/**
	 * Returns all exported packages in this state, according to the OSGi rules for resolution. 
	 * @see org.osgi.service.packageadmin.PackageAdmin#getExportedPackages(Bundle)
	 */
	public PackageSpecification[] getExportedPackages();

	/**
	 * Returns all bundle descriptions with the given bundle global name.  
	 */
	public BundleDescription[] getBundles(String globalName);
	
	/**
	 * Returns the factory that created this state.
	 * @return the state object factory that created this state 
	 */
	public StateObjectFactory getFactory();
}