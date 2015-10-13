/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sergey Prigogin (Google) - use parameterized types (bug 442021)
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 478864
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * An interface for an adaptable object.
 * <p>
 * Adaptable objects can be dynamically extended to provide different 
 * interfaces (or "adapters").  Adapters are created by adapter 
 * factories, which are in turn managed by type by adapter managers.
 * </p>
 * For example,
 * <pre>
 *     IAdaptable a = [some adaptable];
 *     IFoo x = Adapters.getAdapter(a, IFoo.class, true);
 *     if (x != null)
 *         [do IFoo things with x]
 * </pre>
 * <p>
 * This interface can be used without OSGi running.
 * </p><p>
 * Clients may implement this interface, or obtain a default implementation
 * of this interface by subclassing <code>PlatformObject</code>.
 * </p>
 * @see IAdapterFactory
 * @see IAdapterManager
 * @see PlatformObject
 * @see Adapters
 */
public interface IAdaptable {
	/**
	 * Returns an object which is an instance of the given class
	 * associated with this object. Returns <code>null</code> if
	 * no such object can be found.
	 * <p>
	 * Clients may implement this method but should generally call {@link Adapters#adapt(Object, Class, boolean)}
	 * rather than invoking it directly.
	 *
	 * @param adapter the adapter class to look up
	 * @return a object of the given class, 
	 *    or <code>null</code> if this object does not
	 *    have an adapter for the given class
	 */
	public <T> T getAdapter(Class<T> adapter);
}
