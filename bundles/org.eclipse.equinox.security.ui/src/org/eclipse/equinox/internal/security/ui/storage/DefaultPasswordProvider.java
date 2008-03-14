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

import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.provider.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * This password provider prompts user for the password. This provider uses the same password for
 * all secure preferences.
 */
public class DefaultPasswordProvider extends PasswordProvider {

	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {
		boolean newPassword = ((passwordType & CREATE_NEW_PASSWORD) != 0);
		boolean passwordChange = ((passwordType & PASSWORD_CHANGE) != 0);

		String location = container.getLocation().getFile();
		StorageLoginDialog loginDialog = new StorageLoginDialog(newPassword, passwordChange, location);
		if (loginDialog.open() == Window.OK)
			return loginDialog.getGeneratedPassword();
		return null;
	}

	public boolean retryOnError(Exception e, IPreferencesContainer container) {
		boolean canPrompt = true;
		if (container.hasOption(IProviderHints.PROMPT_USER)) {
			Object promptHint = container.getOption(IProviderHints.PROMPT_USER);
			if (promptHint instanceof Boolean)
				canPrompt = ((Boolean) promptHint).booleanValue();
		}
		if (!canPrompt)
			return false;

		MessageBox dialog = new MessageBox(new Shell(), SWT.ICON_ERROR | SWT.YES | SWT.NO);
		dialog.setText(SecUIMessages.exceptionTitle);
		dialog.setMessage(SecUIMessages.exceptionDecode);
		int result = dialog.open();
		return (result == SWT.YES);
	}

}
