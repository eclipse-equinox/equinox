/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.io.Closeable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.osgi.container.Module.StartOptions;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.Module.StopOptions;
import org.eclipse.osgi.container.ModuleContainer.ResolutionLock.Permits;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleDatabase.Sort;
import org.eclipse.osgi.container.ModuleRequirement.DynamicModuleRequirement;
import org.eclipse.osgi.framework.eventmgr.CopyOnWriteIdentityMap;
import org.eclipse.osgi.framework.eventmgr.EventDispatcher;
import org.eclipse.osgi.framework.eventmgr.EventManager;
import org.eclipse.osgi.framework.eventmgr.ListenerQueue;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.framework.util.ThreadInfoReport;
import org.eclipse.osgi.internal.container.InternalUtils;
import org.eclipse.osgi.internal.container.LockSet;
import org.eclipse.osgi.internal.container.NamespaceList;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.report.resolution.ResolutionReport.Entry;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;

/**
 * A container for installing, updating, uninstalling and resolve modules.
 * @since 3.10
 */
public final class ModuleContainer implements DebugOptionsListener {
	private final static SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	/**
	 * Used by install operations to establish a write lock on an install location
	 */
	private final LockSet<String> locationLocks = new LockSet<>();

	/**
	 * Used by install and update operations to establish a write lock for a name
	 */
	private final LockSet<String> nameLocks = new LockSet<>();

	/**
	 * An implementation of FrameworkWiring for this container
	 */
	private final ContainerWiring frameworkWiring;

	/**
	 * An implementation of FrameworkStartLevel for this container
	 */
	private final ContainerStartLevel frameworkStartLevel;

	/**
	 * The module database for this container.
	 */
	final ModuleDatabase moduleDatabase;

	/**
	 * The module adaptor for this container.
	 */
	final ModuleContainerAdaptor adaptor;

	/**
	 * The module resolver which implements the ResolverContext and handles calling the
	 * resolver service.
	 */
	private final ModuleResolver moduleResolver;

	/**
	 * Holds the system module while it is being refreshed
	 */
	private final AtomicReference<SystemModule> refreshingSystemModule = new AtomicReference<>();

	private final long moduleLockTimeout;

	private final boolean autoStartOnResolve;

	final boolean restrictParallelStart;

	boolean DEBUG_MONITOR_LAZY = false;
	boolean DEBUG_BUNDLE_START_TIME = false;

	/**
	 * Constructs a new container with the specified adaptor, module database.
	 * @param adaptor the adaptor for the container
	 * @param moduledataBase the module database
	 */
	public ModuleContainer(ModuleContainerAdaptor adaptor, ModuleDatabase moduledataBase) {
		this.adaptor = adaptor;
		this.moduleResolver = new ModuleResolver(adaptor);
		this.moduleDatabase = moduledataBase;
		this.frameworkWiring = new ContainerWiring();
		this.frameworkStartLevel = new ContainerStartLevel();
		long tempModuleLockTimeout = 30;
		String moduleLockTimeoutProp = adaptor.getProperty(EquinoxConfiguration.PROP_MODULE_LOCK_TIMEOUT);
		if (moduleLockTimeoutProp != null) {
			try {
				tempModuleLockTimeout = Long.parseLong(moduleLockTimeoutProp);
				// don't do anything less than one second
				if (tempModuleLockTimeout < 1) {
					tempModuleLockTimeout = 1;
				}
			} catch (NumberFormatException e) {
				// will default to 30
			}
		}
		this.moduleLockTimeout = tempModuleLockTimeout;
		DebugOptions debugOptions = adaptor.getDebugOptions();
		if (debugOptions != null) {
			this.DEBUG_MONITOR_LAZY = debugOptions.getBooleanOption(Debug.OPTION_MONITOR_LAZY, false);
		}

		String autoStartOnResolveProp = adaptor.getProperty(EquinoxConfiguration.PROP_MODULE_AUTO_START_ON_RESOLVE);
		if (autoStartOnResolveProp == null) {
			autoStartOnResolveProp = Boolean.toString(true);
		}
		this.autoStartOnResolve = Boolean.parseBoolean(autoStartOnResolveProp);
		this.restrictParallelStart = Boolean.parseBoolean(adaptor.getProperty(EquinoxConfiguration.PROP_EQUINOX_START_LEVEL_RESTRICT_PARALLEL));
	}

	/**
	 * Returns the adaptor for this container
	 * @return the adaptor for this container
	 */
	public ModuleContainerAdaptor getAdaptor() {
		return adaptor;
	}

	/**
	 * Returns the list of currently installed modules sorted by module id.
	 * @return the list of currently installed modules sorted by module id.
	 */
	public List<Module> getModules() {
		return moduleDatabase.getModules();
	}

	/**
	 * Returns the module installed with the specified id, or null if no
	 * such module is installed.
	 * @param id the id of the module
	 * @return the module with the specified id, or null of no such module is installed.
	 */
	public Module getModule(long id) {
		return moduleDatabase.getModule(id);
	}

	/**
	 * Returns the module installed with the specified location, or null if no
	 * such module is installed.
	 * @param location the location of the module
	 * @return the module with the specified location, or null of no such module is installed.
	 */
	public Module getModule(String location) {
		return moduleDatabase.getModule(location);
	}

	/**
	 * Creates a synthetic requirement that is not associated with any module revision.
	 * This is useful for calling {@link FrameworkWiring#findProviders(Requirement)}.
	 * @param namespace the requirement namespace
	 * @param directives the requirement directives
	 * @param attributes the requirement attributes
	 * @return a synthetic requirement
	 */
	public static Requirement createRequirement(String namespace, Map<String, String> directives, Map<String, ?> attributes) {
		return new ModuleRequirement(namespace, directives, attributes, null);
	}

