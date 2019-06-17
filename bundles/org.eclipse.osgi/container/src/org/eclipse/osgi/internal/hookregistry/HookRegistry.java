/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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

package org.eclipse.osgi.internal.hookregistry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.cds.CDSHookConfigurator;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.hooks.DevClassLoadingHook;
import org.eclipse.osgi.internal.hooks.EclipseLazyStarter;
import org.eclipse.osgi.internal.signedcontent.SignedBundleHook;
import org.eclipse.osgi.internal.weaving.WeavingHookConfigurator;
import org.eclipse.osgi.util.ManifestElement;

/**
 * The hook registry is used to store all the hooks which are
 * configured by the hook configurators.
 * @see HookConfigurator
 */
public final class HookRegistry {
	/**
	 * The hook configurators properties file (&quot;hookconfigurators.properties&quot;) <p>
	 * A framework extension may supply a hook configurators properties file to specify a 
	 * list of hook configurators.
	 * @see #HOOK_CONFIGURATORS
	 */
	public static final String HOOK_CONFIGURATORS_FILE = "hookconfigurators.properties"; //$NON-NLS-1$

	/**
	 * The hook configurators property key (&quot;hookconfigurators.properties&quot;) used in 
	 * a hook configurators properties file to specify a comma separated list of fully 
	 * qualified hook configurator classes.
	 */
	public static final String HOOK_CONFIGURATORS = "hook.configurators"; //$NON-NLS-1$

	/**
	 * A system property (&quot;osgi.hook.configurators.include&quot;) used to add additional
	 * hook configurators.  This is helpful for configuring optional hook configurators.
	 */
	public static final String PROP_HOOK_CONFIGURATORS_INCLUDE = "osgi.hook.configurators.include"; //$NON-NLS-1$

	/**
	 * A system property (&quot;osgi.hook.configurators.exclude&quot;) used to exclude 
	 * any hook configurators.  This is helpful for disabling hook
	 * configurators that is specified in hook configurator properties files.
	 */
	public static final String PROP_HOOK_CONFIGURATORS_EXCLUDE = "osgi.hook.configurators.exclude"; //$NON-NLS-1$

	/**
	 * A system property (&quot;osgi.hook.configurators&quot;) used to specify the list
	 * of hook configurators.  If this property is set then the list of configurators 
	 * specified will be the only configurators used.
	 */
	public static final String PROP_HOOK_CONFIGURATORS = "osgi.hook.configurators"; //$NON-NLS-1$

	private static final String BUILTIN_HOOKS = "builtin.hooks"; //$NON-NLS-1$

	private final EquinoxContainer container;
	private volatile boolean initialized = false;
	private final List<ClassLoaderHook> classLoaderHooks = new ArrayList<>();
	private final List<ClassLoaderHook> classLoaderHooksRO = Collections.unmodifiableList(classLoaderHooks);
	private final List<StorageHookFactory<?, ?, ?>> storageHookFactories = new ArrayList<>();
	private final List<StorageHookFactory<?, ?, ?>> storageHookFactoriesRO = Collections.unmodifiableList(storageHookFactories);
	private final List<BundleFileWrapperFactoryHook> bundleFileWrapperFactoryHooks = new ArrayList<>();
	private final List<BundleFileWrapperFactoryHook> bundleFileWrapperFactoryHooksRO = Collections.unmodifiableList(bundleFileWrapperFactoryHooks);
	private final List<ActivatorHookFactory> activatorHookFactories = new ArrayList<>();
	private final List<ActivatorHookFactory> activatorHookFactoriesRO = Collections.unmodifiableList(activatorHookFactories);

	public HookRegistry(EquinoxContainer container) {
		this.container = container;
	}

