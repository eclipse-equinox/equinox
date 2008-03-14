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
package org.eclipse.equinox.internal.security.tests.storage;

import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

/**
 * Password provider which is to be added at a with relatively high priority.
 */
public class HighPriorityModule extends PasswordProvider {

	public final static PBEKeySpec PASSWORD = new PBEKeySpec("HighPriorityPassword".toCharArray());

	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {
		return PASSWORD;
	}

	public boolean retryOnError(Exception e, IPreferencesContainer container) {
		return false;
	}
}
