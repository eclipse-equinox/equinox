/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.preferences;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.internal.preferences.exchange.ILegacyPreferences;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Class which handles all registry-related work for the preferences. This code
 * has been separated into a separate class to ensure that the preferences will
 * be able to run without the registry being present.
 */
@SuppressWarnings("deprecation") // ILegacyPreferences
public class PreferenceServiceRegistryHelper implements IRegistryChangeListener {

	private static final String ELEMENT_INITIALIZER = "initializer"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTRIBUTE_STORAGE = "storage"; //$NON-NLS-1$
	private static final String ELEMENT_SCOPE = "scope"; //$NON-NLS-1$
	private static final String ELEMENT_MODIFIER = "modifier"; //$NON-NLS-1$
	// Store this around for performance
	private static final IExtension[] EMPTY_EXTENSION_ARRAY = new IExtension[0];
	private static final Map<String, Object> SCOPE_REGISTRY = new ConcurrentHashMap<>();
	private ListenerList<PreferenceModifyListener> modifyListeners;
	private final PreferencesService service;
	private final IExtensionRegistry registry;

	/*
	 * Constructor for the class.
	 */
	public PreferenceServiceRegistryHelper(PreferencesService service, Object registryObject) {
		super();
		this.service = service;
		this.registry = (IExtensionRegistry) registryObject;
		initializeScopes();
		registry.addRegistryChangeListener(this);
	}

	void stop() {
		registry.removeRegistryChangeListener(this);
	}

	/*
	 * Add the given configuration element into our list of preference modify
	 * listeners.
	 */
	private void addModifyListener(IConfigurationElement element) {
		String key = element.getAttribute(ATTRIBUTE_CLASS);
		if (key == null) {
			String message = NLS.bind(PrefsMessages.preferences_missingClassAttribute,
					element.getDeclaringExtension().getUniqueIdentifier());
			RuntimeLog.log(Status.error(message));
			return;
		}
		try {
			Object listener = element.createExecutableExtension(ATTRIBUTE_CLASS);
			if (!(listener instanceof PreferenceModifyListener modifyListener)) {
				RuntimeLog.log(Status.error(PrefsMessages.preferences_classCastListener));
				return;
			}
			modifyListeners.add(modifyListener);
		} catch (CoreException e) {
			RuntimeLog.log(e.getStatus());
		}
	}

