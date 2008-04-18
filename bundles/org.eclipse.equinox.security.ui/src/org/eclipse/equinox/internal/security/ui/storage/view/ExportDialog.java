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
package org.eclipse.equinox.internal.security.ui.storage.view;

import java.io.File;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

public class ExportDialog extends TitleAreaDialog {

	private static final String HELP_ID = Activator.PLUGIN_ID + ".ExportDialog"; //$NON-NLS-1$

	private static final ImageDescriptor dlgImageDescriptor = ImageDescriptor.createFromFile(ExportDialog.class, "/icons/storage/export_secure_wiz.png"); //$NON-NLS-1$

	protected final static String[] exportExtensions = new String[] {".txt"}; //$NON-NLS-1$

	protected static final String EXPORT_FILE = "org.eclipse.equinox.security.ui.exportfile"; //$NON-NLS-1$

	protected IEclipsePreferences eclipseNode = new ConfigurationScope().getNode(Activator.PLUGIN_ID);

	protected Text fileText;
	protected Button okButton;
	protected String file;

	private Image dlgTitleImage = null;

	public ExportDialog(Shell parentShell) {
		super(parentShell);
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(SecUIMessages.generalTitle);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, HELP_ID);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, SecUIMessages.exportDialogOK, true);
		okButton.setEnabled(validFile());
		createButton(parent, IDialogConstants.CANCEL_ID, SecUIMessages.exportDialogCancel, false);
	}

	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		setTitle(SecUIMessages.exportDialogTitle);
		setMessage(SecUIMessages.exportDialogMsg, IMessageProvider.WARNING);
		dlgTitleImage = dlgImageDescriptor.createImage();
		setTitleImage(dlgTitleImage);
		return contents;
	}

	protected Control createDialogArea(Composite parent) {
		Composite compositeTop = (Composite) super.createDialogArea(parent);
		Composite composite = new Composite(compositeTop, SWT.NONE);

		Label fileLabel = new Label(composite, SWT.LEFT);
		fileLabel.setText(SecUIMessages.exportDialogFileLabel);
		GridData labelData = new GridData();
		labelData.horizontalSpan = 2;
		fileLabel.setLayoutData(labelData);

		fileText = new Text(composite, SWT.LEFT | SWT.BORDER);

		// pick up last used file name
		String lastFileName = eclipseNode.get(EXPORT_FILE, null);
		if (lastFileName != null)
			fileText.setText(lastFileName);

		// add listener after the setText() above
		fileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				okButton.setEnabled(validFile());
			}
		});

		Button browse = new Button(composite, SWT.NONE);
		browse.setText(SecUIMessages.exportDialogBrowse);
		browse.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		browse.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell());
				dialog.setText(SecUIMessages.fileSelectTitle);
				dialog.setFilterExtensions(exportExtensions);
				String tmp = fileText.getText();
				if (tmp != null)
					dialog.setFileName(tmp);
				String result = dialog.open();
				if (result != null)
					fileText.setText(result);
			}
		});

		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayoutFactory.swtDefaults().generateLayout(composite);

		return composite;
	}

	protected boolean validFile() {
		if (fileText == null)
			return true;

		boolean valid;
		String tmp = fileText.getText();
		if ((tmp != null) && (tmp.length() != 0)) {
			File check = new File(tmp);
			if (check.exists())
				valid = check.canWrite();
			else
				valid = true;
		} else
			valid = false;

		if (valid)
			setMessage(SecUIMessages.exportDialogMsg, IMessageProvider.WARNING);
		else
			setMessage(SecUIMessages.exportDialogInvalidMsg, IMessageProvider.ERROR);
		return valid;
	}

	protected void okPressed() {
		file = fileText.getText();

		// remember it for the next time
		if (file != null && !file.equals(eclipseNode.get(EXPORT_FILE, null))) {
			eclipseNode.put(EXPORT_FILE, file);
			try {
				eclipseNode.flush();
			} catch (BackingStoreException e1) {
				// nothing can be done
			}
		}

		super.okPressed();
	}

	public String getFileName() {
		return file;
	}

}
