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
import java.util.concurrent.atomic.AtomicLong;
import org.osgi.framework.Version;

/**
 * 
 *
 */
public abstract class ModuleDataBase {
	protected ModuleContainer container = null;

	private final Map<String, Module> revisionsByLocations;

	private final Map<String, Collection<ModuleRevision>> revisionByName;

	private final Map<ModuleRevision, ModuleWiring> wirings;

	private final AtomicLong nextId;

	private final AtomicLong timeStamp;

	public ModuleDataBase(Map<String, Module> revisionsByLocations, Map<ModuleRevision, ModuleWiring> wirings, long nextBundleId, long timeStamp) {
		this.revisionsByLocations = revisionsByLocations == null ? new HashMap<String, Module>() : new HashMap<String, Module>(revisionsByLocations);
		this.revisionByName = new HashMap<String, Collection<ModuleRevision>>();
		for (Module module : this.revisionsByLocations.values()) {
			ModuleRevisions revisions = module.getRevisions();
			for (ModuleRevision revision : revisions.getModuleRevisions()) {
				addToRevisionByName(revision);
			}
		}
		this.wirings = wirings == null ? new HashMap<ModuleRevision, ModuleWiring>() : new HashMap<ModuleRevision, ModuleWiring>(wirings);
		this.nextId = new AtomicLong(nextBundleId);
		this.timeStamp = new AtomicLong(timeStamp);
	}

	final void setContainer(ModuleContainer container) {
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
		addToRevisionByName(newRevision);
		addCapabilities(newRevision);
		incrementTimestamp();
	}

	private void addToRevisionByName(ModuleRevision revision) {
		String name = revision.getSymbolicName();
		Collection<ModuleRevision> sameName = revisionByName.get(name);
		if (sameName == null) {
			sameName = new ArrayList<ModuleRevision>(1);
			revisionByName.put(name, sameName);
		}
		sameName.add(revision);
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
			ModuleWiring oldWiring = wirings.get(revision);
			if (oldWiring == null) {
				module.getRevisions().removeRevision(revision);
			}
		}
		uninstalling.uninstall();

		cleanupRemovalPending();

		incrementTimestamp();
	}

	final void update(Module module, ModuleRevisionBuilder builder) {
		ModuleRevision oldRevision = module.getCurrentRevision();
		ModuleRevision newRevision = builder.addRevision(module.getRevisions());
		String name = newRevision.getSymbolicName();
		Collection<ModuleRevision> sameName = revisionByName.get(name);
		if (sameName == null) {
			sameName = new ArrayList<ModuleRevision>(1);
			revisionByName.put(name, sameName);
		}
		sameName.add(newRevision);
		addCapabilities(newRevision);

		ModuleWiring oldWiring = wirings.get(oldRevision);
		if (oldWiring == null) {
			module.getRevisions().removeRevision(oldRevision);
			removeCapabilities(oldRevision);
		}

		cleanupRemovalPending();

		incrementTimestamp();
	}

	private void cleanupRemovalPending() {
		Collection<ModuleRevision> removalPending = getRemovalPending();
		for (ModuleRevision removed : removalPending) {
			if (wirings.get(removed) == null)
				continue;
			Collection<ModuleRevision> dependencyClosure = ModuleContainer.getDependencyClosure(removed, wirings);
			boolean allPendingRemoval = true;
			for (ModuleRevision pendingRemoval : dependencyClosure) {
				if (pendingRemoval.isCurrent()) {
					allPendingRemoval = false;
					break;
				}
			}
			if (allPendingRemoval) {
				for (ModuleRevision pendingRemoval : dependencyClosure) {
					pendingRemoval.getRevisions().removeRevision(pendingRemoval);
					removeCapabilities(pendingRemoval);
					wirings.remove(pendingRemoval);
				}
			}
		}
	}

	Collection<ModuleRevision> getRemovalPending() {
		Collection<ModuleRevision> removalPending = new ArrayList<ModuleRevision>();
		for (ModuleWiring wiring : wirings.values()) {
			if (!wiring.isCurrent())
				removalPending.add(wiring.getRevision());
		}
		return removalPending;
	}

	final ModuleWiring getWiring(ModuleRevision revision) {
		return wirings.get(revision);
	}

	final Map<ModuleRevision, ModuleWiring> getWiringsCopy() {
		return new HashMap<ModuleRevision, ModuleWiring>(wirings);
	}

	final void applyWiring(Map<ModuleRevision, ModuleWiring> newWiring) {
		wirings.clear();
		wirings.putAll(newWiring);
		incrementTimestamp();
	}

	final Collection<Module> getModules() {
		return new ArrayList<Module>(revisionsByLocations.values());
	}

	private long getNextIdAndIncrement() {
		return nextId.getAndIncrement();
	}

	final protected long getTimestamp() {
		return timeStamp.get();
	}

	private void incrementTimestamp() {
		timeStamp.incrementAndGet();
	}

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
