/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.socket;

/** ServerSocketInterface.java
 *
 *
 */

import java.io.IOException;

public interface ServerSocketInterface {
	public SocketInterface acceptSock() throws IOException;

	public void close() throws IOException;

	public void setAddress(String address);

	public String getAddress();

	public int getLocalPort();

	public String getScheme();
}
