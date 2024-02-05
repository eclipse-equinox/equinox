/*******************************************************************************
 * Copyright (c) 2018 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - Bug 530063 - CNFE when session replication
 *                    is used with equinox.http.servlet in bridge mode
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.servlet;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.http.HttpSession;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.session.HttpSessionInvalidator;

/**
 * @since 1.5
 */
public class HttpSessionTracker implements HttpSessionInvalidator {

	public HttpSessionTracker(HttpServiceRuntimeImpl httpServiceRuntime) {
		this.httpServiceRuntime = httpServiceRuntime;
	}

	@Override
	public void invalidate(String sessionId, boolean invalidateParent) {
		Set<HttpSessionAdaptor> httpSessionAdaptors = httpSessionAdaptorsMap.remove(sessionId);

		if (httpSessionAdaptors == null) {
			return;
		}

		HttpSession parentSession = null;

		for (HttpSessionAdaptor httpSessionAdaptor : httpSessionAdaptors) {
			parentSession = httpSessionAdaptor.getSession();

			httpSessionAdaptor.invalidate();
		}

		if (invalidateParent && parentSession != null) {
			try {
				parentSession.invalidate();
			} catch (IllegalStateException ise) {
				httpServiceRuntime.debug("Session was already invalidated: " + parentSession.getId(), ise); //$NON-NLS-1$
			}
		}
	}

	public void addHttpSessionAdaptor(String sessionId, HttpSessionAdaptor httpSessionAdaptor) {

		Set<HttpSessionAdaptor> httpSessionAdaptors = httpSessionAdaptorsMap.get(sessionId);

		if (httpSessionAdaptors == null) {
			httpSessionAdaptors = Collections.newSetFromMap(new ConcurrentHashMap<HttpSessionAdaptor, Boolean>());

			Set<HttpSessionAdaptor> previousHttpSessionAdaptors = httpSessionAdaptorsMap.putIfAbsent(sessionId,
					httpSessionAdaptors);

			if (previousHttpSessionAdaptors != null) {
				httpSessionAdaptors = previousHttpSessionAdaptors;
			}
		}

		httpSessionAdaptors.add(httpSessionAdaptor);
	}

	public void clear() {
		// At this point there should be no left over sessions. If
		// there are we'll log it because there's some kind of leak.
		if (!httpSessionAdaptorsMap.isEmpty()) {
			httpServiceRuntime
					.debug("There are HttpSessionAdaptors left over. There might be a context or session leak!"); //$NON-NLS-1$
		}
	}

	public boolean removeHttpSessionAdaptor(String sessionId, HttpSessionAdaptor httpSessionAdaptor) {

		Set<HttpSessionAdaptor> httpSessionAdaptors = httpSessionAdaptorsMap.get(sessionId);

		if (httpSessionAdaptors == null) {
			return false;
		}

		try {
			return httpSessionAdaptors.remove(httpSessionAdaptor);
		} finally {
			if (httpSessionAdaptors.isEmpty()) {
				httpSessionAdaptorsMap.remove(sessionId, httpSessionAdaptors);
			}
		}
	}

	private final ConcurrentMap<String, Set<HttpSessionAdaptor>> httpSessionAdaptorsMap = new ConcurrentHashMap<>();
	private final HttpServiceRuntimeImpl httpServiceRuntime;

}
