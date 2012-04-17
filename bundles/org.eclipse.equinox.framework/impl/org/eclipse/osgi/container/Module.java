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

/**
 * A module represents a set of revisions installed in a
 * module {@link ModuleContainer container}.
 */
public abstract class Module implements BundleReference {
	private volatile ModuleRevisions revisions;

	/**
	 * Returns the {@link ModuleRevisions} associated with this module.
	 * @return the {@link ModuleRevisions} associated with this module
	 */
	protected final ModuleRevisions getRevisions() {
		ModuleRevisions current = revisions;
		if (current == null)
			throw new IllegalStateException("Module installation is not complete."); //$NON-NLS-1$
		return current;
	}

	/**
	 * Returns the current {@link ModuleRevision revision} associated with this module.
	 * @return the current {@link ModuleRevision revision} associated with this module.
	 */
	protected final ModuleRevision getCurrentRevision() {
		ModuleRevisions current = revisions;
		if (current == null)
			throw new IllegalStateException("Module installation is not complete."); //$NON-NLS-1$
		List<ModuleRevision> revisionList = current.getModuleRevisions();
		return revisionList.isEmpty() || current.isUninstalled() ? null : revisionList.get(0);
	}

	/**
	 * Sets the {@link ModuleRevisions revisions} for this module.  This is done by the container when
	 * {@link ModuleContainer#install(Module, org.osgi.framework.BundleContext, String, ModuleRevisionBuilder) install}
	 * is called with this module.
	 * @param revisions The revisions to associate with this module.
	 */
	final void setRevisions(ModuleRevisions revisions) {
		this.revisions = revisions;
	}

}
