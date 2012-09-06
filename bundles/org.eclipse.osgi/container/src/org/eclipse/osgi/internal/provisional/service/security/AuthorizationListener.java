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
package org.eclipse.osgi.internal.provisional.service.security;

import java.util.EventListener;

/**
 * A Listener interface for an authorization handler. Implementors 
 * should register as an OSGI service.
 * @since 3.4
 */
public interface AuthorizationListener extends EventListener {

	/**
	 * Called when an AuthorizationEvent has occurred
	 */
	public void authorizationEvent(AuthorizationEvent event);

}
