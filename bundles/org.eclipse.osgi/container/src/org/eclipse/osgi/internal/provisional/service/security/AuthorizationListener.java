/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
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
