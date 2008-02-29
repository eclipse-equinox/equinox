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
 * This class is used to pass description of a password provider module.
 */
public class PasswordProviderDescription {

	private int priority;
	private String id;

	public PasswordProviderDescription(String id, int priority) {
		this.id = id;
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}

	public String getId() {
		return id;
	}
}
