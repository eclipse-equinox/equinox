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
package org.eclipse.equinox.internal.security.ui.storage;

import java.net.URL;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

/**
 * This password provider prompts user for the password. This provider uses the same password for
 * all secure preferences.
 */
public class DefaultPasswordProvider extends PasswordProvider {

	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {
		if (!StorageUtils.showUI(container))
			return null;

		boolean newPassword = ((passwordType & CREATE_NEW_PASSWORD) != 0);
		boolean passwordChange = ((passwordType & PASSWORD_CHANGE) != 0);

		String location = container.getLocation().getFile();
		URL defaultURL = InternalExchangeUtils.defaultStorageLocation();
		if (defaultURL != null) { // remove default location from the dialog
			String defaultFile = defaultURL.getFile();
			if (defaultFile != null && defaultFile.equals(location))
				location = null;
		}

		final StorageLoginDialog loginDialog = new StorageLoginDialog(newPassword, passwordChange, location);

		final PBEKeySpec[] result = new PBEKeySpec[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				if (loginDialog.open() == Window.OK)
					result[0] = loginDialog.getGeneratedPassword();
				else
					result[0] = null;
			}
		});
		return result[0];
	}

	public boolean retryOnError(Exception e, IPreferencesContainer container) {
		if (!StorageUtils.showUI(container))
			return false;

		final Boolean[] result = new Boolean[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				boolean reply = MessageDialog.openConfirm(StorageUtils.getShell(), SecUIMessages.exceptionTitle, SecUIMessages.exceptionDecode);
				result[0] = new Boolean(reply);
			}
		});
		return result[0].booleanValue();
	}
}