	/**
	 * Initializes the hook configurators.  The following steps are used to initialize the hook configurators. <p>
	 * 1. Get a list of hook configurators from all hook configurators properties files on the classpath, 
	 *    add this list to the overall list of hook configurators, remove duplicates. <p>
	 * 2. Get a list of hook configurators from the (&quot;osgi.hook.configurators.include&quot;) system property 
	 *    and add this list to the overall list of hook configurators, remove duplicates. <p>
	 * 3. Get a list of hook configurators from the (&quot;osgi.hook.configurators.exclude&quot;) system property
	 *    and remove this list from the overall list of hook configurators. <p>
	 * 4. Load each hook configurator class, create a new instance, then call the {@link HookConfigurator#addHooks(HookRegistry)} method <p>
	 * 5. Set this HookRegistry object to read only to prevent any other hooks from being added. <p>
	 */
	public void initialize() {
		List<String> configurators = new ArrayList<>(5);
		List<FrameworkLogEntry> errors = new ArrayList<>(0); // optimistic that no errors will occur
		mergeFileHookConfigurators(configurators, errors);
		mergePropertyHookConfigurators(configurators);
		synchronized (this) {
			addClassLoaderHook(new DevClassLoadingHook(container.getConfiguration()));
			addClassLoaderHook(new EclipseLazyStarter(container));
			addClassLoaderHook(new WeavingHookConfigurator(container));
			configurators.add(SignedBundleHook.class.getName());
			configurators.add(CDSHookConfigurator.class.getName());
			loadConfigurators(configurators, errors);
			// set to read-only
			initialized = true;
		}
		for (FrameworkLogEntry error : errors) {
			container.getLogServices().getFrameworkLog().log(error);
		}
	}

