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
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

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
	 * Monitors read and write access to the moduleDataBase
	 */
	private final UpgradeableReadWriteLock monitor = new UpgradeableReadWriteLock();

	private final FrameworkWiring frameworkWiring = new ModuleFrameworkWiring();

	/* @GuardedBy("monitor") */
	ModuleDataBase moduleDataBase;

	/**
	 * Hook used to determine if a bundle being installed or updated will cause a collision
	 */
	private final CollisionHook bundleCollisionHook;

	private final ModuleResolver moduleResolver;

	public ModuleContainer(CollisionHook bundleCollisionHook, ResolverHookFactory resolverHookFactory, Resolver resolver) {
		this.bundleCollisionHook = bundleCollisionHook;
		this.moduleResolver = new ModuleResolver(resolverHookFactory, resolver);
	}

	public void setModuleDataBase(ModuleDataBase moduleDataBase) {
		int readLocks = monitor.lockWrite();
		try {
			if (this.moduleDataBase != null)
				throw new IllegalStateException("Module Database is already set."); //$NON-NLS-1$
			this.moduleDataBase = moduleDataBase;
			this.moduleDataBase.setContainer(this);
		} finally {
			monitor.unlockWrite(readLocks);
		}
	}

	public Module install(Module module, BundleContext origin, String location, ModuleRevisionBuilder builder) throws BundleException {
		String name = builder.getSymbolicName();
		boolean locationLocked = false;
		boolean nameLocked = false;
		try {
			Module existingLocation = null;
			Collection<Bundle> collisionCandidates = Collections.emptyList();
			monitor.lockRead(false);
			try {
				existingLocation = moduleDataBase.getModule(location);
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
					Collection<ModuleRevision> existingRevisionNames = moduleDataBase.getRevisions(name, builder.getVersion());
					if (!existingRevisionNames.isEmpty()) {
						collisionCandidates = new ArrayList<Bundle>(1);
						for (ModuleRevision equinoxRevision : existingRevisionNames) {
							Bundle b = equinoxRevision.getBundle();
							// need to prevent duplicates here; this is in case a revisions object contains multiple revision objects.
							if (b != null && !collisionCandidates.contains(b))
								collisionCandidates.add(b);
						}
					}
				}
			} finally {
				monitor.unlockRead(false);
			}
			// Check that the existing location is visible from the origin bundle
			if (existingLocation != null) {
				if (origin != null) {
					if (origin.getBundle(existingLocation.getRevisions().getId()) == null) {
						Bundle b = existingLocation.getBundle();
						throw new BundleException("Bundle \"" + b.getSymbolicName() + "\" version \"" + b.getVersion() + "\" is already installed at location: " + location, BundleException.REJECTED_BY_HOOK);
					}
				}
				return existingLocation;
			}
			// Check that the bundle does not collide with other bundles with the same name and version
			// This is from the perspective of the origin bundle
			if (origin != null && !collisionCandidates.isEmpty()) {
				bundleCollisionHook.filterCollisions(CollisionHook.INSTALLING, origin.getBundle(), collisionCandidates);
				if (!collisionCandidates.isEmpty()) {
					throw new BundleException("A bundle is already installed with name \"" + name + "\" and version \"" + builder.getVersion(), BundleException.DUPLICATE_BUNDLE_ERROR);
				}
			}
			int readLocks = monitor.lockWrite();
			try {
				moduleDataBase.install(module, location, builder);
				// downgrade to read lock to fire installed events
				monitor.lockRead(false);
			} finally {
				monitor.unlockWrite(readLocks);
			}
			try {
				// TODO fire installed event
			} finally {
				monitor.unlockRead(false);
			}
			return module;
		} finally {
			if (locationLocked)
				locationLocks.unlock(location);
			if (nameLocked)
				nameLocks.unlock(name);
		}
	}

	public void update(Module module, ModuleRevisionBuilder builder) throws BundleException {
		String name = builder.getSymbolicName();
		boolean nameLocked = false;
		try {
			Collection<Bundle> collisionCandidates = Collections.emptyList();
			monitor.lockRead(false);
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
				Collection<ModuleRevision> existingRevisionNames = moduleDataBase.getRevisions(name, builder.getVersion());
				if (!existingRevisionNames.isEmpty()) {
					collisionCandidates = new ArrayList<Bundle>(1);
					for (ModuleRevision equinoxRevision : existingRevisionNames) {
						Bundle b = equinoxRevision.getBundle();
						// need to prevent duplicates here; this is in case a revisions object contains multiple revision objects.
						if (b != null && !collisionCandidates.contains(b))
							collisionCandidates.add(b);
					}
				}

			} finally {
				monitor.unlockRead(false);
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
			int readLocks = monitor.lockWrite();
			try {
				moduleDataBase.update(module, builder);
			} finally {
				monitor.unlockWrite(readLocks);
			}
		} finally {
			if (nameLocked)
				nameLocks.unlock(name);
		}
	}

	public void uninstall(Module module) {
		int readLocks = monitor.lockWrite();
		try {
			moduleDataBase.uninstall(module);
		} finally {
			monitor.unlockWrite(readLocks);
		}
		// TODO fire uninstalled event
		// no need to hold the read lock because the module has been complete removed
		// from the database at this point. (except for removal pending wirings).
	}

	ModuleWiring getWiring(ModuleRevision revision) {
		monitor.lockRead(false);
		try {
			return moduleDataBase.getWiring(revision);
		} finally {
			monitor.unlockRead(false);
		}
	}

	public FrameworkWiring getFrameworkWiring() {
		return frameworkWiring;
	}

	public void resolve(Collection<Module> triggers) throws ResolutionException {
		monitor.lockRead(true);
		try {
			Map<ModuleRevision, ModuleWiring> wiringCopy = moduleDataBase.getWiringsCopy();
			Collection<ModuleRevision> triggerRevisions = new ArrayList<ModuleRevision>(triggers.size());
			for (Module module : triggers) {
				ModuleRevision current = module.getCurrentRevision();
				if (current != null)
					triggerRevisions.add(current);
			}
			Map<ModuleRevision, ModuleWiring> deltaWiring = moduleResolver.resolveDelta(triggerRevisions, wiringCopy, moduleDataBase);

			Collection<Module> newlyResolved = new ArrayList<Module>();
			// now attempt to apply the delta
			int readLocks = monitor.lockWrite();
			try {
				for (Map.Entry<ModuleRevision, ModuleWiring> deltaEntry : deltaWiring.entrySet()) {
					ModuleWiring current = wiringCopy.get(deltaEntry.getKey());
					if (current != null) {
						// only need to update the provided wires for currently resolved
						current.setProvidedWires(deltaEntry.getValue().getProvidedModuleWires(null));
						deltaEntry.setValue(current); // set the real wiring into the delta
					} else {
						newlyResolved.add(deltaEntry.getValue().getRevision().getRevisions().getModule());
					}
				}
				moduleDataBase.applyWiring(deltaWiring);
			} finally {
				monitor.unlockWrite(readLocks);
			}
			// TODO send out resolved events
		} finally {
			monitor.unlockRead(true);
		}
	}

	private Collection<Module> unresolve(Collection<Module> initial) {
		if (monitor.getReadHoldCount() == 0) // sanity check
			throw new IllegalStateException("Must hold read lock and upgrade request"); //$NON-NLS-1$

		Map<ModuleRevision, ModuleWiring> wiringCopy = moduleDataBase.getWiringsCopy();
		Collection<Module> refreshTriggers = getRefreshClosure(initial, wiringCopy);
		Collection<ModuleRevision> toRemove = new ArrayList<ModuleRevision>();
		for (Module module : refreshTriggers) {
			boolean first = true;
			for (ModuleRevision revision : module.getRevisions().getModuleRevisions()) {
				wiringCopy.remove(revision);
				if (!first || revision.getRevisions().isUninstalled())
					toRemove.add(revision);
				first = false;
			}
			// TODO grab module state change locks and stop modules
			// TODO remove any non-active modules from the refreshTriggers
		}

		int readLocks = monitor.lockWrite();
		try {
			for (ModuleRevision removed : toRemove) {
				removed.getRevisions().removeRevision(removed);
				moduleDataBase.removeCapabilities(removed);
			}
			moduleDataBase.applyWiring(wiringCopy);
		} finally {
			monitor.unlockWrite(readLocks);
		}
		// TODO set unresolved status of modules
		// TODO fire unresolved events
		return refreshTriggers;
	}

	public void refresh(Collection<Module> initial) throws ResolutionException {
		Collection<Module> refreshTriggers;
		monitor.lockRead(true);
		try {
			refreshTriggers = unresolve(initial);
			resolve(refreshTriggers);
		} finally {
			monitor.unlockRead(true);
		}
		// TODO start the trigger modules

	}

	static Collection<Module> getRefreshClosure(Collection<Module> initial, Map<ModuleRevision, ModuleWiring> wiringCopy) {
		Set<Module> refreshClosure = new HashSet<Module>();
		for (Module module : initial)
			addDependents(module, wiringCopy, refreshClosure);
		return refreshClosure;
	}

	private static void addDependents(Module module, Map<ModuleRevision, ModuleWiring> wiringCopy, Set<Module> refreshClosure) {
		if (refreshClosure.contains(module))
			return;
		refreshClosure.add(module);
		List<ModuleRevision> revisions = module.getRevisions().getModuleRevisions();
		for (ModuleRevision revision : revisions) {
			ModuleWiring wiring = wiringCopy.get(revision);
			if (wiring == null)
				continue;
			List<ModuleWire> provided = wiring.getProvidedModuleWires(null);
			// add all requirers of the provided wires
			for (ModuleWire providedWire : provided) {
				addDependents(providedWire.getRequirer().getRevisions().getModule(), wiringCopy, refreshClosure);
			}
			// add all hosts of a fragment
			if (revision.getTypes() == BundleRevision.TYPE_FRAGMENT) {
				List<ModuleWire> hosts = wiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
				for (ModuleWire hostWire : hosts) {
					addDependents(hostWire.getProvider().getRevisions().getModule(), wiringCopy, refreshClosure);
				}
			}
		}
	}

	static Collection<ModuleRevision> getDependencyClosure(ModuleRevision initial, Map<ModuleRevision, ModuleWiring> wiringCopy) {
		Set<ModuleRevision> dependencyClosure = new HashSet<ModuleRevision>();
		addDependents(initial, wiringCopy, dependencyClosure);
		return dependencyClosure;
	}

	private static void addDependents(ModuleRevision revision, Map<ModuleRevision, ModuleWiring> wiringCopy, Set<ModuleRevision> dependencyClosure) {
		if (dependencyClosure.contains(revision))
			return;
		dependencyClosure.add(revision);
		ModuleWiring wiring = wiringCopy.get(revision);
		if (wiring == null)
			return;
		List<ModuleWire> provided = wiring.getProvidedModuleWires(null);
		// add all requirers of the provided wires
		for (ModuleWire providedWire : provided) {
			addDependents(providedWire.getRequirer(), wiringCopy, dependencyClosure);
		}
		// add all hosts of a fragment
		if (revision.getTypes() == BundleRevision.TYPE_FRAGMENT) {
			List<ModuleWire> hosts = wiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
			for (ModuleWire hostWire : hosts) {
				addDependents(hostWire.getProvider(), wiringCopy, dependencyClosure);
			}
		}
	}

	public class ModuleFrameworkWiring implements FrameworkWiring {

		@Override
		public Bundle getBundle() {
			monitor.lockRead(false);
			try {
				Module systemModule = moduleDataBase.getModule(Constants.SYSTEM_BUNDLE_LOCATION);
				return systemModule == null ? null : systemModule.getBundle();
			} finally {
				monitor.unlockRead(false);
			}
		}

		@Override
		public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean resolveBundles(Collection<Bundle> bundles) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Collection<Bundle> getRemovalPendingBundles() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
