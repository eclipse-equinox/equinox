/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Contribute Java search specific menu elements.
 */
public class ImplementorsSearchGroup extends JavaSearchSubGroup  {

	public ImplementorsSearchGroup(IWorkbenchSite site) {
		fSite= site;
	}

	public ImplementorsSearchGroup(JavaEditor editor) {
		fEditor= editor;
	}

	public static final String GROUP_NAME= SearchMessages.getString("group.implementors"); //$NON-NLS-1$

	protected JavaElementSearchAction[] getActions(IWorkbenchSite site) {
		ArrayList actions= new ArrayList(JavaElementSearchAction.LRU_WORKINGSET_LIST_SIZE + 2);		
		actions.add(new FindImplementorsAction(site));
		actions.add(new FindImplementorsInWorkingSetAction(site));
			
		Iterator iter= JavaElementSearchAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetAction(site, new FindImplementorsInWorkingSetAction(site, workingSets), SearchUtil.toString(workingSets)));
		}
		return (JavaElementSearchAction[])actions.toArray(new JavaElementSearchAction[actions.size()]);
	}

	protected JavaElementSearchAction[] getActions(JavaEditor editor) {
		ArrayList actions= new ArrayList(JavaElementSearchAction.LRU_WORKINGSET_LIST_SIZE + 2);		
		actions.add(new FindImplementorsAction(editor));
		actions.add(new FindImplementorsInWorkingSetAction(editor));
			
		Iterator iter= JavaElementSearchAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetAction(editor, new FindImplementorsInWorkingSetAction(editor, workingSets), SearchUtil.toString(workingSets)));
		}
		return (JavaElementSearchAction[])actions.toArray(new JavaElementSearchAction[actions.size()]);
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}

