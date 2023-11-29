/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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

import java.util.Dictionary;
import org.eclipse.osgi.internal.framework.DTOBuilder;
import org.osgi.framework.AdaptPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;

/**
 * A reference to a service.
 *
 * <p>
 * The Framework returns <code>ServiceReference</code> objects from the
 * <code>BundleContext.getServiceReference</code> and
 * <code>BundleContext.getServiceReferences</code> methods.
 * <p>
 * A <code>ServiceReference</code> object may be shared between bundles and
 * can be used to examine the properties of the service and to get the service
 * object.
 * <p>
 * Every service registered in the Framework has a unique
 * <code>ServiceRegistration</code> object and may have multiple, distinct
 * <code>ServiceReference</code> objects referring to it.
 * <code>ServiceReference</code> objects associated with a
 * <code>ServiceRegistration</code> object have the same <code>hashCode</code>
 * and are considered equal (more specifically, their <code>equals()</code>
 * method will return <code>true</code> when compared).
 * <p>
 * If the same service object is registered multiple times,
 * <code>ServiceReference</code> objects associated with different
 * <code>ServiceRegistration</code> objects are not equal.
 *
 * @see BundleContext#getServiceReference
 * @see BundleContext#getServiceReferences
 * @see BundleContext#getService
 * @ThreadSafe
 */
public class ServiceReferenceImpl<S> implements ServiceReference<S> {
	/** Registered Service object. */
	private final ServiceRegistrationImpl<S> registration;

	/**
	 * Construct a reference.
	 */
	ServiceReferenceImpl(ServiceRegistrationImpl<S> registration) {
		this.registration = registration;
		/* We must not dereference registration in the constructor
		 * since it is "leaked" to us in the ServiceRegistrationImpl
		 * constructor.
		 */
	}

	/**
	 * Returns the property value to which the specified property key is mapped
	 * in the properties <code>Dictionary</code> object of the service
	 * referenced by this <code>ServiceReference</code> object.
	 *
	 * <p>
	 * Property keys are case-insensitive.
	 *
	 * <p>
	 * This method must continue to return property values after the service has
	 * been unregistered. This is so references to unregistered services (for
	 * example, <code>ServiceReference</code> objects stored in the log) can
	 * still be interrogated.
	 *
	 * @param key The property key.
	 * @return The property value to which the key is mapped; <code>null</code>
	 *         if there is no property named after the key.
	 */
	@Override
	public Object getProperty(String key) {
		return registration.getProperty(key);
	}

	/**
	 * Returns an array of the keys in the properties <code>Dictionary</code>
	 * object of the service referenced by this <code>ServiceReference</code>
	 * object.
	 *
	 * <p>
	 * This method will continue to return the keys after the service has been
	 * unregistered. This is so references to unregistered services (for
	 * example, <code>ServiceReference</code> objects stored in the log) can
	 * still be interrogated.
	 *
	 * <p>
	 * This method is <i>case-preserving </i>; this means that every key in the
	 * returned array must have the same case as the corresponding key in the
	 * properties <code>Dictionary</code> that was passed to the
	 * {@link BundleContext#registerService(String[],Object,java.util.Dictionary)}
	 * or {@link ServiceRegistration#setProperties} methods.
	 *
	 * @return An array of property keys.
	 */
	@Override
	public String[] getPropertyKeys() {
		return registration.getPropertyKeys();
	}

	/**
	 * Returns the bundle that registered the service referenced by this
	 * <code>ServiceReference</code> object.
	 *
	 * <p>
	 * This method must return <code>null</code> when the service has been
	 * unregistered. This can be used to determine if the service has been
	 * unregistered.
	 *
	 * @return The bundle that registered the service referenced by this
	 *         <code>ServiceReference</code> object; <code>null</code> if
	 *         that service has already been unregistered.
	 * @see BundleContext#registerService(String[],Object,java.util.Dictionary)
	 */
	@Override
	public Bundle getBundle() {
		return registration.getBundle();
	}

	/**
	 * Returns the bundles that are using the service referenced by this
	 * <code>ServiceReference</code> object. Specifically, this method returns
	 * the bundles whose usage count for that service is greater than zero.
	 *
	 * @return An array of bundles whose usage count for the service referenced
	 *         by this <code>ServiceReference</code> object is greater than
	 *         zero; <code>null</code> if no bundles are currently using that
	 *         service.
	 *
	 * @since 1.1
	 */
	@Override
	public Bundle[] getUsingBundles() {
		return registration.getUsingBundles();
	}

	/**
	 * Tests if the bundle that registered the service referenced by this
	 * <code>ServiceReference</code> and the specified bundle use the same
	 * source for the package of the specified class name.
	 * <p>
	 * This method performs the following checks:
	 * <ol>
	 * <li>Get the package name from the specified class name.</li>
	 * <li>For the bundle that registered the service referenced by this
	 * <code>ServiceReference</code> (registrant bundle); find the source for
	 * the package. If no source is found then return <code>true</code> if the
	 * registrant bundle is equal to the specified bundle; otherwise return
	 * <code>false</code>.</li>
	 * <li>If the package source of the registrant bundle is equal to the
	 * package source of the specified bundle then return <code>true</code>;
	 * otherwise return <code>false</code>.</li>
	 * </ol>
	 *
	 * @param bundle The <code>Bundle</code> object to check.
	 * @param className The class name to check.
	 * @return <code>true</code> if the bundle which registered the service
	 *         referenced by this <code>ServiceReference</code> and the
	 *         specified bundle use the same source for the package of the
	 *         specified class name. Otherwise <code>false</code> is returned.
	 *
	 * @since 1.3
	 */
	@Override
	public boolean isAssignableTo(Bundle bundle, String className) {
		return registration.isAssignableTo(bundle, className, true);
	}

