/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.eclipse.osgi.framework.eventmgr.ListenerQueue;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.connect.ConnectBundleFile;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.internal.log.EquinoxLogServices;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistry;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.osgi.storage.BundleInfo;
import org.eclipse.osgi.storage.Storage;
import org.eclipse.osgi.storage.bundlefile.MRUBundleFileList;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.util.tracker.ServiceTracker;

public class EquinoxContainer implements ThreadFactory, Runnable {
	public static final String NAME = "org.eclipse.osgi"; //$NON-NLS-1$
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	private final ConnectModules connectModules;
	private final EquinoxConfiguration equinoxConfig;
	private final EquinoxLogServices logServices;
	private final Storage storage;
	private final Set<String> bootDelegation;
	private final String[] bootDelegationStems;
	private final boolean bootDelegateAll;
	private final boolean isProcessClassRecursionSupportedByAll;
	private final EquinoxEventPublisher eventPublisher;

	private final Object monitor = new Object();

	private final ClassLoader bootLoader;
	private ServiceRegistry serviceRegistry;
	private ContextFinder contextFinder;

	private ServiceTracker<SignedContentFactory, SignedContentFactory> signedContentFactory;

	private ScheduledExecutorService executor;
	private StorageSaver storageSaver;

	public EquinoxContainer(Map<String, ?> configuration, ModuleConnector moduleConnector) {
		ClassLoader platformClassLoader = null;
		try {
			Method getPlatformClassLoader = ClassLoader.class.getMethod("getPlatformClassLoader"); //$NON-NLS-1$
			platformClassLoader = (ClassLoader) getPlatformClassLoader.invoke(null);
		} catch (Throwable t) {
			// try everything possible to not fail
			platformClassLoader = new ClassLoader(Object.class.getClassLoader()) {
				/* boot class loader */};
		}
		this.bootLoader = platformClassLoader;
		this.equinoxConfig = new EquinoxConfiguration(configuration, new HookRegistry(this));
		this.logServices = new EquinoxLogServices(this.equinoxConfig);
		this.equinoxConfig.logMessages(this.logServices);
		this.connectModules = new ConnectModules(moduleConnector);

		initConnectFramework(moduleConnector, this.equinoxConfig);

		this.equinoxConfig.getHookRegistry().initialize();
		try {
			this.storage = Storage.createStorage(this);
		} catch (IOException | BundleException e) {
			throw new RuntimeException("Error initializing storage.", e); //$NON-NLS-1$
		}

		this.eventPublisher = new EquinoxEventPublisher(this);

		// set the boot delegation according to the osgi boot delegation property
		// TODO unfortunately this has to be done after constructing storage so the vm profile is loaded
		// TODO ideally this should be in equinox configuration or perhaps in storage
		String bootDelegationProp = equinoxConfig.getConfiguration(Constants.FRAMEWORK_BOOTDELEGATION);
		String[] bootPackages = ManifestElement.getArrayFromList(bootDelegationProp, ","); //$NON-NLS-1$
		HashSet<String> exactMatch = new HashSet<>(bootPackages.length);
		List<String> stemMatch = new ArrayList<>(bootPackages.length);
		boolean delegateAllValue = false;
		for (String bootPackage : bootPackages) {
			if (bootPackage.equals("*")) { //$NON-NLS-1$
				delegateAllValue = true;
				exactMatch.clear();
				stemMatch.clear();
				break;
			} else if (bootPackage.endsWith("*")) { //$NON-NLS-1$
				if (bootPackage.length() > 2 && bootPackage.endsWith(".*")) { //$NON-NLS-1$
					stemMatch.add(bootPackage.substring(0, bootPackage.length() - 1));
				}
			} else {
				exactMatch.add(bootPackage);
			}
		}
		bootDelegateAll = delegateAllValue;
		bootDelegation = exactMatch;
		bootDelegationStems = stemMatch.isEmpty() ? null : stemMatch.toArray(new String[stemMatch.size()]);

		// Detect if all hooks can support recursive class processing
		boolean supportRecursion = true;
		for (ClassLoaderHook hook : equinoxConfig.getHookRegistry().getClassLoaderHooks()) {
			supportRecursion &= hook.isProcessClassRecursionSupported();
		}
		isProcessClassRecursionSupportedByAll = supportRecursion;
	}

