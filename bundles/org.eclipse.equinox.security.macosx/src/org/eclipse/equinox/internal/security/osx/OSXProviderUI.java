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
package org.eclipse.equinox.internal.security.osx;

import org.eclipse.equinox.internal.security.osx.nls.OSXProviderMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class OSXProviderUI {

	public static boolean canRecreatePassword() {
		MessageBox dialog = new MessageBox(new Shell(), SWT.ICON_ERROR | SWT.YES | SWT.NO);
		dialog.setText(OSXProviderMessages.newPasswordTitle);
		dialog.setMessage(OSXProviderMessages.newPasswordMessage);
		int result = dialog.open();
		return (result == SWT.YES);
	}
}
