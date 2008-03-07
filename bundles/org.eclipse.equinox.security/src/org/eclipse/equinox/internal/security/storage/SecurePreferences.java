/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.storage;

import java.io.IOException;
import java.util.*;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.eclipse.osgi.util.NLS;

public class SecurePreferences {

	/**
	 * Pseudo-module ID to use when encryption is done with the default password.
	 */
	protected final static String DEFAULT_PASSWORD_ID = "org.eclipse.equinox.security.noModule"; //$NON-NLS-1$

	/**
	 * Maximum unsuccessful decryption attempts per operation
	 */
	static protected final int MAX_ATTEMPTS = 20;

	private static final String PATH_SEPARATOR = String.valueOf(IPath.SEPARATOR);

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final String FALSE = "false"; //$NON-NLS-1$
	private static final String TRUE = "true"; //$NON-NLS-1$

	private boolean removed = false;

	/**
	 * Parent node; null if this is a root node
	 */
	final protected SecurePreferences parent;

	/**
	 * Name of this node
	 */
	final private String name;

	/**
	 * Child nodes; created lazily; might be null
	 */
	protected Map children;

	/**
	 * Values associated with this node; created lazily; might be null
	 */
	private Map values;

	/**
	 * Cache root node to improve performance a bit
	 */
	private SecurePreferencesRoot root = null;