	/**
	 * Compares this <code>ServiceReference</code> with the specified
	 * <code>ServiceReference</code> for order.
	 *
	 * <p>
	 * If this <code>ServiceReference</code> and the specified
	 * <code>ServiceReference</code> have the same
	 * {@link Constants#SERVICE_ID service id} they are equal. This
	 * <code>ServiceReference</code> is less than the specified
	 * <code>ServiceReference</code> if it has a lower
	 * {@link Constants#SERVICE_RANKING service ranking} and greater if it has a
	 * higher service ranking. Otherwise, if this <code>ServiceReference</code>
	 * and the specified <code>ServiceReference</code> have the same
	 * {@link Constants#SERVICE_RANKING service ranking}, this
	 * <code>ServiceReference</code> is less than the specified
	 * <code>ServiceReference</code> if it has a higher
	 * {@link Constants#SERVICE_ID service id} and greater if it has a lower
	 * service id.
	 *
	 * @param object The <code>ServiceReference</code> to be compared.
	 * @return Returns a negative integer, zero, or a positive integer if this
	 *         <code>ServiceReference</code> is less than, equal to, or
	 *         greater than the specified <code>ServiceReference</code>.
	 * @since 1.4
	 */
	@Override
	public int compareTo(Object object) {
		ServiceRegistrationImpl<?> other = ((ServiceReferenceImpl<?>) object).registration;

		final int thisRanking = registration.getRanking();
		final int otherRanking = other.getRanking();
		if (thisRanking != otherRanking) {
			if (thisRanking < otherRanking) {
				return -1;
			}
			return 1;
		}
		final long thisId = registration.getId();
		final long otherId = other.getId();
		if (thisId == otherId) {
			return 0;
		}
		if (thisId < otherId) {
			return 1;
		}
		return -1;
	}

	/**
	 * Returns a hash code value for the object.
	 *
	 * @return  a hash code value for this object.
	 */
	@Override
	public int hashCode() {
		return registration.hashCode();
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 *
	 * @param   obj   the reference object with which to compare.
	 * @return  <code>true</code> if this object is the same as the obj
	 *          argument; <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof ServiceReferenceImpl<?>)) {
			return false;
		}

		ServiceReferenceImpl<?> other = (ServiceReferenceImpl<?>) obj;

		return registration == other.registration;
	}

	/**
	 * Return a string representation of this reference.
	 *
	 * @return String
	 */
	@Override
	public String toString() {
		return registration.toString();
	}

	/**
	 * Return the ServiceRegistrationImpl for this ServiceReferenceImpl.
	 *
	 * @return The ServiceRegistrationImpl for this ServiceReferenceImpl.
	 */
	public ServiceRegistrationImpl<S> getRegistration() {
		return registration;
	}

	/**
	 * Return the classes under which the referenced service was registered.
	 *
	 * @return array of class names.
	 */
	String[] getClasses() {
		return registration.getClasses();
	}

	/**
	 * Returns a copy of the properties of the service referenced by this
	 * {@code ServiceReference} object.
	 * <p>
	 * This method will continue to return the properties after the service has
	 * been unregistered. This is so references to unregistered services (for
	 * example, {@code ServiceReference} objects stored in the log) can still be
	 * interrogated.
	 * <p>
	 * The returned {@code Dictionary} object:
	 * <ul>
	 * <li>Must map property values by using property keys in a
	 * <i>case-insensitive manner</i>.</li>
	 * <li>Must return property keys is a <i>case-preserving</i> manner. This
	 * means that the keys must have the same case as the corresponding key in
	 * the properties {@code Dictionary} that was passed to the
	 * {@link BundleContext#registerService(String[],Object,Dictionary)} or
	 * {@link ServiceRegistration#setProperties(Dictionary)} methods.</li>
	 * <li>Is the property of the caller and can be modified by the caller but
	 * any changes are not reflected in the properties of the service.
	 * {@link ServiceRegistration#setProperties(Dictionary)} must be called to
	 * modify the properties of the service.</li>
	 * </ul>
	 *
	 * @return A copy of the properties of the service referenced by this
	 *         {@code ServiceReference} object
	 * @since 1.9
	 */
	@Override
	public Dictionary<String, Object> getProperties() {
		return registration.getPropertiesCopy();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> A adapt(Class<A> type) {
		checkAdaptPermission(type);
		if (ServiceReferenceDTO.class.equals(type)) {
			return (A) DTOBuilder.newServiceReferenceDTO(this);
		}
		return null;
	}

	private <A> void checkAdaptPermission(Class<A> adapterType) {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null) {
			return;
		}
		sm.checkPermission(new AdaptPermission(adapterType.getName(), registration.getRegisteringBundle(), AdaptPermission.ADAPT));
	}
}
