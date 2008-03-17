/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.storage.friends;

/**
 * Interface used for UI dependency injection. Unlike most places,
 * actions in the secure storage can be initiated independently from UI so
 * we can't expect UI bundle to be originator of an action - or even being 
 * activated.
 * 
 * As such, an internal extension point internalUI is used to get UI callbacks.
 * 
 * This is an internal interface used to facilitate exchange between core and
 * UI portions. 
 * 
 * This interface is subject to modifications as all internal code is.
 * 
 * Clients should not extend or implement this interface.
 */
public interface IUICallbacks {

	public String[][] setupPasswordRecovery(int size);

}
