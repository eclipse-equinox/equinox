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
package org.eclipse.equinox.internal.security.win32;

import org.eclipse.equinox.internal.security.win32.nls.WinCryptoMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * Isolates optional UI functionality
 */
public class WinCryptoUI {

	public static boolean canRecreatePassword() {
		MessageBox dialog = new MessageBox(new Shell(), SWT.ICON_ERROR | SWT.YES | SWT.NO);
		dialog.setText(WinCryptoMessages.newPasswordTitle);
		dialog.setMessage(WinCryptoMessages.newPasswordMessage);
		int result = dialog.open();
		return (result == SWT.YES);
	}
}
