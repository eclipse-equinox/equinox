/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.baseadaptor.hooks.*;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.util.ManifestElement;

/**
 * The hook registry is used to store all the hooks which are
 * configured by the hook configurators.
 * @see HookConfigurator
 * @since 3.2
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

	private BaseAdaptor adaptor;
	private boolean readonly = false;
	private AdaptorHook[] adaptorHooks = new AdaptorHook[0];
	private BundleWatcher[] watchers = new BundleWatcher[0];
	private ClassLoadingHook[] classLoadingHooks = new ClassLoadingHook[0];
	private ClassLoadingStatsHook[] classLoadingStatsHooks = new ClassLoadingStatsHook[0];
	private ClassLoaderDelegateHook[] classLoaderDelegateHooks = new ClassLoaderDelegateHook[0];
	private StorageHook[] storageHooks = new StorageHook[0];
	private BundleFileFactoryHook[] bundleFileFactoryHooks = new BundleFileFactoryHook[0];
	private BundleFileWrapperFactoryHook[] bundleFileWrapperFactoryHooks = new BundleFileWrapperFactoryHook[0];

	public HookRegistry(BaseAdaptor adaptor) {
		this.adaptor = adaptor;
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
	 * @return an array of error log entries that occurred while initializing the hooks
	 */
	public FrameworkLogEntry[] initialize() {
		List<String> configurators = new ArrayList<String>(5);
		List<FrameworkLogEntry> errors = new ArrayList<FrameworkLogEntry>(0); // optimistic that no errors will occur
		mergeFileHookConfigurators(configurators, errors);
		mergePropertyHookConfigurators(configurators);
		loadConfigurators(configurators, errors);
		// set to read-only
		readonly = true;
		return errors.toArray(new FrameworkLogEntry[errors.size()]);
	}

	private void mergeFileHookConfigurators(List<String> configuratorList, List<FrameworkLogEntry> errors) {
		ClassLoader cl = getClass().getClassLoader();
		// get all hook configurators files in your classloader delegation
		Enumeration<URL> hookConfigurators;
		try {
			hookConfigurators = cl != null ? cl.getResources(HookRegistry.HOOK_CONFIGURATORS_FILE) : ClassLoader.getSystemResources(HookRegistry.HOOK_CONFIGURATORS_FILE);
		} catch (IOException e) {
			errors.add(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, "getResources error on " + HookRegistry.HOOK_CONFIGURATORS_FILE, 0, e, null)); //$NON-NLS-1$
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
				for (int i = 0; i < configurators.length; i++)
					if (!configuratorList.contains(configurators[i])) {
						if (builtin) // make sure the built-in configurators are listed first (bug 170881)
							configuratorList.add(curBuiltin++, configurators[i]);
						else
							configuratorList.add(configurators[i]);
					}
			} catch (IOException e) {
				errors.add(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, "error loading: " + url.toExternalForm(), 0, e, null)); //$NON-NLS-1$
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
		String[] configurators = ManifestElement.getArrayFromList(FrameworkProperties.getProperty(HookRegistry.PROP_HOOK_CONFIGURATORS), ","); //$NON-NLS-1$
		if (configurators.length > 0) {
			configuratorList.clear(); // clear the list, we are only going to use the configurators from the list
			for (int i = 0; i < configurators.length; i++)
				if (!configuratorList.contains(configurators[i]))
					configuratorList.add(configurators[i]);
			return; // don't do anything else
		}
		// Make sure the configurators from the include property are in the list
		String[] includeConfigurators = ManifestElement.getArrayFromList(FrameworkProperties.getProperty(HookRegistry.PROP_HOOK_CONFIGURATORS_INCLUDE), ","); //$NON-NLS-1$
		for (int i = 0; i < includeConfigurators.length; i++)
			if (!configuratorList.contains(includeConfigurators[i]))
				configuratorList.add(includeConfigurators[i]);
		// Make sure the configurators from the exclude property are no in the list
		String[] excludeHooks = ManifestElement.getArrayFromList(FrameworkProperties.getProperty(HookRegistry.PROP_HOOK_CONFIGURATORS_EXCLUDE), ","); //$NON-NLS-1$
		for (int i = 0; i < excludeHooks.length; i++)
			configuratorList.remove(excludeHooks[i]);
	}

	private void loadConfigurators(List<String> configurators, List<FrameworkLogEntry> errors) {
		for (Iterator<String> iHooks = configurators.iterator(); iHooks.hasNext();) {
			String hookName = iHooks.next();
			try {
				Class<?> clazz = Class.forName(hookName);
				HookConfigurator configurator = (HookConfigurator) clazz.newInstance();
				configurator.addHooks(this);
			} catch (Throwable t) {
				// We expect the follow exeptions may happen; but we need to catch all here
				// ClassNotFoundException
				// IllegalAccessException
				// InstantiationException
				// ClassCastException
				errors.add(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, "error loading hook: " + hookName, 0, t, null)); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Returns the list of configured adaptor hooks.
	 * @return the list of configured adaptor hooks.
	 */
	public AdaptorHook[] getAdaptorHooks() {
		return adaptorHooks;
	}

	/**
	 * Returns the list of configured bundle watchers.
	 * @return the list of configured bundle watchers.
	 */
	public BundleWatcher[] getWatchers() {
		return watchers;
	}

	/**
	 * Returns the list of configured class loading hooks.
	 * @return the list of configured class loading hooks.
	 */
	public ClassLoadingHook[] getClassLoadingHooks() {
		return classLoadingHooks;
	}

	/**
	 * Returns the list of configured class loading stats hooks.
	 * @return the list of configured class loading stats hooks.
	 */
	public ClassLoadingStatsHook[] getClassLoadingStatsHooks() {
		return classLoadingStatsHooks;
	}

	/**
	 * Returns the list of configured class loader delegate hooks.
	 * @return the list of configured class loader delegate hooks.
	 */
	public ClassLoaderDelegateHook[] getClassLoaderDelegateHooks() {
		return classLoaderDelegateHooks;
	}

	/**
	 * Returns the list of configured storage hooks.
	 * @return the list of configured storage hooks.
	 */
	public StorageHook[] getStorageHooks() {
		return storageHooks;
	}

	/**
	 * Returns the list of configured bundle file factories.
	 * @return the list of configured bundle file factories.
	 */
	public BundleFileFactoryHook[] getBundleFileFactoryHooks() {
		return bundleFileFactoryHooks;
	}

	/**
	 * Returns the configured bundle file wrapper factories
	 * @return the configured bundle file wrapper factories
	 */
	public BundleFileWrapperFactoryHook[] getBundleFileWrapperFactoryHooks() {
		return bundleFileWrapperFactoryHooks;
	}

	/**
	 * Adds a adaptor hook to this hook registry.
	 * @param adaptorHook an adaptor hook object.
	 */
	public void addAdaptorHook(AdaptorHook adaptorHook) {
		adaptorHooks = (AdaptorHook[]) add(adaptorHook, adaptorHooks, new AdaptorHook[adaptorHooks.length + 1]);
	}

	/**
	 * Adds a bundle watcher to this hook registry.
	 * @param watcher a bundle watcher object.
	 */
	public void addWatcher(BundleWatcher watcher) {
		watchers = (BundleWatcher[]) add(watcher, watchers, new BundleWatcher[watchers.length + 1]);
	}

	/**
	 * Adds a class loading hook to this hook registry.
	 * @param classLoadingHook a class loading hook object.
	 */
	public void addClassLoadingHook(ClassLoadingHook classLoadingHook) {
		classLoadingHooks = (ClassLoadingHook[]) add(classLoadingHook, classLoadingHooks, new ClassLoadingHook[classLoadingHooks.length + 1]);
	}

	/**
	 * Adds a class loading stats hook to this hook registry.
	 * @param classLoadingStatsHook a class loading hook object.
	 */
	public void addClassLoadingStatsHook(ClassLoadingStatsHook classLoadingStatsHook) {
		classLoadingStatsHooks = (ClassLoadingStatsHook[]) add(classLoadingStatsHook, classLoadingStatsHooks, new ClassLoadingStatsHook[classLoadingStatsHooks.length + 1]);
	}

	/**
	 * Adds a class loader delegate hook to this hook registry.
	 * @param classLoaderDelegateHook a class loader delegate hook.
	 */
	public void addClassLoaderDelegateHook(ClassLoaderDelegateHook classLoaderDelegateHook) {
		classLoaderDelegateHooks = (ClassLoaderDelegateHook[]) add(classLoaderDelegateHook, classLoaderDelegateHooks, new ClassLoaderDelegateHook[classLoaderDelegateHooks.length + 1]);
	}

	/**
	 * Adds a storage hook to this hook registry.
	 * @param storageHook a storage hook object.
	 */
	public void addStorageHook(StorageHook storageHook) {
		storageHooks = (StorageHook[]) add(storageHook, storageHooks, new StorageHook[storageHooks.length + 1]);
	}

	/**
	 * Adds a bundle file factory to this hook registry.
	 * @param factory a bundle file factory object.
	 */
	public void addBundleFileFactoryHook(BundleFileFactoryHook factory) {
		bundleFileFactoryHooks = (BundleFileFactoryHook[]) add(factory, bundleFileFactoryHooks, new BundleFileFactoryHook[bundleFileFactoryHooks.length + 1]);
	}

	/**
	 * Adds a bundle file wrapper factory for this hook registry
	 * @param factory a bundle file wrapper factory object.
	 */
	public void addBundleFileWrapperFactoryHook(BundleFileWrapperFactoryHook factory) {
		bundleFileWrapperFactoryHooks = (BundleFileWrapperFactoryHook[]) add(factory, bundleFileWrapperFactoryHooks, new BundleFileWrapperFactoryHook[bundleFileWrapperFactoryHooks.length + 1]);
	}

	private Object[] add(Object newValue, Object[] oldValues, Object[] newValues) {
		if (readonly)
			throw new IllegalStateException("Cannot add hooks dynamically."); //$NON-NLS-1$
		if (oldValues.length > 0)
			System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
		newValues[oldValues.length] = newValue;
		return newValues;
	}

	/**
	 * Returns the base adaptor associated with this hook registry.
	 * @return the base adaptor associated with this hook registry.
	 */
	public BaseAdaptor getAdaptor() {
		return adaptor;
	}
}
