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
package org.eclipse.equinox.internal.security.storage;

import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

/**
 * This class associates password provider module with a unique ID.
 */
public class PasswordProviderModuleExt {

	final private PasswordProvider providerModule;
	final private String moduleID;

	public PasswordProviderModuleExt(PasswordProvider module, String moduleID) {
		this.providerModule = module;
		this.moduleID = moduleID;
	}

	public String getID() {
		return moduleID;
	}

	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {
		return providerModule.getPassword(container, passwordType);
	}

	public boolean changePassword(Exception e, IPreferencesContainer controller) {
		return providerModule.retryOnError(e, controller);
	}

}