	/**
	 * Generates a human readable string representation of the the given capability,
	 * mapping the namespace to well-known header names.
	 * 
	 * @param capability the {@link Capability} for which a string representation is
	 *                   desired
	 * @since 3.19
	 */
	public static String toString(Capability capability) {
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(capability.getNamespace())) {
			return Constants.EXPORT_PACKAGE + ": " + createOSGiCapability(capability); //$NON-NLS-1$
		} else if (BundleNamespace.BUNDLE_NAMESPACE.equals(capability.getNamespace())) {
			return Constants.BUNDLE_SYMBOLICNAME + ": " + createOSGiCapability(capability); //$NON-NLS-1$
		} else if (HostNamespace.HOST_NAMESPACE.equals(capability.getNamespace())) {
			return Constants.BUNDLE_SYMBOLICNAME + ": " + createOSGiCapability(capability); //$NON-NLS-1$
		}
		return Constants.PROVIDE_CAPABILITY + ": " + capability.toString(); //$NON-NLS-1$
	}

	private static String createOSGiCapability(Capability cap) {
		Map<String, Object> attributes = new HashMap<>(cap.getAttributes());
		Map<String, String> directives = cap.getDirectives();
		String name = String.valueOf(attributes.remove(cap.getNamespace()));
		return name + toString(attributes, false, true) + toString(directives, true, true);
	}

	/**
	 * Generates a human readable string representation of the the given
	 * requirement, mapping the namespace to well-known header names.
	 * 
	 * @param requirement the {@link Requirement} for which a string representation is
	 *                    desired
	 * @since 3.19
	 */
	public static String toString(Requirement requirement) {
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(requirement.getNamespace())) {
			return Constants.IMPORT_PACKAGE + ": " //$NON-NLS-1$
					+ createOSGiRequirement(requirement, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE,
							PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		} else if (BundleNamespace.BUNDLE_NAMESPACE.equals(requirement.getNamespace())) {
			return Constants.REQUIRE_BUNDLE + ": " //$NON-NLS-1$
					+ createOSGiRequirement(requirement, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		} else if (HostNamespace.HOST_NAMESPACE.equals(requirement.getNamespace())) {
			return Constants.FRAGMENT_HOST + ": " //$NON-NLS-1$
					+ createOSGiRequirement(requirement, HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		}
		return Constants.REQUIRE_CAPABILITY + ": " + requirement.toString(); //$NON-NLS-1$
	}

	private static String createOSGiRequirement(Requirement requirement, String... versions) {
		Map<String, String> directives = new HashMap<>(requirement.getDirectives());
		String filter = directives.remove(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filter == null)
			throw new IllegalArgumentException("No filter directive found:" + requirement); //$NON-NLS-1$
		FilterImpl filterImpl;
		try {
			filterImpl = FilterImpl.newInstance(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter directive", e); //$NON-NLS-1$
		}
		Map<String, String> matchingAttributes = filterImpl.getStandardOSGiAttributes(versions);
		String name = matchingAttributes.remove(requirement.getNamespace());
		if (name == null)
			throw new IllegalArgumentException("Invalid requirement: " + requirement); //$NON-NLS-1$
		return name + toString(matchingAttributes, false, true) + toString(directives, true, true);
	}

	static <V> String toString(Map<String, V> map, boolean directives) {
		return toString(map, directives, false);
	}

	static <V> String toString(Map<String, V> map, boolean directives, boolean stringsOnly) {
		if (map.size() == 0)
			return ""; //$NON-NLS-1$
		String assignment = directives ? ":=" : "="; //$NON-NLS-1$ //$NON-NLS-2$
		Set<java.util.Map.Entry<String, V>> set = map.entrySet();
		StringBuilder sb = new StringBuilder();
		for (java.util.Map.Entry<String, V> entry : set) {
			sb.append("; "); //$NON-NLS-1$
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) value;
				if (list.isEmpty())
					continue;
				Object component = list.get(0);
				String className = component.getClass().getName();
				String type = className.substring(className.lastIndexOf('.') + 1);
				sb.append(key).append(':').append("List<").append(type).append(">").append(assignment).append('"'); //$NON-NLS-1$ //$NON-NLS-2$
				for (Object object : list)
					sb.append(object).append(',');
				sb.setLength(sb.length() - 1);
				sb.append('"');
			} else {
				String type = ""; //$NON-NLS-1$
				if (!(value instanceof String) && !stringsOnly) {
					String className = value.getClass().getName();
					type = ":" + className.substring(className.lastIndexOf('.') + 1); //$NON-NLS-1$
				}
				sb.append(key).append(type).append(assignment).append('"').append(value).append('"');
			}
		}
		return sb.toString();
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
	 * @param revisionInfo the revision info for the new revision, may be {@code null}.
	 * @return a new module or a existing module if one exists at the
	 *     specified location.
	 * @throws BundleException if some error occurs installing the module
	 */
	public Module install(Module origin, String location, ModuleRevisionBuilder builder, Object revisionInfo) throws BundleException {
		long id = builder.getId();
		ModuleRevisionBuilder adaptBuilder = getAdaptor().adaptModuleRevisionBuilder(ModuleEvent.INSTALLED, origin, builder, revisionInfo);
		if (adaptBuilder != null) {
			// be sure to restore the id from the original builder
			adaptBuilder.setInternalId(id);
			builder = adaptBuilder;
		}
		String name = builder.getSymbolicName();
		boolean locationLocked = false;
		boolean nameLocked = false;
		try {
			// Attempt to lock the location and name
			try {
				locationLocked = locationLocks.tryLock(location, 5, TimeUnit.SECONDS);
				nameLocked = name != null && nameLocks.tryLock(name, 5, TimeUnit.SECONDS);
				if (!locationLocked) {
					throw new BundleException("Failed to obtain location lock for installation: " + location, BundleException.STATECHANGE_ERROR, new ThreadInfoReport(locationLocks.getLockInfo(location))); //$NON-NLS-1$
				}
				if (name != null && !nameLocked) {
					throw new BundleException("Failed to obtain symbolic name lock for installation: " + name, BundleException.STATECHANGE_ERROR, new ThreadInfoReport(nameLocks.getLockInfo(name))); //$NON-NLS-1$
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new BundleException("Failed to obtain id locks for installation.", BundleException.STATECHANGE_ERROR, e); //$NON-NLS-1$
			}

			Module existingLocation = null;
			Collection<Module> collisionCandidates = Collections.emptyList();
			moduleDatabase.readLock();
			try {
				existingLocation = moduleDatabase.getModule(location);
				if (existingLocation == null) {
					// Collect existing current revisions with the same name and version as the revision we want to install
					// This is to perform the collision check below
					List<ModuleCapability> sameIdentity = moduleDatabase.findCapabilities(getIdentityRequirement(name, builder.getVersion()));
					if (!sameIdentity.isEmpty()) {
						collisionCandidates = new ArrayList<>(1);
						for (ModuleCapability identity : sameIdentity) {
							ModuleRevision equinoxRevision = identity.getRevision();
							if (!equinoxRevision.isCurrent())
								continue; // only pay attention to current revisions
							// need to prevent duplicates here; this is in case a revisions object contains multiple revision objects.
							if (!collisionCandidates.contains(equinoxRevision.getRevisions().getModule()))
								collisionCandidates.add(equinoxRevision.getRevisions().getModule());
						}
					}
				}
			} finally {
				moduleDatabase.readUnlock();
			}
			// Check that the existing location is visible from the origin module
			if (existingLocation != null) {
				if (origin != null) {
					Bundle bundle = origin.getBundle();
					BundleContext context = bundle == null ? null : bundle.getBundleContext();
					if (context != null && context.getBundle(existingLocation.getId()) == null) {
						Bundle b = existingLocation.getBundle();
						throw new BundleException(NLS.bind(Msg.ModuleContainer_NameCollisionWithLocation, new Object[] {b.getSymbolicName(), b.getVersion(), location}), BundleException.REJECTED_BY_HOOK);
					}
				}
				return existingLocation;
			}
			// Check that the bundle does not collide with other bundles with the same name and version
			// This is from the perspective of the origin bundle
			if (origin != null && !collisionCandidates.isEmpty()) {
				adaptor.getModuleCollisionHook().filterCollisions(ModuleCollisionHook.INSTALLING, origin, collisionCandidates);
			}
			if (!collisionCandidates.isEmpty()) {
				throw new BundleException(NLS.bind(Msg.ModuleContainer_NameCollision, name, builder.getVersion()), BundleException.DUPLICATE_BUNDLE_ERROR);
			}

			Module result = moduleDatabase.install(location, builder, revisionInfo);

			adaptor.publishModuleEvent(ModuleEvent.INSTALLED, result, origin);

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
	 * @param revisionInfo the revision info for the new revision, may be {@code null}.
	 * @throws BundleException if some error occurs updating the module
	 */
	public void update(Module module, ModuleRevisionBuilder builder, Object revisionInfo) throws BundleException {
		long id = builder.getId();
		ModuleRevisionBuilder adaptBuilder = getAdaptor().adaptModuleRevisionBuilder(ModuleEvent.UPDATED, module, builder, revisionInfo);
		if (adaptBuilder != null) {
			// be sure to restore the id from the original builder
			adaptBuilder.setInternalId(id);
			builder = adaptBuilder;
		}
		checkAdminPermission(module.getBundle(), AdminPermission.LIFECYCLE);
		String name = builder.getSymbolicName();
		boolean nameLocked = false;
		try {
			// Attempt to lock the name
			try {
				if (name != null && !(nameLocked = nameLocks.tryLock(name, 5, TimeUnit.SECONDS))) {
					throw new BundleException("Failed to obtain id locks for installation: " + name, //$NON-NLS-1$
							BundleException.STATECHANGE_ERROR, new ThreadInfoReport(nameLocks.getLockInfo(name)));
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new BundleException("Failed to obtain id locks for installation.", BundleException.STATECHANGE_ERROR, e); //$NON-NLS-1$
			}

			Collection<Module> collisionCandidates = Collections.emptyList();
			moduleDatabase.readLock();
			try {
				// Collect existing bundles with the same name and version as the bundle we want to install
				// This is to perform the collision check below
				List<ModuleCapability> sameIdentity = moduleDatabase.findCapabilities(getIdentityRequirement(name, builder.getVersion()));
				if (!sameIdentity.isEmpty()) {
					collisionCandidates = new ArrayList<>(1);
					for (ModuleCapability identity : sameIdentity) {
						ModuleRevision equinoxRevision = identity.getRevision();
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
				moduleDatabase.readUnlock();
			}

			// Check that the module does not collide with other modules with the same name and version
			// This is from the perspective of the module being updated
			if (module != null && !collisionCandidates.isEmpty()) {
				adaptor.getModuleCollisionHook().filterCollisions(ModuleCollisionHook.UPDATING, module, collisionCandidates);
			}

			if (!collisionCandidates.isEmpty()) {
				throw new BundleException(NLS.bind(Msg.ModuleContainer_NameCollision, name, builder.getVersion()), BundleException.DUPLICATE_BUNDLE_ERROR);
			}

			module.lockStateChange(ModuleEvent.UPDATED);
			State previousState;
			try {
				module.checkValid();
				previousState = module.getState();
				if (Module.ACTIVE_SET.contains(previousState)) {
					// throwing an exception from stop terminates update
					module.stop(StopOptions.TRANSIENT);
				}
				if (Module.RESOLVED_SET.contains(previousState)) {
					// set the state to installed and publish unresolved event
					module.setState(State.INSTALLED);
					adaptor.publishModuleEvent(ModuleEvent.UNRESOLVED, module, module);
				}
				moduleDatabase.update(module, builder, revisionInfo);
			} finally {
				module.unlockStateChange(ModuleEvent.UPDATED);
			}
			// only publish updated event on success
			adaptor.publishModuleEvent(ModuleEvent.UPDATED, module, module);

			if (Module.ACTIVE_SET.contains(previousState)) {
				try {
					// restart the module if necessary
					module.start(StartOptions.TRANSIENT_RESUME);
				} catch (BundleException e) {
					getAdaptor().publishContainerEvent(ContainerEvent.ERROR, module, e);
				}
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
		checkAdminPermission(module.getBundle(), AdminPermission.LIFECYCLE);
		module.lockStateChange(ModuleEvent.UNINSTALLED);
		State previousState;
		try {
			module.checkValid();
			previousState = module.getState();
			if (Module.ACTIVE_SET.contains(module.getState())) {
				try {
					module.stop(StopOptions.TRANSIENT);
				} catch (BundleException e) {
					adaptor.publishContainerEvent(ContainerEvent.ERROR, module, e);
				}
			}
			if (Module.RESOLVED_SET.contains(previousState)) {
				// set the state to installed and publish unresolved event
				module.setState(State.INSTALLED);
				adaptor.publishModuleEvent(ModuleEvent.UNRESOLVED, module, module);
			}
			moduleDatabase.uninstall(module);
			module.setState(State.UNINSTALLED);
		} finally {
			module.unlockStateChange(ModuleEvent.UNINSTALLED);
		}
		adaptor.publishModuleEvent(ModuleEvent.UNINSTALLED, module, module);
	}

	ModuleWiring getWiring(ModuleRevision revision) {
		return moduleDatabase.getWiring(revision);
	}

	/**
	 * Returns the {@link FrameworkWiring} for this container
	 * @return the framework wiring for this container.
	 */
	public FrameworkWiring getFrameworkWiring() {
		return frameworkWiring;
	}

	/**
	 * Returns the {@link FrameworkStartLevel} for this container
	 * @return the framework start level for this container
	 */
	public FrameworkStartLevel getFrameworkStartLevel() {
		return frameworkStartLevel;
	}

	/**
	 * Attempts to resolve the current revisions of the specified modules.
	 * @param triggers the modules to resolve or {@code null} to resolve all unresolved
	 *    current revisions.
	 * @param triggersMandatory true if the triggers must be resolved.  This will result in
	 *   a {@link ResolutionException} if set to true and one of the triggers could not be resolved.
	 * @see FrameworkWiring#resolveBundles(Collection)
	 * @return A resolution report for the resolve operation
	 */
	public ResolutionReport resolve(Collection<Module> triggers, boolean triggersMandatory) {
		return resolve(triggers, triggersMandatory, false);
	}

	private ResolutionReport resolve(Collection<Module> triggers, boolean triggersMandatory, boolean restartTriggers) {
		if (isRefreshingSystemModule()) {
			return new ModuleResolutionReport(null, Collections.emptyMap(), new ResolutionException("Unable to resolve while shutting down the framework.")); //$NON-NLS-1$
		}
		ResolutionReport report = null;
		try (ResolutionLock.Permits resolutionPermits = _resolutionLock.acquire(1)) {
			do {
				try {
					report = resolveAndApply(triggers, triggersMandatory, restartTriggers, resolutionPermits);
				} catch (RuntimeException e) {
					if (e.getCause() instanceof BundleException) {
						BundleException be = (BundleException) e.getCause();
						if (be.getType() == BundleException.REJECTED_BY_HOOK || be.getType() == BundleException.STATECHANGE_ERROR) {
							return new ModuleResolutionReport(null, Collections.emptyMap(), new ResolutionException(be));
						}
					}
					throw e;
				}
			} while (report == null);
		} catch (ResolutionLockException e) {
			return new ModuleResolutionReport(null, Collections.emptyMap(), new ResolutionException("Timeout acquiring lock for resolution", e, Collections.emptyList())); //$NON-NLS-1$
		}
		return report;
	}

	private ResolutionReport resolveAndApply(Collection<Module> triggers, boolean triggersMandatory, boolean restartTriggers, ResolutionLock.Permits resolutionPermits) {
		if (triggers == null) {
			triggers = new ArrayList<>(0);
		}
		if (triggers.isEmpty()) {
			// if there are no triggers then they cannot be mandatory
			triggersMandatory = false;
		}
		Collection<ModuleRevision> triggerRevisions = new ArrayList<>(triggers.size());
		Collection<ModuleRevision> unresolved = new ArrayList<>();
		Map<ModuleRevision, ModuleWiring> wiringClone;
		long timestamp;
		moduleDatabase.readLock();
		try {
			timestamp = moduleDatabase.getRevisionsTimestamp();
			wiringClone = moduleDatabase.getWiringsClone();
			for (Module module : triggers) {
				if (!State.UNINSTALLED.equals(module.getState())) {
					ModuleRevision current = module.getCurrentRevision();
					if (current != null)
						triggerRevisions.add(current);
				}
			}
			Collection<Module> allModules = moduleDatabase.getModules();
			for (Module module : allModules) {
				ModuleRevision revision = module.getCurrentRevision();
				if (revision != null && !wiringClone.containsKey(revision))
					unresolved.add(revision);
			}
		} finally {
			moduleDatabase.readUnlock();
		}

		ModuleResolutionReport report = moduleResolver.resolveDelta(triggerRevisions, triggersMandatory, unresolved, wiringClone, moduleDatabase);
		Map<Resource, List<Wire>> resolutionResult = report.getResolutionResult();
		Map<ModuleRevision, ModuleWiring> deltaWiring = resolutionResult == null ? Collections.emptyMap() : moduleResolver.generateDelta(resolutionResult, wiringClone);
		if (deltaWiring.isEmpty())
			return report; // nothing to do

		Collection<Module> modulesResolved = new ArrayList<>();
		for (ModuleRevision deltaRevision : deltaWiring.keySet()) {
			if (!wiringClone.containsKey(deltaRevision))
				modulesResolved.add(deltaRevision.getRevisions().getModule());
		}

		return applyDelta(deltaWiring, modulesResolved, triggers, timestamp, restartTriggers, resolutionPermits) ? report : null;
	}

	/**
	 * Attempts to resolve the specified dynamic package name request for the specified revision.
	 * @param dynamicPkgName the package name to attempt a dynamic resolution for
	 * @param revision the module revision the dynamic resolution request is for
	 * @return the new resolution wire establishing a dynamic package resolution or null if
	 * a dynamic wire could not be established.
	 */
	public ModuleWire resolveDynamic(String dynamicPkgName, ModuleRevision revision) {
		ModuleWire result;
		Map<ModuleRevision, ModuleWiring> deltaWiring;
		Collection<Module> modulesResolved;
		long timestamp;
		try (Permits resolutionPermits = _resolutionLock.acquire(ResolutionLock.MAX_RESOLUTION_PERMITS)) {
			do {
				result = null;
				Map<ModuleRevision, ModuleWiring> wiringClone = null;
				List<DynamicModuleRequirement> dynamicReqs = null;
				Collection<ModuleRevision> unresolved = new ArrayList<>();
				moduleDatabase.readLock();
				try {
					ModuleWiring wiring = revision.getWiring();
					if (wiring == null) {
						// not resolved!!
						return null;
					}
					if (wiring.isDynamicPackageMiss(dynamicPkgName)) {
						// cached a miss for this package
						return null;
					}
					// need to check that another thread has not done the work already
					result = findExistingDynamicWire(revision.getWiring(), dynamicPkgName);
					if (result != null) {
						return result;
					}
					dynamicReqs = getDynamicRequirements(dynamicPkgName, revision);
					if (dynamicReqs.isEmpty()) {
						// save the miss for the package name
						wiring.addDynamicPackageMiss(dynamicPkgName);
						return null;
					}
					timestamp = moduleDatabase.getRevisionsTimestamp();
					wiringClone = moduleDatabase.getWiringsClone();
					Collection<Module> allModules = moduleDatabase.getModules();
					for (Module module : allModules) {
						ModuleRevision current = module.getCurrentRevision();
						if (current != null && !wiringClone.containsKey(current))
							unresolved.add(current);
					}
				} finally {
					moduleDatabase.readUnlock();
				}

				deltaWiring = null;
				boolean foundCandidates = false;
				for (DynamicModuleRequirement dynamicReq : dynamicReqs) {
					ModuleResolutionReport report = moduleResolver.resolveDynamicDelta(dynamicReq, unresolved, wiringClone, moduleDatabase);
					Map<Resource, List<Wire>> resolutionResult = report.getResolutionResult();
					deltaWiring = resolutionResult == null ? Collections.emptyMap() : moduleResolver.generateDelta(resolutionResult, wiringClone);
					if (deltaWiring.get(revision) != null) {
						break;
					}
					// Did not establish a valid wire.
					// Check to see if any candidates were available.
					// this is used for caching purposes below
					List<Entry> revisionEntries = report.getEntries().get(revision);
					if (revisionEntries == null || revisionEntries.isEmpty()) {
						foundCandidates = true;
					} else {
						// must make sure there is no MISSING_CAPABILITY type entry
						boolean isMissingCapability = false;
						for (Entry entry : revisionEntries) {
							isMissingCapability |= Entry.Type.MISSING_CAPABILITY.equals(entry.getType());
						}
						foundCandidates |= !isMissingCapability;
					}
				}
				if (deltaWiring == null || deltaWiring.get(revision) == null) {
					if (!foundCandidates) {
						ModuleWiring wiring = revision.getWiring();
						if (wiring != null) {
							// save the miss for the package name
							wiring.addDynamicPackageMiss(dynamicPkgName);
						}
					}
					return null; // nothing to do
				}

				modulesResolved = new ArrayList<>();
				for (ModuleRevision deltaRevision : deltaWiring.keySet()) {
					if (!wiringClone.containsKey(deltaRevision))
						modulesResolved.add(deltaRevision.getRevisions().getModule());
				}

				// Save the result
				ModuleWiring wiring = deltaWiring.get(revision);
				result = findExistingDynamicWire(wiring, dynamicPkgName);
			} while (!applyDelta(deltaWiring, modulesResolved, Collections.emptyList(), timestamp, false, resolutionPermits));
		} catch (ResolutionLockException e) {
			return null;
		}

		return result;
	}

	private ModuleWire findExistingDynamicWire(ModuleWiring wiring, String dynamicPkgName) {
		if (wiring == null) {
			return null;
		}
		List<ModuleWire> wires = wiring.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
		// No null check; we are holding the database lock here.
		// Work backwards to find the first wire with the dynamic requirement that matches package name
		for (int i = wires.size() - 1; i >= 0; i--) {
			ModuleWire wire = wires.get(i);
			if (dynamicPkgName.equals(wire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
				return wire;
			}
			if (!PackageNamespace.RESOLUTION_DYNAMIC.equals(wire.getRequirement().getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
				return null;
			}
		}
		return null;
	}

	// The resolution algorithm uses optimistic locking approach;
	// This involves taking a snapshot of the state and performing an
	// operation on the snapshot while holding no locks and then
	// obtaining the write lock to apply the results.  If we
	// detect the state has changed since the snapshot taken then
	// the process is started over.  If we allow too many threads
	// to try to do this at the same time it causes thrashing
	// between taking the snapshot and successfully applying the
	// results.  Instead of resorting to single threaded operations
	// we choose to limit the number of concurrent resolves
	final ResolutionLock _resolutionLock = new ResolutionLock();
	final ReentrantLock _bundleStateLock = new ReentrantLock();

	static class ResolutionLockException extends Exception {
		private static final long serialVersionUID = 1L;

		public ResolutionLockException() {
			super();
		}

		public ResolutionLockException(Throwable cause) {
			super(cause);
		}
	}

	/**
	 * A resolution hook allows for a max of 10 threads to do resolution operations
	 * at the same time.  The implementation uses a semaphore to grant the max number
	 * of permits (threads) but a reentrant read lock is also used to detect reentry.
	 * If a thread reenters then no extra permits are required by the thread.
	 * This lock returns a Permits object that implements closeable for use in
	 * try->with.  If permits is closed multiple times then the additional close
	 * operations are a no-op.
	 */
	static class ResolutionLock {
		final static int MAX_RESOLUTION_PERMITS = 10;
		final Semaphore permitPool = new Semaphore(MAX_RESOLUTION_PERMITS);
		final ReentrantReadWriteLock reenterLock = new ReentrantReadWriteLock();

		class Permits implements Closeable {
			private final int aquiredPermits;
			private final AtomicBoolean closed = new AtomicBoolean();

			Permits(int requestedPermits) throws ResolutionLockException {
				if (reenterLock.getReadHoldCount() > 0) {
					// thread is already holding permits; don't request more
					requestedPermits = 0;
				}
				this.aquiredPermits = requestedPermits;
				boolean previousInterruption = Thread.interrupted();
				try {
					if (!permitPool.tryAcquire(requestedPermits, 30, TimeUnit.SECONDS)) {
						throw new ResolutionLockException();
					}
				} catch (InterruptedException e) {
					throw new ResolutionLockException(e);
				} finally {
					if (previousInterruption) {
						Thread.currentThread().interrupt();
					}
				}
				// mark this thread as holding permits
				reenterLock.readLock().lock();
			}

			@Override
			public void close() {
				if (closed.compareAndSet(false, true)) {
					permitPool.release(aquiredPermits);
					reenterLock.readLock().unlock();
				}
			}
		}

		Permits acquire(int requestedPermits) throws ResolutionLockException {
			return new Permits(requestedPermits);
		}
	}

	private boolean applyDelta(Map<ModuleRevision, ModuleWiring> deltaWiring, Collection<Module> modulesResolved, Collection<Module> triggers, long timestamp, boolean restartTriggers, ResolutionLock.Permits resolutionPermits) {
		List<Module> modulesLocked = new ArrayList<>(modulesResolved.size());
		// now attempt to apply the delta
		try {
			// Acquire the necessary RESOLVED state change lock.
			// Note this is done while holding a global lock to avoid multiple threads trying to compete over
			// locking multiple modules; otherwise out of order locks between modules can happen
			// NOTE this MUST be done outside of holding the moduleDatabase lock also to avoid
			// introducing out of order locks between the bundle state change lock and the moduleDatabase
			// lock.
			_bundleStateLock.lock();
			try {
				for (Module module : modulesResolved) {
					try {
						// avoid grabbing the lock if the timestamp has changed
						if (timestamp != moduleDatabase.getRevisionsTimestamp()) {
							return false; // need to try again
						}
						module.lockStateChange(ModuleEvent.RESOLVED);
						modulesLocked.add(module);
					} catch (BundleException e) {
						// before throwing an exception here, see if the timestamp has changed
						if (timestamp != moduleDatabase.getRevisionsTimestamp()) {
							return false; // need to try again
						}
						// TODO throw some appropriate exception
						throw new IllegalStateException(Msg.ModuleContainer_StateLockError, e);
					}
				}
			} finally {
				_bundleStateLock.unlock();
			}

			Map<ModuleWiring, Collection<ModuleRevision>> hostsWithDynamicFrags = new HashMap<>(0);
			moduleDatabase.writeLock();
			try {
				if (timestamp != moduleDatabase.getRevisionsTimestamp())
					return false; // need to try again

				Map<ModuleRevision, ModuleWiring> wiringCopy = moduleDatabase.getWiringsCopy();
				for (Map.Entry<ModuleRevision, ModuleWiring> deltaEntry : deltaWiring.entrySet()) {
					ModuleWiring current = wiringCopy.get(deltaEntry.getKey());
					if (current != null) {
						// need to update the provided capabilities, provided and required wires for currently resolved
						current.setCapabilities(deltaEntry.getValue().getCapabilities());
						current.setProvidedWires(deltaEntry.getValue().getProvidedWires());
						current.setRequirements(deltaEntry.getValue().getRequirements());
						current.setRequiredWires(deltaEntry.getValue().getRequiredWires());
						deltaEntry.setValue(current); // set the real wiring into the delta
					} else {
						ModuleRevision revision = deltaEntry.getValue().getRevision();
						modulesResolved.add(revision.getRevisions().getModule());
						if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
							for (ModuleWire hostWire : deltaEntry.getValue().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE)) {
								// check to see if the host revision has a wiring
								ModuleWiring hostWiring = hostWire.getProvider().getWiring();
								if (hostWiring != null) {
									Collection<ModuleRevision> dynamicFragments = hostsWithDynamicFrags.get(hostWiring);
									if (dynamicFragments == null) {
										dynamicFragments = new ArrayList<>();
										hostsWithDynamicFrags.put(hostWiring, dynamicFragments);
									}
									dynamicFragments.add(hostWire.getRequirer());
								}
							}
						}
					}
				}
				moduleDatabase.mergeWiring(deltaWiring);
				moduleDatabase.sortModules(modulesLocked, Sort.BY_DEPENDENCY, Sort.BY_START_LEVEL);
			} finally {
				moduleDatabase.writeUnlock();
			}
			// set the modules state to resolved
			for (Module module : modulesLocked) {
				module.setState(State.RESOLVED);
			}
			// attach fragments to already resolved hosts that have
			// dynamically attached fragments
			for (Map.Entry<ModuleWiring, Collection<ModuleRevision>> dynamicFragments : hostsWithDynamicFrags.entrySet()) {
				dynamicFragments.getKey().loadFragments(dynamicFragments.getValue());
			}
		} finally {
			for (Module module : modulesLocked) {
				module.unlockStateChange(ModuleEvent.RESOLVED);
			}
		}

		// release resolution permits before firing events
		resolutionPermits.close();

		for (Module module : modulesLocked) {
			adaptor.publishModuleEvent(ModuleEvent.RESOLVED, module, module);
		}

		// If there are any triggers re-start them now if requested
		Set<Module> triggerSet = restartTriggers ? new HashSet<>(triggers) : Collections.emptySet();
		if (restartTriggers) {
			for (Module module : triggers) {
				if (module.getId() != 0 && Module.RESOLVED_SET.contains(module.getState())) {
					start(module, StartOptions.TRANSIENT_RESUME);
				}
			}
		}
		if (autoStartOnResolve) {
			// This is questionable behavior according to the spec but this was the way equinox previously behaved
			// Need to auto-start any persistently started bundles that got resolved
			for (Module module : modulesLocked) {
				// Note that we check inStart here.  There is still a timing issue that is impossible to avoid.
				// Another thread could attempt to start the module but we could check inStart() before that thread
				// increments inStart.  One thread will win the race to grab the module STARTED lock.  That thread
				// will end up actually starting the module and the other thread will block.  If a timeout occurs
				// the blocking thread will get an exception.
				if (!module.inStart() && module.getId() != 0 && !triggerSet.contains(module)) {
					start(module, StartOptions.TRANSIENT_IF_AUTO_START, StartOptions.TRANSIENT_RESUME);
				}
			}
		}
		return true;
	}

	private void start(Module module, StartOptions... options) {
		try {
			secureAction.start(module, options);
		} catch (BundleException e) {
			if (e.getType() == BundleException.STATECHANGE_ERROR) {
				if (Module.ACTIVE_SET.contains(module.getState())) {
					// There is still a timing issue here;
					// but at least try to detect that another thread is starting the module
					return;
				}
			}
			adaptor.publishContainerEvent(ContainerEvent.ERROR, module, e);
		} catch (IllegalStateException e) {
			// been uninstalled
			return;
		}
	}

	private List<DynamicModuleRequirement> getDynamicRequirements(String dynamicPkgName, ModuleRevision revision) {
		// TODO Will likely need to optimize this
		if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
			// only do this for hosts
			return Collections.emptyList();
		}
		ModuleWiring wiring = revision.getWiring();
		if (wiring == null) {
			// not resolved!
			return Collections.emptyList();
		}
		List<DynamicModuleRequirement> result = new ArrayList<>(1);
		// check the dynamic import packages
		DynamicModuleRequirement dynamicRequirement;
		for (ModuleRequirement requirement : wiring.getModuleRequirements(PackageNamespace.PACKAGE_NAMESPACE)) {
			dynamicRequirement = requirement.getDynamicPackageRequirement(revision, dynamicPkgName);
			if (dynamicRequirement != null) {
				result.add(dynamicRequirement);
			}
		}

		if (!result.isEmpty()) {
			// must check that the wiring does not export the package
			for (ModuleCapability capability : wiring.getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
				if (dynamicPkgName.equals(capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
					// the package is exported, must not allow dynamic import
					return Collections.emptyList();
				}
			}
		}
		return result;
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
		List<Module> refreshTriggers;
		Collection<ModuleRevision> toRemoveRevisions;
		Collection<ModuleWiring> toRemoveWirings;
		Map<ModuleWiring, Collection<ModuleWire>> toRemoveWireLists;
		long timestamp;
		moduleDatabase.readLock();
		try {
			checkSystemExtensionRefresh(initial);
			timestamp = moduleDatabase.getRevisionsTimestamp();
			wiringCopy = moduleDatabase.getWiringsCopy();
			refreshTriggers = new ArrayList<>(getRefreshClosure(initial, wiringCopy));
			toRemoveRevisions = new ArrayList<>();
			toRemoveWirings = new ArrayList<>();
			toRemoveWireLists = new HashMap<>();
			for (Iterator<Module> iTriggers = refreshTriggers.iterator(); iTriggers.hasNext();) {
				Module module = iTriggers.next();
				boolean first = true;
				for (ModuleRevision revision : module.getRevisions().getModuleRevisions()) {
					ModuleWiring removedWiring = wiringCopy.remove(revision);
					if (removedWiring != null) {
						toRemoveWirings.add(removedWiring);
						List<ModuleWire> removedWires = removedWiring.getRequiredModuleWires(null);
						for (ModuleWire wire : removedWires) {
							Collection<ModuleWire> providerWires = toRemoveWireLists.get(wire.getProviderWiring());
							if (providerWires == null) {
								providerWires = new ArrayList<>();
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
				if (module.getState().equals(State.UNINSTALLED)) {
					iTriggers.remove();
				}
			}
			moduleDatabase.sortModules(refreshTriggers, Sort.BY_START_LEVEL, Sort.BY_DEPENDENCY);
		} finally {
			moduleDatabase.readUnlock();
		}

		Module systemModule = moduleDatabase.getModule(0);
		if (refreshTriggers.contains(systemModule) && Module.ACTIVE_SET.contains(systemModule.getState())) {
			refreshSystemModule();
			return Collections.emptyList();
		}
		Collection<Module> modulesLocked = new ArrayList<>(refreshTriggers.size());
		Collection<Module> modulesUnresolved = new ArrayList<>();
		try {
			// Acquire the module state change locks.
			// Note this is done while holding a global lock to avoid multiple threads trying to compete over
			// locking multiple modules; otherwise out of order locks between modules can happen
			// NOTE this MUST be done outside of holding the moduleDatabase lock also to avoid
			// introducing out of order locks between the bundle state change lock and the moduleDatabase
			// lock.
			_bundleStateLock.lock();
			try {
				// go in reverse order
				for (ListIterator<Module> iTriggers = refreshTriggers.listIterator(refreshTriggers.size()); iTriggers.hasPrevious();) {
					Module refreshModule = iTriggers.previous();
					refreshModule.lockStateChange(ModuleEvent.UNRESOLVED);
					modulesLocked.add(refreshModule);
				}
			} catch (BundleException e) {
				// TODO throw some appropriate exception
				throw new IllegalStateException(Msg.ModuleContainer_StateLockError, e);
			} finally {
				_bundleStateLock.unlock();
			}
			// Must not hold the module database lock while stopping bundles
			// Stop any active bundles and remove non-active modules from the refreshTriggers
			for (ListIterator<Module> iTriggers = refreshTriggers.listIterator(refreshTriggers.size()); iTriggers.hasPrevious();) {
				Module refreshModule = iTriggers.previous();
				State previousState = refreshModule.getState();
				if (Module.ACTIVE_SET.contains(previousState)) {
					try {
						refreshModule.stop(StopOptions.TRANSIENT);
					} catch (BundleException e) {
						adaptor.publishContainerEvent(ContainerEvent.ERROR, refreshModule, e);
					}
				} else {
					iTriggers.remove();
				}
			}

			// do a sanity check on states of the modules, they must be INSTALLED, RESOLVED or UNINSTALLED
			for (Module module : modulesLocked) {
				if (Module.ACTIVE_SET.contains(module.getState())) {
					throw new IllegalStateException("Module is in the wrong state: " + module + ": " + module.getState()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			// finally apply the unresolve to the database
			moduleDatabase.writeLock();
			try {
				if (timestamp != moduleDatabase.getRevisionsTimestamp())
					return null; // need to try again
				// remove any wires from unresolved wirings that got removed
				for (Map.Entry<ModuleWiring, Collection<ModuleWire>> entry : toRemoveWireLists.entrySet()) {
					NamespaceList.Builder<ModuleWire> provided = entry.getKey().getProvidedWires().createBuilder();
					provided.removeAll(entry.getValue());
					entry.getKey().setProvidedWires(provided.build());
					for (ModuleWire removedWire : entry.getValue()) {
						// invalidate the wire
						removedWire.invalidate();
					}

				}
				// remove any revisions that got removed as part of the refresh
				for (ModuleRevision removed : toRemoveRevisions) {
					removed.getRevisions().removeRevision(removed);
					moduleDatabase.removeCapabilities(removed);
				}
				// invalidate any removed wiring objects
				for (ModuleWiring moduleWiring : toRemoveWirings) {
					moduleWiring.invalidate();
				}
				moduleDatabase.setWiring(wiringCopy);
				// check for any removal pendings
				moduleDatabase.cleanupRemovalPending();
			} finally {
				moduleDatabase.writeUnlock();
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
				module.unlockStateChange(ModuleEvent.UNRESOLVED);
			}
		}
		// publish unresolved events after giving up all locks
		for (Module module : modulesUnresolved) {
			adaptor.publishModuleEvent(ModuleEvent.UNRESOLVED, module, module);
		}
		return refreshTriggers;
	}

	private void checkSystemExtensionRefresh(Collection<Module> initial) {
		if (initial == null) {
			return;
		}
		Long zero = Long.valueOf(0);
		for (Iterator<Module> iModules = initial.iterator(); iModules.hasNext();) {
			Module m = iModules.next();
			if (m.getId().equals(zero)) {
				// never allow system bundle to be unresolved directly if the system module is active
				if (Module.ACTIVE_SET.contains(m.getState())) {
					iModules.remove();
				}
			} else {
				if (Module.RESOLVED_SET.contains(m.getState())) {
					// check if current revision is an extension of the system module
					ModuleRevision current = m.getCurrentRevision();
					if ((current.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
						ModuleWiring wiring = current.getWiring();
						if (wiring != null) {
							List<ModuleWire> hostWires = wiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
							for (ModuleWire hostWire : hostWires) {
								if (hostWire.getProvider().getRevisions().getModule().getId().equals(zero)) {
									// The current revision is the extension to allow it to refresh
									// this would just shutdown the framework for no reason
									iModules.remove();
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Refreshes the specified collection of modules.
	 * @param initial the modules to refresh or {@code null} to refresh the
	 *     removal pending.
	 * @return a resolution report for the resolve operation that may have
	 * occurred during the refresh operation.
	 * @see FrameworkWiring#refreshBundles(Collection, FrameworkListener...)
	 */
	public ResolutionReport refresh(Collection<Module> initial) {
		initial = initial == null ? null : new ArrayList<>(initial);
		Collection<Module> refreshTriggers = unresolve(initial);
		if (!isRefreshingSystemModule()) {
			return resolve(refreshTriggers, false, true);
		}
		return new ModuleResolutionReport(null, null, null);
	}

	/**
	 * Returns the dependency closure of for the specified modules.
	 * @param initial The initial modules for which to generate the dependency closure
	 * @return A collection containing a snapshot of the dependency closure of the specified
	 *    modules, or an empty collection if there were no specified modules.
	 */
	public Collection<Module> getDependencyClosure(Collection<Module> initial) {
		moduleDatabase.readLock();
		try {
			return getRefreshClosure(initial, moduleDatabase.getWiringsCopy());
		} finally {
			moduleDatabase.readUnlock();
		}
	}

	/**
	 * Returns the revisions that have {@link ModuleWiring#isCurrent() non-current}, {@link ModuleWiring#isInUse() in use} module wirings.
	 * @return A collection containing a snapshot of the revisions which have non-current, in use ModuleWirings,
	 * or an empty collection if there are no such revisions.
	 */
	public Collection<ModuleRevision> getRemovalPending() {
		return moduleDatabase.getRemovalPending();
	}

	/**
	 * Return the active start level value of this container.
	 *
	 * If the container is in the process of changing the start level this
	 * method must return the active start level if this differs from the
	 * requested start level.
	 *
	 * @return The active start level value of the Framework.
	 */
	public int getStartLevel() {
		return frameworkStartLevel.getStartLevel();
	}

	void setStartLevel(Module module, int startlevel) {
		frameworkStartLevel.setStartLevel(module, startlevel);
	}

	long getModuleLockTimeout() {
		return this.moduleLockTimeout;
	}

	void open() {
		loadModules();
		frameworkStartLevel.open();
		frameworkWiring.open();
		refreshingSystemModule.set(null);
	}

	void close() {
		frameworkStartLevel.close();
		frameworkWiring.close();
		unloadModules();
	}

	private void loadModules() {
		List<Module> modules = null;
		moduleDatabase.readLock();
		try {
			modules = getModules();
			for (Module module : modules) {
				try {
					module.lockStateChange(ModuleEvent.RESOLVED);
					ModuleWiring wiring = moduleDatabase.getWiring(module.getCurrentRevision());
					if (wiring != null) {
						module.setState(State.RESOLVED);
					} else {
						module.setState(State.INSTALLED);
					}
				} catch (BundleException e) {
					throw new IllegalStateException("Unable to lock module state.", e); //$NON-NLS-1$
				}
			}
			Map<ModuleRevision, ModuleWiring> wirings = moduleDatabase.getWiringsCopy();
			for (ModuleWiring wiring : wirings.values()) {
				wiring.validate();
			}
		} finally {
			if (modules != null) {
				for (Module module : modules) {
					try {
						module.unlockStateChange(ModuleEvent.RESOLVED);
					} catch (IllegalMonitorStateException e) {
						// ignore
					}
				}
			}
			moduleDatabase.readUnlock();
		}
	}

	private void unloadModules() {
		List<Module> modules = null;
		moduleDatabase.readLock();
		try {
			modules = getModules();
			for (Module module : modules) {
				if (module.getId() != 0) {
					try {
						module.lockStateChange(ModuleEvent.UNINSTALLED);
					} catch (BundleException e) {
						throw new IllegalStateException("Unable to lock module state.", e); //$NON-NLS-1$
					}
					module.setState(State.UNINSTALLED);
				}
			}
			Map<ModuleRevision, ModuleWiring> wirings = moduleDatabase.getWiringsCopy();
			for (ModuleWiring wiring : wirings.values()) {
				wiring.unload();
			}
		} finally {
			if (modules != null) {
				for (Module module : modules) {
					if (module.getId() != 0) {
						try {
							module.unlockStateChange(ModuleEvent.UNINSTALLED);
						} catch (IllegalMonitorStateException e) {
							// ignore
						}
					}
				}
			}
			moduleDatabase.readUnlock();
		}
	}

	/**
	 * Sets all the module states uninstalled except for the system module.
	 */
	public void setInitialModuleStates() throws BundleException {
		moduleDatabase.readLock();
		try {
			List<Module> modules = getModules();
			for (Module module : modules) {
				if (module.getId() == 0) {
					module.lockStateChange(ModuleEvent.UNINSTALLED);
					try {
						module.setState(State.INSTALLED);
					} finally {
						module.unlockStateChange(ModuleEvent.UNINSTALLED);
					}
				} else {
					module.lockStateChange(ModuleEvent.UNINSTALLED);
					try {
						module.setState(State.UNINSTALLED);
					} finally {
						module.unlockStateChange(ModuleEvent.UNINSTALLED);
					}
				}
			}
			Map<ModuleRevision, ModuleWiring> wirings = moduleDatabase.getWiringsCopy();
			for (ModuleWiring wiring : wirings.values()) {
				wiring.unload();
			}
		} finally {
			moduleDatabase.readUnlock();
		}
	}

	Set<Module> getRefreshClosure(Collection<Module> initial, Map<ModuleRevision, ModuleWiring> wiringCopy) {
		Set<Module> refreshClosure = new HashSet<>();
		if (initial == null) {
			initial = new HashSet<>();
			Collection<ModuleRevision> removalPending = moduleDatabase.getRemovalPending();
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
			// No null checks; we are holding the read lock here.
			// Add all requirers of the provided wires
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
		Set<ModuleRevision> dependencyClosure = new HashSet<>();
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
		// No null checks; we are holding the read lock here.
		// Add all requirers of the provided wires
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

	Bundle getSystemBundle() {
		Module systemModule = moduleDatabase.getModule(0);
		return systemModule == null ? null : systemModule.getBundle();
	}

	void checkAdminPermission(Bundle bundle, String action) {
		if (bundle == null)
			return;
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new AdminPermission(bundle, action));
	}

	void refreshSystemModule() {
		final SystemModule systemModule = (SystemModule) moduleDatabase.getModule(0);
		if (systemModule == refreshingSystemModule.getAndSet(systemModule)) {
			return;
		}
		getAdaptor().refreshedSystemModule();
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					systemModule.lockStateChange(ModuleEvent.UNRESOLVED);
					try {
						systemModule.stop();
					} finally {
						systemModule.unlockStateChange(ModuleEvent.UNRESOLVED);
					}
				} catch (BundleException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	boolean isRefreshingSystemModule() {
		return refreshingSystemModule.get() != null;
	}

	static Requirement getIdentityRequirement(String name, Version version) {
		version = version == null ? Version.emptyVersion : version;
		String filter = "(&(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + name + ")(" + IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE + "=" + version.toString() + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$
		Map<String, String> directives = Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
		return new ModuleRequirement(IdentityNamespace.IDENTITY_NAMESPACE, directives, Collections.emptyMap(), null);
	}

	class ContainerWiring implements FrameworkWiring, EventDispatcher<ContainerWiring, FrameworkListener[], Collection<Module>> {
		private final Object monitor = new Object();
		private EventManager refreshThread = null;

		@Override
		public Bundle getBundle() {
			return getSystemBundle();
		}

		@Override
		public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners) {
			checkAdminPermission(getBundle(), AdminPermission.RESOLVE);
			Collection<Module> modules = getModules(bundles);

			// queue to refresh in the background
			// notice that we only do one refresh operation at a time
			CopyOnWriteIdentityMap<ContainerWiring, FrameworkListener[]> dispatchListeners = new CopyOnWriteIdentityMap<>();
			dispatchListeners.put(this, listeners);
			ListenerQueue<ContainerWiring, FrameworkListener[], Collection<Module>> queue = new ListenerQueue<>(getManager());
			queue.queueListeners(dispatchListeners.entrySet(), this);

			// dispatch the refresh job
			queue.dispatchEventAsynchronous(0, modules);
		}

		@Override
		public boolean resolveBundles(Collection<Bundle> bundles) {
			checkAdminPermission(getBundle(), AdminPermission.RESOLVE);
			Collection<Module> modules = getModules(bundles);
			resolve(modules, false);

			if (modules == null) {
				modules = ModuleContainer.this.getModules();
			}
			for (Module module : modules) {
				if (getWiring(module.getCurrentRevision()) == null)
					return false;
			}
			return true;
		}

		@Override
		public Collection<Bundle> getRemovalPendingBundles() {
			moduleDatabase.readLock();
			try {
				Collection<Bundle> removalPendingBundles = new HashSet<>();
				Collection<ModuleRevision> removalPending = moduleDatabase.getRemovalPending();
				for (ModuleRevision moduleRevision : removalPending) {
					removalPendingBundles.add(moduleRevision.getBundle());
				}
				return removalPendingBundles;
			} finally {
				moduleDatabase.readUnlock();
			}
		}

		@Override
		public Collection<Bundle> getDependencyClosure(Collection<Bundle> bundles) {
			Collection<Module> modules = getModules(bundles);
			moduleDatabase.readLock();
			try {
				Collection<Module> closure = getRefreshClosure(modules, moduleDatabase.getWiringsCopy());
				Collection<Bundle> result = new ArrayList<>(closure.size());
				for (Module module : closure) {
					result.add(module.getBundle());
				}
				return result;
			} finally {
				moduleDatabase.readUnlock();
			}
		}

		@Override
		public Collection<BundleCapability> findProviders(Requirement requirement) {
			return InternalUtils.asList(moduleDatabase.findCapabilities(requirement));
		}

		private Collection<Module> getModules(final Collection<Bundle> bundles) {
			if (bundles == null)
				return null;
			return AccessController.doPrivileged(new PrivilegedAction<Collection<Module>>() {
				@Override
				public Collection<Module> run() {
					Collection<Module> result = new ArrayList<>(bundles.size());
					for (Bundle bundle : bundles) {
						Module module = bundle.adapt(Module.class);
						if (module == null)
							throw new IllegalStateException("Could not adapt a bundle to a module. " + bundle); //$NON-NLS-1$
						result.add(module);
					}
					return result;
				}
			});
		}

		@Override
		public void dispatchEvent(ContainerWiring eventListener, FrameworkListener[] frameworkListeners, int eventAction, Collection<Module> eventObject) {
			try {
				refresh(eventObject);
			} finally {
				adaptor.publishContainerEvent(ContainerEvent.REFRESH, moduleDatabase.getModule(0), null, frameworkListeners);
			}
		}

		private EventManager getManager() {
			synchronized (monitor) {
				if (refreshThread == null) {
					refreshThread = new EventManager("Refresh Thread: " + adaptor.toString()); //$NON-NLS-1$
				}
				return refreshThread;
			}
		}

		// because of bug 378491 we have to synchronize access to the manager
		// so we can close and re-open ourselves
		void close() {
			synchronized (monitor) {
				// force a manager to be created if it did not exist
				EventManager manager = getManager();
				// this prevents any operations until open is called
				manager.close();
			}
		}

		void open() {
			synchronized (monitor) {
				if (refreshThread != null) {
					// Make sure it is closed just incase
					refreshThread.close();
					// a new one will be constructed on demand
					refreshThread = null;
				}
			}
		}
	}

	@Override
	public void optionsChanged(DebugOptions options) {
		moduleResolver.setDebugOptions();
		frameworkStartLevel.setDebugOptions();
		if (options != null) {
			this.DEBUG_MONITOR_LAZY = options.getBooleanOption(Debug.OPTION_MONITOR_LAZY, false);
			this.DEBUG_BUNDLE_START_TIME = options.getBooleanOption(Debug.OPTION_DEBUG_BUNDLE_START_TIME, false);
		}
	}

	class ContainerStartLevel implements FrameworkStartLevel, EventDispatcher<Module, FrameworkListener[], Integer> {
		static final int USE_BEGINNING_START_LEVEL = Integer.MIN_VALUE;
		private static final int FRAMEWORK_STARTLEVEL = 1;
		private static final int MODULE_STARTLEVEL = 2;
		private final AtomicInteger activeStartLevel = new AtomicInteger(0);
		private final Object eventManagerLock = new Object();
		private EventManager startLevelThread = null;
		private final Object frameworkStartLevelLock = new Object();
		boolean debugStartLevel = false;
		{
			setDebugOptions();
		}

		void setDebugOptions() {
			DebugOptions options = getAdaptor().getDebugOptions();
			debugStartLevel = options == null ? false : options.getBooleanOption(Debug.OPTION_DEBUG_STARTLEVEL, false);
		}

		@Override
		public Bundle getBundle() {
			return getSystemBundle();
		}

		@Override
		public int getStartLevel() {
			return activeStartLevel.get();
		}

		void setStartLevel(Module module, int startlevel) {
			checkAdminPermission(module.getBundle(), AdminPermission.EXECUTE);
			if (module.getId() == 0) {
				throw new IllegalArgumentException(Msg.ModuleContainer_SystemStartLevelError);
			}
			if (startlevel < 1) {
				throw new IllegalArgumentException(Msg.ModuleContainer_NegativeStartLevelError + startlevel);
			}
			int currentLevel = module.getStartLevel();
			if (currentLevel == startlevel) {
				return; // do nothing
			}
			moduleDatabase.setStartLevel(module, startlevel);
			// only queue the start level if
			// 1) the current level is less than the new startlevel, may need to stop or
			// 2) the module is marked for persistent activation, may need to start
			if (currentLevel < startlevel || module.isPersistentlyStarted()) {
				// queue start level operation in the background
				// notice that we only do one start level operation at a time
				CopyOnWriteIdentityMap<Module, FrameworkListener[]> dispatchListeners = new CopyOnWriteIdentityMap<>();
				dispatchListeners.put(module, new FrameworkListener[0]);
				ListenerQueue<Module, FrameworkListener[], Integer> queue = new ListenerQueue<>(getManager());
				queue.queueListeners(dispatchListeners.entrySet(), this);

				// dispatch the start level job
				queue.dispatchEventAsynchronous(MODULE_STARTLEVEL, startlevel);
			}
		}

		@Override
		public void setStartLevel(int startlevel, FrameworkListener... listeners) {
			checkAdminPermission(getBundle(), AdminPermission.STARTLEVEL);
			if (startlevel < 1) {
				throw new IllegalArgumentException(Msg.ModuleContainer_NegativeStartLevelError + startlevel);
			}

			if (activeStartLevel.get() == 0) {
				throw new IllegalStateException(Msg.ModuleContainer_SystemNotActiveError);
			}
			if (debugStartLevel) {
				Debug.println("StartLevel: setStartLevel: " + startlevel); //$NON-NLS-1$
			}
			// queue start level operation in the background
			// notice that we only do one start level operation at a time
			CopyOnWriteIdentityMap<Module, FrameworkListener[]> dispatchListeners = new CopyOnWriteIdentityMap<>();
			dispatchListeners.put(moduleDatabase.getModule(0), listeners);
			ListenerQueue<Module, FrameworkListener[], Integer> queue = new ListenerQueue<>(getManager());
			queue.queueListeners(dispatchListeners.entrySet(), this);

			// dispatch the start level job
			queue.dispatchEventAsynchronous(FRAMEWORK_STARTLEVEL, startlevel);
		}

		@Override
		public int getInitialBundleStartLevel() {
			return moduleDatabase.getInitialModuleStartLevel();
		}

		@Override
		public void setInitialBundleStartLevel(int startlevel) {
			checkAdminPermission(getBundle(), AdminPermission.STARTLEVEL);
			if (startlevel < 1) {
				throw new IllegalArgumentException(Msg.ModuleContainer_NegativeStartLevelError + startlevel);
			}
			moduleDatabase.setInitialModuleStartLevel(startlevel);
		}

		@Override
		public void dispatchEvent(Module module, FrameworkListener[] listeners, int eventAction, Integer startlevel) {
			switch (eventAction) {
				case FRAMEWORK_STARTLEVEL :
					doContainerStartLevel(module, startlevel, listeners);
					break;
				case MODULE_STARTLEVEL :
					if (debugStartLevel) {
						Debug.println("StartLevel: changing bundle startlevel; " + toString(module) + "; newSL=" + startlevel + "; activeSL=" + getStartLevel()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					try {
						if (getStartLevel() < startlevel) {
							if (Module.ACTIVE_SET.contains(module.getState())) {
								if (debugStartLevel) {
									Debug.println("StartLevel: stopping bundle; " + toString(module) + "; with startLevel=" + startlevel); //$NON-NLS-1$ //$NON-NLS-2$
								}
								// Note that we don't need to hold the state change lock
								// here when checking the active status because no other
								// thread will successfully be able to start this bundle
								// since the start-level is no longer met.
								module.stop(StopOptions.TRANSIENT);
							}
						} else {
							if (debugStartLevel) {
								Debug.println("StartLevel: resuming bundle; " + toString(module) + "; with startLevel=" + startlevel); //$NON-NLS-1$ //$NON-NLS-2$
							}
							module.start(StartOptions.TRANSIENT_IF_AUTO_START, StartOptions.TRANSIENT_RESUME);
						}
					} catch (BundleException e) {
						adaptor.publishContainerEvent(ContainerEvent.ERROR, module, e);
					}
					break;
				default :
					break;
			}
		}

		void doContainerStartLevel(Module module, int newStartLevel, FrameworkListener... listeners) {
			synchronized (frameworkStartLevelLock) {
				if (newStartLevel == USE_BEGINNING_START_LEVEL) {
					String beginningSL = adaptor.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
					newStartLevel = beginningSL == null ? 1 : Integer.parseInt(beginningSL);
				}
				try {
					int currentSL = getStartLevel();
					if (currentSL == 0) {
						// check for an active framework; this is only valid when the system bundle is starting
						Module systemModule = moduleDatabase.getModule(0);
						if (systemModule != null && !State.STARTING.equals(systemModule.getState())) {
							return;
						}
					}
					// Note that we must get a new list of modules each time;
					// this is because additional modules could have been installed from the previous start-level
					// but only do this if the module database has changed!!
					List<Module> sorted = null;
					long currentTimestamp = Long.MIN_VALUE;
					if (newStartLevel > currentSL) {
						List<Module> lazyStart = null;
						List<Module> lazyStartParallel = null;
						List<Module> eagerStart = null;
						List<Module> eagerStartParallel = null;
						for (int i = currentSL; i < newStartLevel; i++) {
							int toStartLevel = i + 1;
							activeStartLevel.set(toStartLevel);
							if (debugStartLevel) {
								Debug.println("StartLevel: incremented active start level to; " + toStartLevel); //$NON-NLS-1$
							}
							if (sorted == null || currentTimestamp != moduleDatabase.getTimestamp()) {
								moduleDatabase.readLock();
								try {
									sorted = moduleDatabase.getSortedModules(Sort.BY_START_LEVEL);
									lazyStart = new ArrayList<>(sorted.size());
									lazyStartParallel = new ArrayList<>(sorted.size());
									eagerStart = new ArrayList<>(sorted.size());
									eagerStartParallel = new ArrayList<>(sorted.size());
									separateModulesByActivationPolicy(sorted, lazyStart, lazyStartParallel, eagerStart, eagerStartParallel);
									currentTimestamp = moduleDatabase.getTimestamp();
								} finally {
									moduleDatabase.readUnlock();
								}
							}
							incStartLevel(toStartLevel, lazyStart, lazyStartParallel, eagerStart, eagerStartParallel);
						}
					} else {
						for (int i = currentSL; i > newStartLevel; i--) {
							int toStartLevel = i - 1;
							activeStartLevel.set(toStartLevel);
							if (debugStartLevel) {
								Debug.println("StartLevel: decremented active start level to " + toStartLevel); //$NON-NLS-1$
							}
							if (sorted == null || currentTimestamp != moduleDatabase.getTimestamp()) {
								moduleDatabase.readLock();
								try {
									sorted = moduleDatabase.getSortedModules(Sort.BY_START_LEVEL, Sort.BY_DEPENDENCY);
									currentTimestamp = moduleDatabase.getTimestamp();
								} finally {
									moduleDatabase.readUnlock();
								}
							}
							decStartLevel(toStartLevel, sorted);
						}
					}
					if (currentSL > 0 && newStartLevel > 0) {
						// Only fire the start level event if we are not in the middle
						// of launching or shutting down the framework
						adaptor.publishContainerEvent(ContainerEvent.START_LEVEL, module, null, listeners);
					}
				} catch (Error | RuntimeException e) {
					adaptor.publishContainerEvent(ContainerEvent.ERROR, module, e, listeners);
					throw e;
				}
			}
		}

		private void incStartLevel(int toStartLevel, List<Module> lazyStart, List<Module> lazyStartParallel, List<Module> eagerStart, List<Module> eagerStartParallel) {
			// start lazy activated first
			// start parallel bundles first
			incStartLevel(toStartLevel, lazyStartParallel, true);
			incStartLevel(toStartLevel, lazyStart, false);
			incStartLevel(toStartLevel, eagerStartParallel, true);
			incStartLevel(toStartLevel, eagerStart, false);
		}

		private void separateModulesByActivationPolicy(List<Module> sortedModules, List<Module> lazyStart, List<Module> lazyStartParallel, List<Module> eagerStart, List<Module> eagerStartParallel) {
			for (Module module : sortedModules) {
				if (!restrictParallelStart || module.isParallelActivated()) {
					if (module.isLazyActivate()) {
						lazyStartParallel.add(module);
					} else {
						eagerStartParallel.add(module);
					}
				} else {
					if (module.isLazyActivate()) {
						lazyStart.add(module);
					} else {
						eagerStart.add(module);
					}
				}
			}
		}

		private void incStartLevel(final int toStartLevel, List<Module> candidatesToStart, boolean inParallel) {
			if (candidatesToStart.isEmpty()) {
				return;
			}
			final List<Module> toStart = new ArrayList<>();
			for (final Module module : candidatesToStart) {
				if (isRefreshingSystemModule()) {
					return;
				}
				try {
					int moduleStartLevel = module.getStartLevel();
					if (moduleStartLevel < toStartLevel) {
						// skip modules who should have already been started
						continue;
					} else if (moduleStartLevel == toStartLevel) {
						toStart.add(module);
					} else {
						break;
					}
				} catch (IllegalStateException e) {
					// been uninstalled
					continue;
				}
			}
			if (toStart.isEmpty()) {
				return;
			}
			final Executor executor = inParallel ? adaptor.getStartLevelExecutor() : new Executor() {
				@Override
				public void execute(Runnable command) {
					command.run();
				}
			};
			final CountDownLatch done = new CountDownLatch(toStart.size());
			for (final Module module : toStart) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							if (debugStartLevel) {
								Debug.println("StartLevel: resuming bundle; " + ContainerStartLevel.this.toString(module) + "; with startLevel=" + toStartLevel); //$NON-NLS-1$ //$NON-NLS-2$
							}
							module.start(StartOptions.TRANSIENT_IF_AUTO_START, StartOptions.TRANSIENT_RESUME);
						} catch (BundleException e) {
							adaptor.publishContainerEvent(ContainerEvent.ERROR, module, e);
						} catch (IllegalStateException e) {
							// been uninstalled
						} finally {
							done.countDown();
						}
					}
				});

			}
			try {
				done.await();
			} catch (InterruptedException e) {
				adaptor.publishContainerEvent(ContainerEvent.ERROR, moduleDatabase.getModule(0), e);
			}
		}

		private void decStartLevel(int toStartLevel, List<Module> sortedModules) {
			ListIterator<Module> iModules = sortedModules.listIterator(sortedModules.size());
			while (iModules.hasPrevious()) {
				Module module = iModules.previous();
				try {
					int moduleStartLevel = module.getStartLevel();
					if (moduleStartLevel > toStartLevel + 1) {
						// skip modules who should have already been stopped
						continue;
					} else if (moduleStartLevel <= toStartLevel) {
						// stopped all modules we are going to for this start level
						break;
					}
					try {
						if (Module.ACTIVE_SET.contains(module.getState())) {
							if (debugStartLevel) {
								Debug.println("StartLevel: stopping bundle; " + toString(module) + "; with startLevel=" + moduleStartLevel); //$NON-NLS-1$ //$NON-NLS-2$
							}
							// Note that we don't need to hold the state change lock
							// here when checking the active status because no other
							// thread will successfully be able to start this bundle
							// since the start-level is no longer met.
							module.stop(StopOptions.TRANSIENT);
						}
					} catch (BundleException e) {
						adaptor.publishContainerEvent(ContainerEvent.ERROR, module, e);
					}
				} catch (IllegalStateException e) {
					// been uninstalled
					continue;
				}
			}
		}

		private EventManager getManager() {
			synchronized (eventManagerLock) {
				if (startLevelThread == null) {
					startLevelThread = new EventManager("Start Level: " + adaptor.toString()); //$NON-NLS-1$
				}
				return startLevelThread;
			}
		}

		// because of bug 378491 we have to synchronize access to the manager
		// so we can close and re-open ourselves
		void close() {
			synchronized (eventManagerLock) {
				// force a manager to be created if it did not exist
				EventManager manager = getManager();
				// this prevents any operations until open is called
				manager.close();
			}
		}

		void open() {
			synchronized (eventManagerLock) {
				if (startLevelThread != null) {
					// Make sure it is closed just incase
					startLevelThread.close();
					// a new one will be constructed on demand
					startLevelThread = null;
				}
			}
		}

		String toString(Module m) {
			Bundle b = m.getBundle();
			return b != null ? b.toString() : m.toString();
		}
	}
}