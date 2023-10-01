/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Christoph Laeubrich - Bug 567344 - Support registration of IAdapterFactory as OSGi Service
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * An adapter factory defines behavioral extensions for one or more classes that
 * implements the <code>IAdaptable</code> interface. Adapter factories are
 * registered with an adapter manager.
 * <p>
 * This interface can be used without OSGi running.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @see IAdapterManager
 * @see IAdaptable
 */
public interface IAdapterFactory {

	/**
	 * Service property to use when registering a factory as OSGi-service to declare
	 * the adaptable class type, this is a multi-string-property, if more than one
	 * is given the factory will be register multiple times
	 * 
	 * @since 3.14
	 */
	static final String SERVICE_PROPERTY_ADAPTABLE_CLASS = "adaptableClass"; //$NON-NLS-1$

	/**
	 * Optional service property to use when registering a factory as OSGi-service
	 * to declare the possible adapter types. If the property is given, the service
	 * is only queried when actually required, this is a multi-string-property.
	 * 
	 * @since 3.14
	 */
	static final String SERVICE_PROPERTY_ADAPTER_NAMES = "adapterNames"; //$NON-NLS-1$

	/**
	 * Returns an object which is an instance of the given class associated with the
	 * given object. Returns <code>null</code> if no such object can be found.
	 *
	 * @param adaptableObject the adaptable object being queried (usually an
	 *                        instance of <code>IAdaptable</code>)
	 * @param adapterType     the type of adapter to look up
	 * @return a object of the given adapter type, or <code>null</code> if this
	 *         adapter factory does not have an adapter of the given type for the
	 *         given object
	 */
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType);

	/**
	 * Returns the collection of adapter types handled by this factory.
	 * <p>
	 * This method is generally used by an adapter manager to discover which adapter
	 * types are supported, in advance of dispatching any actual
	 * <code>getAdapter</code> requests.
	 * </p>
	 *
	 * @return the collection of adapter types
	 */
	public Class<?>[] getAdapterList();
}
