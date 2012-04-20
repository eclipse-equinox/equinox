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

import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;

/**
 * An implementation of {@link BundleRevisions} which represent a 
 * {@link Module} installed in a {@link ModuleContainer container}.
 * The ModuleRevisions provides a bridge between the revisions, the 
 * module and the container they are associated with.  The 
 * ModuleRevisions holds the information about the installation of
 * a module in a container such as the module id and location.
 */
public class ModuleRevisions implements BundleRevisions {
	private final Object monitor = new Object();
	private final Module module;
	private final ModuleContainer container;
	/* @GuardedBy("monitor") */
	private final List<ModuleRevision> revisions = new ArrayList<ModuleRevision>(1);
	private volatile boolean uninstalled = false;

	ModuleRevisions(Module module, ModuleContainer container) {
		this.module = module;
		this.container = container;
	}

	Module getModule() {
		return module;
	}

	ModuleContainer getContainer() {
		return container;
	}

	@Override
	public Bundle getBundle() {
		return module.getBundle();
	}

	@Override
	public List<BundleRevision> getRevisions() {
		synchronized (monitor) {
			return new ArrayList<BundleRevision>(revisions);
		}
	}

	List<ModuleRevision> getModuleRevisions() {
		synchronized (monitor) {
			return new ArrayList<ModuleRevision>(revisions);
		}
	}

	ModuleRevision addRevision(ModuleRevision revision) {
		synchronized (monitor) {
			revisions.add(0, revision);
		}
		return revision;
	}

	boolean removeRevision(ModuleRevision revision) {
		synchronized (monitor) {
			return revisions.remove(revision);
		}
		// TODO probably a good place to clean up the resources (jars, class loaders etc.).
	}

	boolean isUninstalled() {
		return uninstalled;
	}

	void uninstall() {
		uninstalled = true;
	}

	public String toString() {
		return "moduleID=" + module.getId(); //$NON-NLS-1$
	}
}