	private static void initConnectFramework(ModuleConnector moduleConnector, EquinoxConfiguration equinoxConfig) {
		if (moduleConnector == null) {
			return;
		}
		URL configUrl = equinoxConfig.getEquinoxLocations().getConfigurationLocation().getURL();
		final File fwkStore = new File(configUrl.getPath());
		@SuppressWarnings({"rawtypes", "unchecked"})
		Map<String, String> config = (Map) equinoxConfig.getInitialConfig();
		moduleConnector.initialize(fwkStore, Collections.unmodifiableMap(config));
	}

	public Storage getStorage() {
		return storage;
	}

	public EquinoxConfiguration getConfiguration() {
		return equinoxConfig;
	}

	public EquinoxLocations getLocations() {
		return equinoxConfig.getEquinoxLocations();
	}

	public EquinoxLogServices getLogServices() {
		return logServices;
	}

	public Bundle getBundle(Class<?> clazz) {
		Bundle b = FrameworkUtil.getBundle(clazz);
		if (b != null) {
			return b;
		}
		// check if it is the system bundle
		return AccessController.doPrivileged((PrivilegedAction<Bundle>) () -> {
			if (clazz.getClassLoader() == EquinoxContainer.class.getClassLoader()) {
				return getStorage().getModuleContainer().getModule(0).getBundle();
			}
			return null;
		});
	}

	public SignedContentFactory getSignedContentFactory() {
		ServiceTracker<SignedContentFactory, SignedContentFactory> current;
		synchronized (this.monitor) {
			current = signedContentFactory;
		}
		return current == null ? null : current.getService();
	}

	public boolean isBootDelegationPackage(String name) {
		if (bootDelegateAll)
			return true;
		if (bootDelegation.contains(name))
			return true;
		if (bootDelegationStems != null)
			for (String bootDelegationStem : bootDelegationStems) {
				if (name.startsWith(bootDelegationStem)) {
					return true;
				}
			}
		return false;
	}

	public boolean isProcessClassRecursionSupportedByAll() {
		return isProcessClassRecursionSupportedByAll;
	}

	void init() {
		eventPublisher.init();
		synchronized (this.monitor) {
			serviceRegistry = new ServiceRegistry(this);
			initializeContextFinder();
			executor = Executors.newScheduledThreadPool(1, this);
			// be sure to initialize the executor threads
			executor.execute(this);
			storageSaver = new StorageSaver(this);
		}
	}

	void close() {
		StorageSaver currentSaver;
		Storage currentStorage;
		ScheduledExecutorService currentExecutor;
		synchronized (this.monitor) {
			serviceRegistry = null;
			currentSaver = storageSaver;
			currentStorage = storage;
			currentExecutor = executor;
		}
		// do this outside of the lock to avoid deadlock
		currentSaver.close();
		currentStorage.close();
		// Must be done last since it will result in termination of the
		// framework active thread.
		currentExecutor.shutdown();
	}

	private void initializeContextFinder() {
		Thread current = Thread.currentThread();
		try {
			ClassLoader parent = null;
			// check property for specified parent
			String type = equinoxConfig.getConfiguration(EquinoxConfiguration.PROP_CONTEXTCLASSLOADER_PARENT);
			if (EquinoxConfiguration.CONTEXTCLASSLOADER_PARENT_APP.equals(type))
				parent = ClassLoader.getSystemClassLoader();
			else if (EquinoxConfiguration.CONTEXTCLASSLOADER_PARENT_BOOT.equals(type))
				parent = bootLoader;
			else if (EquinoxConfiguration.CONTEXTCLASSLOADER_PARENT_FWK.equals(type))
				parent = EquinoxContainer.class.getClassLoader();
			else if (EquinoxConfiguration.CONTEXTCLASSLOADER_PARENT_EXT.equals(type)) {
				ClassLoader appCL = ClassLoader.getSystemClassLoader();
				if (appCL != null)
					parent = appCL.getParent();
			} else { // default is ccl (null or any other value will use ccl)
				parent = current.getContextClassLoader();
			}
			contextFinder = new ContextFinder(parent, bootLoader);
			current.setContextClassLoader(contextFinder);
			return;
		} catch (Exception e) {
			logServices.log(NAME, FrameworkLogEntry.INFO, NLS.bind(Msg.CANNOT_SET_CONTEXTFINDER, null), e);
		}

	}

