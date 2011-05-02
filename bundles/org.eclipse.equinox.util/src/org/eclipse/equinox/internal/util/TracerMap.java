/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util;

import org.eclipse.equinox.internal.util.hash.HashIntObjNS;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class TracerMap {

	public static HashIntObjNS getMap() {
		HashIntObjNS map = new HashIntObjNS(29);

		map.put(1, "[BEGIN - start method]");
		map.put(1001, "[BEGIN - Log instance] Loading referent classes took");
		map.put(101, "Getting system props, bundle id and log service took ");
		map.put(102, "Getting Trace Service took ");
		map.put(3001, "Loading tracer map took ");
		map.put(2001, "[END - Log instance] Creating log instance took ");
		map.put(3, "Creating Thread Pool service took ");
		map.put(4, "Registering Thread Pool service took ");
		map.put(33, "Creating Timer service took ");
		map.put(5, "Registering Timer service took ");
		map.put(16, "[END - start method] PutilActivator.start() method executed for ");
		map.put(0, "Putil Activator");
		map.put(-0x0100, "Threadpool");
		map.put(10001, "Registering commands with groupname ");
		map.put(10002, "Unregistering commands with groupname ");
		map.put(10003, "START Running: ");
		map.put(10004, "END Running: ");
		map.put(10005, "Going to run: ");
		return map;
	}

	public static HashIntObjNS getStarts() {
		return null;
	}
}
