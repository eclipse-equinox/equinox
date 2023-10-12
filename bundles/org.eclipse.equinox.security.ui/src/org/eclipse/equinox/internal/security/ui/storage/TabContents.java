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

import java.io.*;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.security.storage.friends.IDeleteListener;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.internal.security.ui.storage.view.*;
import org.eclipse.equinox.security.storage.*;
import org.eclipse.jface.layout.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class TabContents implements ISecurePreferencesSelection, IDeleteListener {

	private Shell shell;
	protected NodesView nodesView = null;
	protected ValuesView valuesView = null;
	protected Button buttonSave = null;

	@Override
	public void setSelection(ISecurePreferences selectedNode) {
		valuesView.setInput(selectedNode);
	}

	public TabContents(TabFolder folder, int index, final Shell shell) {
		this.shell = shell;

		TabItem tab = new TabItem(folder, SWT.NONE, index);
		tab.setText(SecUIMessages.tabContents);
		Composite page = new Composite(folder, SWT.NONE);
		tab.setControl(page);

		SashForm sashForm = new SashForm(page, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		Tree nodeTree = new Tree(sashForm, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		gridData.verticalSpan = 2;
		nodeTree.setLayoutData(gridData);

		Composite rightPane = new Composite(sashForm, SWT.NONE);
		GridLayout rightPaneLayout = new GridLayout();
		rightPaneLayout.marginHeight = 0;
		rightPaneLayout.marginWidth = 2;
		rightPane.setLayout(rightPaneLayout);

		new Label(rightPane, SWT.NONE).setText(SecUIMessages.keysTable);

		Table tableOfValues = new Table(rightPane,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tableOfValues.setLinesVisible(true);
		tableOfValues.setHeaderVisible(true);
		tableOfValues.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		Composite buttonBar = new Composite(rightPane, SWT.NONE);
		GridLayout buttonBarLayout = new GridLayout();
		buttonBarLayout.marginWidth = 0;
		buttonBar.setLayout(buttonBarLayout);
		buttonBar.setLayoutData(new GridData(GridData.END, GridData.BEGINNING, false, false));

		buttonSave = new Button(buttonBar, SWT.PUSH);
		buttonSave.setText(SecUIMessages.saveButton);
		setButtonSize(buttonSave);
		buttonSave.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			ISecurePreferences root = SecurePreferencesFactory.getDefault();
			if (root == null)
				return;
			try {
				root.flush();
			} catch (IOException exception) {
				Activator.log(IStatus.ERROR, exception.getMessage(), null, exception);
			}
			validateSave(); // save could fail so re-check
		}));

		/* Removed for the time being. In future modify/show/export operations could be 
		 * re-introduced with some special access token required to be entered by the user 
		 * @formatter:off
		Button buttonExport = new Button(buttonBar, SWT.CENTER);
		buttonExport.setText(SecUIMessages.exportButton);
		setButtonSize(buttonExport, minButtonWidth);
		buttonExport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				export();
			}
		});
		 * @formatter:on
		*/

		Button buttonDelete = new Button(buttonBar, SWT.PUSH);
		buttonDelete.setText(SecUIMessages.deleteButton);
		setButtonSize(buttonDelete);
		buttonDelete.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> deteleDefaultStorage()));

		URL location = InternalExchangeUtils.defaultStorageLocation();
		if (location != null) {
			new Label(page, SWT.NONE).setText(SecUIMessages.locationButton);
			new Text(page, SWT.READ_ONLY).setText(IPath.fromOSString(location.getFile()).toOSString());
		}

		sashForm.setWeights(new int[] { 30, 70 });

		nodesView = new NodesView(nodeTree, this);
		valuesView = new ValuesView(tableOfValues, this, shell);

		GridLayoutFactory.fillDefaults().margins(LayoutConstants.getSpacing()).generateLayout(page);
		validateSave();
	}

	@Override
	public void modified() {
		validateSave();
	}

	public void validateSave() {
		ISecurePreferences root = SecurePreferencesFactory.getDefault();
		if (root == null)
			return;
		boolean modified = (root == null) ? false : InternalExchangeUtils.isModified(root);
		buttonSave.setEnabled(modified);
	}

	@Override
	public void onDeleted() {
		if (nodesView != null)
			nodesView.postDeleted();
	}

	protected void export() {
		ISecurePreferences root = SecurePreferencesFactory.getDefault();
		if (root == null)
			return;
		ExportDialog dialog = new ExportDialog(shell);
		dialog.open();
		String fileName = dialog.getFileName();
		if (fileName == null)
			return;
		File outputFile = new File(fileName);

		try (PrintStream output = new PrintStream(new FileOutputStream(outputFile))) {
			export(root, output);
			output.flush();
		} catch (FileNotFoundException e) {
			Activator.log(IStatus.ERROR, e.getMessage(), null, e);
			return;
		}
	}

	protected void export(ISecurePreferences node, PrintStream stream) {
		if (IStorageConst.PROVIDER_NODE.equals(node.absolutePath()))
			return; // skip internal node
		String[] keys = node.keys();
		if (keys.length > 0) {
			String header = '[' + node.absolutePath() + ']';
			stream.println(header);
			for (String key : keys) {
				try {
					String data = key + " := " + node.get(key, ""); //$NON-NLS-1$ //$NON-NLS-2$
					stream.println(data);
				} catch (StorageException e) {
					Activator.log(IStatus.ERROR, SecUIMessages.failedDecrypt, null, e);
				}
			}
		}
		String[] children = node.childrenNames();
		for (String child : children) {
			export(node.node(child), stream);
		}
	}

	protected void setButtonSize(Button button) {
		GridDataFactory.defaultsFor(button).align(SWT.FILL, SWT.BEGINNING).grab(false, false).applyTo(button);
	}

	protected void deteleDefaultStorage() {
		URL location = InternalExchangeUtils.defaultStorageLocation();
		if (location == null)
			return;
		MessageBox messageBox = new MessageBox(shell, SWT.YES | SWT.NO);
		messageBox.setText(SecUIMessages.generalDialogTitle);
		messageBox.setMessage(SecUIMessages.confirmDeleteMsg);
		if (messageBox.open() != SWT.YES)
			return;

		// clear the data structure itself in case somebody holds on to it
		ISecurePreferences defaultStorage = SecurePreferencesFactory.getDefault();
		defaultStorage.clear();
		defaultStorage.removeNode();

		// clear it from the list of open storages, delete the file
		InternalExchangeUtils.defaultStorageDelete();

		if (nodesView != null)
			nodesView.postDeleted();

		// suggest restart in case somebody holds on to the deleted storage
		MessageBox postDeletionBox = new MessageBox(shell, SWT.YES | SWT.NO);
		postDeletionBox.setText(SecUIMessages.generalDialogTitle);
		postDeletionBox.setMessage(SecUIMessages.postDeleteMsg);
		int result = postDeletionBox.open();
		if (result == SWT.YES)
			PlatformUI.getWorkbench().restart();
	}

}
