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

public class ModuleRevisions implements BundleRevisions {
	private final Long id;
	private final String location;
	private final List<BundleRevision> revisions = new ArrayList<BundleRevision>(1);
	private final Object monitor = new Object();
	private final Module module;

	ModuleRevisions(Long id, String location, Module module) {
		this.id = id;
		this.location = location;
		this.module = module;
	}

	public Long getId() {
		return id;
	}

	public String getLocation() {
		return location;
	}

	Module getModule() {
		return module;
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

	ModuleRevision addRevision(ModuleRevision revision) {
		synchronized (monitor) {
			revisions.add(0, revision);
		}
		return revision;
	}

}
