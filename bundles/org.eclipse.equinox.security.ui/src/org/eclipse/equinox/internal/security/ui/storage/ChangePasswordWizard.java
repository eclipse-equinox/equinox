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

import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class ChangePasswordWizard extends Wizard implements INewWizard {

	public class DecodePage extends WizardPage {
		public DecodePage() {
			super("decodePage"); //$NON-NLS-1$
			setTitle(SecUIMessages.wizardDecodeTitle);
			setDescription(SecUIMessages.wizardDecode);
		}

		public void createControl(Composite parent) {
			Composite container = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			Label note = new Label(container, SWT.WRAP);
			GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			labelData.widthHint = 340;
			note.setLayoutData(labelData);
			note.setText(SecUIMessages.wizardDecodeLabel);
			setControl(container);
		}

		public IWizardPage getPreviousPage() {
			return null;
		}
	}

	public class EncodePage extends WizardPage {
		public EncodePage() {
			super("encodePage"); //$NON-NLS-1$
			setTitle(SecUIMessages.wizardEncodeTitle);
			setDescription(SecUIMessages.wizardEncode);
		}

		public void createControl(Composite parent) {
			Composite container = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			Label note = new Label(container, SWT.WRAP);
			GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			labelData.widthHint = 340;
			note.setLayoutData(labelData);
			note.setText(SecUIMessages.wizardEncodeLabel);
			setControl(container);
		}

		public IWizardPage getPreviousPage() {
			return null;
		}
	}

	public class DonePage extends WizardPage {
		public DonePage() {
			super("donePage"); //$NON-NLS-1$
			setTitle(SecUIMessages.wizardDoneTitle);
		}

		public void createControl(Composite parent) {
			Composite container = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			new Label(container, SWT.NULL).setText(SecUIMessages.wizardDone);
			container.setLayout(layout);
			setControl(container);
		}

		public IWizardPage getPreviousPage() {
			return null;
		}
	}

	public ChangePasswordWizard() {
		super();
		setWindowTitle(SecUIMessages.changePasswordWizardTitle);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to do here
	}

	public void addPages() {
		addPage(new DecodePage());
		addPage(new EncodePage());
		addPage(new DonePage());
	}

	public boolean canFinish() {
		ChangePasswordWizardDialog recodeWizard = (ChangePasswordWizardDialog) getContainer();
		return recodeWizard.isRecodeDone();
	}

	public boolean performFinish() {
		return true;
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		return null;
	}

}