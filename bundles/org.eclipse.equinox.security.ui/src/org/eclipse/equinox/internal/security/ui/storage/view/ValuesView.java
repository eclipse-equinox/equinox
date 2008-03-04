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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.internal.security.ui.storage.IStorageConst;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

/**
 * Table of { key -> value } pairs associated with the node.
 */
public class ValuesView {

	/**
	 * Line to show for encrypted values
	 */
	private final static String ENCRYPTED_SUBSTITUTE = "••••••••••"; //$NON-NLS-1$

	protected SecurePreferencesView parentView;
	protected TableViewer tableViewer;

	protected ISecurePreferences selectedNode = null;

	protected Action addValueAction;
	protected Action removeValueAction;
	protected Action showValueAction;
	protected Action encryptValueAction;
	protected Action decryptValueAction;

	class TableValuesElement {
		private String key;
		private String value;
		private boolean encrypted;

		public TableValuesElement(String key) {
			this.key = key;
			this.value = null;
			encrypted = true;
		}

		public TableValuesElement(String key, String value) {
			this.key = key;
			this.value = value;
			encrypted = false;
		}

		public String getKey() {
			return key;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			if (value == null)
				return ENCRYPTED_SUBSTITUTE;
			return value;
		}

		public boolean isEncrypted() {
			return encrypted;
		}
	}

