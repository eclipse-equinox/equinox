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
 *     Julian Chen - fix for bug #92572, jclRM
 *     Jan-Ove Weichel (janove.weichel@vogella.com) - bug 474359
 *     InterSystems Corporation - bug 444188
 *     Hannes Wellmann - Leverage Java-NIO to write preferences
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Represents a node in the Eclipse preference node hierarchy. This class is
 * used as a default implementation/super class for those nodes which belong to
 * scopes which are contributed by the Platform.
 *
 * Implementation notes:
 *
 * - For thread safety, we always synchronize on <code>writeLock</code> when writing
 * the children or properties fields. Must ensure we don't synchronize when
 * calling client code such as listeners.
 *
 * @since 3.0
 */
public class EclipsePreferences implements IEclipsePreferences, IScope {

	public static final String DEFAULT_PREFERENCES_DIRNAME = ".settings"; //$NON-NLS-1$
	public static final String PREFS_FILE_EXTENSION = "prefs"; //$NON-NLS-1$
	protected static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final String FALSE = "false"; //$NON-NLS-1$
	private static final String TRUE = "true"; //$NON-NLS-1$
	protected static final String VERSION_KEY = "eclipse.preferences.version"; //$NON-NLS-1$
	protected static final String VERSION_VALUE = "1"; //$NON-NLS-1$
	protected static final String PATH_SEPARATOR = String.valueOf(IPath.SEPARATOR);
	protected static final String DOUBLE_SLASH = "//"; //$NON-NLS-1$
	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String BACKUP_FILE_EXTENSION = ".bak"; //$NON-NLS-1$

	private String cachedPath;
	protected ImmutableMap properties = ImmutableMap.EMPTY;
	protected Map<String, Object> children;
	/**
	 * Protects write access to properties and children.
	 */
	private final Object childAndPropertyLock = new Object();
	protected boolean dirty = false;
	protected boolean loading = false;
	protected final String name;
	// the parent of an EclipsePreference node is always an EclipsePreference node.
	// (or null)
	protected final EclipsePreferences parent;
	protected boolean removed = false;
	private final ListenerList<INodeChangeListener> nodeChangeListeners = new ListenerList<>();
	private final ListenerList<IPreferenceChangeListener> preferenceChangeListeners = new ListenerList<>();
	private ScopeDescriptor descriptor;

	public static final boolean DEBUG_PREFERENCE_GENERAL;
	public static final boolean DEBUG_PREFERENCE_SET;
	public static final boolean DEBUG_PREFERENCE_GET;
	static {
		PreferencesOSGiUtils osgiDefaults = PreferencesOSGiUtils.getDefault();
		DEBUG_PREFERENCE_GENERAL = osgiDefaults.getBooleanDebugOption(Activator.PI_PREFERENCES + "/general", false); //$NON-NLS-1$
		DEBUG_PREFERENCE_SET = osgiDefaults.getBooleanDebugOption(Activator.PI_PREFERENCES + "/set", false); //$NON-NLS-1$
		DEBUG_PREFERENCE_GET = osgiDefaults.getBooleanDebugOption(Activator.PI_PREFERENCES + "/get", false); //$NON-NLS-1$
	}

	public EclipsePreferences() {
		this(null, null);
	}

	protected EclipsePreferences(EclipsePreferences parent, String name) {
		this.parent = parent;
		this.name = name;
		this.cachedPath = null; // make sure the cached path is cleared after setting the parent
	}

	@Override
	public String absolutePath() {
		if (cachedPath == null) {
			if (parent == null) {
				cachedPath = PATH_SEPARATOR;
			} else {
				String parentPath = parent.absolutePath();
				// if the parent is the root then we don't have to add a separator
				// between the parent path and our path
				if (parentPath.length() == 1) {
					cachedPath = parentPath + name();
				} else {
					cachedPath = parentPath + PATH_SEPARATOR + name();
				}
			}
		}
		return cachedPath;
	}

	@Override
	public void accept(IPreferenceNodeVisitor visitor) throws BackingStoreException {
		if (!visitor.visit(this)) {
			return;
		}
		for (IEclipsePreferences p : getChildren(true)) {
			p.accept(visitor);
		}
	}

