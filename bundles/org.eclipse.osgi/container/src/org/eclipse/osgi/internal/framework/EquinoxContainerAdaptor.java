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
package org.eclipse.osgi.internal.framework;

import java.util.*;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.Module.Settings;
import org.eclipse.osgi.internal.loader.*;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.Storage;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.resolver.ResolutionException;

public class EquinoxContainerAdaptor extends ModuleContainerAdaptor {
	private final EquinoxContainer container;
	private final Storage storage;
	private final OSGiFrameworkHooks hooks;
	private final Map<Long, Generation> initial;
	// The ClassLoader parent to use when creating ModuleClassLoaders.
	private final ClassLoader moduleClassLoaderParent;

	public EquinoxContainerAdaptor(EquinoxContainer container, Storage storage, Map<Long, Generation> initial) {
		this.container = container;
		this.storage = storage;
		this.hooks = new OSGiFrameworkHooks(container);
		this.initial = initial;
		this.moduleClassLoaderParent = getModuleClassLoaderParent(container.getConfiguration());
	}

	private static ClassLoader getModuleClassLoaderParent(EquinoxConfiguration configuration) {
		// check property for specified parent
		// check the osgi defined property first
		String type = configuration.getConfiguration(Constants.FRAMEWORK_BUNDLE_PARENT);
		if (type == null) {
			type = configuration.getConfiguration(EquinoxConfiguration.PROP_PARENT_CLASSLOADER, Constants.FRAMEWORK_BUNDLE_PARENT_BOOT);
		}

		if (Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK.equalsIgnoreCase(type) || EquinoxConfiguration.PARENT_CLASSLOADER_FWK.equalsIgnoreCase(type))
			return EquinoxContainer.class.getClassLoader();
		if (Constants.FRAMEWORK_BUNDLE_PARENT_APP.equalsIgnoreCase(type))
			return ClassLoader.getSystemClassLoader();
		if (Constants.FRAMEWORK_BUNDLE_PARENT_EXT.equalsIgnoreCase(type)) {
			ClassLoader appCL = ClassLoader.getSystemClassLoader();
			if (appCL != null)
				return appCL.getParent();
		}
		return new ClassLoader(Object.class.getClassLoader()) {/* boot class loader*/};

	}

	@Override
	public ModuleCollisionHook getModuleCollisionHook() {
		return hooks.getModuleCollisionHook();
	}

	@Override
	public ResolverHookFactory getResolverHookFactory() {
		return hooks.getResolverHookFactory();
	}

	@Override
	public void publishContainerEvent(ContainerEvent type, Module module, Throwable error, FrameworkListener... listeners) {
		EquinoxEventPublisher publisher = container.getEventPublisher();
		if (publisher != null) {
			publisher.publishFrameworkEvent(getType(type), module.getBundle(), error, listeners);
		}
	}

	@Override
	public void publishModuleEvent(ModuleEvent type, Module module, Module origin) {
		EquinoxEventPublisher publisher = container.getEventPublisher();
		if (publisher != null) {
			publisher.publishBundleEvent(getType(type), module.getBundle(), origin.getBundle());
		}
		checkFrameworkExtensions(type, module);
	}

	private void checkFrameworkExtensions(ModuleEvent type, Module module) {
		switch (type) {
			case INSTALLED : {
				ModuleRevision current = module.getCurrentRevision();
				if ((BundleRevision.TYPE_FRAGMENT & current.getTypes()) != 0) {
					Module systemModule = storage.getModuleContainer().getModule(0);
					List<ModuleCapability> candidates = systemModule == null ? Collections.<ModuleCapability> emptyList() : systemModule.getCurrentRevision().getModuleCapabilities(HostNamespace.HOST_NAMESPACE);
					List<ModuleRequirement> hostReqs = current.getModuleRequirements(HostNamespace.HOST_NAMESPACE);
					if (!hostReqs.isEmpty() && !candidates.isEmpty()) {
						if (hostReqs.get(0).matches(candidates.get(0))) {
							try {
								storage.getModuleContainer().resolve(Arrays.asList(module), true);
							} catch (ResolutionException e) {
								publishContainerEvent(ContainerEvent.ERROR, module, e);
							}
						}
					}
				}
				break;
			}
			case RESOLVED : {
				ModuleRevision current = module.getCurrentRevision();
				if ((BundleRevision.TYPE_FRAGMENT & current.getTypes()) != 0) {
					ModuleWiring wiring = current.getWiring();
					if (wiring != null) {
						List<ModuleWire> hosts = wiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
						if (!hosts.isEmpty() && hosts.get(0).getProvider().getRevisions().getModule().getId().longValue() == 0) {
							try {
								storage.getExtensionInstaller().addExtensionContent(current);
							} catch (BundleException e) {
								publishContainerEvent(ContainerEvent.ERROR, module, e);
							}
						}
					}
				}
				break;
			}
			default :
				break;
		}
	}

