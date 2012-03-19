/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.storage;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.internal.security.storage.friends.IStorageConstants;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;
import org.eclipse.osgi.util.NLS;

//XXX add validation on module IDs - AZaz09 and dots, absolutely no tabs 
// XXX reserved name DEFAULT_PASSWORD_ID

/**
 * Finds appropriate password provider module to use.
 */
public class PasswordProviderSelector implements IRegistryEventListener {

	final private static String EXTENSION_POINT = "org.eclipse.equinox.security.secureStorage"; //$NON-NLS-1$
	final private static String STORAGE_MODULE = "provider";//$NON-NLS-1$
	final private static String MODULE_PRIORITY = "priority";//$NON-NLS-1$
	final private static String MODULE_DESCRIPTION = "description";//$NON-NLS-1$
	final private static String CLASS_NAME = "class";//$NON-NLS-1$
	final private static String HINTS_NAME = "hint";//$NON-NLS-1$
	final private static String HINT_VALUE = "value";//$NON-NLS-1$

	private Map modules = new HashMap(5); // cache of modules found

	public class ExtStorageModule {
		public String moduleID;
		public IConfigurationElement element;
		public int priority;
		public String name;
		public String description;
		public List hints;

		public ExtStorageModule(String id, IConfigurationElement element, int priority, String name, String description, List hints) {
			super();
			this.element = element;
			this.moduleID = id;
			this.priority = priority;
			this.name = name;
			this.description = description;
			this.hints = hints;
		}
	}

	static private PasswordProviderSelector instance = null;

	static public PasswordProviderSelector getInstance() {
		if (instance == null) {
			instance = new PasswordProviderSelector();
			IExtensionRegistry registry = RegistryFactory.getRegistry();
			registry.addListener(instance, EXTENSION_POINT);
		}
		return instance;
	}

	static public void stop() {
		if (instance != null) {
			IExtensionRegistry registry = RegistryFactory.getRegistry();
			registry.removeListener(instance);
			instance = null;
		}
	}

	private PasswordProviderSelector() {
		// hides default constructor; use getInstance()
	}

	public List findAvailableModules(String expectedID) {

		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint point = registry.getExtensionPoint(EXTENSION_POINT);
		IExtension[] extensions = point.getExtensions();

		ArrayList allAvailableModules = new ArrayList(extensions.length);

		for (int i = 0; i < extensions.length; i++) {
			String moduleID = extensions[i].getUniqueIdentifier();
			if (moduleID == null) // IDs on those extensions are mandatory; if not specified, ignore the extension
				continue;
			moduleID = moduleID.toLowerCase();
			if (expectedID != null && !expectedID.equals(moduleID))
				continue;
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			if (elements.length == 0)
				continue;
			IConfigurationElement element = elements[0]; // only one module is allowed per extension
			if (!STORAGE_MODULE.equals(element.getName())) {
				reportError(SecAuthMessages.unexpectedConfigElement, element.getName(), element, null);
				continue;
			}
			String attribute = element.getAttribute(MODULE_PRIORITY);
			int priority = -1;
			if (attribute != null) {
				priority = Integer.parseInt(attribute);
				if (priority < 0)
					priority = 0;
				if (priority > 10)
					priority = 10;
			}
			String name = extensions[i].getLabel();

			String description = element.getAttribute(MODULE_DESCRIPTION);

			List suppliedHints = null;
			IConfigurationElement[] hints = element.getChildren(HINTS_NAME);
			if (hints.length != 0) {
				suppliedHints = new ArrayList(hints.length);
				for (int j = 0; j < hints.length; j++) {
					String hint = hints[j].getAttribute(HINT_VALUE);
					if (hint != null)
						suppliedHints.add(hint);
				}
			}

			allAvailableModules.add(new ExtStorageModule(moduleID, element, priority, name, description, suppliedHints));
		}

		Collections.sort(allAvailableModules, new Comparator() {
			public int compare(Object o1, Object o2) {
				int p1 = ((ExtStorageModule) o1).priority;
				int p2 = ((ExtStorageModule) o2).priority;
				return p2 - p1;
			}
		});

		return allAvailableModules;
	}

	public PasswordProviderModuleExt findStorageModule(String expectedID) throws StorageException {
		if (expectedID != null)
			expectedID = expectedID.toLowerCase(); // ID is case-insensitive
		synchronized (modules) {
			if (modules.containsKey(expectedID))
				return (PasswordProviderModuleExt) modules.get(expectedID);
		}

		List allAvailableModules = findAvailableModules(expectedID);
		HashSet disabledModules = getDisabledModules();

		for (Iterator i = allAvailableModules.iterator(); i.hasNext();) {
			ExtStorageModule module = (ExtStorageModule) i.next();

			if (expectedID == null && disabledModules != null && disabledModules.contains(module.moduleID))
				continue;

			Object clazz;
			try {
				clazz = module.element.createExecutableExtension(CLASS_NAME);
			} catch (CoreException e) {
				reportError(SecAuthMessages.instantiationFailed, module.element.getAttribute(CLASS_NAME), module.element, e);
				continue;
			}
			if (!(clazz instanceof PasswordProvider))
				continue;

			PasswordProviderModuleExt result = new PasswordProviderModuleExt((PasswordProvider) clazz, module.moduleID);

			// cache the result
			synchronized (modules) {
				if (expectedID == null)
					modules.put(null, result);
				modules.put(module.moduleID, result);
			}

			return result;
		}

		// the secure storage module was not found - error in app's configuration
		String msg;
		if (expectedID == null)
			msg = SecAuthMessages.noSecureStorageModules;
		else
			msg = NLS.bind(SecAuthMessages.noSecureStorageModule, expectedID);
		throw new StorageException(StorageException.NO_SECURE_MODULE, msg);
	}

	private void reportError(String template, String arg, IConfigurationElement element, Throwable e) {
		String supplier = element.getContributor().getName();
		String message = NLS.bind(template, arg, supplier);
		AuthPlugin.getDefault().logError(message, e);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	// Synch local cache with the registry 
	public void added(IExtension[] extensions) {
		clearCaches();
	}

	public void added(IExtensionPoint[] extensionPoints) {
		clearCaches();
	}

	public void removed(IExtension[] extensions) {
		clearCaches();
	}

	public void removed(IExtensionPoint[] extensionPoints) {
		clearCaches();
	}

	/**
	 * Clear whole cache as priorities might have changed after new modules were added.
	 */
	public void clearCaches() {
		synchronized (modules) {
			modules.clear();
			// If module was removed, clear its entry from the password cache.
			// The code below clears all entries for simplicity, in future this
			// can be made more limiting if a scenario exists where module
			// removal/addition is a frequent event.
			SecurePreferencesMapper.clearPasswordCache();
		}
	}

	public boolean isLoggedIn() {
		synchronized (modules) {
			return (modules.size() != 0);
		}
	}

	protected HashSet getDisabledModules() {
		IEclipsePreferences node = new ConfigurationScope().getNode(AuthPlugin.PI_AUTH);
		String tmp = node.get(IStorageConstants.DISABLED_PROVIDERS_KEY, null);
		if (tmp == null || tmp.length() == 0)
			return null;
		HashSet disabledModules = new HashSet();
		String[] disabledProviders = tmp.split(","); //$NON-NLS-1$
		for (int i = 0; i < disabledProviders.length; i++) {
			disabledModules.add(disabledProviders[i]);
		}
		return disabledModules;
	}
}
