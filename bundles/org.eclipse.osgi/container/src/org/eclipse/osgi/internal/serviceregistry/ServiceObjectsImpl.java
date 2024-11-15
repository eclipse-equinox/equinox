/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.osgi.internal.serviceregistry;

import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.messages.Msg;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * ServiceObjects implementation.
 */
public class ServiceObjectsImpl<S> implements ServiceObjects<S> {
	/** Service registration */
	private final ServiceRegistrationImpl<S> registration;
	/** Reference to service */
	private final ServiceReference<S> reference;
	/** BundleContext associated with this service use */
	private final BundleContextImpl user;

	/**
	 * Constructs a service objects encapsulating the service object.
	 *
	 * @param user         bundle getting the service
	 * @param registration ServiceRegistration of the service
	 */
	ServiceObjectsImpl(BundleContextImpl user, ServiceRegistrationImpl<S> registration) {
		this.registration = registration;
		this.reference = registration.getReference();
		this.user = user;
	}

	/**
	 * Returns a service object for the {@link #getServiceReference() referenced}
	 * service.
	 *
	 * <p>
	 * This {@code ServiceObjects} object can be used to obtain multiple service
	 * objects for the referenced service if the service has
	 * {@link Constants#SCOPE_PROTOTYPE prototype} scope. If the referenced service
	 * has {@link Constants#SCOPE_SINGLETON singleton} or
	 * {@link Constants#SCOPE_BUNDLE bundle} scope, this method behaves the same as
	 * calling the {@link BundleContext#getService(ServiceReference)} method for the
	 * referenced service. That is, only one, use-counted service object is
	 * available from this {@link ServiceObjects} object.
	 *
	 * <p>
	 * This method will always return {@code null} when the referenced service has
	 * been unregistered.
	 *
	 * <p>
	 * For a prototype scope service, the following steps are required to get the
	 * service object:
	 * <ol>
	 * <li>If the referenced service has been unregistered, {@code null} is
	 * returned.</li>
	 * <li>The
	 * {@link PrototypeServiceFactory#getService(Bundle, ServiceRegistration)}
	 * method is called to create a service object for the caller.</li>
	 * <li>If the service object returned by the {@code PrototypeServiceFactory}
	 * object is {@code null}, not an {@code instanceof} all the classes named when
	 * the service was registered or the {@code PrototypeServiceFactory} object
	 * throws an exception, {@code null} is returned and a Framework event of type
	 * {@link FrameworkEvent#ERROR} containing a {@link ServiceException} describing
	 * the error is fired.</li>
	 * <li>The service object is returned.</li>
	 * </ol>
	 *
	 * @return A service object for the referenced service or {@code null} if the
	 *         service is not registered, the service object returned by a
	 *         {@code ServiceFactory} does not implement the classes under which it
	 *         was registered or the {@code ServiceFactory} threw an exception.
	 * @throws IllegalStateException If the BundleContext used to create this
	 *                               {@code ServiceObjects} object is no longer
	 *                               valid.
	 * @see #ungetService(Object)
	 */
	@Override
	public S getService() {
		user.checkValid();
		return registration.getService(user, ServiceConsumer.prototypeConsumer);
	}

	/**
	 * Releases a service object for the {@link #getServiceReference() referenced}
	 * service.
	 *
	 * <p>
	 * This {@code ServiceObjects} object can be used to obtain multiple service
	 * objects for the referenced service if the service has
	 * {@link Constants#SCOPE_PROTOTYPE prototype} scope. If the referenced service
	 * has {@link Constants#SCOPE_SINGLETON singleton} or
	 * {@link Constants#SCOPE_BUNDLE bundle} scope, this method behaves the same as
	 * calling the {@link BundleContext#ungetService(ServiceReference)} method for
	 * the referenced service. That is, only one, use-counted service object is
	 * available from this {@link ServiceObjects} object.
	 *
	 * <p>
	 * For a prototype scope service, the following steps are required to release
	 * the service object:
	 * <ol>
	 * <li>If the referenced service has been unregistered, this method returns
	 * without doing anything.</li>
	 * <li>The
	 * {@link PrototypeServiceFactory#ungetService(Bundle, ServiceRegistration, Object)}
	 * method is called to release the specified service object.</li>
	 * </ol>
	 *
	 * <p>
	 * The specified service object must no longer be used and all references to it
	 * should be destroyed after calling this method.
	 *
	 * @param service A service object previously provided by this
	 *                {@code ServiceObjects} object.
	 * @throws IllegalStateException    If the BundleContext used to create this
	 *                                  {@code ServiceObjects} object is no longer
	 *                                  valid.
	 * @throws IllegalArgumentException If the specified service was not provided by
	 *                                  this {@code ServiceObjects} object.
	 * @see #getService()
	 */
	@Override
	public void ungetService(S service) {
		boolean removed = registration.ungetService(user, ServiceConsumer.prototypeConsumer, service);
		if (!removed) {
			if (registration.isUnregistered()) {
				return;
			}
			if (user.isValid()) {
				throw new IllegalArgumentException(Msg.SERVICE_OBJECTS_UNGET_ARGUMENT_EXCEPTION);
			}
		}
	}

	/**
	 * Returns the {@link ServiceReference} for the service associated with this
	 * {@code ServiceObjects} object.
	 *
	 * @return The {@link ServiceReference} for the service associated with this
	 *         {@code ServiceObjects} object.
	 */
	@Override
	public ServiceReference<S> getServiceReference() {
		return reference;
	}
}
