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

import org.eclipse.equinox.internal.util.hash.HashIntObjNS;

/**
 * @author Lubomir Mitev
 * @author Pavlin Dobrev
 * @version 1.0
 */
public class TracerConfigConnector {

	public static HashIntObjNS getMap() {
		/*
		 * increase size if you add more dumps in the table - at this moment the
		 * entries in table are 159
		 */
		HashIntObjNS map = new HashIntObjNS(28);// (int)(28 * 0.7) = 19, 18
		constructMap(map);
		return map;
	}

	public static void constructMap(HashIntObjNS map) {
		map.put(-0x1200, "Connector Service");
		map.put(16001, "[BEGIN - open connection] The values of the URI and the access mode are");
		map.put(16002, "[END - open connection] took");
		map.put(16003, "IOException when connection is created from ConnectionFactory");
		map.put(16004, "Connection is created from ConnectionFactory");
		map.put(16005, "Connection is created from javax.microedition.io.Connector");
		map.put(16006, "[BEGIN - open DataInputStream] The URI is");
		map.put(16007, "[END - open DataInputStream] took");
		map.put(16008, "[BEGIN - open DataOutputStream] The URI is");
		map.put(16009, "[END - open DataOutputStream] took");
		map.put(16010, "[BEGIN - open InputStream] The URI is");
		map.put(16011, "[END - open InputStream] took");
		map.put(16012, "[BEGIN - open OutputStream] The URI is");
		map.put(16013, "[END - open OutputStream] took");
		map.put(16014, "ConnectionNotFoundException thrown from system javax.microedition.io.Connector");
		map.put(16050, "[BEGIN - add ConnectionListener] The ConnectionListener is");
		map.put(16051, "[END - add ConnectionListener] took");
		map.put(16052, "Remove ConnectionListener");
		map.put(16053, "Close connection. The URI is");
	}
}
