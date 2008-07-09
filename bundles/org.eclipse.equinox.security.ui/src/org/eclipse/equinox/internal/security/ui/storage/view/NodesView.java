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

import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.internal.security.ui.storage.IStorageConst;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;

/**
 * View of nodes available on the secure preferences tree.
 */
public class NodesView {

	/**
	 * For tree viewer to work, the input must not be the same as the root node
	 * or it will get short circuit. Also input can not be null - so have to pass
	 * some dummy value as an input.
	 */
	final private static String defaultPrefs = "default"; //$NON-NLS-1$

	protected ISecurePreferencesSelection parentView;
	protected TreeViewer nodeTreeViewer;

	protected ViewContentProvider contentProvider;

	protected Action addNodeAction;
	protected Action removeNodeAction;
	protected Action refreshNodesAction;

	class ViewContentProvider implements ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			if (nodeTreeViewer != null) {
				nodeTreeViewer.setSelection(null);
				nodeTreeViewer.refresh();
			}
		}

		public void dispose() {
			// nothing to do
		}

		public Object[] getElements(Object parent) {
			if (defaultPrefs.equals(parent))
				return new Object[] {SecurePreferencesFactory.getDefault()};
			return new Object[0];
		}

		public Object getParent(Object child) {
			if (!(child instanceof ISecurePreferences))
				return null;
			ISecurePreferences node = (ISecurePreferences) child;
			ISecurePreferences parentNode = node.parent();
			if (parentNode == null)
				return null;
			return node.parent();
		}

		public Object[] getChildren(Object parent) {
			if (!(parent instanceof ISecurePreferences))
				return new Object[0];
			ISecurePreferences node = (ISecurePreferences) parent;
			String[] childrenNames = node.childrenNames();
			ISecurePreferences[] result = new ISecurePreferences[childrenNames.length];
			for (int i = 0; i < childrenNames.length; i++)
				result[i] = node.node(childrenNames[i]);
			return result;
		}

		public boolean hasChildren(Object parent) {
			if (!(parent instanceof ISecurePreferences))
				return false;
			ISecurePreferences node = (ISecurePreferences) parent;
			String[] childrenNames = node.childrenNames();
			return (childrenNames.length > 0);
		}
	}

	class ViewLabelProvider extends LabelProvider {

		public String getText(Object obj) {
			if (!(obj instanceof ISecurePreferences))
				return obj.toString();
			ISecurePreferences node = (ISecurePreferences) obj;
			if (node.parent() == null)
				return '[' + SecUIMessages.rootNodeName + ']';
			return node.name();
		}

		public Image getImage(Object obj) {
			return null;
		}
	}

	public NodesView(Tree nodeTree, final ISecurePreferencesSelection parentView) {
		this.parentView = parentView;

		nodeTreeViewer = new TreeViewer(nodeTree);
		contentProvider = new ViewContentProvider();
		nodeTreeViewer.setContentProvider(contentProvider);
		nodeTreeViewer.setLabelProvider(new ViewLabelProvider());
		nodeTreeViewer.setInput(defaultPrefs);

		nodeTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				TreeSelection selection = (TreeSelection) event.getSelection();
				Object selected = selection.getFirstElement();
				if (selected instanceof ISecurePreferences)
					parentView.setSelection((ISecurePreferences) selected);
				else
					parentView.setSelection(null);
			}
		});

		if (Activator.getDefault().debugStorageContents()) {
			makeActions();
			hookContextMenu();
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager(SecUIMessages.nodesContextMenu);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				boolean canRemove = false;
				boolean canAdd = false;
				TreeSelection selection = (TreeSelection) nodeTreeViewer.getSelection();
				Object selected = selection.getFirstElement();
				if (selected instanceof ISecurePreferences) {
					ISecurePreferences node = (ISecurePreferences) selected;
					boolean isRoot = (node.parent() == null);
					boolean isInternal = node.absolutePath().startsWith(IStorageConst.PROVIDER_NODE);
					canRemove = (!isRoot && !isInternal);
					canAdd = !isInternal;
				}
				removeNodeAction.setEnabled(canRemove);
				addNodeAction.setEnabled(canAdd);
			}
		});
		Menu menu = menuMgr.createContextMenu(nodeTreeViewer.getControl());
		nodeTreeViewer.getControl().setMenu(menu);

		// fill context menu
		menuMgr.add(refreshNodesAction);
		menuMgr.add(new Separator());
		menuMgr.add(addNodeAction);
		menuMgr.add(removeNodeAction);
	}

	private void makeActions() {
		refreshNodesAction = new Action() {
			public void run() {
				nodeTreeViewer.refresh();
			}
		};
		refreshNodesAction.setText(SecUIMessages.refreshNodesCommand);
		refreshNodesAction.setToolTipText(SecUIMessages.refreshNodesCommandTip);
		refreshNodesAction.setImageDescriptor(ImageDescriptor.createFromFile(NodesView.class, "/icons/storage/node_refresh.gif")); //$NON-NLS-1$

		addNodeAction = new Action() {
			public void run() {
				TreeSelection selection = (TreeSelection) nodeTreeViewer.getSelection();
				Object selected = selection.getFirstElement();
				if (selected instanceof ISecurePreferences) {
					ISecurePreferences node = (ISecurePreferences) selected;

					NewNodeDialog nodeDialog = new NewNodeDialog(nodeTreeViewer.getControl().getShell());
					if (nodeDialog.open() != Window.OK)
						return;
					String name = nodeDialog.getNodeName();
					ISecurePreferences child = node.node(name);
					parentView.modified();

					// expand and select new node
					ISecurePreferences parentNode = child.parent();
					if (parentNode != null)
						nodeTreeViewer.refresh(parentNode, false);
					else
						nodeTreeViewer.refresh(false);
					nodeTreeViewer.expandToLevel(child, 0);
					nodeTreeViewer.setSelection(new StructuredSelection(child), true);
				}

			}
		};
		addNodeAction.setText(SecUIMessages.addNodeCommand);
		addNodeAction.setToolTipText(SecUIMessages.addNodeCommandTip);
		addNodeAction.setImageDescriptor(ImageDescriptor.createFromFile(NodesView.class, "/icons/storage/node_new.gif")); //$NON-NLS-1$

		removeNodeAction = new Action() {
			public void run() {
				TreeSelection selection = (TreeSelection) nodeTreeViewer.getSelection();
				Object selected = selection.getFirstElement();

				if (selected instanceof ISecurePreferences) {
					ISecurePreferences node = (ISecurePreferences) selected;
					ISecurePreferences parentNode = node.parent();
					if (parentNode == null)
						return; // can't remove root node
					node.removeNode();
					parentView.modified();

					// refresh parent node and select it
					nodeTreeViewer.refresh(parentNode, false);
					nodeTreeViewer.setSelection(new StructuredSelection(parentNode), true);
				}
			}
		};
		removeNodeAction.setText(SecUIMessages.removeNodeCommand);
		removeNodeAction.setToolTipText(SecUIMessages.removeNodeCommandTip);
		removeNodeAction.setImageDescriptor(ImageDescriptor.createFromFile(NodesView.class, "/icons/storage/node_delete.gif")); //$NON-NLS-1$
	}

	public void setFocus() {
		nodeTreeViewer.getControl().setFocus();
	}

	public void postDeleted() {
		if (contentProvider == null)
			return;
		nodeTreeViewer.setSelection(StructuredSelection.EMPTY);
		nodeTreeViewer.refresh();
	}
}
