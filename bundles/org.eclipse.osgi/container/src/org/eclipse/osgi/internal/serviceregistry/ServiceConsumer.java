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

/**
 * Closure to allow sharing the same code for getting and ungetting a service.
 * The {@link #prototypeConsumer} closure must be used for calls from
 * ServiceObjects and the {@link #singletonConsumer} closure must be used for
 * calls from BundleContext.
 *
 * The closure instance calls the correct method on the specified ServiceUse
 * object for the current service consumer type.
 */
public interface ServiceConsumer {
	/**
	 * Used for calls from ServiceObjects.
	 */
	static ServiceConsumer prototypeConsumer = new ServiceConsumer() {
		@Override
		public <S> S getService(ServiceUse<S> use) {
			return use.newServiceObject();
		}

		@Override
		public <S> boolean ungetService(ServiceUse<S> use, S service) {
			return use.releaseServiceObject(service);
		}
	};

	/**
	 * Used for calls from BundleContext.
	 */
	static ServiceConsumer singletonConsumer = new ServiceConsumer() {
		@Override
		public <S> S getService(ServiceUse<S> use) {
			return use.getService();
		}

		@Override
		public <S> boolean ungetService(ServiceUse<S> use, S service) {
			return use.ungetService();
		}
	};

	/**
	 * Get a service for the consumer.
	 *
	 * @param use Service Use object to get the service from.
	 * @return The obtained service.
	 */
	<S> S getService(ServiceUse<S> use);

	/**
	 * Unget the service for the consumer.
	 *
	 * @param use     Service Use object to unget the service from.
	 * @param service The Service to unget.
	 * @return true if the service was ungotten, false otherwise.
	 */
	<S> boolean ungetService(ServiceUse<S> use, S service);
}
