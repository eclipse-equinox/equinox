/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.ui.storage;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.security.storage.friends.*;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.progress.UIJob;

/**
 * Methods on this class could be called from non-UI thread so need to wrap into
 * Display.syncExec().
 */
public class UICallbackProvider implements IUICallbacks {

	private static class InitWithProgress implements IRunnableWithProgress {

		private IStorageTask callback;
		private StorageException exception = null;

		public InitWithProgress(IStorageTask callback) {
			this.callback = callback;
		}

		public StorageException getException() {
			return exception;
		}

		@Override
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

	@Override
	public void setupPasswordRecovery(final int size, final String moduleID, final IPreferencesContainer container) {
		if (!StorageUtils.showUI(container))
			return;

		UIJob reciverySetupJob = new UIJob(SecUIMessages.pswJobName) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				boolean reply = MessageDialog.openQuestion(StorageUtils.getShell(),
						SecUIMessages.pswdRecoveryOptionTitle, SecUIMessages.pswdRecoveryOptionMsg);
				if (!reply)
					return Status.OK_STATUS;
				ChallengeResponseDialog dialog = new ChallengeResponseDialog(size, StorageUtils.getShell());
				dialog.open();
				String[][] result = dialog.getResult();
				if (result != null)
					InternalExchangeUtils.setupRecovery(result, moduleID, container);
				return Status.OK_STATUS;
			}
		};
		reciverySetupJob.setUser(false);
		reciverySetupJob.schedule();
	}

	@Override
	public void execute(final IStorageTask callback) throws StorageException {
		if (!StorageUtils.showUI(null)) {
			callback.execute();
			return;
		}

		final StorageException[] exception = new StorageException[1];
		exception[0] = null; // keep exception and throw it on the original thread

		Display display = PlatformUI.getWorkbench().getDisplay();
		if (!display.isDisposed() && (display.getThread() == Thread.currentThread())) { // we are running in a UI thread

			PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
				IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
				InitWithProgress task = new InitWithProgress(callback);
				try {
					progressService.busyCursorWhile(task);
				} catch (InvocationTargetException e1) {
					exception[0] = new StorageException(StorageException.INTERNAL_ERROR, e1);
					return;
				} catch (InterruptedException e2) {
					exception[0] = new StorageException(StorageException.INTERNAL_ERROR, SecUIMessages.initCancelled);
					return;
				}
				exception[0] = task.getException();
			});
		} else { // we are running in non-UI thread, use Job to show small progress indicator on
					// the status bar
			Job job = new Job(SecUIMessages.secureStorageInitialization) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						callback.execute();
					} catch (StorageException e) {
						exception[0] = e;
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
				exception[0] = new StorageException(StorageException.INTERNAL_ERROR, SecUIMessages.initCancelled);
			}
		}
		if (exception[0] != null)
			throw exception[0];
	}

	@Override
	public Boolean ask(final String msg) {
		if (!StorageUtils.showUI(null)) // container-independent operation
			return null;

		return PlatformUI.getWorkbench().getDisplay().syncCall(
				() -> MessageDialog.openConfirm(StorageUtils.getShell(), SecUIMessages.generalDialogTitle, msg));
	}

	@Override
	public boolean runningUI() {
		return StorageUtils.runningUI();
	}

}
