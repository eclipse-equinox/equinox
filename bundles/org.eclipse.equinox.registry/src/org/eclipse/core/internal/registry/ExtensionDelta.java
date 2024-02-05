/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import org.eclipse.core.runtime.*;

public class ExtensionDelta implements IExtensionDelta {
	private int kind;
	private int extension;
	private int extensionPoint;
	private RegistryDelta containingDelta;

	void setContainingDelta(RegistryDelta containingDelta) {
		this.containingDelta = containingDelta;
	}

	int getExtensionId() {
		return extension;
	}

	int getExtensionPointId() {
		return extensionPoint;
	}

	@Override
	public IExtensionPoint getExtensionPoint() {
		return new ExtensionPointHandle(containingDelta.getObjectManager(), extensionPoint);
	}

	public void setExtensionPoint(int extensionPoint) {
		this.extensionPoint = extensionPoint;
	}

	@Override
	public int getKind() {
		return kind;
	}

	@Override
	public IExtension getExtension() {
		return new ExtensionHandle(containingDelta.getObjectManager(), extension);
	}

	public void setExtension(int extension) {
		this.extension = extension;
	}

	public void setKind(int kind) {
		this.kind = kind;
	}

	@Override
	public String toString() {
		return "\n\t\t" + getExtensionPoint().getUniqueIdentifier() + " - " + getExtension().getNamespaceIdentifier() //$NON-NLS-1$ //$NON-NLS-2$
				+ '.' + getExtension().getSimpleIdentifier() + " (" + getKindString(this.getKind()) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String getKindString(int kind) {
		switch (kind) {
		case ADDED:
			return "ADDED"; //$NON-NLS-1$
		case REMOVED:
			return "REMOVED"; //$NON-NLS-1$
		}
		return "UNKNOWN"; //$NON-NLS-1$
	}
}
