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

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.security.storage.friends.IStorageTask;
import org.eclipse.equinox.internal.security.storage.friends.IUICallbacks;
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.osgi.framework.BundleContext;

public class UICallbackProvider implements IUICallbacks {

	static private final String junitApp = "org.eclipse.pde.junit.runtime"; //$NON-NLS-1$

	private class InitWithProgress implements IRunnableWithProgress {

		private IStorageTask callback;
		private StorageException exception = null;

		public InitWithProgress(IStorageTask callback) {
			this.callback = callback;
		}

		public StorageException getException() {
			return exception;
		}

		public void run(IProgressMonitor monitor) {
			monitor.beginTask(SecUIMessages.initializing, IProgressMonitor.UNKNOWN);
			try {
				callback.execute();
			} catch (StorageException e) {
				exception = e;
			}
			monitor.done();
		}
	}

	public String[][] setupPasswordRecovery(int size) {
		if (!showUI())
			return null;

		Shell shell = getShell();
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

	/**
	 * Determines if it is a good idea to show UI prompts
	 */
	private boolean showUI() {
		if (!PlatformUI.isWorkbenchRunning())
			return false;

		// This is a bit of a strange code that tries to see if we are running in a JUnit
		BundleContext context = Activator.getBundleContext();
		if (context == null)
			return false;
		String app = context.getProperty("eclipse.application"); //$NON-NLS-1$
		if (app != null && app.startsWith(junitApp))
			return false;

		return true;
	}

	/**
	 * Finds a shell from an active window, if any; or creates a new one.
	 */
	private Shell getShell() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return (window == null) ? new Shell() : window.getShell();
	}

	public boolean execute(final IStorageTask callback) throws StorageException {
		if (!showUI()) {
			callback.execute();
			return true;
		}

		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		InitWithProgress task = new InitWithProgress(callback);
		try {
			progressService.busyCursorWhile(task);
		} catch (InvocationTargetException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
			return false;
		} catch (InterruptedException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
			return false;
		}
		if (task.getException() != null)
			throw task.getException();
		return true;
	}
}
