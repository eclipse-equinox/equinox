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

import org.eclipse.equinox.internal.util.ref.Log;
import org.osgi.framework.*;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class Activator implements BundleActivator {

	public static BundleContext bc = null;
	private ConnectorServiceImpl connector;
	private Log log;

	public void start(BundleContext bc) throws BundleException {
		Activator.bc = bc;
		log = new Log(bc, false);
		log.setDebug(getBoolean("equinox.connector.debug"));
		log.setPrintOnConsole(getBoolean("equinox.connector.console"));
		if (log.getDebug()) {
			log.setMaps(TracerConfigConnector.getMap(), null);
		}
		connector = new ConnectorServiceImpl(bc, log);
	}

	public void stop(BundleContext bc) {
		if (connector != null) {
			connector.close();
			connector = null;
			log.close();
		}
		Activator.bc = null;
	}

	public static boolean getBoolean(String property) {
		String prop = (bc != null) ? bc.getProperty(property) : System.getProperty(property);
		return ((prop != null) && prop.equalsIgnoreCase("true"));
	}

	public static int getInteger(String property, int defaultValue) {
		String prop = (bc != null) ? bc.getProperty(property) : System.getProperty(property);
		if (prop != null) {
			try {
				return Integer.decode(prop).intValue();
			} catch (NumberFormatException e) {
				//do nothing
			}
		}
		return defaultValue;
	}
}
