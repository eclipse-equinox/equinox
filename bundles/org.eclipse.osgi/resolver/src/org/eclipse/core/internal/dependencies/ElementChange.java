/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.internal.dependencies;

import org.eclipse.core.dependencies.IElement;
import org.eclipse.core.dependencies.IElementChange;

class ElementChange implements IElementChange {
	private IElement element;
	private int kind;

	ElementChange(IElement element, int kind) {
		this.element = element;
		this.kind = kind;
	}

	public Object getVersionId() {
		return element.getVersionId();
	}

	public int getKind() {
		return kind;
	}

	public IElement getElement() {
		return element;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(element.getId());
		result.append('_');
		result.append(getVersionId());
		result.append(" ("); //$NON-NLS-1$
		result.append(getStatusName(getKind()));
		result.append(')');
		return result.toString();
	}

	private String getStatusName(int status) {
		StringBuffer statusStr = new StringBuffer();
		if ((status & ADDED) != 0)
			statusStr.append("ADDED|");
		if ((status & REMOVED) != 0)
			statusStr.append("REMOVED|");
		if ((status & RESOLVED) != 0)
			statusStr.append("RESOLVED|");
		if ((status & UNRESOLVED) != 0)
			statusStr.append("UNRESOLVED|");
		if ((status & LINKAGE_CHANGED) != 0)
			statusStr.append("LINKAGE_CHANGED|");
		if (statusStr.length() == 0)
			statusStr.append("UNKNOWN");
		else
			statusStr.deleteCharAt(statusStr.length() - 1);
		return statusStr.toString();
	}

	void setKind(int kind) {
		this.kind = kind;
	}
}