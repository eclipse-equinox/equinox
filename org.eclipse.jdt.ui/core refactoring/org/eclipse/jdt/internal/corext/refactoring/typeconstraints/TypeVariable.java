/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

public final class TypeVariable extends ConstraintVariable {

	private final String fSource;
	private final CompilationUnitRange fTypeRange;
	
	public TypeVariable(Type type){
		super(type.resolveBinding());
		fSource= type.toString();
		ICompilationUnit cu= ASTCreator.getCu(type);
		Assert.isNotNull(cu);
		fTypeRange= new CompilationUnitRange(cu, ASTNodes.getElementType(type));
	}

	public TypeVariable(ITypeBinding binding, String source, CompilationUnitRange range){
		super(binding);
		fSource= source;
		fTypeRange= range;
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fSource;
	}

	public CompilationUnitRange getCompilationUnitRange() {
		return fTypeRange;
	}
}