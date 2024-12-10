/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sergey Prigogin (Google) - use parameterized types (bug 442021)
 *******************************************************************************/
package org.eclipse.core.runtime;

import org.eclipse.equinox.api.internal.APISupport;

/**
 * An abstract superclass implementing the <code>IAdaptable</code> interface.
 * <code>getAdapter</code> invocations are directed to the platform's adapter
 * manager.
 * <p>
 * Note: In situations where it would be awkward to subclass this class, the
 * same effect can be achieved simply by implementing the {@link IAdaptable}
 * interface and explicitly forwarding the <code>getAdapter</code> request to an
 * implementation of the {@link IAdapterManager} service. The method would look
 * like:
 * </p>
 * 
 * <pre>
 *     public &lt;T&gt; T getAdapter(Class&lt;T&gt; adapter) {
 *         IAdapterManager manager = ...;//lookup the IAdapterManager service         
 *         return manager.getAdapter(this, adapter);
 *     }
 * </pre>
 * <p>
 * This class can be used without OSGi running.
 * </p>
 * <p>
 * Clients may subclass.
 * </p>
 *
 * @see IAdapterManager
 * @see IAdaptable
 */
public abstract class PlatformObject implements IAdaptable {
	/**
	 * Constructs a new platform object.
	 */
	public PlatformObject() {
		super();
	}

	/**
	 * Returns an object which is an instance of the given class associated with
	 * this object. Returns <code>null</code> if no such object can be found.
	 * <p>
	 * This implementation of the method declared by <code>IAdaptable</code> passes
	 * the request along to the platform's adapter manager; roughly
	 * <code>Platform.getAdapterManager().getAdapter(this, adapter)</code>.
	 * Subclasses may override this method (however, if they do so, they should
	 * invoke the method on their superclass to ensure that the Platform's adapter
	 * manager is consulted).
	 * </p>
	 *
	 * @param adapter the class to adapt to
	 * @return the adapted object or <code>null</code>
	 * @see IAdaptable#getAdapter(Class)
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return APISupport.getAdapterManager().getAdapter(this, adapter);
	}
}
