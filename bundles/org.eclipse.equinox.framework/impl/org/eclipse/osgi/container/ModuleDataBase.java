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

import java.util.*;
import org.osgi.framework.Version;

/**
 * 
 *
 */
public abstract class ModuleDataBase {
	protected ModuleContainer container = null;

	private final Map<String, Module> revisionsByLocations = new HashMap<String, Module>();

	private final Map<String, Collection<ModuleRevision>> revisionByName = new HashMap<String, Collection<ModuleRevision>>();

	private final Map<ModuleRevision, ModuleWiring> wirings = new HashMap<ModuleRevision, ModuleWiring>();

	void setContainer(ModuleContainer container) {
		if (this.container != null)
			throw new IllegalStateException("The container is already set."); //$NON-NLS-1$
		this.container = container;
	}

	final Module getModule(String location) {
		return revisionsByLocations.get(location);
	}

	final Collection<ModuleRevision> getRevisions(String name, Version version) {
		if (version == null)
			return revisionByName.get(name);

		Collection<ModuleRevision> existingRevisions = revisionByName.get(name);
		if (existingRevisions == null) {
			return Collections.emptyList();
		}
		Collection<ModuleRevision> sameVersion = new ArrayList<ModuleRevision>(1);
		for (ModuleRevision revision : existingRevisions) {
			if (revision.getVersion().equals(version)) {
				sameVersion.add(revision);
			}
		}
		return sameVersion;
	}

	final void install(Module module, String location, ModuleRevisionBuilder builder) {
		ModuleRevision newRevision = builder.buildRevision(getNextIdAndIncrement(), location, module, container);
		revisionsByLocations.put(location, module);
		String name = newRevision.getSymbolicName();
		Collection<ModuleRevision> sameName = revisionByName.get(name);
		if (sameName == null) {
			sameName = new ArrayList<ModuleRevision>(1);
			revisionByName.put(name, sameName);
		}
		sameName.add(newRevision);
		addCapabilities(newRevision);
		incrementTimestamp();
	}

	final void uninstall(Module module) {
		ModuleRevisions uninstalling = module.getRevisions();
		revisionsByLocations.remove(uninstalling.getLocation());
		List<ModuleRevision> revisions = uninstalling.getModuleRevisions();
		for (ModuleRevision revision : revisions) {
			removeCapabilities(revision);
			String name = revision.getSymbolicName();
			if (name != null) {
				Collection<ModuleRevision> sameName = revisionByName.get(name);
				if (sameName != null) {
					sameName.remove(revision);
				}
			}
		}
		uninstalling.uninstall();
		incrementTimestamp();
	}

	final ModuleRevision update(Module module, ModuleRevisionBuilder builder) {
		ModuleRevision newRevision = builder.addRevision(module.getRevisions());
		String name = newRevision.getSymbolicName();
		Collection<ModuleRevision> sameName = revisionByName.get(name);
		if (sameName == null) {
			sameName = new ArrayList<ModuleRevision>(1);
			revisionByName.put(name, sameName);
		}
		sameName.add(newRevision);
		addCapabilities(newRevision);
		incrementTimestamp();
		return newRevision;
	}

	ModuleWiring getWiring(ModuleRevision revision) {
		return wirings.get(revision);
	}

	Map<ModuleRevision, ModuleWiring> getWiringsCopy() {
		return new HashMap<ModuleRevision, ModuleWiring>(wirings);
	}

	Collection<ModuleRevision> getCurrentUnresolved() {
		Collection<ModuleRevision> unresolved = new ArrayList<ModuleRevision>();
		for (Module module : revisionsByLocations.values()) {
			List<ModuleRevision> revisions = module.getRevisions().getModuleRevisions();
			if (!revisions.isEmpty())
				unresolved.add(revisions.get(0));
		}
		return unresolved;
	}

	protected abstract long getNextIdAndIncrement();

	protected abstract long getTimestamp();

	protected abstract long incrementTimestamp();

	protected abstract void addCapabilities(ModuleRevision revision);

	protected abstract void removeCapabilities(ModuleRevision revision);

	/**
	 * Returns a mutable snapshot of capabilities that are candidates for 
	 * satisfying the specified requirement.
	 * @param requirement the requirement
	 * @return the candidates for the requirement
	 */
	protected abstract List<ModuleCapability> findCapabilities(ModuleRequirement requirement);
}
