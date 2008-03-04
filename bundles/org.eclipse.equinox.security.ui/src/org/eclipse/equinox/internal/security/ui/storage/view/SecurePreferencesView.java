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

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.security.storage.friends.IDeleteListener;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.internal.security.ui.storage.IStorageConst;
import org.eclipse.equinox.security.storage.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

public class SecurePreferencesView extends ViewPart implements IDeleteListener {

	protected NodesView nodesView = null;
	protected ValuesView valuesView = null;

	protected Action saveAction;
	protected Action exportAction;

	public void setSelection(ISecurePreferences selectedNode) {
		valuesView.setInput(selectedNode);
	}

	public SecurePreferencesView() {
		// nothing to do
	}

	public void createPartControl(Composite parent) {
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
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

		nodesView = new NodesView(nodeTree, this);
		valuesView = new ValuesView(tableOfValues, this);

		makeActions();

		// add pull down menu
		IActionBars bars = getViewSite().getActionBars();
		IMenuManager manager = bars.getMenuManager();
		manager.add(saveAction);
		manager.add(new Separator());
		manager.add(exportAction);

		InternalExchangeUtils.addDeleteListener(this);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		nodesView.setFocus();
	}

	private void makeActions() {
		saveAction = new Action() {
			public void run() {
				ISecurePreferences root = SecurePreferencesFactory.getDefault();
				try {
					root.flush();
				} catch (IOException e) {
					Activator.log(IStatus.ERROR, e.getMessage(), null, e);
				}
			}
		};
		saveAction.setText(SecUIMessages.saveCommand);
		saveAction.setToolTipText(SecUIMessages.saveCommandTip);

		exportAction = new Action() {
			public void run() {
				export();
			}
		};
		exportAction.setText(SecUIMessages.exportCommand);
		exportAction.setToolTipText(SecUIMessages.exportCommandTip);
	}

	protected void export() {
		ISecurePreferences root = SecurePreferencesFactory.getDefault();
		if (root == null)
			return;
		ExportDialog dialog = new ExportDialog(new Shell());
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

	public void onDeleted() {
		if (nodesView != null)
			nodesView.postDeleted();
	}
}
