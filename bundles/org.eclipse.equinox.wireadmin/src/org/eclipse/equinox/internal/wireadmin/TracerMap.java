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
package org.eclipse.equinox.internal.wireadmin;

import org.eclipse.equinox.internal.util.hash.HashIntObjNS;

/**
 * @author Lubomir Mitev
 * @author Pavlin Dobrev
 * @version 1.0
 */
public class TracerMap {

	public static HashIntObjNS getMap() {
		HashIntObjNS map = new HashIntObjNS(18);

		map.put(0, "WireAdmin");
		map.put(10001, "Event model is ");
		map.put(10002, "Event type / source : ");
		map.put(10003, "Exception while notifying WireAdminListener: ");
		map.put(10004, "Will create wire producerPID/consumerPID/props : ");
		map.put(10005, "I/O error creating a configuration!");
		map.put(10006, "Found case variants while updating wire properties");
		map.put(10007, "deleting wire with pid ");
		map.put(10008, "deleting complete");
		map.put(10009, "Updating properties for wire with pid=");
		map.put(10011, "Will set new properties for wire ");
		map.put(10012, "Filter doesn't match value : ");
		map.put(10013, "Wire started : ");
		map.put(10014, "Disconnecting ");
		map.put(10015, "Service is not connected to any producer/consumer : ");
		map.put(10016, "The producer and consumer could not interoperate using wire ");
		map.put(10017, "No ServiceReference for Wire events! event: ");
		map.put(10018, "WireAdminEvent redispatched: ");

		return map;
	}

	public static HashIntObjNS getStarts() {
		return null;
	}
}
