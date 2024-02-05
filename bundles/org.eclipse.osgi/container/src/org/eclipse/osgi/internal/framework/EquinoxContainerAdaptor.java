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
package org.eclipse.osgi.internal.framework;

import java.security.ProtectionDomain;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.Module.Settings;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.ModuleCollisionHook;
import org.eclipse.osgi.container.ModuleContainerAdaptor;
import org.eclipse.osgi.container.ModuleLoader;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.container.SystemModule;
import org.eclipse.osgi.internal.container.AtomicLazyInitializer;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.FragmentLoader;
import org.eclipse.osgi.internal.loader.SystemBundleLoader;
import org.eclipse.osgi.internal.permadmin.BundlePermissions;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.Storage;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;

public class EquinoxContainerAdaptor extends ModuleContainerAdaptor {
	private final EquinoxContainer container;
	private final Storage storage;
	private final OSGiFrameworkHooks hooks;
	private final Map<Long, Generation> initial;
	// The ClassLoader parent to use when creating ModuleClassLoaders.
	private final ClassLoader moduleClassLoaderParent;
	private final AtomicLong lastSecurityAdminFlush;

	final AtomicLazyInitializer<Executor> resolverExecutor;
	final Callable<Executor> lazyResolverExecutorCreator;
	final AtomicLazyInitializer<Executor> startLevelExecutor;
	final Callable<Executor> lazyStartLevelExecutorCreator;

	public EquinoxContainerAdaptor(EquinoxContainer container, Storage storage, Map<Long, Generation> initial) {
		this.container = container;
		this.storage = storage;
		this.hooks = new OSGiFrameworkHooks(container, storage);
		this.initial = initial;
		this.moduleClassLoaderParent = getModuleClassLoaderParent(container.getConfiguration(),
				container.getBootLoader());
		this.lastSecurityAdminFlush = new AtomicLong();

		EquinoxConfiguration config = container.getConfiguration();
		@SuppressWarnings("deprecation")
		String resolverThreadCntProp = config.getConfiguration(EquinoxConfiguration.PROP_EQUINOX_RESOLVER_THREAD_COUNT, //
				config.getConfiguration(EquinoxConfiguration.PROP_RESOLVER_THREAD_COUNT));

		int resolverThreadCnt;
		try {
			// note that resolver thread count defaults to -1 (compute based on processor
			// number)
			resolverThreadCnt = resolverThreadCntProp == null ? -1 : Integer.parseInt(resolverThreadCntProp);
		} catch (NumberFormatException e) {
			resolverThreadCnt = -1;
		}
		String startLevelThreadCntProp = config
				.getConfiguration(EquinoxConfiguration.PROP_EQUINOX_START_LEVEL_THREAD_COUNT);
		int startLevelThreadCnt;
		try {
			// Note that start-level thread count defaults to 1 (synchronous start)
			startLevelThreadCnt = startLevelThreadCntProp == null ? 1 : Integer.parseInt(startLevelThreadCntProp);
		} catch (NumberFormatException e) {
			startLevelThreadCnt = 1;
		}

		// Use two different executors for resolver and start-level because of the
		// different queue requirements

		// For the resolver we must use a SynchronousQueue because multiple threads
		// can kick off a resolution operation and block one of the executor threads
		// per resolution operation.
		// If the number of concurrent resolution operations reaches the number of
		// executor threads then each executor thread may end up blocked causing the
		// executor to no longer accept work. A SynchronousQueue prevents that from
		// happening.
		this.resolverExecutor = new AtomicLazyInitializer<>();
		this.lazyResolverExecutorCreator = createLazyExecutorCreator( //
				"Equinox resolver thread - " + EquinoxContainerAdaptor.this.toString(), //$NON-NLS-1$
				resolverThreadCnt, new SynchronousQueue<>());

		// For the start-level we can safely use a growing queue because the thread
		// feeding the
		// start-level executor with work is a single thread and it can safely block
		// waiting
		// for the work of the executor threads to finish.
		this.startLevelExecutor = new AtomicLazyInitializer<>();
		this.lazyStartLevelExecutorCreator = createLazyExecutorCreator(//
				"Equinox start level thread - " + EquinoxContainerAdaptor.this.toString(), //$NON-NLS-1$
				startLevelThreadCnt, new LinkedBlockingQueue<>(1000));

	}