	public EquinoxEventPublisher getEventPublisher() {
		synchronized (this.monitor) {
			return eventPublisher;
		}
	}

	ScheduledExecutorService getScheduledExecutor() {
		synchronized (this.monitor) {
			return executor;
		}
	}

	public ServiceRegistry getServiceRegistry() {
		synchronized (this.monitor) {
			return serviceRegistry;
		}
	}

	public ContextFinder getContextFinder() {
		synchronized (this.monitor) {
			return contextFinder;
		}
	}

	public <K, V, E> ListenerQueue<K, V, E> newListenerQueue() {
		return eventPublisher.newListenerQueue();
	}

	void checkAdminPermission(Bundle bundle, String action) {
		if (bundle == null)
			return;
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new AdminPermission(bundle, action));
	}

	public void handleRuntimeError(Throwable t) {
		// TODO need to call some hook here
	}

	void systemStart(BundleContext bc) {
		synchronized (this.monitor) {
			signedContentFactory = new ServiceTracker<>(bc, SignedContentFactory.class, null);
		}
		signedContentFactory.open();
	}

	void systemStop(BundleContext bc) {
		ServiceTracker<SignedContentFactory, SignedContentFactory> current;
		synchronized (this.monitor) {
			current = signedContentFactory;
		}
		if (current != null) {
			current.close();
		}
	}

	@Override
	public String toString() {
		String UUID = equinoxConfig == null ? null : equinoxConfig.getConfiguration(Constants.FRAMEWORK_UUID);
		return "Equinox Container: " + UUID; //$NON-NLS-1$
	}

	StorageSaver getStorageSaver() {
		synchronized (this.monitor) {
			return storageSaver;
		}
	}

	@Override
	public Thread newThread(Runnable r) {
		String type = equinoxConfig.getConfiguration(EquinoxConfiguration.PROP_ACTIVE_THREAD_TYPE, EquinoxConfiguration.ACTIVE_THREAD_TYPE_NORMAL);
		Thread t = new Thread(r, "Active Thread: " + toString()); //$NON-NLS-1$
		if (EquinoxConfiguration.ACTIVE_THREAD_TYPE_NORMAL.equals(type)) {
			t.setDaemon(false);
		} else {
			t.setDaemon(true);
		}
		t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}

	@Override
	public void run() {
		// Do nothing; just used to ensure the active thread is created during init
	}

	public ClassLoader getBootLoader() {
		return bootLoader;
	}

	public ConnectModules getConnectModules() {
		return connectModules;
	}

	public static class ConnectModules {
		final ModuleConnector moduleConnector;
		private final ConcurrentMap<String, ConnectModule> connectModules = new ConcurrentHashMap<>();
		private final WeakHashMap<ConnectContent, WeakReference<ConnectBundleFile>> contents = new WeakHashMap<>();

		public ConnectModules(ModuleConnector moduleConnector) {
			this.moduleConnector = moduleConnector;
		}

		public ConnectModule connect(String location) {
			if (moduleConnector == null) {
				return null;
			}
			ConnectModule result = connectModules.compute(location, (k, v) -> {
				try {
					return moduleConnector.connect(location).orElse(null);
				} catch (BundleException e) {
					throw new IllegalStateException(e);
				}
			});
			return result;
		}

		public ConnectBundleFile getConnectBundleFile(ConnectModule module, File basefile,
				BundleInfo.Generation generation, MRUBundleFileList mruList, Debug debug) throws IOException {
			ConnectContent content = module.getContent();
			synchronized (contents) {
				WeakReference<ConnectBundleFile> ref = contents.get(content);
				if (ref != null) {
					ConnectBundleFile bundleFile = ref.get();
					if (bundleFile != null) {
						return bundleFile;
					}
				}
				ConnectBundleFile bundleFile = new ConnectBundleFile(module, basefile, generation, mruList, debug);
				contents.put(content, new WeakReference<>(bundleFile));
				return bundleFile;
			}
		}

		public ModuleConnector getModuleConnector() {
			return moduleConnector;
		}

		public ConnectModule getConnectModule(String location) {
			return connectModules.get(location);
		}
	}

	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}
}