	protected IEclipsePreferences addChild(String childName, IEclipsePreferences child) {
		// Thread safety: synchronize method to protect modification of children field
		synchronized (childAndPropertyLock) {
			if (children == null) {
				children = new ConcurrentHashMap<>();
			}
			children.put(childName, child == null ? (Object) childName : child);
			return child;
		}
	}

	@Override
	public void addNodeChangeListener(INodeChangeListener listener) {
		checkRemoved();
		nodeChangeListeners.add(listener);
		if (DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Added preference node change listener: " + listener + " to: " + absolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
		checkRemoved();
		preferenceChangeListeners.add(listener);
		if (DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Added preference property change listener: " + listener + " to: " + absolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private IEclipsePreferences calculateRoot() {
		IEclipsePreferences result = this;
		while (result.parent() != null) {
			result = (IEclipsePreferences) result.parent();
		}
		return result;
	}

	/*
	 * Convenience method for throwing an exception when methods are called on a
	 * removed node.
	 */
	protected void checkRemoved() {
		if (removed) {
			throw new IllegalStateException(NLS.bind(PrefsMessages.preferences_removedNode, name));
		}
	}

	@Override
	public String[] childrenNames() throws BackingStoreException {
		// illegal state if this node has been removed
		checkRemoved();
		String[] internal = internalChildNames();
		// if we are != 0 then we have already been initialized
		if (internal.length != 0) {
			return internal;
		}
		// we only want to query the descriptor for the child names if
		// this node is the scope root
		if (descriptor != null && getSegmentCount(absolutePath()) == 1) {
			return descriptor.childrenNames(absolutePath());
		}
		return internal;
	}

	protected String[] internalChildNames() {
		synchronized (childAndPropertyLock) {
			if (children == null || children.isEmpty()) {
				return EMPTY_STRING_ARRAY;
			}
			return children.keySet().toArray(String[]::new);
		}
	}

	@Override
	public void clear() {
		// illegal state if this node has been removed
		checkRemoved();
		// call each one separately (instead of Properties.clear) so
		// clients get change notification
		String[] keys;
		synchronized (childAndPropertyLock) {
			keys = properties.keys();
		}
		// don't synchronize remove call because it calls listeners
		for (String key : keys) {
			remove(key);
		}
		makeDirty();
	}

	protected List<String> computeChildren(IPath root) {
		if (root == null) {
			return List.of();
		}
		IPath dir = root.append(DEFAULT_PREFERENCES_DIRNAME);
		List<String> result = new ArrayList<>();
		String extension = '.' + PREFS_FILE_EXTENSION;
		File[] totalFiles = dir.toFile().listFiles();
		if (totalFiles != null) {
			for (File totalFile : totalFiles) {
				String filename = totalFile.getName();
				if (filename.endsWith(extension) && totalFile.isFile()) {
					String shortName = filename.substring(0, filename.length() - extension.length());
					result.add(shortName);
				}
			}
		}
		return result;
	}

	protected IPath computeLocation(IPath root, String qualifier) {
		return root == null ? null
				: root.append(DEFAULT_PREFERENCES_DIRNAME).append(qualifier).addFileExtension(PREFS_FILE_EXTENSION);
	}

	/*
	 * Version 1 (current version) path/key=value
	 */
	protected static void convertFromProperties(EclipsePreferences node, Properties table, boolean notify) {
		String version = table.getProperty(VERSION_KEY);
		if (version == null || !VERSION_VALUE.equals(version)) {
			// ignore for now
		}
		table.remove(VERSION_KEY);
		for (Object propName : table.keySet()) {
			String fullKey = (String) propName;
			String value = table.getProperty(fullKey);
			if (value != null) {
				String[] splitPath = decodePath(fullKey);
				String path = splitPath[0];
				path = makeRelative(path);
				String key = splitPath[1];
				if (DEBUG_PREFERENCE_SET) {
					PrefsMessages.message("Setting preference: " + path + '/' + key + '=' + value); //$NON-NLS-1$
				}
				// use internal methods to avoid notifying listeners
				EclipsePreferences childNode = (EclipsePreferences) node.internalNode(path, false, null);
				String oldValue = childNode.internalPut(key, value);
				// notify listeners if applicable
				if (notify && !value.equals(oldValue)) {
					childNode.firePreferenceEvent(key, oldValue, value);
				}
			}
		}
	}

	private final Object writeLock = new Object();

	/*
	 * Helper method to persist a Properties object to the filesystem. We use this
	 * helper so we can remove the date/timestamp that Properties#store always puts
	 * in the file.
	 */
	private void write(Properties props, IPath location) throws BackingStoreException {
		Path preferenceFile = location.toFile().toPath();
		Path parentFile = preferenceFile.getParent();
		if (parentFile == null) {
			return;
		}
		try {
			Files.createDirectories(parentFile);
			String fileContent = removeTimestampFromTable(props);
			synchronized (writeLock) {
				if (Files.exists(preferenceFile)) {
					// Write new file content to a temporary file first to not loose the old content
					// in case of a failure. If everything goes OK, it is moved to the right place.
					Path tmp = preferenceFile.resolveSibling(preferenceFile.getFileName() + BACKUP_FILE_EXTENSION);
					Files.writeString(tmp, fileContent, StandardCharsets.UTF_8);
					Files.move(tmp, preferenceFile, StandardCopyOption.REPLACE_EXISTING);
				} else {
					Files.writeString(preferenceFile, fileContent, StandardCharsets.UTF_8);
				}
			}
		} catch (IOException e) {
			String message = NLS.bind(PrefsMessages.preferences_saveException, location);
			log(Status.error(message, e));
			throw new BackingStoreException(message, e);
		}
	}

	protected static String removeTimestampFromTable(Properties properties) throws IOException {
		// store the properties in a string and then skip the first line
		// (date/timestamp)
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		properties.store(output, null);
		String string = output.toString(StandardCharsets.UTF_8);
		String separator = System.lineSeparator();
		return string.substring(string.indexOf(separator) + separator.length());
	}

	/*
	 * Helper method to convert this node to a Properties file suitable for
	 * persistence.
	 */
	protected Properties convertToProperties(Properties result, String prefix) throws BackingStoreException {
		// add the key/value pairs from this node
		boolean addSeparator = prefix.length() != 0;
		// thread safety: copy reference in case of concurrent change
		ImmutableMap temp;
		synchronized (childAndPropertyLock) {
			temp = properties;
		}
		for (String key : temp.keys()) {
			String value = temp.get(key);
			if (value != null) {
				result.put(encodePath(prefix, key), value);
			}
		}
		// recursively add the child information
		for (IEclipsePreferences childNode : getChildren(true)) {
			EclipsePreferences child = (EclipsePreferences) childNode;
			String fullPath = addSeparator ? prefix + PATH_SEPARATOR + child.name() : child.name();
			child.convertToProperties(result, fullPath);
		}
		return result;
	}

	@Override
	public IEclipsePreferences create(IEclipsePreferences nodeParent, String nodeName) {
		return create((EclipsePreferences) nodeParent, nodeName, null);
	}

	protected boolean isLoading() {
		return loading;
	}

	protected void setLoading(boolean isLoading) {
		loading = isLoading;
	}

	public IEclipsePreferences create(EclipsePreferences nodeParent, String nodeName, Object context) {
		EclipsePreferences result = internalCreate(nodeParent, nodeName, context);
		nodeParent.addChild(nodeName, result);
		IEclipsePreferences loadLevel = result.getLoadLevel();

		// if this node or a parent node is not the load level then return
		// if the result node is not a load level, then a child must be
		if (loadLevel == null || result != loadLevel || isAlreadyLoaded(result) || result.isLoading()) {
			return result;
		}
		try {
			result.setLoading(true);
			result.load();
			result.loaded();
			result.flush();
		} catch (BackingStoreException e) {
			IPath location = result.getLocation();
			String message = NLS.bind(PrefsMessages.preferences_loadException,
					location == null ? EMPTY_STRING : location.toString());
			IStatus status = Status.error(message, e);
			RuntimeLog.log(status);
		} finally {
			result.setLoading(false);
		}
		return result;
	}

	@Override
	public void flush() throws BackingStoreException {
		IEclipsePreferences toFlush = null;
		synchronized (childAndPropertyLock) {
			toFlush = internalFlush();
		}
		// if we aren't at the right level, then flush the appropriate node
		if (toFlush != null) {
			toFlush.flush();
		}
	}

	/*
	 * Do the real flushing in a non-synchronized internal method so sub-classes
	 * (mainly ProjectPreferences and ProfilePreferences) don't cause deadlocks.
	 *
	 * If this node is not responsible for persistence (a load level), then this
	 * method returns the node that should be flushed. Returns null if this method
	 * performed the flush.
	 */
	protected IEclipsePreferences internalFlush() throws BackingStoreException {
		// illegal state if this node has been removed
		checkRemoved();

		IEclipsePreferences loadLevel = getLoadLevel();

		// if this node or a parent is not the load level, then flush the children
		if (loadLevel == null) {
			for (String childrenName : childrenNames()) {
				node(childrenName).flush();
			}
			return null;
		}
		// a parent is the load level for this node
		if (this != loadLevel) {
			return loadLevel;
		}
		// this node is a load level
		// any work to do?
		if (!dirty) {
			return null;
		}
		// remove dirty bit before saving, to ensure that concurrent
		// changes during save mark the store as dirty
		dirty = false;
		try {
			save();
		} catch (BackingStoreException e) {
			// mark it dirty again because the save failed
			dirty = true;
			throw e;
		}
		return null;
	}

	@Override
	public String get(String key, String defaultValue) {
		String value = internalGet(key);
		return value == null ? defaultValue : value;
	}

	@Override
	public boolean getBoolean(String key, boolean defaultValue) {
		String value = internalGet(key);
		return value == null ? defaultValue : TRUE.equalsIgnoreCase(value);
	}

	@Override
	public byte[] getByteArray(String key, byte[] defaultValue) {
		String value = internalGet(key);
		return value == null ? defaultValue : Base64.decode(value.getBytes());
	}

	/*
	 * Return a boolean value indicating whether or not a child with the given name
	 * is known to this node.
	 */
	protected boolean childExists(String childName) {
		synchronized (childAndPropertyLock) {
			if (children == null) {
				return false;
			}
			return children.get(childName) != null;
		}
	}

	/**
	 * Thread safe way to obtain a child for a given key. Returns the child that
	 * matches the given key, or null if there is no matching child.
	 */
	protected IEclipsePreferences getChild(String key, Object context, boolean create) {
		synchronized (childAndPropertyLock) {
			if (children == null) {
				return null;
			}
			Object value = children.get(key);
			if (value == null) {
				return null;
			} else if (value instanceof IEclipsePreferences eclipsePreferences) {
				return eclipsePreferences;
			}
			// if we aren't supposed to create this node, then
			// just return null
			if (!create) {
				return null;
			}
		}
		return addChild(key, create(this, key, context));
	}

	/**
	 * Thread safe way to obtain all children of this node. Never returns null.
	 */
	private List<IEclipsePreferences> getChildren(boolean create) {
		List<IEclipsePreferences> result = new ArrayList<>();
		for (String n : internalChildNames()) {
			IEclipsePreferences child = getChild(n, null, create);
			if (child != null) {
				result.add(child);
			}
		}
		return result;
	}

	@Override
	public double getDouble(String key, double defaultValue) {
		String value = internalGet(key);
		double result = defaultValue;
		if (value != null) {
			try {
				result = Double.parseDouble(value);
			} catch (NumberFormatException e) {
				// use default
			}
		}
		return result;
	}

	@Override
	public float getFloat(String key, float defaultValue) {
		String value = internalGet(key);
		float result = defaultValue;
		if (value != null) {
			try {
				result = Float.parseFloat(value);
			} catch (NumberFormatException e) {
				// use default
			}
		}
		return result;
	}

	@Override
	public int getInt(String key, int defaultValue) {
		String value = internalGet(key);
		int result = defaultValue;
		if (value != null) {
			try {
				result = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				// use default
			}
		}
		return result;
	}

	protected IEclipsePreferences getLoadLevel() {
		return descriptor == null ? null : descriptor.getLoadLevel(this);
	}

	/*
	 * Subclasses to over-ride
	 */
	protected IPath getLocation() {
		return null;
	}

	@Override
	public long getLong(String key, long defaultValue) {
		String value = internalGet(key);
		long result = defaultValue;
		if (value != null) {
			try {
				result = Long.parseLong(value);
			} catch (NumberFormatException e) {
				// use default
			}
		}
		return result;
	}

	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		EclipsePreferences result = new EclipsePreferences(nodeParent, nodeName);
		result.descriptor = this.descriptor;
		return result;
	}

	/**
	 * Returns the existing value at the given key, or null if no such value exists.
	 */
	protected String internalGet(String key) {
		// throw NPE if key is null
		if (key == null) {
			throw new NullPointerException();
		}
		// illegal state if this node has been removed
		checkRemoved();
		String result;
		synchronized (childAndPropertyLock) {
			result = properties.get(key);
		}
		if (DEBUG_PREFERENCE_GET) {
			PrefsMessages.message("Getting preference value: " + absolutePath() + '/' + key + "->" + result); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}

	/**
	 * Implements the node(String) method, and optionally notifies listeners.
	 */
	protected IEclipsePreferences internalNode(String path, boolean notify, Object context) {

		// illegal state if this node has been removed
		checkRemoved();

		// short circuit this node
		if (path.isEmpty()) {
			return this;
		}
		// if we have an absolute path use the root relative to
		// this node instead of the global root
		// in case we have a different hierarchy. (e.g. export)
		if (path.charAt(0) == IPath.SEPARATOR) {
			return (IEclipsePreferences) calculateRoot().node(path.substring(1));
		}
		int index = path.indexOf(IPath.SEPARATOR);
		String key = index == -1 ? path : path.substring(0, index);
		boolean added = false;
		IEclipsePreferences child = getChild(key, context, true);
		if (child == null) {
			child = create(this, key, context);
			added = true;
		}
		// notify listeners if a child was added
		if (added && notify) {
			fireNodeEvent(new NodeChangeEvent(this, child), true);
		}
		return (IEclipsePreferences) child.node(index == -1 ? EMPTY_STRING : path.substring(index + 1));
	}

	/**
	 * Stores the given (key,value) pair, performing lazy initialization of the
	 * properties field if necessary. Returns the old value for the given key, or
	 * null if no value existed.
	 */
	protected String internalPut(String key, String newValue) {
		synchronized (childAndPropertyLock) {
			// illegal state if this node has been removed
			checkRemoved();
			String oldValue = properties.get(key);
			if (oldValue != null && oldValue.equals(newValue)) {
				return oldValue;
			} else if (DEBUG_PREFERENCE_SET) {
				PrefsMessages.message("Setting preference: " + absolutePath() + '/' + key + '=' + newValue); //$NON-NLS-1$
			}
			properties = properties.put(key.intern(), newValue.intern());
			return oldValue;
		}
	}

	/*
	 * Subclasses to over-ride.
	 */
	protected boolean isAlreadyLoaded(IEclipsePreferences node) {
		return descriptor == null ? true : descriptor.isAlreadyLoaded(node.absolutePath());
	}

	@Override
	public String[] keys() {
		// illegal state if this node has been removed
		synchronized (childAndPropertyLock) {
			checkRemoved();
			return properties.keys();
		}
	}

	/**
	 * Loads the preference node. This method returns silently if the node does not
	 * exist in the backing store (for example non-existent project).
	 *
	 * @throws BackingStoreException if the node exists in the backing store but it
	 *                               could not be loaded
	 */
	protected void load() throws BackingStoreException {
		if (descriptor == null) {
			load(getLocation());
		} else {
			// load the properties then set them without sending out change events
			Properties props = descriptor.load(absolutePath());
			if (props == null || props.isEmpty()) {
				return;
			}
			convertFromProperties(this, props, false);
		}
	}

	protected static Properties loadProperties(IPath location) throws BackingStoreException {
		if (DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Loading preferences from file: " + location); //$NON-NLS-1$
		}
		Properties result = new Properties();
		try (InputStream input = getSaveInputStream(location)) {
			result.load(input);
		} catch (FileNotFoundException e) {
			// file doesn't exist but that's ok.
			if (DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages.message("Preference file does not exist: " + location); //$NON-NLS-1$
			}
		} catch (IOException | IllegalArgumentException e) {
			String message = NLS.bind(PrefsMessages.preferences_loadException, location);
			log(new Status(IStatus.INFO, PrefsMessages.OWNER_NAME, IStatus.INFO, message, e));
			throw new BackingStoreException(message, e);
		}
		return result;
	}

	private static InputStream getSaveInputStream(IPath location) throws IOException {
		File target = location.toFile().getAbsoluteFile();
		if (!target.exists()) {
			target = new File(target + BACKUP_FILE_EXTENSION);
		}
		return new FileInputStream(target);
	}

	protected void load(IPath location) throws BackingStoreException {
		if (location == null) {
			if (DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages.message("Unable to determine location of preference file for node: " + absolutePath()); //$NON-NLS-1$
			}
			return;
		}
		Properties fromDisk = loadProperties(location);
		convertFromProperties(this, fromDisk, false);
	}

	protected void loaded() {
		if (descriptor == null) {
			// do nothing
		} else {
			descriptor.loaded(absolutePath());
		}
	}

	public static void log(IStatus status) {
		RuntimeLog.log(status);
	}

	protected void makeDirty() {
		EclipsePreferences node = this;
		while (node != null && !node.removed) {
			node.dirty = true;
			node = (EclipsePreferences) node.parent();
		}
	}

	public boolean isDirty() {
		return dirty;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Preferences node(String pathName) {
		return internalNode(pathName, true, null);
	}

	protected void fireNodeEvent(final NodeChangeEvent event, final boolean added) {
		for (final INodeChangeListener listener : nodeChangeListeners) {
			SafeRunner.run(() -> {
				if (added) {
					listener.added(event);
				} else {
					listener.removed(event);
				}
			});
		}
	}

	@Override
	public boolean nodeExists(String path) throws BackingStoreException {
		// short circuit for checking this node
		if (path.isEmpty()) {
			return !removed;
		}
		// illegal state if this node has been removed.
		// do this AFTER checking for the empty string.
		checkRemoved();

		// use the root relative to this node instead of the global root
		// in case we have a different hierarchy. (e.g. export)
		if (path.charAt(0) == IPath.SEPARATOR) {
			return calculateRoot().nodeExists(path.substring(1));
		}
		int index = path.indexOf(IPath.SEPARATOR);
		boolean noSlash = index == -1;

		// if we are looking for a simple child then just look in the table and return
		if (noSlash) {
			return childExists(path);
		}
		// otherwise load the parent of the child and then recursively ask
		String childName = path.substring(0, index);
		if (!childExists(childName)) {
			return false;
		}
		IEclipsePreferences child = getChild(childName, null, true);
		if (child == null) {
			return false;
		}
		return child.nodeExists(path.substring(index + 1));
	}

	@Override
	public Preferences parent() {
		// illegal state if this node has been removed
		checkRemoved();
		return parent;
	}

	/*
	 * Convenience method for notifying preference change listeners.
	 */
	protected void firePreferenceEvent(String key, Object oldValue, Object newValue) {
		final PreferenceChangeEvent event = new PreferenceChangeEvent(this, key, oldValue, newValue);
		for (final IPreferenceChangeListener listener : preferenceChangeListeners) {
			SafeRunner.run(() -> listener.preferenceChange(event));
		}
	}

	@Override
	public void put(String key, String newValue) {
		if (key == null || newValue == null) {
			throw new NullPointerException();
		}
		String oldValue = internalPut(key, newValue);
		if (!newValue.equals(oldValue)) {
			makeDirty();
			firePreferenceEvent(key, oldValue, newValue);
		}
	}

	@Override
	public void putBoolean(String key, boolean value) {
		put(key, value ? TRUE : FALSE);
	}

	@Override
	public void putByteArray(String key, byte[] value) {
		put(key, new String(Base64.encode(value)));
	}

	@Override
	public void putDouble(String key, double value) {
		put(key, Double.toString(value));
	}

	@Override
	public void putFloat(String key, float value) {
		put(key, Float.toString(value));
	}

	@Override
	public void putInt(String key, int value) {
		put(key, Integer.toString(value));
	}

	@Override
	public void putLong(String key, long value) {
		put(key, Long.toString(value));
	}

	@Override
	public void remove(String key) {
		String oldValue;
		synchronized (childAndPropertyLock) {
			// illegal state if this node has been removed
			checkRemoved();
			oldValue = properties.get(key);
			if (oldValue == null) {
				return;
			}
			properties = properties.removeKey(key);
		}
		makeDirty();
		firePreferenceEvent(key, oldValue, null);
	}

	@Override
	public void removeNode() throws BackingStoreException {
		// illegal state if this node has been removed
		checkRemoved();
		// clear all the property values. do it "the long way" so
		// everyone gets notification
		String[] keys = keys();
		for (String key : keys) {
			remove(key);
		}
		// don't remove the global root or the scope root from the
		// parent but remove all its children
		if (parent != null && !(parent instanceof RootPreferences)) {
			// remove the node from the parent's collection and notify listeners
			removed = true;
			parent.removeNode(this);
		}
		for (IEclipsePreferences childNode : getChildren(false)) {
			try {
				childNode.removeNode();
			} catch (IllegalStateException e) {
				// ignore since we only get this exception if we have already
				// been removed. no work to do.
			}
		}
	}

	/*
	 * Remove the child from the collection and notify the listeners if something
	 * was actually removed.
	 */
	protected void removeNode(IEclipsePreferences child) {
		if (removeNode(child.name()) != null) {
			fireNodeEvent(new NodeChangeEvent(this, child), false);
			if (descriptor != null) {
				descriptor.removed(child.absolutePath());
			}
		}
	}

	/*
	 * Remove non-initialized node from the collection.
	 */
	protected Object removeNode(String key) {
		synchronized (childAndPropertyLock) {
			if (children != null) {
				Object result = children.remove(key);
				if (result != null) {
					makeDirty();
				}
				if (children.isEmpty()) {
					children = null;
				}
				return result;
			}
		}
		return null;
	}

	@Override
	public void removeNodeChangeListener(INodeChangeListener listener) {
		checkRemoved();
		nodeChangeListeners.remove(listener);
		if (DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Removed preference node change listener: " + listener + " from: " + absolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public void removePreferenceChangeListener(IPreferenceChangeListener listener) {
		checkRemoved();
		preferenceChangeListeners.remove(listener);
		if (DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages
					.message("Removed preference property change listener: " + listener + " from: " + absolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Saves the preference node. This method returns silently if the node does not
	 * exist in the backing store (for example non-existent project)
	 *
	 * @throws BackingStoreException if the node exists in the backing store but it
	 *                               could not be saved
	 */
	protected void save() throws BackingStoreException {
		if (descriptor == null) {
			save(getLocation());
		} else {
			descriptor.save(absolutePath(), convertToProperties(new Properties(), "")); //$NON-NLS-1$
		}
	}

	protected void save(IPath location) throws BackingStoreException {
		if (location == null) {
			if (DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages.message("Unable to determine location of preference file for node: " + absolutePath()); //$NON-NLS-1$
			}
			return;
		}
		if (DEBUG_PREFERENCE_GENERAL) {
			PrefsMessages.message("Saving preferences to file: " + location); //$NON-NLS-1$
		}
		Properties table = convertToProperties(new SortedProperties(), EMPTY_STRING);
		if (table.isEmpty()) {
			// nothing to save. delete existing file if one exists.
			if (location.toFile().exists() && !location.toFile().delete()) {
				String message = NLS.bind(PrefsMessages.preferences_failedDelete, location);
				log(Status.warning(message));
			}
			return;
		}
		table.put(VERSION_KEY, VERSION_VALUE);
		write(table, location);
	}

	/*
	 * Encode the given path and key combo to a form which is suitable for
	 * persisting or using when searching. If the key contains a slash character
	 * then we must use a double-slash to indicate the end of the path/the beginning
	 * of the key.
	 */
	public static String encodePath(String path, String key) {
		int pathLength = path == null ? 0 : path.length();
		if (key.indexOf(IPath.SEPARATOR) == -1) {
			if (pathLength == 0) {
				return key;
			}
			return path + IPath.SEPARATOR + key;
		}
		if (pathLength == 0) {
			return DOUBLE_SLASH + key;
		}
		return path + DOUBLE_SLASH + key;
	}

	/*
	 * Return the segment from the given path or null. "segment" parameter is
	 * 0-based.
	 */
	public static String getSegment(String path, int segment) {
		int start = path.indexOf(IPath.SEPARATOR) == 0 ? 1 : 0;
		int end = path.indexOf(IPath.SEPARATOR, start);
		if (end == path.length() - 1) {
			end = -1;
		}
		for (int i = 0; i < segment; i++) {
			if (end == -1) {
				return null;
			}
			start = end + 1;
			end = path.indexOf(IPath.SEPARATOR, start);
		}
		if (end == -1) {
			end = path.length();
		}
		return path.substring(start, end);
	}

	public static int getSegmentCount(String path) {
		StringTokenizer tokenizer = new StringTokenizer(path, String.valueOf(IPath.SEPARATOR));
		return tokenizer.countTokens();
	}

	/*
	 * Return a relative path
	 */
	public static String makeRelative(String path) {
		if (path == null) {
			return EMPTY_STRING;
		}
		if (path.length() > 0 && path.charAt(0) == IPath.SEPARATOR) {
			return path.substring(1);
		}
		return path;
	}

	/*
	 * Return a 2 element String array. element 0 - the path element 1 - the key The
	 * path may be null. The key is never null.
	 */
	public static String[] decodePath(String fullPath) {
		String key;
		String path = null;

		// check to see if we have an indicator which tells us where the path ends
		int index = fullPath.indexOf(DOUBLE_SLASH);
		if (index == -1) {
			// we don't have a double-slash telling us where the path ends
			// so the path is up to the last slash character
			int lastIndex = fullPath.lastIndexOf(IPath.SEPARATOR);
			if (lastIndex == -1) {
				key = fullPath;
			} else {
				path = fullPath.substring(0, lastIndex);
				key = fullPath.substring(lastIndex + 1);
			}
		} else {
			// the child path is up to the double-slash and the key
			// is the string after it
			path = fullPath.substring(0, index);
			key = fullPath.substring(index + 2);
		}
		// adjust if we have an absolute path
		if (path != null) {
			if (path.isEmpty()) {
				path = null;
			} else if (path.charAt(0) == IPath.SEPARATOR) {
				path = path.substring(1);
			}
		}
		return new String[] { path, key };
	}

	@Override
	public void sync() throws BackingStoreException {
		// illegal state if this node has been removed
		checkRemoved();
		IEclipsePreferences node = getLoadLevel();
		if (node == null) {
			if (DEBUG_PREFERENCE_GENERAL) {
				PrefsMessages.message("Preference node is not a load root: " + absolutePath()); //$NON-NLS-1$
			}
			return;
		}
		if (node instanceof EclipsePreferences eclipsePreferences) {
			eclipsePreferences.load();
			node.flush();
		}
	}

	public String toDeepDebugString() {
		final StringBuilder buffer = new StringBuilder();
		try {
			accept(node -> {
				buffer.append(node).append('\n');
				for (String key : node.keys()) {
					buffer.append(node.absolutePath()).append(PATH_SEPARATOR);
					buffer.append(key).append('=').append(node.get(key, "*default*")).append('\n'); //$NON-NLS-1$
				}
				return true;
			});
		} catch (BackingStoreException e) {
			System.out.println("Exception while calling #toDeepDebugString()"); //$NON-NLS-1$
			e.printStackTrace();
		}
		return buffer.toString();
	}

	@Override
	public String toString() {
		return absolutePath();
	}

	void setDescriptor(ScopeDescriptor descriptor) {
		this.descriptor = descriptor;
	}
}