	private Callable<Executor> createLazyExecutorCreator(final String threadName, int threadCnt,
			final BlockingQueue<Runnable> queue) {
		// use the number of processors when configured value is <=0
		final int maxThreads = threadCnt <= 0 ? Runtime.getRuntime().availableProcessors() : threadCnt;
		return new Callable<Executor>() {
			@Override
			public Executor call() throws Exception {
				if (maxThreads == 1) {
					// just do synchronous execution with current thread
					return new Executor() {
						@Override
						public void execute(Runnable command) {
							command.run();
						}
					};
				}
				// Always want to create core threads until max size
				int coreThreads = maxThreads;
				// idle timeout; make it short to get rid of threads quickly after use
				int idleTimeout = 10;
				// try to name the threads with useful name
				ThreadFactory threadFactory = new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r, threadName);
						t.setDaemon(true);
						return t;
					}
				};
				// use a rejection policy that simply runs the task in the current thread once
				// the max pool size is reached
				RejectedExecutionHandler rejectHandler = new ThreadPoolExecutor.CallerRunsPolicy();

				ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThreads, maxThreads, idleTimeout,
						TimeUnit.SECONDS, queue, threadFactory, rejectHandler);
				executor.allowCoreThreadTimeOut(true);
				return executor;
			}
		};
	}

	private static ClassLoader getModuleClassLoaderParent(EquinoxConfiguration configuration, ClassLoader bootLoader) {
		// allow hooks to determine the parent class loader
		for (ClassLoaderHook hook : configuration.getHookRegistry().getClassLoaderHooks()) {
			ClassLoader parent = hook.getModuleClassLoaderParent(configuration);
			if (parent != null) {
				// first one to return non-null wins.
				return parent;
			}
		}
		// DEFAULT behavior:
		// check property for specified parent
		// check the osgi defined property first
		String type = configuration.getConfiguration(Constants.FRAMEWORK_BUNDLE_PARENT);
		if (type == null) {
			type = configuration.getConfiguration(EquinoxConfiguration.PROP_PARENT_CLASSLOADER,
					Constants.FRAMEWORK_BUNDLE_PARENT_BOOT);
		}

		if (Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK.equalsIgnoreCase(type)
				|| EquinoxConfiguration.PARENT_CLASSLOADER_FWK.equalsIgnoreCase(type)) {
			ClassLoader cl = EquinoxContainer.class.getClassLoader();
			return cl == null ? bootLoader : cl;
		}
		if (Constants.FRAMEWORK_BUNDLE_PARENT_APP.equalsIgnoreCase(type))
			return ClassLoader.getSystemClassLoader();
		if (Constants.FRAMEWORK_BUNDLE_PARENT_EXT.equalsIgnoreCase(type)) {
			ClassLoader appCL = ClassLoader.getSystemClassLoader();
			if (appCL != null)
				return appCL.getParent();
		}
		return bootLoader;

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
	public void publishContainerEvent(ContainerEvent type, Module module, Throwable error,
			FrameworkListener... listeners) {
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
	}

	@Override
	public Module createModule(String location, long id, EnumSet<Settings> settings, int startlevel) {
		EquinoxBundle bundle = new EquinoxBundle(id, location, storage.getModuleContainer(), settings, startlevel,
				container);
		return bundle.getModule();
	}

	@Override
	public SystemModule createSystemModule() {
		return (SystemModule) new EquinoxBundle.SystemBundle(storage.getModuleContainer(), container).getModule();
	}

	@Override
	public String getProperty(String key) {
		return storage.getConfiguration().getConfiguration(key);
	}

	@Override
	public ModuleLoader createModuleLoader(ModuleWiring wiring) {
		if (wiring.getBundle().getBundleId() == 0) {
			ClassLoader cl = EquinoxContainer.class.getClassLoader();
			cl = cl == null ? container.getBootLoader() : cl;
			return new SystemBundleLoader(wiring, container, cl);
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
		long updatedTimestamp = storage.getModuleDatabase().getRevisionsTimestamp();
		if (System.getSecurityManager() != null
				&& updatedTimestamp != lastSecurityAdminFlush.getAndSet(updatedTimestamp)) {
			storage.getSecurityAdmin().clearCaches();
			List<Module> modules = storage.getModuleContainer().getModules();
			for (Module module : modules) {
				for (ModuleRevision revision : module.getRevisions().getModuleRevisions()) {
					Generation generation = (Generation) revision.getRevisionInfo();
					if (generation != null) {
						ProtectionDomain domain = generation.getDomain(false);
						if (domain != null) {
							((BundlePermissions) domain.getPermissions()).clearPermissionCache();
						}
					}
				}
			}
		}
		clearManifestCache(moduleWiring);
	}

	private void clearManifestCache(ModuleWiring moduleWiring) {
		boolean frameworkActive = Module.ACTIVE_SET.contains(storage.getModuleContainer().getModule(0).getState());
		ModuleRevision revision = moduleWiring.getRevision();
		Module module = revision.getRevisions().getModule();
		boolean isUninstallingOrUninstalled = State.UNINSTALLED.equals(module.getState())
				^ module.holdsTransitionEventLock(ModuleEvent.UNINSTALLED);
		if (!frameworkActive || !isUninstallingOrUninstalled) {
			// only do this when the framework is not active or when the bundle is not
			// uninstalled
			Generation generation = (Generation) moduleWiring.getRevision().getRevisionInfo();
			generation.clearManifestCache();
		}
	}

	static int getType(ContainerEvent type) {
		switch (type) {
		case ERROR:
			return FrameworkEvent.ERROR;
		case INFO:
			return FrameworkEvent.INFO;
		case WARNING:
			return FrameworkEvent.WARNING;
		case REFRESH:
			return FrameworkEvent.PACKAGES_REFRESHED;
		case START_LEVEL:
			return FrameworkEvent.STARTLEVEL_CHANGED;
		case STARTED:
			return FrameworkEvent.STARTED;
		case STOPPED:
			return FrameworkEvent.STOPPED;
		case STOPPED_REFRESH:
			return FrameworkEvent.STOPPED_SYSTEM_REFRESHED;
		case STOPPED_UPDATE:
			return FrameworkEvent.STOPPED_UPDATE;
		case STOPPED_TIMEOUT:
			return FrameworkEvent.WAIT_TIMEDOUT;
		default:
			// default to error
			return FrameworkEvent.ERROR;
		}
	}

	private int getType(ModuleEvent type) {
		switch (type) {
		case INSTALLED:
			return BundleEvent.INSTALLED;
		case LAZY_ACTIVATION:
			return BundleEvent.LAZY_ACTIVATION;
		case RESOLVED:
			return BundleEvent.RESOLVED;
		case STARTED:
			return BundleEvent.STARTED;
		case STARTING:
			return BundleEvent.STARTING;
		case STOPPING:
			return BundleEvent.STOPPING;
		case STOPPED:
			return BundleEvent.STOPPED;
		case UNINSTALLED:
			return BundleEvent.UNINSTALLED;
		case UNRESOLVED:
			return BundleEvent.UNRESOLVED;
		case UPDATED:
			return BundleEvent.UPDATED;
		default:
			// TODO log error?
			return 0;
		}
	}

	@Override
	public void refreshedSystemModule() {
		storage.getConfiguration().setConfiguration(EquinoxConfiguration.PROP_FORCED_RESTART, "true"); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return container.toString();
	}

	@Override
	public void updatedDatabase() {
		StorageSaver saver = container.getStorageSaver();
		if (saver == null)
			return;
		saver.save();
	}

	@Override
	public void initBegin() {
		hooks.initBegin();
	}

	@Override
	public void initEnd() {
		hooks.initEnd();
	}

	@Override
	public DebugOptions getDebugOptions() {
		return container.getConfiguration().getDebugOptions();
	}

	@Override
	public Executor getResolverExecutor() {
		return resolverExecutor.getInitialized(lazyResolverExecutorCreator);
	}

	@Override
	public Executor getStartLevelExecutor() {
		return startLevelExecutor.getInitialized(lazyStartLevelExecutorCreator);
	}

	@Override
	public ScheduledExecutorService getScheduledExecutor() {
		return container.getScheduledExecutor();
	}

	public void shutdownExecutors() {
		Executor current = resolverExecutor.getAndClear();
		if (current instanceof ExecutorService) {
			((ExecutorService) current).shutdown();
		}
		current = startLevelExecutor.getAndClear();
		if (current instanceof ExecutorService) {
			((ExecutorService) current).shutdown();
		}
	}

	@Override
	public ModuleRevisionBuilder adaptModuleRevisionBuilder(ModuleEvent operation, Module origin,
			ModuleRevisionBuilder builder, Object revisionInfo) {
		Generation generation = (Generation) revisionInfo;
		return generation.adaptModuleRevisionBuilder(operation, origin, builder);
	}
}