	/*
	 * Apply the runtime defaults for the bundle with the given name. Check to see
	 * if there is a preference initializer registered and if so, then run it.
	 * Otherwise call the legacy Plugin preference initialization code.
	 */
	public WeakReference<Object> applyRuntimeDefaults(String name, WeakReference<Object> pluginReference) {
		IExtension[] extensions = getPrefExtensions();
		if (extensions.length == 0) {
			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages.message("Skipping runtime default preference customization."); //$NON-NLS-1$
			}
			return null;
		}
		boolean foundInitializer = false;
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				if (ELEMENT_INITIALIZER.equals(element.getName())) {
					if (name.equals(element.getContributor().getName())) {
						if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
							IExtension theExtension = element.getDeclaringExtension();
							String extensionNamespace = theExtension.getContributor().getName();
							Bundle underlyingBundle = PreferencesOSGiUtils.getDefault().getBundle(extensionNamespace);
							String ownerName = underlyingBundle != null //
									? underlyingBundle.getSymbolicName()
									: extensionNamespace;
							PrefsMessages
									.message("Running default preference customization as defined by: " + ownerName); //$NON-NLS-1$
						}
						runInitializer(element);
						// don't return yet in case we have multiple initializers registered
						foundInitializer = true;
					}
				}
			}
		}
		if (foundInitializer) {
			return null;
		}
		// TODO this means that we don't call the legacy Plugin code if the registry
		// isn't present.
		// I don't think this is the desired behaviour

		// Do legacy plugin preference initialization
		Object plugin = pluginReference.get();
		ILegacyPreferences initService = PreferencesOSGiUtils.getDefault().getLegacyPreferences();
		if (initService != null)
			plugin = initService.init(plugin, name);
		return new WeakReference<>(plugin);
	}

	/*
	 * Create a new child node with the given parent. Look up the name in the
	 * registry as it may map to a configuration element. This is done for lazy
	 * initialization of user-contributed scopes.
	 */
	public IEclipsePreferences createNode(RootPreferences parent, String name) {
		IScope scope = null;
		Object value = SCOPE_REGISTRY.get(name);
		if (value instanceof IConfigurationElement element) {
			// did the user define their own class?
			if (element.getAttribute(ATTRIBUTE_CLASS) != null) {
				try {
					scope = (IScope) element.createExecutableExtension(ATTRIBUTE_CLASS);
					SCOPE_REGISTRY.put(name, scope);
				} catch (ClassCastException e) {
					RuntimeLog.log(Status.error(PrefsMessages.preferences_classCastScope, e));
					return new EclipsePreferences(parent, name);
				} catch (CoreException e) {
					RuntimeLog.log(e.getStatus());
					return new EclipsePreferences(parent, name);
				}
			} else if (element.getAttribute(ATTRIBUTE_STORAGE) != null) {
				// or if they defined a storage class then use EclipsePreferences to model the
				// prefs.
				try {
					Object storage = element.createExecutableExtension(ATTRIBUTE_STORAGE);
					ScopeDescriptor descriptor = new ScopeDescriptor((AbstractPreferenceStorage) storage);
					EclipsePreferences result = new EclipsePreferences(parent, name);
					result.setDescriptor(descriptor);
					return result;
				} catch (ClassCastException e) {
					RuntimeLog.log(Status.error(PrefsMessages.preferences_classCastStorage, e));
					return new EclipsePreferences(parent, name);
				} catch (CoreException e) {
					RuntimeLog.log(e.getStatus());
					return new EclipsePreferences(parent, name);
				}
			}
		} else {
			scope = (IScope) value;
		}
		return scope.create(parent, name);
	}

	/*
	 * Return a list of the preference modify listeners. They are called during
	 * preference import and given the chance to modify the imported tree.
	 */
	public ListenerList<PreferenceModifyListener> getModifyListeners() {
		if (modifyListeners == null) {
			modifyListeners = new ListenerList<>();
			IExtension[] extensions = getPrefExtensions();
			for (IExtension extension : extensions) {
				IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					if (ELEMENT_MODIFIER.equalsIgnoreCase(element.getName())) {
						addModifyListener(element);
					}
				}
			}
		}
		return modifyListeners;
	}

	/*
	 * Return a list of the extensions which are plugged into the preference
	 * extension point.
	 */
	private IExtension[] getPrefExtensions() {
		IExtension[] extensionsOld = EMPTY_EXTENSION_ARRAY;
		IExtension[] extensionsNew = EMPTY_EXTENSION_ARRAY;
		// "old"
		IExtensionPoint pointOld = registry.getExtensionPoint(IPreferencesConstants.RUNTIME_NAME,
				IPreferencesConstants.PT_PREFERENCES);
		if (pointOld != null)
			extensionsOld = pointOld.getExtensions();
		// "new"
		IExtensionPoint pointNew = registry.getExtensionPoint(IPreferencesConstants.PREFERS_NAME,
				IPreferencesConstants.PT_PREFERENCES);
		if (pointNew != null)
			extensionsNew = pointNew.getExtensions();
		// combine
		IExtension[] extensions = new IExtension[extensionsOld.length + extensionsNew.length];
		System.arraycopy(extensionsOld, 0, extensions, 0, extensionsOld.length);
		System.arraycopy(extensionsNew, 0, extensions, extensionsOld.length, extensionsNew.length);

		if (extensions.length == 0 && EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("No extensions for org.eclipse.core.contenttype."); //$NON-NLS-1$
		}
		return extensions;
	}

	/*
	 * See who is plugged into the extension point.
	 */
	private void initializeScopes() {
		IExtension[] extensions = getPrefExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				if (ELEMENT_SCOPE.equalsIgnoreCase(element.getName())) {
					scopeAdded(element);
				}
			}
		}
	}

	@Override
	public void registryChanged(IRegistryChangeEvent event) {
		IExtensionDelta[] deltasOld = event.getExtensionDeltas(IPreferencesConstants.RUNTIME_NAME,
				IPreferencesConstants.PT_PREFERENCES);
		IExtensionDelta[] deltasNew = event.getExtensionDeltas(IPreferencesConstants.PREFERS_NAME,
				IPreferencesConstants.PT_PREFERENCES);
		IExtensionDelta[] deltas = new IExtensionDelta[deltasOld.length + deltasNew.length];
		System.arraycopy(deltasOld, 0, deltas, 0, deltasOld.length);
		System.arraycopy(deltasNew, 0, deltas, deltasOld.length, deltasNew.length);

		if (deltas.length == 0)
			return;
		// dynamically adjust the registered scopes
		for (IExtensionDelta delta : deltas) {
			IConfigurationElement[] elements = delta.getExtension().getConfigurationElements();
			for (IConfigurationElement element : elements) {
				switch (delta.getKind()) {
				case IExtensionDelta.ADDED:
					if (ELEMENT_SCOPE.equalsIgnoreCase(element.getName())) {
						scopeAdded(element);
					}
					break;
				case IExtensionDelta.REMOVED:
					String scope = element.getAttribute(ATTRIBUTE_NAME);
					if (scope != null)
						scopeRemoved(scope);
					break;
				}
			}
		}
		// initialize the preference modify listeners
		modifyListeners = null;
	}

	/*
	 * Run the preference initializer as specified by the given configuration
	 * element.
	 */
	private void runInitializer(IConfigurationElement element) {
		try {
			final AbstractPreferenceInitializer initializer = (AbstractPreferenceInitializer) element
					.createExecutableExtension(ATTRIBUTE_CLASS);
			SafeRunner.run(initializer::initializeDefaultPreferences);
		} catch (ClassCastException e) {
			RuntimeLog.log(Status.error(PrefsMessages.preferences_invalidExtensionSuperclass, e));
		} catch (CoreException e) {
			RuntimeLog.log(e.getStatus());
		}
	}

	/*
	 * A preference scope defined by the given element was added to the extension
	 * registry. Add it to our registry and make it a child of the root.
	 */
	private void scopeAdded(IConfigurationElement element) {
		String key = element.getAttribute(ATTRIBUTE_NAME);
		if (key == null) {
			String message = NLS.bind(PrefsMessages.preferences_missingScopeAttribute,
					element.getDeclaringExtension().getUniqueIdentifier());
			RuntimeLog.log(Status.warning(message));
			return;
		}
		SCOPE_REGISTRY.put(key, element);
		((RootPreferences) service.getRootNode()).addChild(key, null);
	}

	/*
	 * A preference scope with the given name was removed from the extension
	 * registry. Remove the node and its children from the preference tree.
	 */
	private void scopeRemoved(String key) {
		RootPreferences rootPreferences = (RootPreferences) service.getRootNode();
		IEclipsePreferences node = (IEclipsePreferences) rootPreferences.getNode(key, false);
		if (node != null) {
			rootPreferences.removeNode(node);
		} else {
			rootPreferences.removeNode(key);
		}
		SCOPE_REGISTRY.remove(key);
	}

}
