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

import java.io.*;
import javax.microedition.io.*;
import org.eclipse.equinox.internal.util.ref.Log;
import org.osgi.framework.*;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.service.io.ConnectorService;

/**
 * ConnectorService implementation.
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ConnectorServiceImpl implements ConnectorService {
	static BundleContext bc;
	ServiceRegistration reg;
	ConnectionFactoryListener listener;

	static boolean enableNotification = Activator.getBoolean("eclipse.io.enable.notification");
	public static boolean hasDebug;
	private static Log log;

	public ConnectorServiceImpl(BundleContext bc, Log log) {
		ConnectorServiceImpl.log = log;
		ConnectorServiceImpl.hasDebug = log.getDebug();
		init(bc);
	}

	public static void debug(int id, String message, Throwable t) {
		log.debug(0x1200, id, message, t, false);
	}

	public ConnectorServiceImpl(BundleContext bc) {
		init(bc);
	}

	private void init(BundleContext bc) {
		ConnectorServiceImpl.bc = bc;

		if (enableNotification) {
			listener = new ConnectionFactoryListener(bc);
		}

		reg = bc.registerService(ConnectorService.class.getName(), this, null);
	}

	public void close() {
		reg.unregister();

		if (listener != null) {
			listener.close();
		}
	}

	public Connection open(String uri) throws IOException {
		return open(uri, READ_WRITE);
	}

	public Connection open(String uri, int mode) throws IOException {
		return open(uri, mode, false);
	}

	static char[] chars = {'~', '='};

	public Connection open(String uri, int mode, boolean timeouts) throws IOException {
		long timeStart = 0;
		if (hasDebug) {
			debug(16001, uri + ", " + (mode == READ_WRITE ? "READ_WRITE" : (mode == READ) ? "READ" : "WRITE"), null);
			timeStart = System.currentTimeMillis();
		}
		try {
			if (uri == null) {
				throw new IllegalArgumentException("URL cannot be NULL!");
			}

			int sPos = uri.indexOf(":");

			if (sPos < 1) { // scheme must be at least with 1 symbol
				throw new IllegalArgumentException("Does not have scheme");
			}

			String scheme = uri.substring(0, sPos);
			StringBuffer filter = new StringBuffer(scheme.length() + 13);
			filter.append('(');
			filter.append(ConnectionFactory.IO_SCHEME);
			filter.append(chars);
			filter.append(scheme);
			filter.append(')');

			int count = 0;
			if (listener != null)
				count = ConnectionFactoryListener.count;
			Connection c = getConnection(filter.toString(), uri, mode, timeouts, true);

			if (c == null && listener != null)
				c = ConnectionFactoryListener.getConnectionNotifier(scheme, uri, mode, timeouts, filter.toString(), count);
			if (c == null)
				throw new ConnectionNotFoundException("Failed to create connection " + uri);
			return c;
		} finally {
			if (hasDebug) {
				debug(16002, String.valueOf(System.currentTimeMillis() - timeStart), null);
			}
		}
	}

	static protected Connection getConnection(String filter, String uri, int mode, boolean timeouts, boolean connector) throws IOException {
		ServiceReference[] cfRefs = null;
		Connection ret = null;
		try {
			cfRefs = bc.getServiceReferences(ConnectionFactory.class.getName(), filter);
		} catch (InvalidSyntaxException ex) {
		}

		if (cfRefs != null) {
			sort(cfRefs, 0, cfRefs.length);
		}

		IOException ioExc = null;
		boolean not_found = false;
		if (cfRefs != null)
			for (int i = 0; i < cfRefs.length; i++) {
				ConnectionFactory prov = (ConnectionFactory) bc.getService(cfRefs[i]);
				if (prov != null)
					try {
						not_found = true;
						ret = prov.createConnection(uri, mode, timeouts);
					} catch (IOException e) {
						if (hasDebug) {
							debug(16003, prov.getClass().getName(), e);
						}
						if (ioExc == null)
							ioExc = e;
					} finally {
						if (ret == null)
							bc.ungetService(cfRefs[i]);
						else {
							if (hasDebug) {
								debug(16004, prov.getClass().getName(), null);
							}
							return ret;
						}
					}
			}

		if (connector)
			try {
				ret = Connector.open(uri, mode, timeouts);
			} catch (ConnectionNotFoundException ignore) { // returns null ->
				// ConnectionNotifier
				// is created
				debug(16014, null, ignore);
			} finally {
				if (ret == null) {
					if (ioExc != null)
						throw ioExc;
					if (not_found)
						throw new ConnectionNotFoundException("Failed to create connection " + uri);

				} else {
					if (hasDebug) {
						debug(16005, null, null);
					}
				}
			}
		return ret;
	}

	private static void sort(ServiceReference[] array, int start, int end) {
		int middle = (start + end) / 2;
		if (start + 1 < middle)
			sort(array, start, middle);
		if (middle + 1 < end)
			sort(array, middle, end);
		if (start + 1 >= end)
			return; // this case can only happen when this method is called by
		// the user

		if (getRanking(array[middle - 1]) == getRanking(array[middle])) {
			if (getServiceID(array[middle - 1]) < getServiceID(array[middle])) {
				return;
			}
		} else if (getRanking(array[middle - 1]) >= getRanking(array[middle])) {
			return;
		}

		if (start + 2 == end) {
			ServiceReference temp = array[start];
			array[start] = array[middle];
			array[middle] = temp;
			return;
		}
		int i1 = start, i2 = middle, i3 = 0;
		Object[] merge = new Object[end - start];
		while (i1 < middle && i2 < end) {
			if (getRanking(array[i1]) == getRanking(array[i2])) {
				merge[i3++] = getServiceID(array[i1]) < getServiceID(array[i2]) ? array[i1++] : array[i2++];
			} else {
				merge[i3++] = getRanking(array[i1]) >= getRanking(array[i2]) ? array[i1++] : array[i2++];
			}
		}
		if (i1 < middle)
			System.arraycopy(array, i1, merge, i3, middle - i1);
		System.arraycopy(merge, 0, array, start, i2 - start);
	}

	private static int getRanking(ServiceReference ref) {
		Object rank = ref.getProperty(Constants.SERVICE_RANKING);

		if (rank == null || !(rank instanceof Integer)) {
			return 0;
		}

		return ((Integer) rank).intValue();
	}

	private static long getServiceID(ServiceReference ref) {
		Object sid = ref.getProperty(Constants.SERVICE_ID);

		if (sid == null || !(sid instanceof Long)) {
			return 0;
		}

		return ((Long) sid).intValue();
	}

	/**
	 * Create and open an <tt>DataInputStream</tt> object for the specified
	 * name.
	 * 
	 * @param name
	 *            the URI for the connection.
	 * @throws IOException
	 *             if and I/O error occurs
	 * @throws ConnectionNotFoundException
	 *             if the <tt>Connection</tt> object can not be made or if no
	 *             handler for the requested scheme can be found.
	 * @throws IllegalArgumentException
	 *             if the given uri is invalid
	 * @return A <tt>DataInputStream</tt> to the given URI
	 */
	public DataInputStream openDataInputStream(String name) throws IOException {
		long timeBegin = 0;
		if (hasDebug) {
			debug(16006, name, null);
			timeBegin = System.currentTimeMillis();
		}
		try {
			Connection conn = open(name, READ);

			if (!(conn instanceof InputConnection)) {
				try {
					conn.close();
				} catch (IOException e) {
				}
				throw new IOException("Connection does not implement InputConnection:" + conn.getClass());
			}

			return ((InputConnection) conn).openDataInputStream();
		} finally {
			if (hasDebug) {
				debug(16007, String.valueOf(System.currentTimeMillis() - timeBegin), null);
			}
		}
	}

	/**
	 * Create and open an <tt>DataOutputStream</tt> object for the specified
	 * name.
	 * 
	 * @param name
	 *            the URI for the connection.
	 * @throws IOException
	 *             if and I/O error occurs
	 * @throws ConnectionNotFoundException
	 *             if the <tt>Connection</tt> object can not be made or if no
	 *             handler for the requested scheme can be found.
	 * @throws IllegalArgumentException
	 *             if the given uri is invalid
	 * @return A <tt>DataOutputStream</tt> to the given URI
	 */
	public DataOutputStream openDataOutputStream(String name) throws IOException {
		long timeBegin = 0;
		if (hasDebug) {
			debug(16008, name, null);
		}
		try {
			Connection conn = open(name, WRITE);

			if (!(conn instanceof OutputConnection)) {
				try {
					conn.close();
				} catch (IOException e) {
				}

				throw new IOException("Connection does not implement OutputConnection:" + conn.getClass());
			}

			return ((OutputConnection) conn).openDataOutputStream();
		} finally {
			if (hasDebug) {
				debug(16009, String.valueOf(System.currentTimeMillis() - timeBegin), null);
			}
		}
	}

	/**
	 * Create and open an <tt>InputStream</tt> object for the specified name.
	 * 
	 * 
	 * @param name
	 *            the URI for the connection.
	 * @throws IOException
	 *             if and I/O error occurs
	 * @throws ConnectionNotFoundException
	 *             if the <tt>Connection</tt> object can not be made or if no
	 *             handler for the requested scheme can be found.
	 * @throws IllegalArgumentException
	 *             if the given uri is invalid
	 * @return A <tt>InputStream</tt> to the given URI
	 */
	public InputStream openInputStream(String name) throws IOException {
		long timeBegin = 0;
		if (hasDebug) {
			debug(16010, name, null);
			timeBegin = System.currentTimeMillis();
		}
		try {
			Connection conn = open(name, READ);

			if (!(conn instanceof InputConnection)) {
				try {
					conn.close();
				} catch (IOException e) {
				}

				throw new IOException("Connection does not implement InputConnection:" + conn.getClass());
			}

			return ((InputConnection) conn).openInputStream();
		} finally {
			if (hasDebug) {
				debug(16011, String.valueOf(System.currentTimeMillis() - timeBegin), null);
			}
		}
	}

	/**
	 * Create and open an <tt>OutputStream</tt> object for the specified name.
	 * 
	 * @param name
	 *            the URI for the connection.
	 * @throws IOException
	 *             if and I/O error occurs
	 * @throws ConnectionNotFoundException
	 *             if the <tt>Connection</tt> object can not be made or if no
	 *             handler for the requested scheme can be found.
	 * @throws IllegalArgumentException
	 *             if the given uri is invalid
	 * @return A <tt>OutputStream</tt> to the given URI
	 */
	public OutputStream openOutputStream(String name) throws IOException {
		long timeBegin = 0;
		if (hasDebug) {
			debug(16012, name, null);
			timeBegin = System.currentTimeMillis();
		}
		try {
			Connection conn = open(name, WRITE);

			if (!(conn instanceof OutputConnection)) {
				try {
					conn.close();
				} catch (IOException e) {
				}

				throw new IOException("Connection does not implement OutputConnection:" + conn.getClass());
			}

			return ((OutputConnection) conn).openOutputStream();
		} finally {
			if (hasDebug) {
				debug(16013, String.valueOf(System.currentTimeMillis() - timeBegin), null);
			}
		}
	}
}
