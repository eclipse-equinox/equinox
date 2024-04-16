/*******************************************************************************
 * Copyright (c) 2004, 2023 IBM Corporation and others.
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
 *     Gunnar Wagenknecht - Bug 179695 - [prefs] NPE when using Preferences API without a product
 *     Thirumala Reddy Mutchukota, Google Inc - Bug 380859 - [prefs] Inconsistency between DefaultPreferences and InstancePreferences
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.internal.preferences.exchange.IProductPreferencesService;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.BundleDefaultsScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @since 3.0
 */
public class DefaultPreferences extends EclipsePreferences {
	// cache which nodes have been loaded from disk
	private static final Set<String> LOADED_NODES = ConcurrentHashMap.newKeySet();
	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	private static final String KEY_DOUBLE_PREFIX = "%%"; //$NON-NLS-1$
	private static final IPath NL_DIR = IPath.fromOSString("$nl$"); //$NON-NLS-1$

	private static final String PROPERTIES_FILE_EXTENSION = "properties"; //$NON-NLS-1$
	private static Properties productCustomization;
	private static Properties productTranslation;
	private static Properties commandLineCustomization;
	private EclipsePreferences loadLevel;
	private Thread initializingThread;

	// cached values
	private final String qualifier;
	private final int segmentCount;
	private WeakReference<Object> pluginReference;

	static String pluginCustomizationFile = null;

	public DefaultPreferences() {
		this(null, null);
	}

	private DefaultPreferences(EclipsePreferences parent, String name, Object context) {
		this(parent, name);
		this.pluginReference = new WeakReference<>(context);
	}

	private DefaultPreferences(EclipsePreferences parent, String name) {
		super(parent, name);

		if (parent instanceof DefaultPreferences defaultPreferences) {
			this.pluginReference = defaultPreferences.pluginReference;
		}
		// cache the segment count
		String path = absolutePath();
		segmentCount = getSegmentCount(path);
		qualifier = segmentCount > 1 ? getSegment(path, 1) : null; // cache the qualifier
	}

