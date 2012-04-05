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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.osgi.container.wiring.ModuleWiring;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;

public class ModuleContainer {
	/**
	 * Used by install operations to establish a write lock on an install location
	 */
	private final LockSet<String> locationLocks = new LockSet<String>(false);

	/**
	 * Used by install and update operations to establish a write lock for a name
	 */
	private final LockSet<String> nameLocks = new LockSet<String>(false);

	/**
	 * Monitors read and write access to the revision maps and nextId
	 */
	private final ReentrantReadWriteLock monitor = new ReentrantReadWriteLock();
	/* @GuardedBy("monitor") */
	private final Map<Long, ModuleRevisions> revisionsByIds = new HashMap<Long, ModuleRevisions>();
	/* @GuardedBy("monitor") */
	private final Map<String, ModuleRevisions> revisionsByLocations = new HashMap<String, ModuleRevisions>();
	/* @GuardedBy("monitor") */
	private final Map<String, Collection<ModuleRevision>> revisionByName = new HashMap<String, Collection<ModuleRevision>>();
	/* @GuardedBy("monitor") */
	private final Map<ModuleRevision, ModuleWiring> wirings = new HashMap<ModuleRevision, ModuleWiring>();
	/**
	 * Hook used to determine if a bundle being installed or updated will cause a collision
	 */
	private final CollisionHook bundleCollisionHook;
	/**
	 * Hook used to control the resolution process
	 */
	private final ResolverHookFactory resolverHookFactory;

	/**
	 * The bundle ID used for the next install operation
	 */
	/* @GuardedBy("monitor") */
	private long nextId = 0;

	public ModuleContainer(CollisionHook bundleCollisionHook, ResolverHookFactory resolverHookFactory) {
		this.bundleCollisionHook = bundleCollisionHook;
		this.resolverHookFactory = resolverHookFactory;
	}

