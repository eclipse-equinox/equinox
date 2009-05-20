/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.auth.events;

import java.util.Iterator;
import java.util.Vector;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import org.eclipse.equinox.security.auth.ILoginContextListener;

public class SecurityEventsManager {

	private Vector listeners = new Vector(5);

	synchronized public void addListener(ILoginContextListener listener) {
		listeners.add(listener);
	}

	synchronized public void removeListener(ILoginContextListener listener) {
		listeners.remove(listener);
	}

	public void notifyLoginBegin(Subject subject) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			Object listener = i.next();
			if (listener instanceof ILoginContextListener)
				((ILoginContextListener) listener).onLoginStart(subject);
		}
	}

	public void notifyLoginEnd(Subject subject, LoginException loginException) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			Object listener = i.next();
			if (listener instanceof ILoginContextListener)
				((ILoginContextListener) listener).onLoginFinish(subject, loginException);
		}
	}

	public void notifyLogoutBegin(Subject subject) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			Object listener = i.next();
			if (listener instanceof ILoginContextListener)
				((ILoginContextListener) listener).onLogoutStart(subject);
		}
	}

	public void notifyLogoutEnd(Subject subject, LoginException loginException) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			Object listener = i.next();
			if (listener instanceof ILoginContextListener)
				((ILoginContextListener) listener).onLogoutFinish(subject, loginException);
		}
	}
}
