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
	private final Long id;
	private final String location;
	private final ModuleRevisions revisions;

	public Module(Long id, String location, ModuleContainer container) {
		this.id = id;
		this.location = location;
		this.revisions = new ModuleRevisions(this, container);
	}

	public Long getId() {
		return id;
	}

	public String getLocation() {
		return location;
	}

	/**
	 * Returns the {@link ModuleRevisions} associated with this module.
	 * @return the {@link ModuleRevisions} associated with this module
	 */
	public final ModuleRevisions getRevisions() {
		return revisions;
	}

	/**
	 * Returns the current {@link ModuleRevision revision} associated with this module.
	 * @return the current {@link ModuleRevision revision} associated with this module.
	 */
	public final ModuleRevision getCurrentRevision() {
		List<ModuleRevision> revisionList = revisions.getModuleRevisions();
		return revisionList.isEmpty() || revisions.isUninstalled() ? null : revisionList.get(0);
	}

}
