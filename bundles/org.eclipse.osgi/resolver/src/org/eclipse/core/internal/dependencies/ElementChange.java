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

package org.eclipse.core.internal.dependencies;


/**
 * Represents a change that happened to an element's resolution status.
 */
public class ElementChange {
	/** State transitions. */
	public final static int ADDED = 0x01;
	public final static int LINKAGE_CHANGED = 0x10;
	public final static int REMOVED = 0x02;
	public final static int RESOLVED = 0x04;
	public final static int UNRESOLVED = 0x08;
	public final static int UPDATED = ADDED | REMOVED;
	private Element element;
	private int kind;

	ElementChange(Element element, int kind) {
		this.element = element;
		this.kind = kind;
	}

	/**
	 * Returns the affected element.
	 */

	public Element getElement() {
		return element;
	}
	/**
	 * Returns the kind of the transition.
	 */

	public int getKind() {
		return kind;
	}

	private String getStatusName(int status) {
		StringBuffer statusStr = new StringBuffer();
		if ((status & ADDED) != 0)
			statusStr.append("ADDED|"); //$NON-NLS-1$
		if ((status & REMOVED) != 0)
			statusStr.append("REMOVED|"); //$NON-NLS-1$
		if ((status & RESOLVED) != 0)
			statusStr.append("RESOLVED|"); //$NON-NLS-1$
		if ((status & UNRESOLVED) != 0)
			statusStr.append("UNRESOLVED|"); //$NON-NLS-1$
		if ((status & LINKAGE_CHANGED) != 0)
			statusStr.append("LINKAGE_CHANGED|"); //$NON-NLS-1$
		if (statusStr.length() == 0)
			statusStr.append("UNKNOWN"); //$NON-NLS-1$
		else
			statusStr.deleteCharAt(statusStr.length() - 1);
		return statusStr.toString();
	}

	public Object getVersionId() {
		return element.getVersionId();
	}

	void setKind(int kind) {
		this.kind = kind;
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
}