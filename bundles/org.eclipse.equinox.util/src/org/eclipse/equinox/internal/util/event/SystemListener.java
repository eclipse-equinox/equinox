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
package org.eclipse.equinox.internal.util.event;

/**
 * This interface marks any listener implementation as system listener. System
 * listener are very important for the correct functionality of the system. When
 * the execution of user implemented listener's methods is too slow this
 * listener is presumed as "bad" listener and it is removed automatically by the
 * framework unless it is implementing the SystemListener interface. Instead of
 * removing it automatically as a bad listener, the system dispatchers will
 * notify the listener by calling timeoutOccured().
 * 
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.0
 * 
 */
public interface SystemListener {

	/**
	 * Notifies the listener that a timeout has occured while processing one of
	 * its methods. The listeners logic may decide whether it has to be removed
	 * as listener or not
	 * 
	 * @return true, if the listener has to be removed from the list of
	 *         listeners
	 */
	public boolean timeoutOccured();

}
