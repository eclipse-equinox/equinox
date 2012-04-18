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
 * A database for storing modules, their revisions and wiring states.  The
 * database is responsible for assigning ids and providing access to the
 * capabilities provided by the revisions currently installed.
 * <p>
 * This database is not thread safe all read and write access to this 
 * database must be protected by external means.  The 
 * {@link ModuleContainer container} this database is associated with
 * is responsible for accessing the database in a thread safe way.
 */
public abstract class ModuleDataBase {
	/**
	 * The container this database is associated with
	 */
	protected ModuleContainer container = null;

	/**
	 * A map of modules by location.
	 */
	private final Map<String, Module> modulesByLocations;

	/**
	 * A map of modules by id.
	 */
	private final Map<Long, Module> modulesById;

	/**
	 * A map of revision collections by symbolic name
	 */
	private final Map<String, Collection<ModuleRevision>> revisionByName;

	/**
	 * A map of revision wiring objects.
	 */
	private final Map<ModuleRevision, ModuleWiring> wirings;

	/**
	 * Holds the next id to be assigned to a module when it is installed
	 */
	private final AtomicLong nextId;

	/**
	 * Holds the current timestamp of this database.
	 */
	private final AtomicLong timeStamp;

	/**
	 * Constructs a new database with the the specified nextId and timeStamp. 
	 * @param nextId the next module id for installation.
	 * @param timeStamp the current timestamp of the database
	 */
	public ModuleDataBase(long nextId, long timeStamp) {
		this.modulesByLocations = new HashMap<String, Module>();
		this.modulesById = new HashMap<Long, Module>();
		this.revisionByName = new HashMap<String, Collection<ModuleRevision>>();
		this.wirings = new HashMap<ModuleRevision, ModuleWiring>();
		this.nextId = new AtomicLong(nextId);
		this.timeStamp = new AtomicLong(timeStamp);
	}

	/**
	 * Sets the container for this database.  A database can only
	 * be associated with a single container.  This database gets 
	 * associated with a container when {@link ModuleContainer#setModuleDataBase(ModuleDataBase)}
	 * is called with this database.
	 * @param container the container to associate this database with.
	 */
	final void setContainer(ModuleContainer container) {
		if (this.container != null)
			throw new IllegalStateException("The container is already set."); //$NON-NLS-1$
		this.container = container;
	}

	/**
	 * Returns the module at the given location or null if no module exists
	 * at the given location.
	 * @param location the location of the module.
	 * @return the module at the given location or null.
	 */
	final Module getModule(String location) {
		return modulesByLocations.get(location);
	}

	/**
	 * Returns the module at the given id or null if no module exists
	 * at the given location.
	 * @param id the id of the module.
	 * @return the module at the given id or null.
	 */
	final Module getModule(long id) {
		return modulesById.get(id);
	}

	/**
	 * Returns a snapshot collection of revisions with the specified name 
	 * and version.  If version is {@code null} then all revisions with
	 * the specified name are returned.
	 * @param name the name of the modules
	 * @param version the version of the modules or {@code null}
	 * @return a snapshot collection of revisions with the specified name
	 * and version.
	 */
	final Collection<ModuleRevision> getRevisions(String name, Version version) {
		if (version == null)
			return new ArrayList<ModuleRevision>(revisionByName.get(name));

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

	/**
	 * Installs a new revision using the specified builder, location and module
	 * @param location the location to use for the installation
	 * @param builder the builder to use to create the new revision
	 * @return the installed module
	 */
	final Module install(String location, ModuleRevisionBuilder builder) {
		Module module = populate(location, builder, getNextIdAndIncrement());
		incrementTimestamp();
		return module;
	}

	final protected Module populate(String location, ModuleRevisionBuilder builder, long id) {
		if (container == null)
			throw new IllegalStateException("Container is not set."); //$NON-NLS-1$
		if (modulesByLocations.get(location) != null)
			throw new IllegalArgumentException("Location is already used."); //$NON-NLS-1$
		Module module = createModule(location, id);
		ModuleRevision newRevision = builder.buildRevision(id, location, module, container);
		modulesByLocations.put(location, module);
		modulesById.put(id, module);
		addToRevisionByName(newRevision);
		addCapabilities(newRevision);
		return module;
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

	/**
	 * Uninstalls the specified module from this database.
	 * Uninstalling a module will attempt to clean up any removal pending
	 * revisions possible.
	 * @param module the module to uninstall
	 */
	final void uninstall(Module module) {
		ModuleRevisions uninstalling = module.getRevisions();
		// remove the location
		modulesByLocations.remove(uninstalling.getLocation());
		modulesById.remove(uninstalling.getId());
		// remove the revisions by name
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
			// if the revision does not have a wiring it can safely be removed
			// from the revisions for the module
			ModuleWiring oldWiring = wirings.get(revision);
			if (oldWiring == null) {
				module.getRevisions().removeRevision(revision);
			}
		}
		// marke the revisions as uninstalled
		uninstalling.uninstall();
		// attempt to cleanup any removal pendings
		cleanupRemovalPending();

		incrementTimestamp();
	}

	/**
	 * Installs a new revision using the specified builder, location and module
	 * @param module the module for which the revision is being installed for
	 * @param location the location to use for the installation
	 * @param builder the builder to use to create the new revision
	 */
	/**
	 * Updates the specified module with anew revision using the specified builder.
	 * @param module the module for which the revision provides an update for
	 * @param builder the builder to use to create the new revision
	 */
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

		// remove the old revision by name
		String oldName = oldRevision.getSymbolicName();
		if (oldName != null) {
			Collection<ModuleRevision> oldSameName = revisionByName.get(oldName);
			if (oldSameName != null) {
				oldSameName.remove(oldRevision);
			}
		}

		// if the old revision does not have a wiring it can safely be removed
		ModuleWiring oldWiring = wirings.get(oldRevision);
		if (oldWiring == null) {
			module.getRevisions().removeRevision(oldRevision);
			removeCapabilities(oldRevision);
		}
		// attempt to clean up removal pendings
		cleanupRemovalPending();

		incrementTimestamp();
	}

