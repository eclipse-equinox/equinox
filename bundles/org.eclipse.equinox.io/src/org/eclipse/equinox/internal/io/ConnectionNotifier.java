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
package org.eclipse.equinox.internal.io;

/**
 * Provides methods for registering listeners for receiving events from a
 * connection (that implements this interface).
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface ConnectionNotifier {
	/**
	 * Adds the given listener to the set of listeners that will be notified
	 * when the connection is created or closed.
	 */
	public void addConnectionListener(ConnectionListener l);

	/**
	 * Removes the given listener from the set of listeners that will be
	 * notified when the connection is created or closed.
	 */
	public void removeConnectionListener(ConnectionListener l);
}
