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

import java.util.EventListener;
import javax.microedition.io.Connection;

/**
 * Interface for a listener that will receive notification when the connection
 * is created and when is closed.
 * <p>
 * Listener is notified for creation of the connection only when the connector
 * service creates connection that implements only this and the Connection
 * interface, when the needed IOProvider is not available at the moment of
 * creation. When the provider becomes available then all registered listeners
 * will receive event
 * <p>
 * <code>CONNECTION_CREATED</code> and the created connection. Event
 * <code>CONNECTION_CLOSED</code> must be send from every connection that
 * implements this interface when it is closing.
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface ConnectionListener extends EventListener {
	/**
	 * Constant for event type created
	 */
	public static final int CONNECTION_CREATED = 0;
	/**
	 * Constant for event type closed
	 */
	public static final int CONNECTION_CLOSED = 1;

	/**
	 * Receives notification that a connection has been created or closed.
	 */
	public void notify(String uri, int eventType, Connection conn);

}
