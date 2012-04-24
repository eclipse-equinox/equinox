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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.eclipse.osgi.container.Module.Event;
import org.eclipse.osgi.container.Module.START_OPTIONS;
import org.eclipse.osgi.container.Module.STOP_OPTIONS;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.internal.container.LockSet;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

/**
 * A container for installing, updating, uninstalling and resolve modules.
 *
 */
public final class ModuleContainer {

	/**
	 * Used by install operations to establish a write lock on an install location
	 */
	private final LockSet<String> locationLocks = new LockSet<String>(false);

	/**
	 * Used by install and update operations to establish a write lock for a name
	 */
	private final LockSet<String> nameLocks = new LockSet<String>(false);

	/**
	 * An implementation of FrameworkWiring for this container
	 */
	private final FrameworkWiring frameworkWiring = new ModuleFrameworkWiring();

	/**
	 * The module database for this container.  All access to this database MUST
	 * be guarded by the database lock
	 */
	/* @GuardedBy("moduleDataBase") */
	final ModuleDataBase moduleDataBase;

	/**
	 * Hook used to determine if a module revision being installed or updated will cause a collision
	 */
	private final ModuleCollisionHook moduleCollisionHook;

	/**
	 * The module resolver which implements the ResolverContext and handles calling the 
	 * resolver service.
	 */
	private final ModuleResolver moduleResolver;

	/**
	 * Constructs a new container with the specified collision hook, resolver hook, resolver and module database.
	 * @param moduleCollisionHook the collision hook
	 * @param resolverHookFactory the resolver hook
	 * @param resolver the resolver
	 * @param moduleDataBase the module database
	 */
	public ModuleContainer(ModuleCollisionHook moduleCollisionHook, ResolverHookFactory resolverHookFactory, Resolver resolver, ModuleDataBase moduleDataBase) {
		this.moduleCollisionHook = moduleCollisionHook;
		this.moduleResolver = new ModuleResolver(resolverHookFactory, resolver);
		this.moduleDataBase = moduleDataBase;
	}

	/**
	 * Returns the list of currently installed modules sorted by module id.
	 * @return the list of currently installed modules sorted by module id.
	 */
	public List<Module> getModules() {
		return moduleDataBase.getModules();
	}

	/**
	 * Returns the module installed with the specified id, or null if no 
	 * such module is installed.
	 * @param id the id of the module
	 * @return the module with the specified id, or null of no such module is installed.
	 */
	public Module getModule(long id) {
		return moduleDataBase.getModule(id);
	}

	/**
	 * Returns the module installed with the specified location, or null if no 
	 * such module is installed.
	 * @param location the location of the module
	 * @return the module with the specified location, or null of no such module is installed.
	 */
	public Module getModule(String location) {
		return moduleDataBase.getModule(location);
	}

