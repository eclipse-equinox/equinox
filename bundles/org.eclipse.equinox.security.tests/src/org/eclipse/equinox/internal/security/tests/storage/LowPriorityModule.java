/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.tests.storage;

import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

/**
 * Password provider which is to be added at a with relatively low priority.
 */
public class LowPriorityModule extends PasswordProvider {

	public final static PBEKeySpec PASSWORD = new PBEKeySpec("LowPriorityPassword".toCharArray());

	@Override
	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {
		return PASSWORD;
	}

	@Override
	public boolean retryOnError(Exception e, IPreferencesContainer container) {
		return false;
	}
}
