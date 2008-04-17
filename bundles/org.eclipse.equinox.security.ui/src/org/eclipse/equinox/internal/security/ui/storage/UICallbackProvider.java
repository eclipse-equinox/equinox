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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * Methods on this class could be called from non-UI thread so need
 * to wrap into Display.syncExec().
 */
public class UICallbackProvider implements IUICallbacks {

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

	public String[][] setupPasswordRecovery(final int size) {
		if (!StorageUtils.showUI())
			return null;

		final int[] result = new int[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				MessageBox prompt = new MessageBox(StorageUtils.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				prompt.setText(SecUIMessages.pswdRecoveryOptionTitle);
				prompt.setMessage(SecUIMessages.pswdRecoveryOptionMsg);
				result[0] = prompt.open();
			}
		});
		if (result[0] != SWT.YES)
			return null;

		final Object[] responseResult = new Object[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				ChallengeResponseDialog dialog = new ChallengeResponseDialog(size, StorageUtils.getShell());
				dialog.open();
				responseResult[0] = dialog.getResult();
			}
		});
		return (String[][]) responseResult[0];
	}

	public boolean execute(final IStorageTask callback) throws StorageException {
		if (!StorageUtils.showUI()) {
			callback.execute();
			return true;
		}

		final boolean[] result = new boolean[1];
		final StorageException[] exception = new StorageException[1];
		exception[0] = null;

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
				InitWithProgress task = new InitWithProgress(callback);
				try {
					progressService.busyCursorWhile(task);
				} catch (InvocationTargetException e) {
					Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
					result[0] = false;
					return;
				} catch (InterruptedException e) {
					Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
					result[0] = false;
					return;
				}
				if (task.getException() != null)
					exception[0] = task.getException();
				result[0] = true;
			}
		});
		if (exception[0] != null)
			throw exception[0];
		return result[0];

	}

	public Boolean ask(final String msg) {
		if (!StorageUtils.showUI())
			return null;

		final int[] result = new int[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				MessageBox prompt = new MessageBox(StorageUtils.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				prompt.setText(SecUIMessages.generalDialogTitle);
				prompt.setMessage(msg);
				result[0] = prompt.open();
			}
		});
		return new Boolean(result[0] == SWT.YES);
	}

	public boolean runningUI() {
		return StorageUtils.runningUI();
	}

}