	public Module install(Module module, BundleContext origin, String location, ModuleRevisionBuilder builder) throws BundleException {
		String name = builder.getSymbolicName();
		boolean locationLocked = false;
		boolean nameLocked = false;
		try {
			ModuleRevisions existingLocation = null;
			Collection<Bundle> collisionCandidates = Collections.emptyList();
			monitor.readLock().lock();
			try {
				existingLocation = revisionsByLocations.get(location);
				if (existingLocation == null) {
					// Attempt to lock the location and name
					try {
						locationLocked = locationLocks.tryLock(location, 5, TimeUnit.SECONDS);
						nameLocked = name != null && nameLocks.tryLock(name, 5, TimeUnit.SECONDS);
						if (!locationLocked || !nameLocked) {
							throw new BundleException("Failed to obtain id locks for installation.", BundleException.STATECHANGE_ERROR);
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new BundleException("Failed to obtain id locks for installation.", BundleException.STATECHANGE_ERROR, e);
					}
					// Collect existing bundles with the same name and version as the bundle we want to install
					// This is to perform the collision check below
					Collection<ModuleRevision> existingRevisionNames = revisionByName.get(name);
					if (existingRevisionNames != null) {
						collisionCandidates = new ArrayList<Bundle>(1);
						for (ModuleRevision equinoxRevision : existingRevisionNames) {
							if (equinoxRevision.getVersion().equals(builder.getVersion())) {
								Bundle b = equinoxRevision.getBundle();
								// need to prevent duplicates here; this is in case a revisions object contains multiple revision objects.
								if (b != null && !collisionCandidates.contains(b))
									collisionCandidates.add(b);
							}
						}
					}
				}
			} finally {
				monitor.readLock().unlock();
			}
			// Check that the existing location is visible from the origin bundle
			if (existingLocation != null) {
				if (origin != null) {
					if (origin.getBundle(existingLocation.getId()) == null) {
						Bundle b = existingLocation.getBundle();
						throw new BundleException("Bundle \"" + b.getSymbolicName() + "\" version \"" + b.getVersion() + "\" is already installed at location: " + location, BundleException.REJECTED_BY_HOOK);
					}
				}
				return existingLocation.getModule();
			}
			// Check that the bundle does not collide with other bundles with the same name and version
			// This is from the perspective of the origin bundle
			if (origin != null && !collisionCandidates.isEmpty()) {
				bundleCollisionHook.filterCollisions(CollisionHook.INSTALLING, origin.getBundle(), collisionCandidates);
				if (!collisionCandidates.isEmpty()) {
					throw new BundleException("A bundle is already installed with name \"" + name + "\" and version \"" + builder.getVersion(), BundleException.DUPLICATE_BUNDLE_ERROR);
				}
			}
			monitor.writeLock().lock();
			try {
				ModuleRevision result = builder.buildRevision(nextId, location, module, this);
				nextId += 1;
				revisionsByLocations.put(location, result.getRevisions());
				revisionsByIds.put(result.getRevisions().getId(), result.getRevisions());
				Collection<ModuleRevision> sameName = revisionByName.get(name);
				if (sameName == null) {
					sameName = new ArrayList<ModuleRevision>(1);
					revisionByName.put(name, sameName);
				}
				sameName.add(result);
				return result.getRevisions().getModule();
			} finally {
				monitor.writeLock().unlock();
			}
		} finally {
			if (locationLocked)
				locationLocks.unlock(location);
			if (nameLocked)
				nameLocks.unlock(name);
		}
	}

	public ModuleRevision update(Module module, ModuleRevisionBuilder builder) throws BundleException {
		String name = builder.getSymbolicName();
		boolean nameLocked = false;
		try {
			Collection<Bundle> collisionCandidates = Collections.emptyList();
			monitor.readLock().lock();
			try {
				// Attempt to lock the name
				try {
					nameLocked = name != null && nameLocks.tryLock(name, 5, TimeUnit.SECONDS);
					if (!nameLocked) {
						throw new BundleException("Failed to obtain id locks for installation.", BundleException.STATECHANGE_ERROR);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new BundleException("Failed to obtain id locks for installation.", BundleException.STATECHANGE_ERROR, e);
				}
				// Collect existing bundles with the same name and version as the bundle we want to install
				// This is to perform the collision check below
				Collection<ModuleRevision> existingRevisionNames = revisionByName.get(name);
				if (existingRevisionNames != null) {
					collisionCandidates = new ArrayList<Bundle>(1);
					for (ModuleRevision equinoxRevision : existingRevisionNames) {
						if (equinoxRevision.getVersion().equals(builder.getVersion())) {
							Bundle b = equinoxRevision.getBundle();
							// need to prevent duplicates here; this is in case a revisions object contains multiple revision objects.
							// Also remove the bundle object we are actually updating since we allow it to update to the 
							// same name and version
							if (b != null && !b.equals(module.getBundle()) && !collisionCandidates.contains(b))
								collisionCandidates.add(b);
						}
					}
				}

			} finally {
				monitor.readLock().unlock();
			}

			// Check that the bundle does not collide with other bundles with the same name and version
			// This is from the perspective of the origin bundle being updated
			Bundle origin = module.getBundle();
			if (origin != null && !collisionCandidates.isEmpty()) {
				bundleCollisionHook.filterCollisions(CollisionHook.UPDATING, origin, collisionCandidates);
				if (!collisionCandidates.isEmpty()) {
					throw new BundleException("A bundle is already installed with name \"" + name + "\" and version \"" + builder.getVersion(), BundleException.DUPLICATE_BUNDLE_ERROR);
				}
			}
			monitor.writeLock().lock();
			try {
				ModuleRevision result = builder.addRevision(module.getRevisions(), this);
				Collection<ModuleRevision> sameName = revisionByName.get(name);
				if (sameName == null) {
					sameName = new ArrayList<ModuleRevision>(1);
					revisionByName.put(name, sameName);
				}
				sameName.add(result);
				return result;
			} finally {
				monitor.writeLock().unlock();
			}
		} finally {
			if (nameLocked)
				nameLocks.unlock(name);
		}
	}

	public void uninstall(Module module) {
		monitor.writeLock().lock();
		try {
			ModuleRevisions uninstalling = module.getRevisions();
			revisionsByIds.remove(uninstalling.getId());
			revisionsByLocations.remove(uninstalling).getLocation();
			List<BundleRevision> revisions = uninstalling.getRevisions();
			for (BundleRevision revision : revisions) {
				String name = revision.getSymbolicName();
				if (name != null) {
					Collection<ModuleRevision> sameName = revisionByName.get(name);
					if (sameName != null) {
						sameName.remove(revision);
					}
				}
			}
			uninstalling.uninsetall();
		} finally {
			monitor.writeLock().unlock();
		}
	}

	public ModuleWiring getWiring(ModuleRevision revision) {
		monitor.readLock().lock();
		try {
			return wirings.get(revision);
		} finally {
			monitor.readLock().unlock();
		}
	}
}