	/**
	 * Installs a new module using the specified location.  The specified
	 * builder is used to create a new {@link ModuleRevision revision} 
	 * which will become the {@link Module#getCurrentRevision() current}
	 * revision of the new module.
	 * <p>
	 * If a module already exists with the specified location then the 
	 * existing module is returned and the builder is not used.
	 * @param origin the module performing the install, may be {@code null}.
	 * @param location The location identifier of the module to install. 
	 * @param builder the builder used to create the revision to install.
	 * @return a new module or a existing module if one exists at the 
	 *     specified location.
	 * @throws BundleException if some error occurs installing the module
	 */
	public Module install(Module origin, String location, ModuleRevisionBuilder builder) throws BundleException {
		String name = builder.getSymbolicName();
		boolean locationLocked = false;
		boolean nameLocked = false;
		try {
			Module existingLocation = null;
			Collection<Module> collisionCandidates = Collections.emptyList();
			moduleDataBase.lockRead();
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
					// Collect existing current revisions with the same name and version as the revision we want to install
					// This is to perform the collision check below
					Collection<ModuleRevision> existingRevisionNames = moduleDataBase.getRevisions(name, builder.getVersion());
					if (!existingRevisionNames.isEmpty()) {
						collisionCandidates = new ArrayList<Module>(1);
						for (ModuleRevision equinoxRevision : existingRevisionNames) {
							if (!equinoxRevision.isCurrent())
								continue; // only pay attention to current revisions
							// need to prevent duplicates here; this is in case a revisions object contains multiple revision objects.
							if (!collisionCandidates.contains(equinoxRevision.getRevisions().getModule()))
								collisionCandidates.add(equinoxRevision.getRevisions().getModule());
						}
					}
				}
			} finally {
				moduleDataBase.unlockRead();
			}
			// Check that the existing location is visible from the origin module
			if (existingLocation != null) {
				if (origin != null) {
					Bundle bundle = origin.getBundle();
					BundleContext context = bundle == null ? null : bundle.getBundleContext();
					if (context != null && context.getBundle(existingLocation.getId()) == null) {
						Bundle b = existingLocation.getBundle();
						throw new BundleException("Bundle \"" + b.getSymbolicName() + "\" version \"" + b.getVersion() + "\" is already installed at location: " + location, BundleException.REJECTED_BY_HOOK);
					}
				}
				return existingLocation;
			}
			// Check that the bundle does not collide with other bundles with the same name and version
			// This is from the perspective of the origin bundle
			if (origin != null && !collisionCandidates.isEmpty()) {
				moduleCollisionHook.filterCollisions(ModuleCollisionHook.INSTALLING, origin, collisionCandidates);
			}
			if (!collisionCandidates.isEmpty()) {
				throw new BundleException("A bundle is already installed with name \"" + name + "\" and version \"" + builder.getVersion(), BundleException.DUPLICATE_BUNDLE_ERROR);
			}

			Module result = moduleDataBase.install(location, builder);

			result.fireEvent(Event.INSTALLED);