	public SecurePreferences(SecurePreferences parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// Navigation

	public SecurePreferences parent() {
		checkRemoved();
		return parent;
	}

	public String name() {
		checkRemoved();
		return name;
	}

	public String absolutePath() {
		checkRemoved();
		if (parent == null)
			return PATH_SEPARATOR;
		String parentPath = parent.absolutePath();
		if (PATH_SEPARATOR.equals(parentPath)) // parent is the root node?
			return parentPath + name;
		return parentPath + PATH_SEPARATOR + name;
	}

	public SecurePreferences node(String pathName) {
		checkRemoved();
		validatePath(pathName);
		return navigateToNode(pathName, true);
	}

	public boolean nodeExists(String pathName) {
		checkRemoved();
		validatePath(pathName);
		return (navigateToNode(pathName, false) != null);
	}

	public String[] keys() {
		checkRemoved();
		if (values == null)
			return EMPTY_STRING_ARRAY;
		Set keys = values.keySet();
		int size = keys.size();
		String[] result = new String[size];
		int pos = 0;
		for (Iterator i = keys.iterator(); i.hasNext();) {
			result[pos++] = (String) i.next();
		}
		return result;
	}

	public String[] childrenNames() {
		checkRemoved();
		if (children == null)
			return EMPTY_STRING_ARRAY;
		Set keys = children.keySet();
		int size = keys.size();
		String[] result = new String[size];
		int pos = 0;
		for (Iterator i = keys.iterator(); i.hasNext();) {
			result[pos++] = (String) i.next();
		}
		return result;
	}

	protected SecurePreferencesRoot getRoot() {
		if (root == null) {
			SecurePreferences result = this;
			while (result.parent() != null)
				result = result.parent();
			root = (SecurePreferencesRoot) result;
		}
		return root;
	}

	protected SecurePreferences navigateToNode(String pathName, boolean create) {
		if (pathName == null || pathName.length() == 0)
			return this;
		int pos = pathName.indexOf(IPath.SEPARATOR);
		if (pos == -1)
			return getChild(pathName, create);
		else if (pos == 0) // if path requested is absolute, pass it to the root without "/"
			return getRoot().navigateToNode(pathName.substring(1), create);
		else { // if path requested contains segments, isolate top segment and rest
			String topSegment = pathName.substring(0, pos);
			String otherSegments = pathName.substring(pos + 1);
			SecurePreferences child = getChild(topSegment, create);
			if (child == null && !create)
				return null;
			return child.navigateToNode(otherSegments, create);
		}
	}

	synchronized private SecurePreferences getChild(String segment, boolean create) {
		if (children == null) {
			if (create)
				children = new HashMap(5);
			else
				return null;
		}
		SecurePreferences child = (SecurePreferences) children.get(segment);
		if (!create || (child != null))
			return child;
		child = new SecurePreferences(this, segment);
		children.put(segment, child);
		return child;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// Load and save

	public void flush() throws IOException {
		getRoot().flush();
	}

	public void flush(Properties properties, String parentsPath) {
		String thisNodePath;
		if (name == null)
			thisNodePath = null;
		else if (parentsPath == null)
			thisNodePath = PATH_SEPARATOR + name;
		else
			thisNodePath = parentsPath + PATH_SEPARATOR + name;

		if (values != null) {
			for (Iterator i = values.keySet().iterator(); i.hasNext();) {
				String key = (String) i.next();
				PersistedPath extenalTag = new PersistedPath(thisNodePath, key);
				properties.put(extenalTag.toString(), values.get(key));
			}
		}

		if (children != null) {
			for (Iterator i = children.entrySet().iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry) i.next();
				SecurePreferences child = (SecurePreferences) entry.getValue();
				child.flush(properties, thisNodePath);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////
	// Basic put() and get()

	public void put(String key, String value, boolean encrypt, SecurePreferencesContainer container) throws StorageException {
		if (key == null)
			throw new NullPointerException();
		checkRemoved();

		if (!encrypt) {
			CryptoData clearValue = new CryptoData(null, null, value.getBytes());
			internalPut(key, clearValue.toString());
			markModified();
			return;
		}

		PasswordExt passwordExt = getPassword(container);

		CryptoData encryptedValue;
		try {
			encryptedValue = getRoot().getCipher().encrypt(passwordExt, value.getBytes());
		} catch (StorageException e) {
			RuntimeException exception = new IllegalStateException();
			exception.initCause(e);
			throw exception;
		}
		internalPut(key, encryptedValue.toString());
		markModified();
	}

	public String get(String key, String def, SecurePreferencesContainer container) throws StorageException {
		checkRemoved();
		if (!hasKey(key))
			return def;
		String encryptedValue = internalGet(key);
		if (encryptedValue == null)
			return null;

		CryptoData data = new CryptoData(encryptedValue);
		String moduleID = data.getModuleID();
		if (moduleID == null) { // clear-text value, not encrypted
			return new String(data.getData());
		}

		try {
			byte[] clearText = getRoot().getCipher().decrypt(getPassword(moduleID, container), data);
			return new String(clearText);
		} catch (IllegalBlockSizeException e) { // invalid password?
			throw new StorageException(StorageException.DECRYPTION_ERROR, e);
		} catch (BadPaddingException e) { // invalid password?
			throw new StorageException(StorageException.DECRYPTION_ERROR, e);
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////
	// Password handling routines

	/**
	 * Provides password for a new entry using:
	 * 1) default password, if any
	 * 2a) if options specify usage of specific module, that module is polled to produce password
	 * 2b) otherwise, password provider with highest priority is used to produce password
	 */
	private PasswordExt getPassword(SecurePreferencesContainer container) throws StorageException {
		PasswordExt defaultPassword = getDefaultPassword(container);
		if (defaultPassword != null)
			return defaultPassword;

		String moduleID = null;
		if (container.hasOption(IProviderHints.REQUIRED_MODULE_ID)) {
			Object idHint = container.getOption(IProviderHints.REQUIRED_MODULE_ID);
			if (idHint instanceof String)
				moduleID = (String) idHint;
		}
		return getRoot().getModulePassword(moduleID, container);
	}

	/**
	 * Provides password using specified password provider module
	 */
	private PasswordExt getPassword(String moduleID, SecurePreferencesContainer container) throws StorageException {
		if (moduleID == null)
			throw new StorageException(StorageException.NO_SECURE_MODULE, SecAuthMessages.invalidEntryFormat);
		if (DEFAULT_PASSWORD_ID.equals(moduleID)) {
			PasswordExt defaultPassword = getDefaultPassword(container);
			if (defaultPassword != null)
				return defaultPassword;
			throw new StorageException(StorageException.NO_SECURE_MODULE, SecAuthMessages.noDefaultPassword);
		}

		return getRoot().getModulePassword(moduleID, container);
	}

	/**
	 * Retrieves default password from options, if any
	 */
	private PasswordExt getDefaultPassword(IPreferencesContainer container) {
		if (container.hasOption(IProviderHints.DEFAULT_PASSWORD)) {
			Object passwordHint = container.getOption(IProviderHints.DEFAULT_PASSWORD);
			if (passwordHint instanceof PBEKeySpec)
				return new PasswordExt((PBEKeySpec) passwordHint, DEFAULT_PASSWORD_ID);
		}
		return null;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////

	synchronized protected void internalPut(String key, String value) {
		if (values == null)
			values = new HashMap(5);
		values.put(key, value);
	}

	protected String internalGet(String key) {
		if (values == null)
			return null;
		return (String) values.get(key);
	}

	protected void markModified() {
		getRoot().setModified(true);
	}

	synchronized public void clear() {
		checkRemoved();
		if (values != null)
			values.clear();
		markModified();
	}

	synchronized public void remove(String key) {
		checkRemoved();
		if (values != null) {
			values.remove(key);
			markModified();
		}
	}

	public void removeNode() {
		checkRemoved();
		if (parent != null)
			parent.removeNode(name);
		markRemoved();
	}

	public void markRemoved() {
		removed = true;
		if (children == null)
			return;
		for (Iterator i = children.entrySet().iterator(); i.hasNext();) {
			Map.Entry entry = (Map.Entry) i.next();
			SecurePreferences child = (SecurePreferences) entry.getValue();
			child.markRemoved();
		}
	}

	synchronized protected void removeNode(String childName) {
		if (children == null)
			return;
		if (children.remove(childName) != null)
			markModified();
	}

	private void checkRemoved() {
		if (removed)
			throw new IllegalStateException(NLS.bind(SecAuthMessages.removedNode, name));
	}

	private void validatePath(String path) {
		if (isValid(path))
			return;
		String msg = NLS.bind(SecAuthMessages.invalidNodePath, path);
		throw new IllegalArgumentException(msg);
	}

	/**
	 * In additions to standard Preferences descriptions of paths, the following 
	 * conditions apply:
	 * Path can contains ASCII characters between 32 and 126 (alphanumerics and printable
	 * characters). 
	 * Path can not contain two or more consecutive forward slashes ('/').
	 * Path can not end with a trailing forward slash. 
	 */
	private boolean isValid(String path) {
		if (path == null || path.length() == 0)
			return true;
		char[] chars = path.toCharArray();
		boolean lastSlash = false;
		for (int i = 0; i < chars.length; i++) {
			if ((chars[i] <= 31) || (chars[i] >= 127))
				return false;
			boolean isSlash = (chars[i] == IPath.SEPARATOR);
			if (lastSlash && isSlash)
				return false;
			lastSlash = isSlash;
		}
		return (chars.length > 1) ? (chars[chars.length - 1] != IPath.SEPARATOR) : true;
	}

	/////////////////////////////////////////////////////////////////////////////////
	// Variations of get() / put() methods adapted to different data types

	public boolean getBoolean(String key, boolean defaultValue, SecurePreferencesContainer container) throws StorageException {
		if (!hasKey(key))
			return defaultValue;
		String value = get(key, null, container);
		return value == null ? defaultValue : TRUE.equalsIgnoreCase(value);
	}

	public void putBoolean(String key, boolean value, boolean encrypt, SecurePreferencesContainer container) throws StorageException {
		put(key, value ? TRUE : FALSE, encrypt, container);
	}

	public int getInt(String key, int defaultValue, SecurePreferencesContainer container) throws StorageException {
		if (!hasKey(key))
			return defaultValue;
		String value = get(key, null, container);
		if (value == null)
			return defaultValue;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public void putInt(String key, int value, boolean encrypt, SecurePreferencesContainer container) throws StorageException {
		put(key, Integer.toString(value), encrypt, container);
	}

	public long getLong(String key, long defaultValue, SecurePreferencesContainer container) throws StorageException {
		if (!hasKey(key))
			return defaultValue;
		String value = get(key, null, container);
		if (value == null)
			return defaultValue;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public void putLong(String key, long value, boolean encrypt, SecurePreferencesContainer container) throws StorageException {
		put(key, Long.toString(value), encrypt, container);
	}

	public float getFloat(String key, float defaultValue, SecurePreferencesContainer container) throws StorageException {
		if (!hasKey(key))
			return defaultValue;
		String value = get(key, null, container);
		if (value == null)
			return defaultValue;
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public void putFloat(String key, float value, boolean encrypt, SecurePreferencesContainer container) throws StorageException {
		put(key, Float.toString(value), encrypt, container);
	}

	public double getDouble(String key, double defaultValue, SecurePreferencesContainer container) throws StorageException {
		if (!hasKey(key))
			return defaultValue;
		String value = get(key, null, container);
		if (value == null)
			return defaultValue;
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public void putDouble(String key, double value, boolean encrypt, SecurePreferencesContainer container) throws StorageException {
		put(key, Double.toString(value), encrypt, container);
	}

	public byte[] getByteArray(String key, byte[] defaultValue, SecurePreferencesContainer container) throws StorageException {
		if (!hasKey(key))
			return defaultValue;
		String value = get(key, null, container);
		return (value == null) ? defaultValue : Base64.decode(value);
	}

	public void putByteArray(String key, byte[] value, boolean encrypt, SecurePreferencesContainer container) throws StorageException {
		put(key, Base64.encode(value), encrypt, container);
	}

	protected boolean hasKey(String key) {
		checkRemoved();
		return (values == null) ? false : values.containsKey(key);
	}

	public boolean isModified() {
		return getRoot().isModified();
	}

	public boolean isEncrypted(String key) throws StorageException {
		checkRemoved();
		if (!hasKey(key))
			return false;
		String encryptedValue = internalGet(key);
		if (encryptedValue == null)
			return false;

		CryptoData data = new CryptoData(encryptedValue);
		String moduleID = data.getModuleID();
		return (moduleID != null);
	}
}
