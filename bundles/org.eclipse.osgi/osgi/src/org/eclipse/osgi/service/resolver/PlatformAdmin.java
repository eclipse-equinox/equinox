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
 */
public interface PlatformAdmin {

	/** 
	 * Returns a state representing the current system.  The returned state 
	 * will not be associated with any resolver.
	 * @return a state representing the current framework.
	 */
	public State getState();
	
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
	 */
	public Resolver getResolver();
	
	/**
	 * Returns a factory that knows how to create state objects, such as bundle 
	 * descriptions and the different types of version constraints.
	 * @return a state object factory
	 */
	public StateObjectFactory getFactory();
}
