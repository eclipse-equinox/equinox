/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.provisional.service.security;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.framework.eventmgr.*;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * An authorization engine is used to grant authorization to {@link SignedContent}.
 * For example, an engine could determine if <code>SignedContent</code> is authorized
 * to enable code from a signed bundle.
 * @since 3.4
 */
public abstract class AuthorizationEngine {

	private EventManager manager = new EventManager();
	private EventDispatcher<AuthorizationListener, Object, AuthorizationEvent> dispatcher = new AuthEventDispatcher();
	private final ServiceTracker<AuthorizationListener, AuthorizationListener> listenerTracker;

	public AuthorizationEngine(BundleContext context) {
		listenerTracker = new ServiceTracker<AuthorizationListener, AuthorizationListener>(context, AuthorizationListener.class.getName(), null);
		listenerTracker.open();
	}

	/**
	 * Authorizes a <code>SignedContent</code> object.  The engine determines if the 
	 * signed content authorization should be granted.  The context is the entity 
	 * associated with the signed content.  For example, signed content 
	 * for a bundle will have a <code>Bundle</code> object as the context.
	 * @param content the signed content. The value may be <code>null</code>.
	 * @param context the context associated with the signed content. The value may be <code>null</code>.
	 */
	public final void authorize(SignedContent content, Object context) {
		fireEvent(doAuthorize(content, context));
	}

	private void fireEvent(AuthorizationEvent event) {
		if (event == null)
			return;
		Object[] services = listenerTracker.getServices();
		if (services == null)
			return;
		Map<AuthorizationListener, Object> listeners = new HashMap<AuthorizationListener, Object>();
		for (Object service : services) {
			listeners.put((AuthorizationListener) service, service);
		}
		ListenerQueue<AuthorizationListener, Object, AuthorizationEvent> queue = new ListenerQueue<AuthorizationListener, Object, AuthorizationEvent>(manager);
		queue.queueListeners(listeners.entrySet(), dispatcher);
		queue.dispatchEventSynchronous(0, event);
	}

	/**
	 * Authorizes a <code>SignedContent</code> object.  The engine determines if the 
	 * signed content authorization should be granted.
	 * @param content
	 * @param context the context associated with the signed content
	 * @return an authorization event which will be fired.  A value of <code>null</code>
	 * may be returned; in this case no authorization event will be fired.
	 */
	protected abstract AuthorizationEvent doAuthorize(SignedContent content, Object context);

	/**
	 * Return the current status of the Authorization system.
	 * 
	 * @return A value of {@link AuthorizationStatus#OK} or {@link AuthorizationStatus#ERROR}
	 * @see AuthorizationStatus#OK
	 * @see AuthorizationStatus#ERROR
	 */
	abstract public int getStatus();

	class AuthEventDispatcher implements EventDispatcher<AuthorizationListener, Object, AuthorizationEvent> {
		public void dispatchEvent(AuthorizationListener eventListener, Object listenerObject, int eventAction, AuthorizationEvent eventObject) {
			eventListener.authorizationEvent(eventObject);
		}
	}
}