			return result;
		} finally {
			if (locationLocked)
				locationLocks.unlock(location);
			if (nameLocked)
				nameLocks.unlock(name);
		}
	}

	/**
	 * Updates the specified module with a new revision.  The specified
	 * builder is used to create a new {@link ModuleRevision revision} 
	 * which will become the {@link Module#getCurrentRevision() current}
	 * revision of the new module.
	 * @param module the module to update
	 * @param builder the builder used to create the revision for the update.
	 * @throws BundleException if some error occurs updating the module
	 */
	public void update(Module module, ModuleRevisionBuilder builder) throws BundleException {
		String name = builder.getSymbolicName();
		boolean nameLocked = false;
		try {
			Collection<Module> collisionCandidates = Collections.emptyList();
			moduleDataBase.lockRead();
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
					collisionCandidates = new ArrayList<Module>(1);
					for (ModuleRevision equinoxRevision : existingRevisionNames) {
						if (!equinoxRevision.isCurrent())
							continue;
						Module m = equinoxRevision.getRevisions().getModule();
						if (m.equals(module))
							continue; // don't worry about the updating modules revisions
						// need to prevent duplicates here; this is in case a revisions object contains multiple revision objects.
						if (!collisionCandidates.contains(m))
							collisionCandidates.add(m);
					}
				}

			} finally {
				moduleDataBase.unlockRead();
			}

			// Check that the module does not collide with other modules with the same name and version
			// This is from the perspective of the module being updated
			if (module != null && !collisionCandidates.isEmpty()) {
				moduleCollisionHook.filterCollisions(ModuleCollisionHook.UPDATING, module, collisionCandidates);
			}

			if (!collisionCandidates.isEmpty()) {
				throw new BundleException("A bundle is already installed with name \"" + name + "\" and version \"" + builder.getVersion(), BundleException.DUPLICATE_BUNDLE_ERROR);
			}

			module.lockStateChange(Event.UPDATED);
			State previousState = module.getState();
			BundleException updateError = null;
			try {
				// throwing an exception from stop terminates update
				module.stop(EnumSet.of(STOP_OPTIONS.TRANSIENT));
				try {
					// throwing an exception from updateWorker keeps the previous revision
					module.updateWorker(builder);
					if (Module.RESOLVED_SET.contains(previousState)) {
						// set the state to installed and fire unresolved event
						module.setState(State.INSTALLED);
						module.fireEvent(Event.UNRESOLVED);
					}
					moduleDataBase.update(module, builder);
				} catch (BundleException e) {
					updateError = e;
				}

			} finally {
				module.unlockStateChange(Event.UPDATED);
			}
			if (updateError == null) {
				// only fire updated event on success
				module.fireEvent(Event.UPDATED);
			}
			if (Module.ACTIVE_SET.contains(previousState)) {
				// restart the module if necessary
				module.start(EnumSet.of(START_OPTIONS.TRANSIENT, START_OPTIONS.ACTIVATION_POLICY));
			}
			if (updateError != null) {
				// throw cause of update error
				throw updateError;
			}
		} finally {
			if (nameLocked)
				nameLocks.unlock(name);
		}
	}

	/**
	 * Uninstalls the specified module.
	 * @param module the module to uninstall
	 * @throws BundleException if some error occurs uninstalling the module
	 */
	public void uninstall(Module module) throws BundleException {
		module.lockStateChange(Event.UNINSTALLED);
		try {
			if (Module.ACTIVE_SET.equals(module.getState())) {
				try {
					module.stop(EnumSet.of(STOP_OPTIONS.TRANSIENT));
				} catch (BundleException e) {
					// TODO fire error event
					e.printStackTrace();
				}
			}
			moduleDataBase.uninstall(module);
			module.setState(State.UNINSTALLED);
		} finally {
			module.unlockStateChange(Event.UNINSTALLED);
		}
		module.fireEvent(Event.UNINSTALLED);
	}

	ModuleWiring getWiring(ModuleRevision revision) {
		return moduleDataBase.getWiring(revision);
	}

	/**
	 * Returns the {@link FrameworkWiring} for this container
	 * @return the framework wiring for this container.
	 */
	public FrameworkWiring getFrameworkWiring() {
		return frameworkWiring;
	}

	/**
	 * Attempts to resolve the current revisions of the specified modules.
	 * @param triggers the modules to resolve or {@code null} to resolve all unresolved
	 *    current revisions.
	 * @param triggersMandatory true if the triggers must be resolved.  This will result in 
	 *   a {@link ResolutionException} if set to true and one of the triggers could not be resolved
	 * @throws ResolutionException if a resolution error occurs
	 * @see FrameworkWiring#resolveBundles(Collection)
	 */
	public void resolve(Collection<Module> triggers, boolean triggersMandatory) throws ResolutionException {
		while (!resolve0(triggers, triggersMandatory)) {
			// nothing
		}
	}

	private boolean resolve0(Collection<Module> triggers, boolean triggersMandatory) throws ResolutionException {
		if (triggers == null)
			triggers = new ArrayList<Module>(0);
		Collection<ModuleRevision> triggerRevisions = new ArrayList<ModuleRevision>(triggers.size());
		Collection<ModuleRevision> unresolved = new ArrayList<ModuleRevision>();
		Map<ModuleRevision, ModuleWiring> wiringClone;
		long timestamp;
		moduleDataBase.lockRead();
		try {
			timestamp = moduleDataBase.getTimestamp();
			wiringClone = moduleDataBase.getWiringsClone();
			for (Module module : triggers) {
				ModuleRevision current = module.getCurrentRevision();
				if (current != null)
					triggerRevisions.add(current);
			}
			Collection<Module> allModules = moduleDataBase.getModules();
			for (Module module : allModules) {
				ModuleRevision revision = module.getCurrentRevision();
				if (revision != null && !wiringClone.containsKey(revision))
					unresolved.add(revision);
			}
		} finally {
			moduleDataBase.unlockRead();
		}

		Map<ModuleRevision, ModuleWiring> deltaWiring = moduleResolver.resolveDelta(triggerRevisions, triggersMandatory, unresolved, wiringClone, moduleDataBase);
		if (deltaWiring.isEmpty())
			return true; // nothing to do

		Collection<Module> modulesResolved = new ArrayList<Module>();
		for (ModuleRevision deltaRevision : deltaWiring.keySet()) {
			if (!wiringClone.containsKey(deltaRevision))
				modulesResolved.add(deltaRevision.getRevisions().getModule());
		}
		Collection<Module> modulesLocked = new ArrayList<Module>(modulesResolved.size());
		// now attempt to apply the delta
		try {
			// acquire the necessary RESOLVED state change lock
			for (Module module : modulesResolved) {
				try {
					module.lockStateChange(Event.RESOLVED);
					modulesLocked.add(module);
				} catch (BundleException e) {
					// TODO throw some appropriate exception
					throw new IllegalStateException("Could not acquire state change lock.", e);
				}
			}
			moduleDataBase.lockWrite();
			try {
				if (timestamp != moduleDataBase.getTimestamp())
					return false; // need to try again TODO release state change locks
				Map<ModuleRevision, ModuleWiring> wiringCopy = moduleDataBase.getWiringsCopy();
				for (Map.Entry<ModuleRevision, ModuleWiring> deltaEntry : deltaWiring.entrySet()) {
					ModuleWiring current = wiringCopy.get(deltaEntry.getKey());
					if (current != null) {
						// only need to update the provided and required wires for currently resolved
						current.setProvidedWires(deltaEntry.getValue().getProvidedModuleWires(null));
						current.setRequiredWires(deltaEntry.getValue().getRequiredModuleWires(null));
						deltaEntry.setValue(current); // set the real wiring into the delta
					} else {
						modulesResolved.add(deltaEntry.getValue().getRevision().getRevisions().getModule());
					}
				}
				moduleDataBase.mergeWiring(deltaWiring);
			} finally {
				moduleDataBase.unlockWrite();
			}
			// set the modules state to resolved
			for (Module module : modulesLocked) {
				module.setState(State.RESOLVED);
			}
		} finally {
			for (Module module : modulesLocked) {
				module.unlockStateChange(Event.RESOLVED);
			}
		}

		for (Module module : modulesLocked) {
			module.fireEvent(Event.RESOLVED);
		}
		return true;
	}

	private Collection<Module> unresolve(Collection<Module> initial) {
		Collection<Module> refreshTriggers = null;
		while (refreshTriggers == null) {
			refreshTriggers = unresolve0(initial);
		}
		return refreshTriggers;
	}

	private Collection<Module> unresolve0(Collection<Module> initial) {
		Map<ModuleRevision, ModuleWiring> wiringCopy;
		Collection<Module> refreshTriggers;
		Collection<ModuleRevision> toRemoveRevisions;
		Collection<ModuleWiring> toRemoveWirings;
		Map<ModuleWiring, Collection<ModuleWire>> toRemoveWireLists;
		long timestamp;
		moduleDataBase.lockRead();
		try {
			timestamp = moduleDataBase.getTimestamp();
			wiringCopy = moduleDataBase.getWiringsCopy();
			refreshTriggers = getRefreshClosure(initial, wiringCopy);
			toRemoveRevisions = new ArrayList<ModuleRevision>();
			toRemoveWirings = new ArrayList<ModuleWiring>();
			toRemoveWireLists = new HashMap<ModuleWiring, Collection<ModuleWire>>();
			for (Module module : refreshTriggers) {
				boolean first = true;
				for (ModuleRevision revision : module.getRevisions().getModuleRevisions()) {
					ModuleWiring removedWiring = wiringCopy.remove(revision);
					if (removedWiring != null) {
						toRemoveWirings.add(removedWiring);
						List<ModuleWire> removedWires = removedWiring.getRequiredModuleWires(null);
						for (ModuleWire wire : removedWires) {
							Collection<ModuleWire> providerWires = toRemoveWireLists.get(wire.getProviderWiring());
							if (providerWires == null) {
								providerWires = new ArrayList<ModuleWire>();
								toRemoveWireLists.put(wire.getProviderWiring(), providerWires);
							}
							providerWires.add(wire);
						}
					}
					if (!first || revision.getRevisions().isUninstalled()) {
						toRemoveRevisions.add(revision);
					}
					first = false;
				}
			}
		} finally {
			moduleDataBase.unlockRead();
		}
		Collection<Module> modulesLocked = new ArrayList<Module>(refreshTriggers.size());
		Collection<Module> modulesUnresolved = new ArrayList<Module>();
		try {
			// acquire module state change locks
			try {
				for (Module refreshModule : refreshTriggers) {
					refreshModule.lockStateChange(Event.UNRESOLVED);
					modulesLocked.add(refreshModule);
				}
			} catch (BundleException e) {
				// TODO throw some appropriate exception
				throw new IllegalStateException("Could not acquire state change lock.", e);
			}
			// Stop any active bundles and remove non-active modules from the refreshTriggers
			for (Iterator<Module> iTriggers = refreshTriggers.iterator(); iTriggers.hasNext();) {
				Module refreshModule = iTriggers.next();
				if (Module.ACTIVE_SET.contains(refreshModule.getState())) {
					try {
						refreshModule.stop(EnumSet.of(STOP_OPTIONS.TRANSIENT));
					} catch (BundleException e) {
						// TODO log error
						e.printStackTrace();
					}
				} else {
					iTriggers.remove();
				}
			}

			// do a sanity check on states of the modules, they must be INSTALLED, RESOLVED or UNINSTALLED
			for (Module module : modulesLocked) {
				if (Module.ACTIVE_SET.contains(module.getState())) {
					throw new IllegalStateException("Module is in the wrong state: " + module + ": " + module.getState());
				}
			}

			// finally apply the unresolve to the database
			moduleDataBase.lockWrite();
			try {
				if (timestamp != moduleDataBase.getTimestamp())
					return null; // need to try again
				// remove any wires from unresolved wirings that got removed
				for (Map.Entry<ModuleWiring, Collection<ModuleWire>> entry : toRemoveWireLists.entrySet()) {
					List<ModuleWire> provided = entry.getKey().getProvidedModuleWires(null);
					provided.removeAll(entry.getValue());
					entry.getKey().setProvidedWires(provided);
					for (ModuleWire removedWire : entry.getValue()) {
						// invalidate the wire
						removedWire.invalidate();
					}

				}
				// remove any revisions that got removed as part of the refresh
				for (ModuleRevision removed : toRemoveRevisions) {
					removed.getRevisions().removeRevision(removed);
					moduleDataBase.removeCapabilities(removed);
				}
				// invalidate any removed wiring objects
				for (ModuleWiring moduleWiring : toRemoveWirings) {
					moduleWiring.invalidate();
				}
				moduleDataBase.setWiring(wiringCopy);
			} finally {
				moduleDataBase.unlockWrite();
			}
			// set the state of modules to unresolved
			for (Module module : modulesLocked) {
				if (State.RESOLVED.equals(module.getState())) {
					module.setState(State.INSTALLED);
					modulesUnresolved.add(module);
				}
			}
		} finally {
			for (Module module : modulesLocked) {
				module.unlockStateChange(Event.UNRESOLVED);
			}
		}

		// fire unresolved events after giving up all locks
		for (Module module : modulesUnresolved) {
			module.fireEvent(Event.UNRESOLVED);
		}
		return refreshTriggers;
	}

	/**
	 * Refreshes the specified collection of modules.
	 * @param initial the modules to refresh or {@code null} to refresh the
	 *     removal pending.
	 * @throws ResolutionException
	 * @see FrameworkWiring#refreshBundles(Collection, FrameworkListener...)
	 */
	public void refresh(Collection<Module> initial) throws ResolutionException {
		Collection<Module> refreshTriggers = unresolve(initial);
		resolve(refreshTriggers, false);
		for (Module module : refreshTriggers) {
			try {
				module.start(EnumSet.of(START_OPTIONS.TRANSIENT, START_OPTIONS.ACTIVATION_POLICY));
			} catch (BundleException e) {
				// TODO fire error event
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the dependency closure of for the specified modules.
	 * @param initial The initial modules for which to generate the dependency closure
	 * @return A collection containing a snapshot of the dependency closure of the specified 
	 *    modules, or an empty collection if there were no specified modules. 
	 */
	public Collection<Module> getDependencyClosure(Collection<Module> initial) {
		moduleDataBase.lockRead();
		try {
			return getRefreshClosure(initial, moduleDataBase.getWiringsCopy());
		} finally {
			moduleDataBase.unlockRead();
		}
	}

	/**
	 * Returns the revisions that have {@link ModuleWiring#isCurrent() non-current}, {@link ModuleWiring#isInUse() in use} module wirings.
	 * @return A collection containing a snapshot of the revisions which have non-current, in use ModuleWirings,
	 * or an empty collection if there are no such revisions.
	 */
	public Collection<ModuleRevision> getRemovalPending() {
		return moduleDataBase.getRemovalPending();
	}

	Collection<Module> getRefreshClosure(Collection<Module> initial, Map<ModuleRevision, ModuleWiring> wiringCopy) {
		Set<Module> refreshClosure = new HashSet<Module>();
		if (initial == null) {
			initial = new HashSet<Module>();
			Collection<ModuleRevision> removalPending = moduleDataBase.getRemovalPending();
			for (ModuleRevision revision : removalPending) {
				initial.add(revision.getRevisions().getModule());
			}
		}
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

	class ModuleFrameworkWiring implements FrameworkWiring {

		@Override
		public Bundle getBundle() {
			Module systemModule = moduleDataBase.getModule(Constants.SYSTEM_BUNDLE_LOCATION);
			return systemModule == null ? null : systemModule.getBundle();
		}

		@Override
		public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners) {
			Collection<Module> modules = getModules(bundles);
			// TODO must happen in background
			try {
				refresh(modules);
			} catch (ResolutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// TODO Must fire refresh event to listeners
		}

		@Override
		public boolean resolveBundles(Collection<Bundle> bundles) {
			Collection<Module> modules = getModules(bundles);
			try {
				resolve(modules, false);
			} catch (ResolutionException e) {
				return false;
			}
			for (Module module : modules) {
				if (getWiring(module.getCurrentRevision()) == null)
					return false;
			}
			return true;
		}

		@Override
		public Collection<Bundle> getRemovalPendingBundles() {
			moduleDataBase.lockRead();
			try {
				Collection<Bundle> removalPendingBundles = new HashSet<Bundle>();
				Collection<ModuleRevision> removalPending = moduleDataBase.getRemovalPending();
				for (ModuleRevision moduleRevision : removalPending) {
					removalPendingBundles.add(moduleRevision.getBundle());
				}
				return removalPendingBundles;
			} finally {
				moduleDataBase.unlockRead();
			}
		}

		@Override
		public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
			Collection<Module> modules = getModules(bundles);
			moduleDataBase.lockRead();
			try {
				Collection<Module> closure = getRefreshClosure(modules, moduleDataBase.getWiringsCopy());
				Collection<Bundle> result = new ArrayList<Bundle>(closure.size());
				for (Module module : closure) {
					result.add(module.getBundle());
				}
				return result;
			} finally {
				moduleDataBase.unlockRead();
			}
		}

		private Collection<Module> getModules(final Collection<Bundle> bundles) {
			if (bundles == null)
				return null;
			return AccessController.doPrivileged(new PrivilegedAction<Collection<Module>>() {
				@Override
				public Collection<Module> run() {
					Collection<Module> result = new ArrayList<Module>(bundles.size());
					for (Bundle bundle : bundles) {
						Module module = bundle.adapt(Module.class);
						if (module == null)
							throw new IllegalStateException("Could not adapt a bundle to a module."); //$NON-NLS-1$
						result.add(module);
					}
					return result;
				}
			});
		}
	}
}
