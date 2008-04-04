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
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;

public class UICallbackProvider implements IUICallbacks {

	static private final String junitApp = "org.eclipse.pde.junit.runtime"; //$NON-NLS-1$

	public String[][] setupPasswordRecovery(int size) {
		if (!PlatformUI.isWorkbenchRunning())
			return null;

		// This is a bit of a strange code that tries to see if we are running in a JUnit
		BundleContext context = Activator.getBundleContext();
		if (context == null)
			return null;
		String app = context.getProperty("eclipse.application"); //$NON-NLS-1$
		if (app != null && app.startsWith(junitApp))
			return null;

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Shell shell = (window == null) ? new Shell() : window.getShell();
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
