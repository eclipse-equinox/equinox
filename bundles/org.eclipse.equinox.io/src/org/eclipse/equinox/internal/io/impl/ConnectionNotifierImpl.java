/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.io.impl;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connection;
import org.eclipse.equinox.internal.io.ConnectionListener;
import org.eclipse.equinox.internal.io.ConnectionNotifier;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

class ConnectionNotifierImpl implements ConnectionNotifier, Connection {
	private Vector list;
	String scheme;
	int mode;
	boolean timeouts;
	String url;
	String filter;

	boolean notified = false;
	Object context;

	ConnectionNotifierImpl(String scheme, String url, int mode, boolean timeouts, String filter) {
		list = new Vector(3, 5);
		this.scheme = scheme;
		this.url = url;
		this.mode = mode;
		this.timeouts = timeouts;
		this.filter = filter;
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			context = System.getSecurityManager().getSecurityContext();
	}

	public void addConnectionListener(ConnectionListener l) {
		long timeStart = 0;
		if (ConnectorServiceImpl.hasDebug) {
			ConnectorServiceImpl.debug(16050, l.getClass().getName(), null);
			timeStart = System.currentTimeMillis();
		}
		try {
			synchronized (list) {
				if (!notified) {
					if (!list.contains(l))
						list.addElement(l);
					return;
				}
			}
			Connection c = ConnectorServiceImpl.getConnection(filter, url, mode, timeouts, false);
			if (c != null)
				l.notify(url, ConnectionListener.CONNECTION_CREATED, c);
		} catch (Exception exc) {
		} finally {
			if (ConnectorServiceImpl.hasDebug) {
				ConnectorServiceImpl.debug(16051, String.valueOf(System.currentTimeMillis() - timeStart), null);
			}
		}
	}

	public void removeConnectionListener(ConnectionListener l) {
		if (ConnectorServiceImpl.hasDebug) {
			ConnectorServiceImpl.debug(16052, l.getClass().getName(), null);
		}
		list.removeElement(l);
	}

	boolean hasListeners() {
		return !list.isEmpty();
	}

	void notifyCreated(Connection conn) {
		notify(ConnectionListener.CONNECTION_CREATED, conn);
	}

	private void notify(int eventType, Connection conn) {
		if (eventType != ConnectionListener.CONNECTION_CREATED && eventType != ConnectionListener.CONNECTION_CLOSED) {
			return;
		}

		synchronized (list) {
			if (notified)
				return;
			notified = true;
		}
		ConnectionFactoryListener.removeConnectionNotifier(url);
		for (Enumeration en = list.elements(); en.hasMoreElements();) {
			ConnectionListener l = (ConnectionListener) en.nextElement();
			l.notify(url, eventType, conn);
		}
	}

	public void close() throws IOException {
		if (ConnectorServiceImpl.hasDebug) {
			ConnectorServiceImpl.debug(16053, url, null);
		}
		synchronized (list) {
			if (notified)
				return;
			notified = true;
		}
		ConnectionFactoryListener.removeConnectionNotifier(url);
	}
}
