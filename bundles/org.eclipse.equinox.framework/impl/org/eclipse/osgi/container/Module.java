/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.util.List;
import org.osgi.framework.BundleReference;

public abstract class Module implements BundleReference {
	private volatile ModuleRevisions revisions;

	protected final ModuleRevisions getRevisions() {
		ModuleRevisions current = revisions;
		if (current == null)
			throw new IllegalStateException("Module installation is not complete."); //$NON-NLS-1$
		return current;
	}

	protected final ModuleRevision getCurrentRevision() {
		ModuleRevisions current = revisions;
		if (current == null)
			throw new IllegalStateException("Module installation is not complete."); //$NON-NLS-1$
		List<ModuleRevision> revisionList = current.getModuleRevisions();
		return revisionList.isEmpty() || current.isUninstalled() ? null : revisionList.get(0);
	}

	final void setRevisions(ModuleRevisions revisions) {
		this.revisions = revisions;
	}
}
