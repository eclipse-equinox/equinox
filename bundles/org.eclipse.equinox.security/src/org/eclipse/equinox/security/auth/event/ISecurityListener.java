/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.security.auth.event;

/**
 * This is a common interface that tags a class that can be registered 
 * as a listener for security events. 
 * <p>
 * This interface is not intended to be implemented or extended by clients.
 * </p>
 * @see ILoginListener
 * @see ILogoutListener
 */
public interface ISecurityListener {
	// empty
}
