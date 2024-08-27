/*******************************************************************************
 * Copyright (c) 2004, 2024 IBM Corporation and others.
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
 *     Semion Chichelnitsky (semion@il.ibm.com) - bug 208564
 *     Jan-Ove Weichel (janove.weichel@vogella.com) - bug 474359
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.0
 */
public class PreferencesService implements IPreferencesService {
	private static final String MATCH_TYPE_PREFIX = "prefix"; //$NON-NLS-1$

	// the order of search scopes when people don't have a specific order set
	private static List<String> DEFAULT_DEFAULT_LOOKUP_ORDER = List.of( //
			InstanceScope.SCOPE, //
			ConfigurationScope.SCOPE, //
			UserScope.SCOPE, //
			DefaultScope.SCOPE);
	private static final char EXPORT_ROOT_PREFIX = '!';
	private static final char BUNDLE_VERSION_PREFIX = '@';
	private static final float EXPORT_VERSION = 3;
	private static final String VERSION_KEY = "file_export_version"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static PreferencesService instance;
	private static final RootPreferences root = new RootPreferences();
	private static final Map<String, String[]> DEFAULTS_REGISTRY = new ConcurrentHashMap<>();
	private Object registryHelper = null;
	private final Map<String, EclipsePreferences> defaultScopes = new HashMap<>();

	/*
	 * Return the instance.
	 */
	public static PreferencesService getDefault() {
		if (instance == null) {
			instance = new PreferencesService();
		}
		return instance;
	}

	private PreferencesService() {
		initializeDefaultScope(BundleDefaultsScope.SCOPE, new BundleDefaultPreferences());
		initializeDefaultScope(DefaultScope.SCOPE, new DefaultPreferences());
		initializeDefaultScope(InstanceScope.SCOPE, new InstancePreferences());
		initializeDefaultScope(ConfigurationScope.SCOPE, new ConfigurationPreferences());
		initializeDefaultScope(UserScope.SCOPE, new UserPreferences());
	}

	@Override
	public void applyPreferences(IEclipsePreferences tree, IPreferenceFilter[] filters) throws CoreException {
		if (filters == null || filters.length == 0) {
			return;
		}
		try {
			internalApply(tree, filters);
		} catch (BackingStoreException e) {
			throw new CoreException(Status.error(PrefsMessages.preferences_applyProblems, e));
		}
		// save the preferences
		try {
			getRootNode().node(tree.absolutePath()).flush();
		} catch (BackingStoreException e) {
			throw new CoreException(Status.error(PrefsMessages.preferences_saveProblems, e));
		}
	}

	@Override
	public IStatus applyPreferences(IExportedPreferences preferences) throws CoreException {
		// TODO investigate refactoring to merge with new #apply(IEclipsePreferences,
		// IPreferenceFilter[]) APIs
		if (preferences == null) {
			throw new IllegalArgumentException();
		} else if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message(
					"Applying exported preferences: " + ((ExportedPreferences) preferences).toDeepDebugString()); //$NON-NLS-1$
		}
		final MultiStatus result = new MultiStatus(PrefsMessages.OWNER_NAME, IStatus.OK,
				PrefsMessages.preferences_applyProblems, null);

