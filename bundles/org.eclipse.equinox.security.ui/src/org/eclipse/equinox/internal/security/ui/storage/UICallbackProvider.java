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

import org.eclipse.equinox.internal.security.storage.friends.IUICallbacks;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class UICallbackProvider implements IUICallbacks {

	public String[][] setupPasswordRecovery(int size) {
		Shell shell = new Shell(); // TBD or get it from an active window?
		MessageBox prompt = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		prompt.setText(SecUIMessages.pswdRecoveryOptionTitle);
		prompt.setMessage(SecUIMessages.pswdRecoveryOptionMsg);
		int result = prompt.open();
		if (result != SWT.YES)
			return null;

		ChallengeResponseDialog dialog = new ChallengeResponseDialog(size, shell);
		dialog.open();
		return dialog.getResult();
	}

}