	/*
	 * Apply the values set in the bundle's install directory.
	 *
	 * In Eclipse 2.1 this is equivalent to: /eclipse/plugins/<pluginID>/prefs.ini
	 */
	private void applyBundleDefaults() {
		Bundle bundle = PreferencesOSGiUtils.getDefault().getBundle(name());
		if (bundle == null) {
			return;
		}
		URL url = FileLocator.find(bundle,
				IPath.fromOSString(IPreferencesConstants.PREFERENCES_DEFAULT_OVERRIDE_FILE_NAME), null);
		if (url == null) {
			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages
						.message("Preference default override file not found for bundle: " + bundle.getSymbolicName()); //$NON-NLS-1$
			}
			return;
		}
		URL transURL = FileLocator.find(bundle,
				NL_DIR.append(IPreferencesConstants.PREFERENCES_DEFAULT_OVERRIDE_BASE_NAME)
						.addFileExtension(PROPERTIES_FILE_EXTENSION),
				null);
		if (transURL == null && EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Preference translation file not found for bundle: " + bundle.getSymbolicName()); //$NON-NLS-1$
		}
		applyDefaults(name(), loadProperties(url), loadProperties(transURL));
	}

	/*
	 * Apply the default values as specified in the file as an argument on the
	 * command-line.
	 */
	private void applyCommandLineDefaults() {
		if (commandLineCustomization != null) {
			applyDefaults(null, commandLineCustomization, null);
		}
	}

	/*
	 * If the qualifier is null then the file is of the format: pluginID/key=value
	 * otherwise the file is of the format: key=value
	 */
	private void applyDefaults(String id, Properties defaultValues, Properties translations) {
		for (Enumeration<?> e = defaultValues.keys(); e.hasMoreElements();) {
			String fullKey = (String) e.nextElement();
			String value = defaultValues.getProperty(fullKey);
			if (value == null) {
				continue;
			}
			String localQualifier = id;
			String fullPath = fullKey;
			int firstIndex = fullKey.indexOf(PATH_SEPARATOR);
			if (id == null && firstIndex > 0) {
				localQualifier = fullKey.substring(0, firstIndex);
				fullPath = fullKey.substring(firstIndex, fullKey.length());
			}
			String[] splitPath = decodePath(fullPath);
			String childPath = splitPath[0];
			childPath = makeRelative(childPath);
			String key = splitPath[1];
			if (name().equals(localQualifier)) {
				value = translatePreference(value, translations);
				if (EclipsePreferences.DEBUG_PREFERENCE_SET) {
					PrefsMessages.message("Setting default preference: " //$NON-NLS-1$
							+ IPath.fromOSString(absolutePath()).append(childPath).append(key) + '=' + value);
				}
				((EclipsePreferences) internalNode(childPath, false, null)).internalPut(key, value);
			}
		}
	}

	public IEclipsePreferences node(String childName, Object context) {
		return internalNode(childName, true, context);
	}

	private boolean containsNode(Properties props, IPath path) {
		if (props == null) {
			return false;
		}
		return props.entrySet().stream().filter(e -> e.getValue() != null).anyMatch(e -> {
			String fullKey = (String) e.getKey();
			// remove last segment which stands for key
			IPath nodePath = IPath.fromOSString(fullKey).removeLastSegments(1);
			return path.isPrefixOf(nodePath);
		});
	}

	@Override
	public boolean nodeExists(String path) throws BackingStoreException {
		// use super implementation for empty and absolute paths
		if (path.length() == 0 || path.charAt(0) == IPath.SEPARATOR) {
			return super.nodeExists(path);
		}
		// if the node already exists, nothing more to do
		if (super.nodeExists(path)) {
			return true;
		}
		// if the node does not exist, maybe it has not been loaded yet
		initializeCustomizations();
		// scope based path is a path relative to the "/default" node; this is the path
		// that appears in customizations
		IPath scopeBasedPath = IPath.fromOSString(absolutePath() + PATH_SEPARATOR + path).removeFirstSegments(1);
		return containsNode(productCustomization, scopeBasedPath)
				|| containsNode(commandLineCustomization, scopeBasedPath);
	}

	private static void initializeCustomizations() {
		// prime the cache the first time
		if (productCustomization == null) {
			BundleContext context = Activator.getContext();
			if (context != null) {
				ServiceTracker<?, IProductPreferencesService> productTracker = new ServiceTracker<>(context,
						IProductPreferencesService.class, null);
				productTracker.open();
				IProductPreferencesService productSpecials = productTracker.getService();
				if (productSpecials != null) {
					productCustomization = productSpecials.getProductCustomization();
					productTranslation = productSpecials.getProductTranslation();
				}
				productTracker.close();
			} else {
				PrefsMessages.message("Product-specified preferences called before plugin is started"); //$NON-NLS-1$
			}
			if (productCustomization == null) {
				productCustomization = new Properties();
			}
		}
		if (commandLineCustomization == null) {
			String filename = pluginCustomizationFile;
			if (filename == null) {
				if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
					PrefsMessages.message("Command-line preferences customization file not specified."); //$NON-NLS-1$
				}
			} else {
				if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
					PrefsMessages.message("Using command-line preference customization file: " + filename); //$NON-NLS-1$
				}
				commandLineCustomization = loadProperties(filename);
			}
		}
	}

	/*
	 * Runtime defaults are the ones which are specified in code at runtime.
	 *
	 * In the Eclipse 2.1 world they were the ones which were specified in the
	 * over-ridden Plugin#initializeDefaultPluginPreferences() method.
	 *
	 * In Eclipse 3.0 they are set in the code which is indicated by the extension
	 * to the plug-in default customizer extension point.
	 */
	private void applyRuntimeDefaults() {
		WeakReference<Object> ref = PreferencesService.getDefault().applyRuntimeDefaults(name(), pluginReference);
		if (ref != null) {
			pluginReference = ref;
		}
	}

	/*
	 * Apply the default values as specified by the file in the product extension.
	 *
	 * In Eclipse 2.1 this is equivalent to the plugin_customization.ini file in the
	 * primary feature's plug-in directory.
	 */
	private void applyProductDefaults() {
		if (!productCustomization.isEmpty()) {
			applyDefaults(null, productCustomization, productTranslation);
		}
	}

	@Override
	public void flush() {
		// default values are not persisted
	}

	@Override
	protected IEclipsePreferences getLoadLevel() {
		if (loadLevel == null) {
			if (qualifier == null) {
				return null;
			}
			// Make it relative to this node rather than navigating to it from the root.
			// Walk backwards up the tree starting at this node.
			// This is important to avoid a chicken/egg thing on startup.
			EclipsePreferences node = this;
			for (int i = 2; i < segmentCount; i++) {
				node = (EclipsePreferences) node.parent();
			}
			loadLevel = node;
		}
		return loadLevel;
	}

	@Override
	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new DefaultPreferences(nodeParent, nodeName, context);
	}

	@Override
	protected boolean isAlreadyLoaded(IEclipsePreferences node) {
		return LOADED_NODES.contains(node.name());
	}

	@Override
	protected void load() {
		setInitializingBundleDefaults();
		try {
			applyRuntimeDefaults();
			applyBundleDefaults();
		} finally {
			clearInitializingBundleDefaults();
		}
		initializeCustomizations();
		applyProductDefaults();
		applyCommandLineDefaults();
	}

	@Override
	protected String internalPut(String key, String newValue) {
		// set the value in this node
		String result = super.internalPut(key, newValue);

		// if we are setting the bundle defaults, then set the corresponding value in
		// the bundle_defaults scope
		if (isInitializingBundleDefaults()) {
			String relativePath = getScopeRelativePath(absolutePath());
			if (relativePath != null) {
				IEclipsePreferences rootNode = PreferencesService.getDefault().getRootNode();
				Preferences node = rootNode.node(BundleDefaultsScope.SCOPE).node(relativePath);
				node.put(key, newValue);
			}
		}
		return result;
	}

	/*
	 * Set that we are in the middle of initializing the bundle defaults. This is
	 * stored on the load level so we know where to look when we are setting values
	 * on sub-nodes.
	 */
	private void setInitializingBundleDefaults() {
		IEclipsePreferences node = getLoadLevel();
		if (node instanceof DefaultPreferences loader) {
			loader.initializingThread = Thread.currentThread();
		}
	}

	/*
	 * Clear the bit saying we are in the middle of initializing the bundle
	 * defaults. This is stored on the load level so we know where to look when we
	 * are setting values on sub-nodes.
	 */
	private void clearInitializingBundleDefaults() {
		IEclipsePreferences node = getLoadLevel();
		if (node instanceof DefaultPreferences loader) {
			loader.initializingThread = null;
		}
	}

	/*
	 * Are we in the middle of initializing defaults from the bundle initializer or
	 * found in the bundle itself? Look on the load level in case we are in a
	 * sub-node.
	 */
	private boolean isInitializingBundleDefaults() {
		IEclipsePreferences node = getLoadLevel();
		if (node instanceof DefaultPreferences loader) {
			return loader.initializingThread == Thread.currentThread();
		}
		return false;
	}

	/*
	 * Return a path which is relative to the scope of this node. e.g.
	 * com.example.foo for /instance/com.example.foo
	 */
	protected static String getScopeRelativePath(String absolutePath) {
		// shouldn't happen but handle empty or root
		if (absolutePath.length() < 2) {
			return null;
		}
		int index = absolutePath.indexOf('/', 1);
		if (index == -1 || index + 1 >= absolutePath.length()) {
			return null;
		}
		return absolutePath.substring(index + 1);
	}

	private Properties loadProperties(URL url) {
		Properties result = new Properties();
		if (url == null) {
			return result;
		}
		try (InputStream input = url.openStream()) {
			result.load(input);
		} catch (IOException | IllegalArgumentException e) {
			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages.message("Problem opening stream to preference customization file: " + url); //$NON-NLS-1$
				e.printStackTrace();
			}
		}
		return result;
	}

	private static Properties loadProperties(String filename) {
		Properties result = new Properties();
		try (InputStream input = new FileInputStream(filename)) {
			result.load(input);
		} catch (FileNotFoundException e) {
			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages.message("Preference customization file not found: " + filename); //$NON-NLS-1$
			}
		} catch (IOException | IllegalArgumentException e) {
			RuntimeLog.log(Status.error(NLS.bind(PrefsMessages.preferences_loadException, filename), e));
		}
		return result;
	}

	@Override
	protected void loaded() {
		LOADED_NODES.add(name());
	}

	@Override
	public void sync() {
		// default values are not persisted
	}

	/**
	 * Takes a preference value and a related resource bundle and returns the
	 * translated version of this value (if one exists).
	 */
	private String translatePreference(String origValue, Properties props) {
		if (props == null || origValue.startsWith(KEY_DOUBLE_PREFIX)) {
			return origValue;
		}
		if (origValue.startsWith(KEY_PREFIX)) {
			String value = origValue.trim();
			int ix = value.indexOf(" "); //$NON-NLS-1$
			String key = ix == -1 ? value.substring(1) : value.substring(1, ix);
			String dflt = ix == -1 ? value : value.substring(ix + 1);
			return props.getProperty(key, dflt);
		}
		return origValue;
	}
}
