/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class FindReferencesAction extends JavaElementSearchAction {

	public FindReferencesAction(IWorkbenchSite site) {
		this(site, SearchMessages.getString("Search.FindReferencesAction.label"), new Class[] {IType.class, IMethod.class, IField.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReferencesAction.tooltip")); //$NON-NLS-1$
	}

	public FindReferencesAction(JavaEditor editor) {
		this(editor, SearchMessages.getString("Search.FindReferencesAction.label"), new Class[] {IType.class, IMethod.class, IField.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReferencesAction.tooltip")); //$NON-NLS-1$
	}

	FindReferencesAction(IWorkbenchSite site, String label, Class[] validTypes) {
		super(site, label, validTypes);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_REF);
	}

	FindReferencesAction(JavaEditor editor, String label, Class[] validTypes) {
		super(editor, label, validTypes);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_REF);
	}

	boolean canOperateOn(IJavaElement element) {
		if (super.canOperateOn(element)) {
			if (element.getElementType() == IJavaElement.FIELD) {
				IField field= (IField)element;
				int flags;
				try {
					flags= field.getFlags();
				} catch (JavaModelException ex) {
					return true;
				}
				return !field.isBinary() || !(Flags.isStatic(flags) && Flags.isFinal(flags) && isPrimitive(field));
			}
			return true;
		}
		return false;
	}

	int getLimitTo() {
		return IJavaSearchConstants.REFERENCES;
	}	

	private static boolean isPrimitive(IField field) {
		String fieldType;
		try {
			fieldType= field.getTypeSignature();
		} catch (JavaModelException ex) {
			return false;
		}
		char first= Signature.getElementType(fieldType).charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED);
	}
}