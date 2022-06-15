/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
import org.osgi.framework.ServiceException;

/**
 * This class represents the use of a service by a bundle. One is created for each
 * service acquired by a bundle.
 *
 * <p>
 * This class manages a singleton service.
 *
 * @ThreadSafe
 */
public class ServiceUse<S> {
	/** ServiceDescription of the registered service */
	final ServiceRegistrationImpl<S> registration;

	/** bundle's use count for this service */
	/* @GuardedBy("this") */
	private int useCount;

	/**
	 * Constructs a service use encapsulating the service object.
	 *
	 * @param   context bundle getting the service
	 * @param   registration ServiceRegistration of the service
	 */
	ServiceUse(BundleContextImpl context, ServiceRegistrationImpl<S> registration) {
		this.useCount = 0;
		this.registration = registration;
	}

	/**
	 * Get a service's service object and increment the use count.
	 *
	 * @return The service object.
	 */
	/* @GuardedBy("this") */
	S getService() {
		assert Thread.holdsLock(this);
		incrementUse();
		return registration.getServiceObject();
	}

	/**
	 * Unget a service's service object.
	 *
	 * <p>
	 * Decrements the use count if the service was being used.
	 *
	 * @return true if the service was ungotten; otherwise false.
	 */
	/* @GuardedBy("this") */
	boolean ungetService() {
		assert Thread.holdsLock(this);
		if (!inUse()) {
			return false;
		}
		decrementUse();
		return true;
	}

	/**
	 * Return the service object for this service use.
	 *
	 * @return The service object.
	 */
	/* @GuardedBy("this") */
	S getCachedService() {
		return registration.getServiceObject();
	}

	/**
	 * Get a new service object for the service.
	 *
	 * <p>
	 * By default, this returns the result of {@link #getService()}.
	 *
	 * @return The service object.
	 */
	/* @GuardedBy("this") */
	S newServiceObject() {
		return getService();
	}

	/**
	 * Release a service object for the service.
	 *
	 * <p>
	 * By default, this returns the result of {@link #ungetService()}.
	 *
	 * @param service The service object to release.
	 * @return true if the service was released; otherwise false.
	 * @throws IllegalArgumentException If the specified service was not
	 *         provided by this object.
	 */
	/* @GuardedBy("this") */
	boolean releaseServiceObject(final S service) {
		if ((service == null) || (service != getCachedService())) {
			throw new IllegalArgumentException(Msg.SERVICE_OBJECTS_UNGET_ARGUMENT_EXCEPTION);
		}
		return ungetService();
	}

	/**
	 * Release all uses of the service and reset the use count to zero.
	 */
	/* @GuardedBy("this") */
	void release() {
		assert Thread.holdsLock(this);
		resetUse();
	}

	/**
	 * Is this service use using any services?
	 *
	 * @return true if no services are being used and this service use can be discarded.
	 */
	/* @GuardedBy("this") */
	boolean isEmpty() {
		assert Thread.holdsLock(this);
		return !inUse();
	}

	/**
	 * Is the use count non zero?
	 *
	 * @return true if the use count is greater than zero.
	 */
	/* @GuardedBy("this") */
	boolean inUse() {
		return useCount > 0;
	}

	/**
	 * Incrementing the use count.
	 */
	/* @GuardedBy("this") */
	void incrementUse() {
		if (useCount == Integer.MAX_VALUE) {
			throw new ServiceException(Msg.SERVICE_USE_OVERFLOW);
		}
		useCount++;
	}

	/**
	 * Decrementing the use count.
	 */
	/* @GuardedBy("this") */
	void decrementUse() {
		assert inUse();
		useCount--;
	}

	/**
	 * Reset the use count to zero.
	 */
	/* @GuardedBy("this") */
	void resetUse() {
		useCount = 0;
	}
}
