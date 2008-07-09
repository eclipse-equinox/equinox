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
import org.eclipse.jface.resource.ImageDescriptor;
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
	 * The default value of this variable (false, meaning "not in debug of secure storage"
	 * removes showValueAction, encryptValueAction, and decryptValueAction.
	 */
	static private boolean inDevelopmentMode = false;

	/**
	 * Line to show for encrypted values
	 */
	private final static String ENCRYPTED_SUBSTITUTE = "**********"; //$NON-NLS-1$

	protected ISecurePreferencesSelection parentView;
	protected TableViewer tableViewer;

	protected ISecurePreferences selectedNode = null;

	protected Action addValueAction;
	protected Action removeValueAction;
	protected Action showValueAction = null;
	protected Action encryptValueAction = null;
	protected Action decryptValueAction = null;

	protected Shell shell;

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

	public ValuesView(Table table, final ISecurePreferencesSelection parentView, Shell shell) {
		this.parentView = parentView;
		this.shell = shell;

		TableColumn keysColumn = new TableColumn(table, SWT.LEFT);
		keysColumn.setText(SecUIMessages.keysColumn);
		TableColumn valuesColumn = new TableColumn(table, SWT.LEFT);
		valuesColumn.setText(SecUIMessages.valuesColumn);

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(1));
		layout.addColumnData(new ColumnWeightData(2));
		table.setLayout(layout);

		tableViewer = new TableViewer(table);

		tableViewer.setContentProvider(new TableContentProvider());
		tableViewer.setLabelProvider(new TableLabelProvider());
		tableViewer.setSorter(new TableNameSorter());

		if (Activator.getDefault().debugStorageContents()) {
			makeActions();
			hookContextMenu();
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager(SecUIMessages.nodesContextMenu);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				if (selectedNode == null) {
					addValueAction.setEnabled(false);
					removeValueAction.setEnabled(false);
					return;
				}
				boolean isInternal = selectedNode.absolutePath().startsWith(IStorageConst.PROVIDER_NODE);
				addValueAction.setEnabled(!isInternal);
				removeValueAction.setEnabled(!isInternal);
				if (encryptValueAction != null)
					encryptValueAction.setEnabled(!isInternal);
				if (decryptValueAction != null)
					decryptValueAction.setEnabled(!isInternal);
				if (showValueAction != null)
					showValueAction.setEnabled(false);

				// enablement of encrypted/decrypted
				StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
				Object selected = selection.getFirstElement();
				if (selected instanceof TableValuesElement) {
					String key = ((TableValuesElement) selected).getKey();
					try {
						boolean encrypted = selectedNode.isEncrypted(key);
						if (encryptValueAction != null)
							encryptValueAction.setEnabled(!isInternal && !encrypted);
						if (decryptValueAction != null)
							decryptValueAction.setEnabled(!isInternal && encrypted);
						if (showValueAction != null)
							showValueAction.setEnabled(encrypted);
					} catch (StorageException e) {
						Activator.log(IStatus.ERROR, SecUIMessages.failedDecrypt, null, e);
					}
				}
			}
		});
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);

		// fill context menu
		menuMgr.add(addValueAction);
		menuMgr.add(removeValueAction);
		if (showValueAction != null) {
			menuMgr.add(new Separator());
			menuMgr.add(showValueAction);
		}
		if (encryptValueAction != null) {
			menuMgr.add(new Separator());
			menuMgr.add(encryptValueAction);
		}
		if (decryptValueAction != null)
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
					parentView.modified();
				} catch (StorageException e) {
					Activator.log(IStatus.ERROR, SecUIMessages.failedEncrypt, null, e);
				}
				tableViewer.refresh();
			}
		};
		addValueAction.setText(SecUIMessages.addValueCommand);
		addValueAction.setToolTipText(SecUIMessages.addValueCommandTmp);
		addValueAction.setImageDescriptor(ImageDescriptor.createFromFile(NodesView.class, "/icons/storage/value_new.gif")); //$NON-NLS-1$

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
				MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
				dialog.setText(SecUIMessages.removeValueTitle);
				String msg = NLS.bind(SecUIMessages.removeValueMsg, key);
				dialog.setMessage(msg);
				if (dialog.open() != SWT.YES)
					return;
				selectedNode.remove(key);
				parentView.modified();
				tableViewer.refresh();
			}
		};
		removeValueAction.setText(SecUIMessages.removeValueCommand);
		removeValueAction.setToolTipText(SecUIMessages.removeValueCommandTmp);
		removeValueAction.setImageDescriptor(ImageDescriptor.createFromFile(NodesView.class, "/icons/storage/value_delete.gif")); //$NON-NLS-1$

		if (inDevelopmentMode)
			addDevelopmentMenuOptions();
	}

	private void addDevelopmentMenuOptions() {
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
					MessageBox dialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
					dialog.setText(SecUIMessages.showValueTitle);
					String msg = NLS.bind(SecUIMessages.showValueMsg, key, value);
					dialog.setMessage(msg);
					dialog.open();
				} catch (StorageException e) {
					Activator.log(IStatus.ERROR, SecUIMessages.failedDecrypt, null, e);
					MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
					dialog.setMessage(SecUIMessages.failedDecrypt);
					dialog.open();
					return;
				}
			}
		};
		showValueAction.setText(SecUIMessages.showValueCommand);
		showValueAction.setToolTipText(SecUIMessages.showValueCommandTmp);
		showValueAction.setImageDescriptor(ImageDescriptor.createFromFile(NodesView.class, "/icons/storage/value_show.gif")); //$NON-NLS-1$

		encryptValueAction = new Action() {
			public void run() {
				reCodeValue(true);
			}
		};
		encryptValueAction.setText(SecUIMessages.encryptValueCommand);
		encryptValueAction.setToolTipText(SecUIMessages.encryptValueCommandTmp);
		encryptValueAction.setImageDescriptor(ImageDescriptor.createFromFile(NodesView.class, "/icons/storage/encrypt.gif")); //$NON-NLS-1$

		decryptValueAction = new Action() {
			public void run() {
				reCodeValue(false);
			}
		};
		decryptValueAction.setText(SecUIMessages.decryptValueCommand);
		decryptValueAction.setToolTipText(SecUIMessages.decryptValueCommandTmp);
		decryptValueAction.setImageDescriptor(ImageDescriptor.createFromFile(NodesView.class, "/icons/storage/decrypt.gif")); //$NON-NLS-1$
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
			MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
			dialog.setMessage(SecUIMessages.failedDecrypt);
			dialog.open();
			Activator.log(IStatus.ERROR, SecUIMessages.failedDecrypt, null, e);
			tableViewer.refresh();
			return;
		}
		try {
			selectedNode.put(key, value, encrypted);
			parentView.modified();
		} catch (StorageException e) {
			MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
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