	class TableContentProvider implements IStructuredContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			selectedNode = (ISecurePreferences) newInput;
		}

		public void dispose() {
			// nothing to do
		}

		public Object[] getElements(Object parent) {
			if (selectedNode == null)
				return new Object[0];
			String[] keys = selectedNode.keys();
			TableValuesElement[] result = new TableValuesElement[keys.length];
			for (int i = 0; i < keys.length; i++) {
				try {
					if (selectedNode.isEncrypted(keys[i]))
						result[i] = new TableValuesElement(keys[i]);
					else
						result[i] = new TableValuesElement(keys[i], selectedNode.get(keys[i], null));
				} catch (StorageException e) {
					Activator.log(IStatus.ERROR, SecUIMessages.failedDecrypt, null, e);
				}
			}
			return result;
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj == null)
				return null;
			if (!(obj instanceof TableValuesElement))
				return obj.toString();
			switch (index) {
				case 0 :
					return ((TableValuesElement) obj).getKey();
				case 1 :
					return ((TableValuesElement) obj).getValue();
				default :
					return obj.toString();
			}
		}

		public String getText(Object element) {
			return getColumnText(element, 0);
		}

		public Image getColumnImage(Object obj, int index) {
			return null;
		}

		public Image getImage(Object obj) {
			return null;
		}
	}

	class TableNameSorter extends ViewerSorter {
		// using default implementation for now
	}

	public ValuesView(Table table, final SecurePreferencesView parentView) {
		this.parentView = parentView;

		TableColumn keysColumn = new TableColumn(table, SWT.LEFT);
		keysColumn.setText(SecUIMessages.keysColumn);
		keysColumn.setWidth(150);
		TableColumn valuesColumn = new TableColumn(table, SWT.LEFT);
		valuesColumn.setText(SecUIMessages.valuesColumn);
		valuesColumn.setWidth(350);

		tableViewer = new TableViewer(table);

		tableViewer.setContentProvider(new TableContentProvider());
		tableViewer.setLabelProvider(new TableLabelProvider());
		tableViewer.setSorter(new TableNameSorter());

		makeActions();
		hookContextMenu();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager(SecUIMessages.nodesContextMenu);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				boolean isInternal = selectedNode.absolutePath().startsWith(IStorageConst.PROVIDER_NODE);
				addValueAction.setEnabled(!isInternal);
				removeValueAction.setEnabled(!isInternal);
				encryptValueAction.setEnabled(!isInternal);
				decryptValueAction.setEnabled(!isInternal);
				showValueAction.setEnabled(false);

				// enablement of encrypted/decrypted
				StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
				Object selected = selection.getFirstElement();
				if (selected instanceof TableValuesElement) {
					String key = ((TableValuesElement) selected).getKey();
					try {
						boolean encrypted = selectedNode.isEncrypted(key);
						encryptValueAction.setEnabled(!isInternal && !encrypted);
						decryptValueAction.setEnabled(!isInternal && encrypted);
						showValueAction.setEnabled(encrypted);
					} catch (StorageException e) {
						Activator.log(IStatus.ERROR, SecUIMessages.failedDecrypt, null, e);
					}
				}
			}
		});
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		parentView.getSite().registerContextMenu(menuMgr, tableViewer);

		// fill context menu
		menuMgr.add(addValueAction);
		menuMgr.add(removeValueAction);
		menuMgr.add(new Separator());
		menuMgr.add(showValueAction);
		menuMgr.add(new Separator());
		menuMgr.add(encryptValueAction);
		menuMgr.add(decryptValueAction);
	}

	private void makeActions() {
		addValueAction = new Action() {
			public void run() {
				if (selectedNode == null)
					return;

				NewValueDialog newValueDialog = new NewValueDialog(selectedNode.keys(), tableViewer.getControl().getShell());
				if (newValueDialog.open() != Window.OK)
					return;
				String key = newValueDialog.getKey();
				String value = newValueDialog.getValue();
				boolean encrypt = newValueDialog.encrypt();
				try {
					selectedNode.put(key, value, encrypt);
				} catch (StorageException e) {
					Activator.log(IStatus.ERROR, SecUIMessages.failedEncrypt, null, e);
				}
				tableViewer.refresh();
			}
		};
		addValueAction.setText(SecUIMessages.addValueCommand);
		addValueAction.setToolTipText(SecUIMessages.addValueCommandTmp);

		removeValueAction = new Action() {
			public void run() {
				if (selectedNode == null)
					return;
				StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
				Object selected = selection.getFirstElement();
				if (!(selected instanceof TableValuesElement))
					return;

				TableValuesElement node = (TableValuesElement) selected;
				String key = node.getKey();

				// "Are you sure?" dialog 
				MessageBox dialog = new MessageBox(new Shell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
				dialog.setText(SecUIMessages.removeValueTitle);
				String msg = NLS.bind(SecUIMessages.removeValueMsg, key);
				dialog.setMessage(msg);
				if (dialog.open() != SWT.YES)
					return;
				selectedNode.remove(key);
				tableViewer.refresh();
			}
		};
		removeValueAction.setText(SecUIMessages.removeValueCommand);
		removeValueAction.setToolTipText(SecUIMessages.removeValueCommandTmp);

		showValueAction = new Action() {
			public void run() {
				if (selectedNode == null)
					return;
				StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
				Object selected = selection.getFirstElement();
				if (!(selected instanceof TableValuesElement))
					return;

				TableValuesElement node = (TableValuesElement) selected;
				String key = node.getKey();
				try {
					String value = selectedNode.get(key, null);
					MessageBox dialog = new MessageBox(new Shell(), SWT.ICON_INFORMATION | SWT.OK);
					dialog.setText(SecUIMessages.showValueTitle);
					String msg = NLS.bind(SecUIMessages.showValueMsg, key, value);
					dialog.setMessage(msg);
					dialog.open();
				} catch (StorageException e) {
					Activator.log(IStatus.ERROR, SecUIMessages.failedDecrypt, null, e);
					MessageBox dialog = new MessageBox(new Shell(), SWT.ICON_WARNING | SWT.OK);
					dialog.setMessage(SecUIMessages.failedDecrypt);
					dialog.open();
					return;
				}
			}
		};
		showValueAction.setText(SecUIMessages.showValueCommand);
		showValueAction.setToolTipText(SecUIMessages.showValueCommandTmp);

		encryptValueAction = new Action() {
			public void run() {
				reCodeValue(true);
			}
		};
		encryptValueAction.setText(SecUIMessages.encryptValueCommand);
		encryptValueAction.setToolTipText(SecUIMessages.encryptValueCommandTmp);

		decryptValueAction = new Action() {
			public void run() {
				reCodeValue(false);
			}
		};
		decryptValueAction.setText(SecUIMessages.decryptValueCommand);
		decryptValueAction.setToolTipText(SecUIMessages.decryptValueCommandTmp);
	}

	protected void reCodeValue(boolean encrypted) {
		if (selectedNode == null)
			return;
		StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
		Object selected = selection.getFirstElement();
		if (!(selected instanceof TableValuesElement))
			return;

		TableValuesElement node = (TableValuesElement) selected;
		String key = node.getKey();
		String value;
		try {
			value = selectedNode.get(key, null);
		} catch (StorageException e) {
			MessageBox dialog = new MessageBox(new Shell(), SWT.ICON_WARNING | SWT.OK);
			dialog.setMessage(SecUIMessages.failedDecrypt);
			dialog.open();
			Activator.log(IStatus.ERROR, SecUIMessages.failedDecrypt, null, e);
			tableViewer.refresh();
			return;
		}
		try {
			selectedNode.put(key, value, encrypted);
		} catch (StorageException e) {
			MessageBox dialog = new MessageBox(new Shell(), SWT.ICON_WARNING | SWT.OK);
			dialog.setMessage(SecUIMessages.failedEncrypt);
			dialog.open();
			Activator.log(IStatus.ERROR, SecUIMessages.failedEncrypt, null, e);
			tableViewer.refresh();
			return;
		}
		tableViewer.refresh();
	}

	public void setInput(Object input) {
		tableViewer.setInput(input);
	}

}
