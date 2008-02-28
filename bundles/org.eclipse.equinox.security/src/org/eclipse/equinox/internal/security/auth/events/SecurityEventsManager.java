/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.equinox.security.auth.event.*;

public class SecurityEventsManager {

	private Vector listeners = new Vector(5);

	synchronized public void addListener(ISecurityListener listener) {
		listeners.add(listener);
	}

	synchronized public void removeListener(ISecurityListener listener) {
		listeners.remove(listener);
	}

	public void notifyLoginBegin(Subject subject) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			Object listener = i.next();
			if (listener instanceof ILoginListener)
				((ILoginListener) listener).onLoginStart(subject);
		}
	}

	public void notifyLoginEnd(Subject subject, LoginException loginException) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			Object listener = i.next();
			if (listener instanceof ILoginListener)
				((ILoginListener) listener).onLoginFinish(subject, loginException);
		}
	}

	public void notifyLogoutBegin(Subject subject) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			Object listener = i.next();
			if (listener instanceof ILogoutListener)
				((ILogoutListener) listener).onLogoutStart(subject);
		}
	}

	public void notifyLogoutEnd(Subject subject, LoginException loginException) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			Object listener = i.next();
			if (listener instanceof ILogoutListener)
				((ILogoutListener) listener).onLogoutFinish(subject, loginException);
		}
	}
}
