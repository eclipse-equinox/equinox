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
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;

/**
 * This class combines secure preferences node with specific container. See
 * container for the description of relationships.
 */
public class SecurePreferencesWrapper implements ISecurePreferences {

	final protected SecurePreferences node;

	final protected SecurePreferencesContainer container;

	public SecurePreferencesWrapper(SecurePreferences node, SecurePreferencesContainer container) {
		this.node = node;
		this.container = container;
	}

	// Testing only
	public SecurePreferencesContainer getContainer() {
		return container;
	}

	public String absolutePath() {
		return node.absolutePath();
	}

	public String[] childrenNames() {
		return node.childrenNames();
	}

	public void clear() {
		node.clear();
	}

	public void flush() throws IOException {
		node.flush();
	}

	public String[] keys() {
		return node.keys();
	}

	public String name() {
		return node.name();
	}

	public void remove(String key) {
		node.remove(key);
	}

	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof SecurePreferencesWrapper))
			return false;
		SecurePreferencesWrapper other = (SecurePreferencesWrapper) obj;
		return container.equals(other.container) && node.equals(other.node);
	}

	public int hashCode() {
		String tmp = Integer.toString(container.hashCode()) + '|' + Integer.toString(node.hashCode());
		return tmp.hashCode();
	}

	///////////////////////////////////////////////////////////////////////////
	// Navigation

	public ISecurePreferences node(String pathName) {
		return container.wrapper(node.node(pathName));
	}

	public ISecurePreferences parent() {
		SecurePreferences parent = node.parent();
		if (parent == null)
			return null;
		return container.wrapper(node.parent());
	}

	public boolean nodeExists(String pathName) {
		return node.nodeExists(pathName);
	}

	public void removeNode() {
		container.removeWrapper(node);
		node.removeNode();
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// puts() and gets()

	public String get(String key, String def) throws StorageException {
		return node.get(key, def, container);
	}

	public void put(String key, String value, boolean encrypt) throws StorageException {
		node.put(key, value, encrypt, container);
	}

	public boolean getBoolean(String key, boolean def) throws StorageException {
		return node.getBoolean(key, def, container);
	}

	public void putBoolean(String key, boolean value, boolean encrypt) throws StorageException {
		node.putBoolean(key, value, encrypt, container);
	}

	public int getInt(String key, int def) throws StorageException {
		return node.getInt(key, def, container);
	}

	public void putInt(String key, int value, boolean encrypt) throws StorageException {
		node.putInt(key, value, encrypt, container);
	}

	public float getFloat(String key, float def) throws StorageException {
		return node.getFloat(key, def, container);
	}

	public void putFloat(String key, float value, boolean encrypt) throws StorageException {
		node.putFloat(key, value, encrypt, container);
	}

	public long getLong(String key, long def) throws StorageException {
		return node.getLong(key, def, container);
	}

	public void putLong(String key, long value, boolean encrypt) throws StorageException {
		node.putLong(key, value, encrypt, container);
	}

	public double getDouble(String key, double def) throws StorageException {
		return node.getDouble(key, def, container);
	}

	public void putDouble(String key, double value, boolean encrypt) throws StorageException {
		node.putDouble(key, value, encrypt, container);
	}

	public byte[] getByteArray(String key, byte[] def) throws StorageException {
		return node.getByteArray(key, def, container);
	}

	public void putByteArray(String key, byte[] value, boolean encrypt) throws StorageException {
		node.putByteArray(key, value, encrypt, container);
	}

	public boolean isEncrypted(String key) throws StorageException {
		return node.isEncrypted(key);
	}

	public String getModule(String key) {
		return node.getModule(key);
	}

	public boolean passwordChanging(String moduleID) {
		return node.passwordChanging(container, moduleID);
	}
}
