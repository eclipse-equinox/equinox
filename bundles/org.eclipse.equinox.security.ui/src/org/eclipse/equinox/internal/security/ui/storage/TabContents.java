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

import java.io.*;
import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.security.storage.friends.IDeleteListener;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.internal.security.ui.storage.view.*;
import org.eclipse.equinox.security.storage.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class TabContents implements ISecurePreferencesSelection, IDeleteListener {

	private Shell shell;
	protected NodesView nodesView = null;
	protected ValuesView valuesView = null;

	public void setSelection(ISecurePreferences selectedNode) {
		valuesView.setInput(selectedNode);
	}

	public TabContents(TabFolder folder, int index, final Shell shell, int minButtonWidth) {
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
		rightPane.setLayout(new GridLayout());

		new Label(rightPane, SWT.NONE).setText(SecUIMessages.keysTable);

		Table tableOfValues = new Table(rightPane, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tableOfValues.setLinesVisible(true);
		tableOfValues.setHeaderVisible(true);
		tableOfValues.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		Composite buttonBar = new Composite(rightPane, SWT.NONE);
		GridLayout buttonBarLayout = new GridLayout();
		buttonBarLayout.marginRight = 0;
		buttonBar.setLayout(buttonBarLayout);
		buttonBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		Button buttonSave = new Button(buttonBar, SWT.CENTER);
		buttonSave.setText(SecUIMessages.saveButton);
		setButtonSize(buttonSave, minButtonWidth);
		buttonSave.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				ISecurePreferences root = SecurePreferencesFactory.getDefault();
				try {
					root.flush();
				} catch (IOException exception) {
					Activator.log(IStatus.ERROR, exception.getMessage(), null, exception);
				}
			}
		});

		Button buttonExport = new Button(buttonBar, SWT.CENTER);
		buttonExport.setText(SecUIMessages.exportButton);
		setButtonSize(buttonExport, minButtonWidth);
		buttonExport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				export();
			}
		});

		Button buttonDelete = new Button(buttonBar, SWT.CENTER);
		buttonDelete.setText(SecUIMessages.deleteButton);
		setButtonSize(buttonDelete, minButtonWidth);
		buttonDelete.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deteleDefaultStorage();
			}
		});

		URL location = InternalExchangeUtils.defaultStorageLocation();
		if (location != null) {
			new Label(page, SWT.NONE).setText(SecUIMessages.locationButton);
			new Text(page, SWT.READ_ONLY).setText(location.getFile().toString());
		}

		sashForm.setWeights(new int[] {30, 70});

		nodesView = new NodesView(nodeTree, this);
		valuesView = new ValuesView(tableOfValues, this, shell);

		GridLayoutFactory.fillDefaults().margins(LayoutConstants.getMargins()).numColumns(1).generateLayout(page);
		InternalExchangeUtils.addDeleteListener(this);
	}

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

		PrintStream output;
		try {
			output = new PrintStream(new FileOutputStream(outputFile));
		} catch (FileNotFoundException e) {
			Activator.log(IStatus.ERROR, e.getMessage(), null, e);
			return;
		}
		export(root, output);
		output.flush();
		output.close();
	}

	protected void export(ISecurePreferences node, PrintStream stream) {
		if (IStorageConst.PROVIDER_NODE.equals(node.absolutePath()))
			return; // skip internal node
		String[] keys = node.keys();
		if (keys.length > 0) {
			String header = '[' + node.absolutePath() + ']';
			stream.println(header);
			for (int i = 0; i < keys.length; i++) {
				try {
					String data = keys[i] + " := " + node.get(keys[i], ""); //$NON-NLS-1$ //$NON-NLS-2$
					stream.println(data);
				} catch (StorageException e) {
					Activator.log(IStatus.ERROR, SecUIMessages.failedDecrypt, null, e);
				}
			}
		}
		String[] children = node.childrenNames();
		for (int i = 0; i < children.length; i++) {
			export(node.node(children[i]), stream);
		}
	}

	protected void setButtonSize(Button button, int minButtonWidth) {
		Dialog.applyDialogFont(button);
		GridData data = new GridData();
		Point minButtonSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(minButtonWidth, minButtonSize.x);
		button.setLayoutData(data);
	}

	protected void deteleDefaultStorage() {
		URL location = InternalExchangeUtils.defaultStorageLocation();
		if (location == null)
			return;
		MessageBox messageBox = new MessageBox(shell, SWT.YES | SWT.NO);
		messageBox.setText(location.getFile().toString());
		messageBox.setMessage(SecUIMessages.confirmDeleteMsg);
		if (messageBox.open() != SWT.YES)
			return;

		// clear the data structure itself in case somebody holds on to it
		ISecurePreferences defaultStorage = SecurePreferencesFactory.getDefault();
		defaultStorage.clear();
		defaultStorage.removeNode();

		// clear it from the list of open storages, delete the file 
		InternalExchangeUtils.defaultStorageDelete();

		// suggest restart in case somebody holds on to the deleted storage
		MessageBox postDeletionBox = new MessageBox(shell, SWT.OK);
		postDeletionBox.setText(SecUIMessages.postDeleteTitle);
		postDeletionBox.setMessage(SecUIMessages.postDeleteMsg);
		postDeletionBox.open();
	}

}