	private void mergeFileHookConfigurators(List<String> configuratorList, List<FrameworkLogEntry> errors) {
		ClassLoader cl = getClass().getClassLoader();
		// get all hook configurators files in your classloader delegation
		Enumeration<URL> hookConfigurators;
		try {
			hookConfigurators = cl != null ? cl.getResources(HookRegistry.HOOK_CONFIGURATORS_FILE) : ClassLoader.getSystemResources(HookRegistry.HOOK_CONFIGURATORS_FILE);
		} catch (IOException e) {
			errors.add(new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, "getResources error on " + HookRegistry.HOOK_CONFIGURATORS_FILE, 0, e, null)); //$NON-NLS-1$
			return;
		}
		int curBuiltin = 0;
		while (hookConfigurators.hasMoreElements()) {
			URL url = hookConfigurators.nextElement();
			InputStream input = null;
			try {
				// check each file for a hook.configurators property
				Properties configuratorProps = new Properties();
				input = url.openStream();
				configuratorProps.load(input);
				String hooksValue = configuratorProps.getProperty(HOOK_CONFIGURATORS);
				if (hooksValue == null)
					continue;
				boolean builtin = Boolean.valueOf(configuratorProps.getProperty(BUILTIN_HOOKS)).booleanValue();
				String[] configurators = ManifestElement.getArrayFromList(hooksValue, ","); //$NON-NLS-1$
				for (String configurator : configurators) {
					if (!configuratorList.contains(configurator)) {
						if (builtin) {
							configuratorList.add(curBuiltin++, configurator);
						} else {
							configuratorList.add(configurator);
						}
					}
				}
			} catch (IOException e) {
				errors.add(new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, "error loading: " + url.toExternalForm(), 0, e, null)); //$NON-NLS-1$
				// ignore and continue to next URL
			} finally {
				if (input != null)
					try {
						input.close();
					} catch (IOException e) {
						// do nothing
					}
			}
		}
	}

	private void mergePropertyHookConfigurators(List<String> configuratorList) {
		// see if there is a configurators list
		String[] configurators = ManifestElement.getArrayFromList(container.getConfiguration().getConfiguration(HookRegistry.PROP_HOOK_CONFIGURATORS), ","); //$NON-NLS-1$
		if (configurators.length > 0) {
			configuratorList.clear(); // clear the list, we are only going to use the configurators from the list
			for (String configurator : configurators) {
				if (!configuratorList.contains(configurator)) {
					configuratorList.add(configurator);
				}
			}
			return; // don't do anything else
		}
		// Make sure the configurators from the include property are in the list
		String[] includeConfigurators = ManifestElement.getArrayFromList(container.getConfiguration().getConfiguration(HookRegistry.PROP_HOOK_CONFIGURATORS_INCLUDE), ","); //$NON-NLS-1$
		for (String includeConfigurator : includeConfigurators) {
			if (!configuratorList.contains(includeConfigurator)) {
				configuratorList.add(includeConfigurator);
			}
		}
		// Make sure the configurators from the exclude property are no in the list
		String[] excludeHooks = ManifestElement.getArrayFromList(container.getConfiguration().getConfiguration(HookRegistry.PROP_HOOK_CONFIGURATORS_EXCLUDE), ","); //$NON-NLS-1$
		for (String excludeHook : excludeHooks) {
			configuratorList.remove(excludeHook);
		}
	}

	private void loadConfigurators(List<String> configurators, List<FrameworkLogEntry> errors) {
		for (String hookName : configurators) {
			try {
				Class<?> clazz = Class.forName(hookName);
				HookConfigurator configurator = (HookConfigurator) clazz.getConstructor().newInstance();
				configurator.addHooks(this);
			} catch (Throwable t) {
				// We expect the follow exeptions may happen; but we need to catch all here
				// ClassNotFoundException
				// IllegalAccessException
				// InstantiationException
				// ClassCastException
				errors.add(new FrameworkLogEntry(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, 0, "error loading hook: " + hookName, 0, t, null)); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Returns the list of configured class loading hooks.
	 * @return the list of configured class loading hooks.
	 */
	public List<ClassLoaderHook> getClassLoaderHooks() {
		return classLoaderHooksRO;
	}

	/**
	 * Returns the list of configured storage hooks.
	 * @return the list of configured storage hooks.
	 */
	public List<StorageHookFactory<?, ?, ?>> getStorageHookFactories() {
		return storageHookFactoriesRO;
	}

	/**
	 * Returns the configured bundle file wrapper factories
	 * @return the configured bundle file wrapper factories
	 */
	public List<BundleFileWrapperFactoryHook> getBundleFileWrapperFactoryHooks() {
		return bundleFileWrapperFactoryHooksRO;
	}

	/**
	 * Returns the configured activator hook factories
	 * @return the configured activator hook factories
	 */
	public List<ActivatorHookFactory> getActivatorHookFactories() {
		return activatorHookFactoriesRO;
	}

	private <H> void add(H hook, List<H> hooks) {
		if (initialized)
			throw new IllegalStateException("Cannot add hooks dynamically."); //$NON-NLS-1$
		hooks.add(hook);
	}

	/**
	 * Adds a class loader hook to this hook registry.
	 * @param classLoaderHook a class loading hook object.
	 */
	public void addClassLoaderHook(ClassLoaderHook classLoaderHook) {
		add(classLoaderHook, classLoaderHooks);
	}

	/**
	 * Adds a storage hook to this hook registry.
	 * @param storageHookFactory a storage hook object.
	 */
	public void addStorageHookFactory(StorageHookFactory<?, ?, ?> storageHookFactory) {
		add(storageHookFactory, storageHookFactories);
	}

	/**
	 * Adds a bundle file wrapper factory for this hook registry
	 * @param factory a bundle file wrapper factory object.
	 */
	public void addBundleFileWrapperFactoryHook(BundleFileWrapperFactoryHook factory) {
		add(factory, bundleFileWrapperFactoryHooks);
	}

	/**
	 * Adds an activator hook factory.  The activators created by this factory will be started and stopped
	 * when the system bundle is started and stopped.
	 * @param activatorHookFactory the activator hook factory.
	 */
	public void addActivatorHookFactory(ActivatorHookFactory activatorHookFactory) {
		add(activatorHookFactory, activatorHookFactories);
	}

	/**
	 * Returns the configuration associated with this hook registry.
	 * @return the configuration associated with this hook registry.
	 */
	public EquinoxConfiguration getConfiguration() {
		return container.getConfiguration();
	}

	/**
	 * Returns the equinox container associated with this hook registry.
	 * @return the equinox container associated with this hook registry.
	 */
	public EquinoxContainer getContainer() {
		return container;
	}
}
