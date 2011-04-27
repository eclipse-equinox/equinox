/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import org.osgi.framework.BundleException;

/**
 * Framework service which allows bundle programmers to inspect the bundles and
 * packages known to the Framework.  The PlatformAdmin service also allows bundles
 * with sufficient privileges to update the state of the framework by committing a new
 * configuration of bundles and packages.
 *
 * If present, there will only be a single instance of this service
 * registered with the Framework.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface PlatformAdmin {

	/** 
	 * Returns a mutable state representing the current system.
	 * <p>
	 * This is a convenience method, fully equivalent to 
	 * <code>getState(true)</code>.
	 * </p> 
	 * @return a state representing the current framework.
	 */
	public State getState();

	/** 
	 * Returns a state representing the current system. If there is need to make
	 * changes to the returned state, a mutable state must be requested. 
	 * Otherwise, an immutable state should be requested. In this case, invoking 
	 * any of the operations that could cause the state to be changed will throw 
	 * an <code>java.lang.UnsupportedOperationException</code>.
	 * <p>
	 * If a mutable state is requested, the resulting state will <strong>not</strong> 
	 * be resolved and the user objects from the system state bundle descriptions will 
	 * not be copied.
	 * </p> 
	 * @param mutable whether the returned state should mutable
	 * @return a state representing the current framework.
	 */
	public State getState(boolean mutable);

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
	 */
	public StateHelper getStateHelper();

	/**
	 * Commit the differences between the current state and the given state.
	 * The given state must return true from State.isResolved() or an exception 
	 * is thrown.  The resolved state is committed verbatim, as-is.  
	 * 
	 * @param state the future state of the framework
	 * @throws BundleException if the id of the given state does not match that of the
	 * 	current state or if the given state is not resolved.
	 */
	public void commit(State state) throws BundleException;

	/**
	 * Returns a resolver supplied by the system.  The returned resolver 
	 * will not be associated with any state.
	 * @return a system resolver
	 * @deprecated in favour of {@link #createResolver()}.
	 */
	public Resolver getResolver();

	/**
	 * Creates a new {@link Resolver} that is not associated with any {@link State}.
	 * @return the new <code>Resolver</code>.
	 * @since 3.5
	 */
	public Resolver createResolver();

	/**
	 * Returns a factory that knows how to create state objects, such as bundle 
	 * descriptions and the different types of version constraints.
	 * @return a state object factory
	 */
	public StateObjectFactory getFactory();

	/**
	 * Adds the disabled info to the state managed by this platform admin. 
	 *  If a disable info already exists for the specified policy and the specified bundle 
	 *  then it is replaced with the given disabled info.
	 * @param disabledInfo the disabled info to add.
	 * @throws IllegalArgumentException if the <code>BundleDescription</code> for
	 * the specified disabled info does not exist in the state managed by this platform admin.
	 * @since 3.4
	 */
	public void addDisabledInfo(DisabledInfo disabledInfo);

	/**
	 * Removes the disabled info from the state managed by this platform admin.
	 * @param disabledInfo the disabled info to remove
	 * @since 3.4
	 */
	public void removeDisabledInfo(DisabledInfo disabledInfo);
}
