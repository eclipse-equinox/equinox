/*******************************************************************************
 * Copyright (c) 2018 Liferay, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Liferay, Inc. - Bug 530063 - CNFE when session replication
 *                    is used with equinox.http.servlet in bridge mode
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.servlet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.util.EventListeners;
import org.eclipse.equinox.http.servlet.session.HttpSessionInvalidator;

/**
 * @since 1.4
 */
public class HttpSessionTracker implements HttpSessionInvalidator {

	public HttpSessionTracker(HttpServiceRuntimeImpl httpServiceRuntime) {
		this.httpServiceRuntime = httpServiceRuntime;
	}

	@Override
	public void invalidate(String sessionId, boolean invalidateParent) {
		Set<HttpSessionAdaptor> httpSessionAdaptors =
			httpSessionAdaptorsMap.remove(sessionId);

		if (httpSessionAdaptors == null) {
			return;
		}

		for (HttpSessionAdaptor httpSessionAdaptor : httpSessionAdaptors) {
			ContextController contextController =
				httpSessionAdaptor.getController();

			EventListeners eventListeners =
				contextController.getEventListeners();

			List<HttpSessionListener> httpSessionListeners = eventListeners.get(
				HttpSessionListener.class);

			if (!httpSessionListeners.isEmpty()) {
				HttpSessionEvent httpSessionEvent = new HttpSessionEvent(
					httpSessionAdaptor);

				for (HttpSessionListener listener : httpSessionListeners) {
					try {
						listener.sessionDestroyed(httpSessionEvent);
					}
					catch (IllegalStateException ise) {
						// outer session is already invalidated
					}
				}
			}

			List<HttpSessionAttributeListener> httpSessionAttributeListeners =
				eventListeners.get(HttpSessionAttributeListener.class);

			if (!httpSessionListeners.isEmpty()) {
				Enumeration<String> enumeration =
					httpSessionAdaptor.getAttributeNames();

				while (enumeration.hasMoreElements()) {
					HttpSessionBindingEvent httpSessionBindingEvent =
						new HttpSessionBindingEvent(
							httpSessionAdaptor, enumeration.nextElement());

					for (HttpSessionAttributeListener
							httpSessionAttributeListener :
								httpSessionAttributeListeners) {

						httpSessionAttributeListener.attributeRemoved(
							httpSessionBindingEvent);
					}
				}
			}

			contextController.removeActiveSession(
				httpSessionAdaptor.getSession());

			if (invalidateParent) {
				try {
					httpSessionAdaptor.getSession().invalidate();
				}
				catch (IllegalStateException ise) {
					httpServiceRuntime.log(
						"Session was already invalidated!", ise); //$NON-NLS-1$
				}
			}
		}
	}

	public void addHttpSessionAdaptor(
		String sessionId, HttpSessionAdaptor httpSessionAdaptor) {

		Set<HttpSessionAdaptor> httpSessionAdaptors =
			httpSessionAdaptorsMap.get(sessionId);

		if (httpSessionAdaptors == null) {
			httpSessionAdaptors = Collections.newSetFromMap(
				new ConcurrentHashMap<HttpSessionAdaptor, Boolean>());

			Set<HttpSessionAdaptor> previousHttpSessionAdaptors =
				httpSessionAdaptorsMap.putIfAbsent(
					sessionId, httpSessionAdaptors);

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
			httpServiceRuntime.log(
				"There are HttpSessionAdaptors left over. There might be a context or session leak!"); //$NON-NLS-1$
		}
	}

	public boolean removeHttpSessionAdaptor(
		String sessionId, HttpSessionAdaptor httpSessionAdaptor) {

		Set<HttpSessionAdaptor> httpSessionAdaptors =
			httpSessionAdaptorsMap.get(sessionId);

		if (httpSessionAdaptors == null) {
			return false;
		}

		try {
			return httpSessionAdaptors.remove(httpSessionAdaptor);
		}
		finally {
			if (httpSessionAdaptors.isEmpty()) {
				httpSessionAdaptorsMap.remove(sessionId, httpSessionAdaptors);
			}
		}
	}

	private final ConcurrentMap<String, Set<HttpSessionAdaptor>>
		httpSessionAdaptorsMap =
			new ConcurrentHashMap<String, Set<HttpSessionAdaptor>>();
	private final HttpServiceRuntimeImpl httpServiceRuntime;

}