	/**
	 * Examines the wirings to determine if there are any removal
	 * pending wiring objects that can be removed.  We consider
	 * a removal pending wiring as removable if all dependent
	 * wiring are also removal pending.
	 */
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

	/**
	 * Gets all revisions with a removal pending wiring.
	 * @return all revisions with a removal pending wiring.
	 */
	final Collection<ModuleRevision> getRemovalPending() {
		Collection<ModuleRevision> removalPending = new ArrayList<ModuleRevision>();
		for (ModuleWiring wiring : wirings.values()) {
			if (!wiring.isCurrent())
				removalPending.add(wiring.getRevision());
		}
		return removalPending;
	}

	/**
	 * Returns the current wiring for the specified revision or
	 * null of no wiring exists for the revision.
	 * @param revision the revision to get the wiring for
	 * @return the current wiring for the specified revision.
	 */
	final ModuleWiring getWiring(ModuleRevision revision) {
		return wirings.get(revision);
	}

	/**
	 * Returns a snapshot of the wirings for all revisions.
	 * @return a snapshot of the wirings for all revisions.
	 */
	final protected Map<ModuleRevision, ModuleWiring> getWiringsCopy() {
		return new HashMap<ModuleRevision, ModuleWiring>(wirings);
	}

	/**
	 * Replaces the complete wiring map with the specified wiring
	 * @param newWiring the new wiring to take effect.  The values
	 * from the new wiring are copied.
	 */
	protected final void setWiring(Map<ModuleRevision, ModuleWiring> newWiring) {
		wirings.clear();
		wirings.putAll(newWiring);
		incrementTimestamp();
	}

	/**
	 * Adds all the values from the specified delta wirings to the
	 * wirings current wirings
	 * @param deltaWiring the new wiring values to take effect.
	 * The values from the delta wiring are copied.
	 */
	final void mergeWiring(Map<ModuleRevision, ModuleWiring> deltaWiring) {
		wirings.putAll(deltaWiring);
		incrementTimestamp();
	}

	/**
	 * Returns a snapshot of all modules.
	 * @return a snapshot of all modules.
	 */
	final Collection<Module> getModules() {
		return new ArrayList<Module>(modulesByLocations.values());
	}

	/**
	 * Increments by one the next module ID
	 * @return the previous module ID
	 */
	private long getNextIdAndIncrement() {
		return nextId.getAndIncrement();
	}

	/**
	 * Returns the next module ID
	 * @return the next module ID
	 */
	protected long getNextId() {
		return nextId.get();
	}

	/**
	 * Returns the current timestamp of this database.
	 * The timestamp is incremented any time a modification
	 * is made to this database.  For example:
	 * <ul>
	 *   <li> installing a module
	 *   <li> updating a module
	 *   <li> uninstalling a module
	 *   <li> modifying the wirings
	 * </ul>
	 * @return the current timestamp of this database.
	 */
	final protected long getTimestamp() {
		return timeStamp.get();
	}

	/**
	 * Increments the timestamp of this database.
	 */
	private void incrementTimestamp() {
		timeStamp.incrementAndGet();
	}

	/**
	 * Returns a snapshot map of all modules by location.
	 * @return a snapshot map of all modules by location.
	 */
	final protected Map<String, Module> getModuleLocations() {
		return new HashMap<String, Module>(modulesByLocations);
	}

	/**
	 * Adds the {@link ModuleRevision#getModuleCapabilities(String) capabilities}
	 * provided by the specified revision to this database.  These capabilities must 
	 * become available for lookup with the {@link ModuleDataBase#findCapabilities(ModuleRequirement)}
	 * method.
	 * @param revision the revision which has capabilities to add
	 */
	protected abstract void addCapabilities(ModuleRevision revision);

	/**
	 * Removes the {@link ModuleRevision#getModuleCapabilities(String) capabilities}
	 * provided by the specified revision from this database.  These capabilities
	 * must no longer be available for lookup with the 
	 * {@link ModuleDataBase#findCapabilities(ModuleRequirement)} method.
	 * @param revision
	 */
	protected abstract void removeCapabilities(ModuleRevision revision);

	/**
	 * Returns a mutable snapshot of capabilities that are candidates for 
	 * satisfying the specified requirement.
	 * @param requirement the requirement
	 * @return the candidates for the requirement
	 */
	protected abstract List<ModuleCapability> findCapabilities(ModuleRequirement requirement);

	/**
	 * Creates a new module.  This gets called when a new module is installed.
	 * @param location the location for the module
	 * @param id the id for the module
	 * @return the Module
	 */
	protected abstract Module createModule(String location, long id);

}
