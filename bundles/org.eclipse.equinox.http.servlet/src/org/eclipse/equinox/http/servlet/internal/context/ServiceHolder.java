/*******************************************************************************
 * Copyright (c) Feb. 2, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - initial API and implementation and/or initial
 *                    documentation
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.context;

import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.osgi.framework.*;

public final class ServiceHolder<S> implements Comparable<ServiceHolder<?>> {

	final ServiceObjects<S> serviceObjects;
	final S service;
	final Bundle bundle;
	final long serviceId;
	final int serviceRanking;
	final ClassLoader legacyTCCL;
	private volatile boolean released = false;

	public ServiceHolder(ServiceObjects<S> serviceObjects) {
		this.serviceObjects = serviceObjects;
		this.bundle = serviceObjects.getServiceReference().getBundle();
		this.service = serviceObjects.getService();
		this.legacyTCCL = (ClassLoader) serviceObjects.getServiceReference()
				.getProperty(Const.EQUINOX_LEGACY_TCCL_PROP);
		Long serviceIdProp = (Long) serviceObjects.getServiceReference().getProperty(Constants.SERVICE_ID);
		if (legacyTCCL != null) {
			// this is a legacy registration; use a negative id for the DTO
			serviceIdProp = -serviceIdProp;
		}
		this.serviceId = serviceIdProp;
		Object rankProp = serviceObjects.getServiceReference().getProperty(Constants.SERVICE_RANKING);
		this.serviceRanking = !Integer.class.isInstance(rankProp) ? 0 : ((Integer) rankProp).intValue();
	}

	public ServiceHolder(S service, Bundle bundle, long serviceId, int serviceRanking, ClassLoader legacyTCCL) {
		this.service = service;
		this.bundle = bundle;
		this.serviceObjects = null;
		this.serviceId = serviceId;
		this.serviceRanking = serviceRanking;
		this.legacyTCCL = legacyTCCL;
	}

	public S get() {
		return service;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public ClassLoader getLegacyTCCL() {
		return legacyTCCL;
	}

	public long getServiceId() {
		return serviceId;
	}

	public void release() {
		if (!released && (serviceObjects != null) && (service != null)) {
			try {
				serviceObjects.ungetService(service);
			} catch (IllegalStateException e) {
				// this can happen if the whiteboard bundle is in the process of stopping
				// and the framework is in the middle of auto-unregistering any services
				// the bundle forgot to unregister on stop
			} finally {
				released = true;
			}
		}
	}

	public ServiceReference<S> getServiceReference() {
		return serviceObjects == null ? null : serviceObjects.getServiceReference();
	}

	@Override
	public int compareTo(ServiceHolder<?> o) {
		final int thisRanking = serviceRanking;
		final int otherRanking = o.serviceRanking;
		if (thisRanking != otherRanking) {
			if (thisRanking < otherRanking) {
				return 1;
			}
			return -1;
		}
		final long thisId = this.getServiceId();
		final long otherId = o.getServiceId();
		if (thisId == otherId) {
			return 0;
		}
		if (thisId < otherId) {
			return -1;
		}
		return 1;
	}
}