		IEclipsePreferences modifiedNode = firePreApplyEvent(preferences);
		try {
			// start by visiting the root
			modifiedNode.accept(node -> { // create a visitor to apply the given set of preferences
				IEclipsePreferences globalNode;
				if (node.parent() == null) {
					globalNode = root;
				} else {
					globalNode = (IEclipsePreferences) root.node(node.absolutePath());
				}
				ExportedPreferences epNode = (ExportedPreferences) node;

				// if this node is an export root then we need to remove
				// it from the global preferences before continuing.
				boolean removed = false;
				if (epNode.isExportRoot()) {
					if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
						PrefsMessages.message("Found export root: " + epNode.absolutePath()); //$NON-NLS-1$
					}
					// TODO should only have to do this if any of my children have properties to set
					globalNode.removeNode();
					removed = true;
				}

				// iterate over the preferences in this node and set them
				// in the global space.
				String[] keys = epNode.keys();

				// if this node was removed then we need to create a new one
				if (removed) {
					globalNode = (IEclipsePreferences) root.node(node.absolutePath());
				}
				// the list for properties to remove
				List<String> propsToRemove = new ArrayList<>(Arrays.asList(globalNode.keys()));
				if (keys.length > 0) {
					for (String key : keys) {
						// preferences that are not in the applied node will be removed
						propsToRemove.remove(key);
						String value = node.get(key, null);
						if (value != null) {
							if (EclipsePreferences.DEBUG_PREFERENCE_SET) {
								PrefsMessages
										.message("Setting: " + globalNode.absolutePath() + '/' + key + '=' + value); //$NON-NLS-1$
							}
							globalNode.put(key, value);
						}
					}
				}
				if (!propsToRemove.isEmpty() && !(globalNode instanceof EclipsePreferences)) {
					// intern strings we import because some people in their property change
					// listeners use identity instead of equals. See bug 20193 and 20534.
					propsToRemove.replaceAll(String::intern);
				}
				for (String keyToRemove : propsToRemove) {
					if (EclipsePreferences.DEBUG_PREFERENCE_SET) {
						PrefsMessages.message("Removing: " + globalNode.absolutePath() + '/' + keyToRemove); //$NON-NLS-1$
					}
					globalNode.remove(keyToRemove);
				}

				// keep visiting children
				return true;
			});
		} catch (BackingStoreException e) {
			throw new CoreException(Status.error(PrefsMessages.preferences_applyProblems, e));
		}

		// save the preferences
		try {
			getRootNode().node(modifiedNode.absolutePath()).flush();
		} catch (BackingStoreException e) {
			throw new CoreException(Status.error(PrefsMessages.preferences_saveProblems, e));
		}

		if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Current list of all settings: " + ((EclipsePreferences) getRootNode()).toDeepDebugString()); //$NON-NLS-1$
		}
		return result;
	}

	private boolean containsKeys(IEclipsePreferences aRoot) throws BackingStoreException {
		final boolean[] result = new boolean[] { false };
		aRoot.accept(node -> {
			if (node.keys().length != 0) {
				result[0] = true;
			}
			return !result[0];
		});
		return result[0];
	}

	/*
	 * Convert the given properties file from legacy format to one which is Eclipse
	 * 3.0 compliant.
	 *
	 * Convert the plug-in version indicator entries to export roots.
	 */
	private Properties convertFromLegacy(Properties properties) {
		Properties result = new Properties();
		String prefix = IPath.SEPARATOR + InstanceScope.SCOPE + IPath.SEPARATOR;
		for (Entry<?, ?> entry : properties.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			if (value != null) {
				int index = key.indexOf(IPath.SEPARATOR);
				if (index == -1) {
					result.put(BUNDLE_VERSION_PREFIX + key, value);
					result.put(EXPORT_ROOT_PREFIX + prefix + key, EMPTY_STRING);
				} else {
					String path = key.substring(0, index);
					key = key.substring(index + 1);
					result.put(EclipsePreferences.encodePath(prefix + path, key), value);
				}
			}
		}
		return result;
	}

	/*
	 * Convert the given properties file into a node hierarchy suitable for
	 * importing.
	 */
	private IExportedPreferences convertFromProperties(Properties properties) {
		IExportedPreferences result = ExportedPreferences.newRoot();
		for (Entry<?, ?> entry : properties.entrySet()) {
			String path = (String) entry.getKey();
			String value = (String) entry.getValue();
			if (path.charAt(0) == EXPORT_ROOT_PREFIX) {
				ExportedPreferences current = (ExportedPreferences) result.node(path.substring(1));
				current.setExportRoot();
			} else if (path.charAt(0) == BUNDLE_VERSION_PREFIX) {
				ExportedPreferences current = (ExportedPreferences) result.node(InstanceScope.SCOPE)
						.node(path.substring(1));
				current.setVersion(value);
			} else {
				String[] decoded = EclipsePreferences.decodePath(path);
				path = decoded[0] == null ? EMPTY_STRING : decoded[0];
				ExportedPreferences current = (ExportedPreferences) result.node(path);
				String key = decoded[1];
				current.put(key, value);
			}
		}
		if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Converted preferences file to IExportedPreferences tree: " //$NON-NLS-1$
					+ ((ExportedPreferences) result).toDeepDebugString());
		}
		return result;
	}

	/*
	 * excludesList is guaranteed not to be null
	 */
	private SortedProperties convertToProperties(IEclipsePreferences preferences, final String[] excludesList)
			throws BackingStoreException {
		final SortedProperties result = new SortedProperties();
		final int baseLength = preferences.absolutePath().length();
		// start by visiting the root that we were passed in
		preferences.accept(node -> { // create a visitor to do the export
			// don't store defaults
			String absolutePath = node.absolutePath();
			String scope = getScope(absolutePath);
			if (DefaultScope.SCOPE.equals(scope)) {
				return false;
			}
			String path = absolutePath.length() <= baseLength ? EMPTY_STRING
					: EclipsePreferences.makeRelative(absolutePath.substring(baseLength));
			// check the excludes list to see if this node should be considered
			for (String exclude : excludesList) {
				String exclusion = EclipsePreferences.makeRelative(exclude);
				if (path.startsWith(exclusion)) {
					return false;
				}
			}
			boolean needToAddVersion = InstanceScope.SCOPE.equals(scope);
			// check the excludes list for each preference
			String[] keys = node.keys();
			for (String key : keys) {
				boolean ignore = Arrays.stream(excludesList).map(EclipsePreferences::makeRelative)
						.anyMatch(EclipsePreferences.encodePath(path, key)::startsWith);
				if (!ignore) {
					String value = node.get(key, null);
					if (value != null) {
						if (needToAddVersion) {
							String bundle = getBundleName(absolutePath);
							if (bundle != null) {
								String version = getBundleVersion(bundle);
								if (version != null) {
									result.put(BUNDLE_VERSION_PREFIX + bundle, version);
								}
							}
							needToAddVersion = false;
						}
						result.put(EclipsePreferences.encodePath(absolutePath, key), value);
					}
				}
			}
			return true;
		});
		// return the properties object
		return result;
	}

	/**
	 * Copy key/value pairs from the source to the destination. If the key list is
	 * null then copy all associations.
	 *
	 * If the depth is 0, then this operation is performed only on the source node.
	 * Otherwise it is performed on the source node's subtree.
	 *
	 * @param depth one of 0 or -1
	 */
	void copyFromTo(Preferences source, Preferences destination, String[] keys, int depth)
			throws BackingStoreException {
		String[] keysToCopy = keys == null ? source.keys() : keys;
		for (String key : keysToCopy) {
			String value = source.get(key, null);
			if (value != null) {
				destination.put(key, value);
			}
		}
		if (depth == 0) {
			return;
		}
		String[] children = source.childrenNames();
		for (String child : children) {
			copyFromTo(source.node(child), destination.node(child), keys, depth);
		}
	}

	public WeakReference<Object> applyRuntimeDefaults(String name, WeakReference<Object> pluginReference) {
		if (registryHelper == null) {
			return null;
		}
		return ((PreferenceServiceRegistryHelper) registryHelper).applyRuntimeDefaults(name, pluginReference);
	}

	private void initializeDefaultScope(String scope, EclipsePreferences preferences) {
		defaultScopes.put(scope, preferences);
		root.addChild(scope, null);
	}

	public IEclipsePreferences createNode(String key) {
		IScope scope = defaultScopes.get(key);
		if (scope == null) {
			if (registryHelper == null) {
				return new EclipsePreferences(root, key);
			}
			return ((PreferenceServiceRegistryHelper) registryHelper).createNode(root, key);
		}
		return scope.create(root, key);
	}

	@Override
	public void exportPreferences(IEclipsePreferences node, IPreferenceFilter[] filters, OutputStream stream)
			throws CoreException {
		if (filters == null || filters.length == 0) {
			return;
		}
		try {
			internalExport(node, filters, stream);
		} catch (BackingStoreException e) {
			throw new CoreException(Status.error(PrefsMessages.preferences_exportProblems, e));
		}
	}

	@Override
	public IStatus exportPreferences(IEclipsePreferences node, OutputStream output, String[] excludesList)
			throws CoreException {
		// TODO investigate refactoring to merge with new #export(IEclipsePreferences,
		// IPreferenceFilter[]) APIs
		if (node == null || output == null) {
			throw new IllegalArgumentException();
		}
		SortedProperties properties = null;
		if (excludesList == null) {
			excludesList = new String[0];
		}
		try {
			properties = convertToProperties(node, excludesList);
			if (properties.isEmpty()) {
				return Status.OK_STATUS;
			}
			properties.put(VERSION_KEY, Float.toString(EXPORT_VERSION));
			properties.put(EXPORT_ROOT_PREFIX + node.absolutePath(), EMPTY_STRING);
		} catch (BackingStoreException e) {
			throw new CoreException(Status.error(e.getMessage(), e));
		}
		try {
			properties.store(output, null);
		} catch (IOException e) {
			throw new CoreException(Status.error(PrefsMessages.preferences_exportProblems, e));
		}
		return Status.OK_STATUS;
	}

	/*
	 * Give clients a chance to modify the tree before it is applied globally
	 */
	private IEclipsePreferences firePreApplyEvent(IEclipsePreferences tree) {
		if (registryHelper == null) {
			return tree;
		}
		final IEclipsePreferences[] result = new IEclipsePreferences[] { tree };
		ListenerList<PreferenceModifyListener> listeners = ((PreferenceServiceRegistryHelper) registryHelper)
				.getModifyListeners();
		for (final PreferenceModifyListener listener : listeners) {
			SafeRunner.run(() -> result[0] = listener.preApply(result[0]));
		}
		return result[0];
	}

	@Override
	public String get(String key, String defaultValue, Preferences[] nodes) {
		if (nodes == null) {
			return defaultValue;
		}
		for (Preferences node : nodes) {
			if (node != null) {
				String result = node.get(key, null);
				if (result != null) {
					return result;
				}
			}
		}
		return defaultValue;
	}

	@Override
	public boolean getBoolean(String qualifier, String key, boolean defaultValue, IScopeContext[] scopes) {
		String result = get(EclipsePreferences.decodePath(key)[1], null, getNodes(qualifier, key, scopes));
		return result == null ? defaultValue : Boolean.parseBoolean(result);
	}

	/*
	 * Return the name of the bundle from the given path. It is assumed that that
	 * path is: - absolute - in the instance scope
	 */
	String getBundleName(String path) {
		if (path.length() == 0 || path.charAt(0) != IPath.SEPARATOR) {
			return null;
		}
		int first = path.indexOf(IPath.SEPARATOR, 1);
		if (first == -1) {
			return null;
		}
		int second = path.indexOf(IPath.SEPARATOR, first + 1);
		return second == -1 ? path.substring(first + 1) : path.substring(first + 1, second);
	}

	/*
	 * Return the version for the bundle with the given name. Return null if it is
	 * not known or there is a problem.
	 */
	String getBundleVersion(String bundleName) {
		Bundle bundle = PreferencesOSGiUtils.getDefault().getBundle(bundleName);
		if (bundle != null) {
			Object version = bundle.getHeaders(EMPTY_STRING).get(Constants.BUNDLE_VERSION);
			if (version instanceof String versionString) {
				return versionString;
			}
		}
		return null;
	}

	@Override
	public byte[] getByteArray(String qualifier, String key, byte[] defaultValue, IScopeContext[] scopes) {
		String result = get(EclipsePreferences.decodePath(key)[1], null, getNodes(qualifier, key, scopes));
		return result == null ? defaultValue : Base64.decode(result.getBytes());
	}

	@Override
	public String[] getDefaultLookupOrder(String qualifier, String key) {
		return DEFAULTS_REGISTRY.get(getRegistryKey(qualifier, key));
	}

	@Override
	public double getDouble(String qualifier, String key, double defaultValue, IScopeContext[] scopes) {
		String value = get(EclipsePreferences.decodePath(key)[1], null, getNodes(qualifier, key, scopes));
		if (value == null) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public float getFloat(String qualifier, String key, float defaultValue, IScopeContext[] scopes) {
		String value = get(EclipsePreferences.decodePath(key)[1], null, getNodes(qualifier, key, scopes));
		if (value == null) {
			return defaultValue;
		}
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public int getInt(String qualifier, String key, int defaultValue, IScopeContext[] scopes) {
		String value = get(EclipsePreferences.decodePath(key)[1], null, getNodes(qualifier, key, scopes));
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public long getLong(String qualifier, String key, long defaultValue, IScopeContext[] scopes) {
		String value = get(EclipsePreferences.decodePath(key)[1], null, getNodes(qualifier, key, scopes));
		if (value == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public String[] getLookupOrder(String qualifier, String key) {
		String[] order = getDefaultLookupOrder(qualifier, key);
		// if there wasn't an exact match based on both qualifier and simple name
		// then do a lookup based only on the qualifier
		if (order == null && key != null) {
			order = getDefaultLookupOrder(qualifier, null);
		}
		if (order == null) {
			order = DEFAULT_DEFAULT_LOOKUP_ORDER.toArray(String[]::new); // prevent mutations of the original
		}
		return order;
	}

	private Preferences[] getNodes(final String qualifier, String key, final IScopeContext[] contexts) {
		String[] order = getLookupOrder(qualifier, key);
		final String childPath = EclipsePreferences.makeRelative(EclipsePreferences.decodePath(key)[0]);
		final List<Preferences> result = new ArrayList<>();
		for (String scopeString : order) {
			AtomicReference<IllegalStateException> error = new AtomicReference<>();
			SafeRunner.run(new ISafeRunnable() {

				private IScopeContext context;

				@Override
				public void run() throws Exception {
					boolean found = false;
					for (int j = 0; contexts != null && j < contexts.length; j++) {
						context = contexts[j];
						if (context != null && context.getName().equals(scopeString)) {
							Preferences node = context.getNode(qualifier);
							if (node != null) {
								found = true;
								if (childPath != null) {
									node = node.node(childPath);
								}
								result.add(node);
							}
						}
					}
					if (!found) {
						Preferences node = getRootNode().node(scopeString).node(qualifier);
						if (childPath != null) {
							node = node.node(childPath);
						}
						result.add(node);
					}
				}

				@Override
				public void handleException(Throwable exception) {
					// See bug 514333 and
					// org.eclipse.core.internal.runtime.DataArea.assertLocationInitialized()
					// If we see an IllegalStateException flying and the explicit init is required,
					// we should not continue
					if (context instanceof InstanceScope && exception instanceof IllegalStateException
							&& Boolean.getBoolean("osgi.dataAreaRequiresExplicitInit")) { //$NON-NLS-1$
						error.set((IllegalStateException) exception);
					} else {
						RuntimeLog.log(Status.error(PrefsMessages.preferences_contextError, exception));
					}
				}
			});

			IllegalStateException illegalState = error.get();
			if (illegalState != null) {
				// See bug 514333: don't allow clients to read (empty) instance prefs if the
				// instance area is not initialized yet
				throw illegalState;
			}
		}
		return result.toArray(new Preferences[result.size()]);
	}

	/*
	 * Convert the given qualifier and key into a key to use in the look-up
	 * registry.
	 */
	private String getRegistryKey(String qualifier, String key) {
		if (qualifier == null) {
			throw new IllegalArgumentException();
		} else if (key == null) {
			return qualifier;
		}
		return qualifier + '/' + key;
	}

	@Override
	public IEclipsePreferences getRootNode() {
		return root;
	}

	/*
	 * Return the string which is the scope for the given path. Return the empty
	 * string if it cannot be determined.
	 */
	String getScope(String path) {
		if (path == null || path.length() == 0) {
			return EMPTY_STRING;
		}
		int startIndex = path.indexOf(IPath.SEPARATOR);
		if (startIndex == -1) {
			return path;
		}
		if (path.length() == 1) {
			return EMPTY_STRING;
		}
		int endIndex = path.indexOf(IPath.SEPARATOR, startIndex + 1);
		if (endIndex == -1) {
			endIndex = path.length();
		}
		return path.substring(startIndex + 1, endIndex);
	}

	@Override
	public String getString(String qualifier, String key, String defaultValue, IScopeContext[] scopes) {
		return get(EclipsePreferences.decodePath(key)[1], defaultValue, getNodes(qualifier, key, scopes));
	}

	@Override
	public IStatus importPreferences(InputStream input) throws CoreException {
		if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Importing preferences..."); //$NON-NLS-1$
		}
		return applyPreferences(readPreferences(input));
	}

	/**
	 * Filter the given tree so it only contains values which apply to the specified
	 * filters then apply the resulting tree to the main preference tree.
	 */
	private void internalApply(IEclipsePreferences tree, IPreferenceFilter[] filters) throws BackingStoreException {
		List<IEclipsePreferences> trees = new ArrayList<>();
		for (IPreferenceFilter filter : filters) {
			trees.add(trimTree(tree, filter));
		}
		// merge the union of the matching filters
		IEclipsePreferences toApply = mergeTrees(trees.toArray(new IEclipsePreferences[trees.size()]));

		// fire an event to give people a chance to modify the tree
		toApply = firePreApplyEvent(toApply);

		// actually apply the settings
		toApply.accept(node -> {
			String[] keys = node.keys();
			if (keys.length != 0) {
				copyFromTo(node, getRootNode().node(node.absolutePath()), keys, 0);
			}
			return true;
		});
	}

	/**
	 * Take the preference tree and trim it so it only holds values applying to the
	 * given filters. Then export the resulting tree to the given output stream.
	 */
	private void internalExport(IEclipsePreferences node, IPreferenceFilter[] filters, OutputStream output)
			throws BackingStoreException, CoreException {
		List<IEclipsePreferences> trees = new ArrayList<>();
		for (IPreferenceFilter filter : filters) {
			trees.add(trimTree(node, filter));
		}
		IEclipsePreferences toExport = mergeTrees(trees.toArray(new IEclipsePreferences[trees.size()]));
		exportPreferences(toExport, output, (String[]) null);
	}

	/*
	 * Return true if the given tree contains information that the specified filter
	 * is interested in, and false otherwise.
	 */
	private boolean internalMatches(IEclipsePreferences tree, IPreferenceFilter filter) throws BackingStoreException {
		String[] scopes = filter.getScopes();
		if (scopes == null) {
			throw new IllegalArgumentException();
		}
		String treePath = tree.absolutePath();
		// see if this node is applicable by going over all our scopes
		for (String scope : scopes) {
			Map<String, PreferenceFilterEntry[]> mapping = filter.getMapping(scope);
			// if the mapping is null then we match everything
			if (mapping == null) {
				// if we are the root check to see if the scope exists
				if (tree.parent() == null && tree.nodeExists(scope)
						&& containsKeys((IEclipsePreferences) tree.node(scope))) {
					return true;
				}
				// otherwise check to see if we are in the right scope
				if (scopeMatches(scope, tree) && containsKeys(tree)) {
					return true;
				}
				continue;
			}
			// iterate over the list of declared nodes
			for (String nodePath : mapping.keySet()) {
				String nodeFullPath = '/' + scope + '/' + nodePath;
				// if this subtree isn't in a hierarchy we are interested in, then go to the
				// next one
				if (!nodeFullPath.startsWith(treePath)) {
					continue;
				}
				// get the child node
				String childPath = nodeFullPath.substring(treePath.length());
				childPath = EclipsePreferences.makeRelative(childPath);
				if (tree.nodeExists(childPath)) {
					PreferenceFilterEntry[] entries;
					// protect against wrong classes since this is user-code
					try {
						entries = mapping.get(nodePath);
					} catch (ClassCastException e) {
						RuntimeLog.log(Status.error(PrefsMessages.preferences_classCastFilterEntry, e));
						continue;
					}
					// if there are no entries defined then we return false even if we
					// are supposed to match on the existence of the node as a whole (bug 88820)
					Preferences child = tree.node(childPath);
					if (entries == null) {
						if (child.keys().length != 0 || child.childrenNames().length != 0) {
							return true;
						}
					} else {
						// otherwise check to see if we have any applicable keys
						for (PreferenceFilterEntry entry : entries) {
							if (entry == null) {
								continue;
							}
							if (entry.getMatchType() == null) {
								if (child.get(entry.getKey(), null) != null) {
									return true;
								}
							} else if (internalMatchesWithMatchType(entry, child.keys())) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/*
	 * Internal method that collects the matching filters for the given tree and
	 * returns them.
	 */
	private IPreferenceFilter[] internalMatches(IEclipsePreferences tree, IPreferenceFilter[] filters)
			throws BackingStoreException {
		List<IPreferenceFilter> result = new ArrayList<>();
		for (IPreferenceFilter filter : filters) {
			if (internalMatches(tree, filter)) {
				result.add(filter);
			}
		}
		return result.toArray(new IPreferenceFilter[result.size()]);
	}

	/*
	 * Internal method that check the matching preferences for entry with specific
	 * match type.
	 */
	private boolean internalMatchesWithMatchType(PreferenceFilterEntry entry, String[] keys) {
		if (keys == null || keys.length == 0) {
			return false;
		}
		String key = entry.getKey();
		String matchType = entry.getMatchType();
		if (!matchType.equalsIgnoreCase(MATCH_TYPE_PREFIX)) {
			return false;
		}
		for (String k : keys) {
			if (k.startsWith(key)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Returns a boolean value indicating whether or not the given Properties object
	 * is the result of a preference export previous to Eclipse 3.0.
	 *
	 * Check the contents of the file. In Eclipse 3.0 we printed out a file version
	 * key.
	 */
	private boolean isLegacy(Properties properties) {
		return properties.getProperty(VERSION_KEY) == null;
	}

	@Override
	public IPreferenceFilter[] matches(IEclipsePreferences tree, IPreferenceFilter[] filters) throws CoreException {
		if (filters == null || filters.length == 0) {
			return new IPreferenceFilter[0];
		}
		try {
			return internalMatches(tree, filters);
		} catch (BackingStoreException e) {
			throw new CoreException(Status.error(PrefsMessages.preferences_matching, e));
		}
	}

	private IEclipsePreferences mergeTrees(IEclipsePreferences[] trees) throws BackingStoreException {
		if (trees.length == 1) {
			return trees[0];
		}
		final IEclipsePreferences result = ExportedPreferences.newRoot();
		if (trees.length == 0) {
			return result;
		}
		IPreferenceNodeVisitor visitor = node -> {
			Preferences destination = result.node(node.absolutePath());
			copyFromTo(node, destination, null, 0);
			return true;
		};
		for (IEclipsePreferences tree : trees) {
			tree.accept(visitor);
		}
		return result;
	}

	@Override
	public IExportedPreferences readPreferences(InputStream input) throws CoreException {
		if (input == null) {
			throw new IllegalArgumentException();
		} else if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Reading preferences from stream..."); //$NON-NLS-1$
		}
		// read the file into a properties object
		Properties properties = new Properties();
		try (input) {
			properties.load(input);
		} catch (IOException | IllegalArgumentException e) {
			throw new CoreException(Status.error(PrefsMessages.preferences_importProblems, e));
		}
		// an empty file is an invalid file format
		if (properties.isEmpty()) {
			throw new CoreException(Status.error(PrefsMessages.preferences_invalidFileFormat, null));
		}
		// manipulate the file if it from a legacy preference export
		if (isLegacy(properties)) {
			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages.message("Read legacy preferences file, converting to 3.0 format..."); //$NON-NLS-1$
			}
			properties = convertFromLegacy(properties);
		} else {
			if (EclipsePreferences.DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages.message("Read preferences file."); //$NON-NLS-1$
			}
			properties.remove(VERSION_KEY);
		}

		// convert the Properties object into an object to return
		return convertFromProperties(properties);
	}

	/**
	 * Return true if the given node is in the specified scope and false otherwise.
	 */
	private boolean scopeMatches(String scope, IEclipsePreferences tree) {
		// the root isn't in any scope
		if (tree.parent() == null) {
			return false;
		}
		// fancy math to get the first segment of the path
		String path = tree.absolutePath();
		int index = path.indexOf('/', 1);
		String sub = path.substring(1, index == -1 ? path.length() : index);
		return scope.equals(sub);
	}

	@Override
	public void setDefaultLookupOrder(String qualifier, String key, String[] order) {
		String registryKey = getRegistryKey(qualifier, key);
		if (order == null) {
			DEFAULTS_REGISTRY.remove(registryKey);
		} else {
			if (Arrays.stream(order).anyMatch(Objects::isNull)) {
				throw new IllegalArgumentException();
			}
			DEFAULTS_REGISTRY.put(registryKey, order);
		}
	}

	public void setRegistryHelper(Object registryHelper) {
		if (this.registryHelper != null && this.registryHelper != registryHelper) {
			((PreferenceServiceRegistryHelper) this.registryHelper).stop();
		}
		this.registryHelper = registryHelper;
	}

	/*
	 * Return a tree which contains only nodes and keys which are applicable to the
	 * given filter.
	 */
	private IEclipsePreferences trimTree(IEclipsePreferences tree, IPreferenceFilter filter)
			throws BackingStoreException {
		IEclipsePreferences result = (IEclipsePreferences) ExportedPreferences.newRoot().node(tree.absolutePath());
		String[] scopes = filter.getScopes();
		if (scopes == null) {
			throw new IllegalArgumentException();
		}
		String treePath = tree.absolutePath();
		// see if this node is applicable by going over all our scopes
		for (String scope : scopes) {
			Map<String, PreferenceFilterEntry[]> mapping = filter.getMapping(scope);
			// if the mapping is null then copy everything if the scope matches
			if (mapping == null) {
				// if we are the root node then check our children
				if (tree.parent() == null && tree.nodeExists(scope)) {
					copyFromTo(tree.node(scope), result.node(scope), null, -1);
				} else if (scopeMatches(scope, tree)) { // ensure we are in the correct scope
					copyFromTo(tree, result, null, -1);
				}
				continue;
			}
			// iterate over the list of declared nodes
			for (String nodePath : mapping.keySet()) {
				String nodeFullPath = '/' + scope + '/' + nodePath;
				// if this subtree isn't in a hierarchy we are interested in, then go to the
				// next one
				if (!nodeFullPath.startsWith(treePath)) {
					continue;
				}
				// get the child node
				String childPath = nodeFullPath.substring(treePath.length());
				childPath = EclipsePreferences.makeRelative(childPath);
				if (tree.nodeExists(childPath)) {
					Preferences child = tree.node(childPath);
					PreferenceFilterEntry[] entries;
					// protect against wrong classes since this is passed in by the user
					try {
						entries = mapping.get(nodePath);
					} catch (ClassCastException e) {
						RuntimeLog.log(Status.error(PrefsMessages.preferences_classCastFilterEntry, e));
						continue;
					}
					String[] keys = null;
					if (entries != null) {
						List<String> list = new ArrayList<>();
						for (PreferenceFilterEntry entry : entries) {
							if (entry != null) {
								addMatchedKeys(list, entry, child.keys());
							}
						}
						keys = list.toArray(new String[list.size()]);
					}
					// do infinite depth if there are no keys specified since the parent matched.
					copyFromTo(tree.node(childPath), result.node(childPath), keys, keys == null ? -1 : 0);
				}
			}
		}
		return result;
	}

	/*
	 * Internal method that adds to the given list the matching preferences for
	 * entry with or without specific match type.
	 */
	private void addMatchedKeys(List<String> list, PreferenceFilterEntry entry, String[] keys) {
		String matchType = entry.getMatchType();
		if (matchType == null) {
			list.add(entry.getKey());
			return;
		}
		if (keys == null) {
			return;
		}
		String key = entry.getKey();
		for (String k : keys) {
			if (matchType.equals(MATCH_TYPE_PREFIX) && k.startsWith(key)) {
				list.add(k);
			}
		}
	}

	/**
	 * Compares two plugin version identifiers to see if their preferences are
	 * compatible. If they are not compatible, a warning message is added to the
	 * given multi-status, according to the following rules:
	 *
	 * - plugins that differ in service number: no status - plugins that differ in
	 * minor version: WARNING status - plugins that differ in major version: - where
	 * installed plugin is newer: WARNING status - where installed plugin is older:
	 * ERROR status
	 *
	 * @param bundle    the name of the bundle
	 * @param pref      The version identifier of the preferences to be loaded
	 * @param installed The version identifier of the installed plugin
	 */
	private IStatus validatePluginVersions(String bundle, Version pref, Version installed) {
		if (installed.getMajor() == pref.getMajor() && installed.getMinor() == pref.getMinor()) {
			return null;
		}
		String msg = NLS.bind(PrefsMessages.preferences_incompatible, new Object[] { pref, bundle, installed });
		boolean isError = installed.getMajor() < pref.getMajor();
		return isError ? Status.error(msg) : Status.warning(msg);
	}

	private static Version parseVersion(String version) {
		if (version != null) {
			try {
				return new Version(version);
			} catch (IllegalArgumentException e) { // invalid
			}
		}
		return null;
	}

	public IStatus validateVersions(IPath path) {
		final MultiStatus result = new MultiStatus(PrefsMessages.OWNER_NAME, IStatus.INFO,
				PrefsMessages.preferences_validate, null);
		IPreferenceNodeVisitor visitor = node -> {
			if (!(node instanceof ExportedPreferences realNode)) {
				return false;
			}
			// calculate the version in the file
			Version versionInFile = parseVersion(realNode.getVersion());
			if (versionInFile == null) {
				return true;
			}
			// calculate the version of the installed bundle
			String bundleName = getBundleName(node.absolutePath());
			if (bundleName == null) {
				return true;
			}
			Version versionInMemory = parseVersion(getBundleVersion(bundleName));
			if (versionInMemory == null) {
				return true;
			}
			// verify the versions based on the matching rules
			IStatus verification = validatePluginVersions(bundleName, versionInFile, versionInMemory);
			if (verification != null) {
				result.add(verification);
			}
			return true;
		};
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(path.toFile()));
			IExportedPreferences prefs = readPreferences(input);
			prefs.accept(visitor);
		} catch (FileNotFoundException e) {
			// ignore...if the file does not exist then all is OK
		} catch (CoreException | BackingStoreException e) {
			result.add(Status.error(PrefsMessages.preferences_validationException, e));
		}
		return result;
	}

	/*
	 * Prepend the given scope to the default search order to use when there is
	 * nothing else set. Clients should not call this method because it is in an
	 * internal class and has been created solely for use by the
	 * org.eclipse.core.resources bundle in response to this bug:
	 * https://bugs.eclipse.org/330320
	 */
	public void prependScopeToDefaultDefaultLookupOrder(String firstScope) {
		List<String> scopes = new ArrayList<>(DEFAULT_DEFAULT_LOOKUP_ORDER);
		scopes.add(0, firstScope);
		DEFAULT_DEFAULT_LOOKUP_ORDER = List.copyOf(scopes);
	}

}
