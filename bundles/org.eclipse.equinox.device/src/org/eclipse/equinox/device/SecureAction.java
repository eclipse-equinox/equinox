/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.device;

import java.security.*;

/**
 * Utility class to execute common privileged code.
 */
public class SecureAction {
	// make sure we use the correct controlContext;
	private final AccessControlContext controlContext;

	/**
	 * Constructs a new SecureAction object. The constructed SecureAction object
	 * uses the caller's AccessControlContext to perform security checks
	 */
	public SecureAction() {
		// save the control context to be used.
		this.controlContext = AccessController.getContext();
	}

	/**
	 * Creates a new Thread from a Runnable. Same as calling new
	 * Thread(target,name).
	 * 
	 * @param target the Runnable to create the Thread from.
	 * @param name   The name of the Thread.
	 * @return The new Thread
	 */
	public Thread createThread(final Runnable target, final String name) {
		if (System.getSecurityManager() == null) {
			return new Thread(target, name);
		}
		return AccessController.doPrivileged((PrivilegedAction<Thread>) () -> new Thread(target, name), controlContext);
	}

}
