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
import java.util.*;
import javax.microedition.io.Connection;
import org.eclipse.equinox.internal.io.impl.PrivilegedRunner.PrivilegedDispatcher;
import org.osgi.framework.*;
import org.osgi.service.io.ConnectionFactory;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ConnectionFactoryListener implements ServiceListener, PrivilegedDispatcher {
	private static Hashtable urlToConN = new Hashtable(5);
	private BundleContext bc;

	public ConnectionFactoryListener(BundleContext bc) {
		this.bc = bc;

		try {
			bc.addServiceListener(this, '(' + Constants.OBJECTCLASS + '=' + ConnectionFactory.class.getName() + ')');
		} catch (InvalidSyntaxException ex) {
			// Ignored - syntax is right!
		}
	}

	public void close() {
		bc.removeServiceListener(this);

		if (!urlToConN.isEmpty()) {
			Vector copyV = new Vector(urlToConN.size());

			for (Enumeration en = urlToConN.elements(); en.hasMoreElements();) {
				copyV.addElement(en.nextElement());
			}

			for (Enumeration en = copyV.elements(); en.hasMoreElements();) {
				ConnectionNotifierImpl cn = (ConnectionNotifierImpl) en.nextElement();
				try {
					cn.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	static void removeConnectionNotifier(String url) {
		urlToConN.remove(url);
	}

	static int count = 0;

	static Connection getConnectionNotifier(String scheme, String url, int mode, boolean timeouts, String filter, int count) throws IOException {
		ConnectionNotifierImpl ret = null;
		synchronized (urlToConN) {
			if (urlToConN.containsKey(url)) {
				return (Connection) urlToConN.get(url);
			}

			ret = new ConnectionNotifierImpl(scheme, url, mode, timeouts, filter);
			urlToConN.put(url, ret);
			if (count == ConnectionFactoryListener.count)
				return ret;
		}

		Connection c = ConnectorServiceImpl.getConnection(filter, url, mode, timeouts, false);
		if (c != null) {
			if (ret.hasListeners())
				ret.notifyCreated(c);
			else
				ret.close();
		}
		return c;
	}

	public void serviceChanged(ServiceEvent event) {
		if (event.getType() == ServiceEvent.REGISTERED && !urlToConN.isEmpty()) {
			ServiceReference ref = event.getServiceReference();
			ConnectionFactory factory = (ConnectionFactory) bc.getService(ref);
			String[] schemes = (String[]) ref.getProperty(ConnectionFactory.IO_SCHEME);

			Vector toNotify = new Vector(urlToConN.size());
			synchronized (urlToConN) {
				for (Enumeration en = urlToConN.elements(); en.hasMoreElements();) {
					ConnectionNotifierImpl cn = (ConnectionNotifierImpl) en.nextElement();
					if (match(schemes, cn.scheme))
						toNotify.addElement(cn);
				}
			}
			for (int i = 0; i < toNotify.size(); i++) {
				ConnectionNotifierImpl cn = (ConnectionNotifierImpl) toNotify.elementAt(i);
				try {
					if (cn.context != null) {
						PrivilegedRunner.doPrivileged(cn.context, this, 0, cn, factory, null, null);
						return;
					}
					Connection connection = factory.createConnection(cn.url, cn.mode, cn.timeouts);
					if (cn.hasListeners())
						cn.notifyCreated(connection);
					else
						cn.close();
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}

			bc.ungetService(ref);
		}
	}

	static boolean match(String[] schemes, String scheme) {
		if (schemes != null && scheme != null) {
			for (int i = 0; i < schemes.length; i++) {
				if (scheme.equals(schemes[i])) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * @see org.eclipse.equinox.internal.io.impl.PrivilegedRunner.PrivilegedDispatcher#dispatchPrivileged(int,
	 *      java.lang.Object, java.lang.Object, java.lang.Object,
	 *      java.lang.Object)
	 */
	public Object dispatchPrivileged(int type, Object arg1, Object arg2, Object arg3, Object arg4) throws Exception {
		ConnectionNotifierImpl cn = (ConnectionNotifierImpl) arg1;
		ConnectionFactory factory = (ConnectionFactory) arg2;
		Connection connection = factory.createConnection(cn.url, cn.mode, cn.timeouts);
		if (cn.hasListeners()) {
			cn.notifyCreated(connection);
		} else {
			cn.close();
		}
		return null;
	}
}
