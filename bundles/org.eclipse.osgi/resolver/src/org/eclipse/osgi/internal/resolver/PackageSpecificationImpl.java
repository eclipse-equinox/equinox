/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.service.resolver.PackageSpecification;

public class PackageSpecificationImpl extends VersionConstraintImpl implements PackageSpecification {
	private boolean export;

	public boolean isExported() {
		return export;
	}

	public void setExport(boolean export) {
		this.export = export;
	}

	public String toString() {
		return super.toString() + " (" + (export ? "exp" : " imp") + ")"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}