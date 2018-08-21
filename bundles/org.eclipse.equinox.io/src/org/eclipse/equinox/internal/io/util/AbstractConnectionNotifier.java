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
package org.eclipse.equinox.internal.io.util;

import java.util.*;
import javax.microedition.io.Connection;
import org.eclipse.equinox.internal.io.ConnectionListener;
import org.eclipse.equinox.internal.io.ConnectionNotifier;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public abstract class AbstractConnectionNotifier extends Dictionary implements ConnectionNotifier {
	private Vector list;
	private Hashtable info;

	// the local address to which the connection is bound

	public final static String LOCAL_ADDRESS = "local_address";

	// the local port to which the connection is bound.

	public final static String LOCAL_PORT = "local_port";

	// the remote address to which the connection is connected.

	public final static String ADDRESS = "address";

	// the remote port to which the connection is connected.

	public final static String PORT = "port";

	// the encoder used

	public final static String ENCODER = "enc";

	public static final String SO_TIMEOUT = "timeout";

	protected AbstractConnectionNotifier() {
		list = new Vector(2, 3);
		info = new Hashtable(30);
	}

	// ConnectionNotifier methods
	public void addConnectionListener(ConnectionListener l) {
		if (!list.contains(l)) {
			list.addElement(l);
		}
	}

	public void removeConnectionListener(ConnectionListener l) {
		list.removeElement(l);
	}

	// returns the url of connection
	protected abstract String getURL();

	// the connection to which this ConnectionLife is bound to
	protected abstract Connection getConnection();

	protected void notifyClosed() {
		for (Enumeration en = list.elements(); en.hasMoreElements();) {
			ConnectionListener l = (ConnectionListener) en.nextElement();
			l.notify(getURL(), ConnectionListener.CONNECTION_CLOSED, getConnection());
		}
	}

	protected void setInfo(String key, Object value) {
		if (key != null && value != null) {
			info.put(key, value);
		}
	}

	public int size() {
		return info.size();
	}

	public boolean isEmpty() {
		return info.isEmpty();
	}

	public Enumeration keys() {
		return info.keys();
	}

	public Enumeration elements() {
		return info.elements();
	}

	public Object get(Object key) {
		return info.get(key);
	}

	public Object remove(Object key) {
		return info.remove(key);
	}

}
