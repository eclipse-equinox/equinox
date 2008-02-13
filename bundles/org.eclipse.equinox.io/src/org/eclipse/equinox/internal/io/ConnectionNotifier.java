/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