	@Override
	public Module createModule(String location, long id, EnumSet<Settings> settings, int startlevel) {
		EquinoxBundle bundle = new EquinoxBundle(id, location, storage.getModuleContainer(), settings, startlevel, container);
		return bundle.getModule();
	}

	@Override
	public SystemModule createSystemModule() {
		return (SystemModule) new EquinoxBundle.SystemBundle(storage.getModuleContainer(), container).getModule();
	}

	@Override
	public String getProperty(String key) {
		return storage.getConfiguration().getProperty(key);
	}

	@Override
	public ModuleLoader createModuleLoader(ModuleWiring wiring) {
		if (wiring.getBundle().getBundleId() == 0) {
			return new SystemBundleLoader(wiring, container);
		}
		if ((wiring.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
			return new FragmentLoader();
		}
		return new BundleLoader(wiring, container, moduleClassLoaderParent);
	}

	@Override
	public Generation getRevisionInfo(String location, long id) {
		return initial.remove(id);
	}

	@Override
	public void associateRevision(ModuleRevision revision, Object revisionInfo) {
		((Generation) revisionInfo).setRevision(revision);
	}

	@Override
	public void invalidateWiring(ModuleWiring moduleWiring, ModuleLoader current) {
		if (current instanceof BundleLoader) {
			BundleLoader bundleLoader = (BundleLoader) current;
			bundleLoader.close();
		}
		Generation generation = (Generation) moduleWiring.getRevision().getRevisionInfo();
		generation.clearManifestCache();
	}

	static int getType(ContainerEvent type) {
		switch (type) {
			case ERROR :
				return FrameworkEvent.ERROR;
			case INFO :
				return FrameworkEvent.INFO;
			case WARNING :
				return FrameworkEvent.WARNING;
			case REFRESH :
				return FrameworkEvent.PACKAGES_REFRESHED;
			case START_LEVEL :
				return FrameworkEvent.STARTLEVEL_CHANGED;
			case STARTED :
				return FrameworkEvent.STARTED;
			case STOPPED :
				return FrameworkEvent.STOPPED;
			case STOPPED_REFRESH :
			case STOPPED_UPDATE :
				return FrameworkEvent.STOPPED_UPDATE;
			case STOPPED_TIMEOUT :
				return FrameworkEvent.WAIT_TIMEDOUT;
			default :
				// default to error
				return FrameworkEvent.ERROR;
		}
	}

	private int getType(ModuleEvent type) {
		switch (type) {
			case INSTALLED :
				return BundleEvent.INSTALLED;
			case LAZY_ACTIVATION :
				return BundleEvent.LAZY_ACTIVATION;
			case RESOLVED :
				return BundleEvent.RESOLVED;
			case STARTED :
				return BundleEvent.STARTED;
			case STARTING :
				return BundleEvent.STARTING;
			case STOPPING :
				return BundleEvent.STOPPING;
			case STOPPED :
				return BundleEvent.STOPPED;
			case UNINSTALLED :
				return BundleEvent.UNINSTALLED;
			case UNRESOLVED :
				return BundleEvent.UNRESOLVED;
			case UPDATED :
				return BundleEvent.UPDATED;
			default :
				// TODO log error?
				return 0;
		}
	}
}