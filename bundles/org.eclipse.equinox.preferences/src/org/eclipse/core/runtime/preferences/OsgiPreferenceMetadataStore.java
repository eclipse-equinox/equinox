/*******************************************************************************
 * Copyright (c) 2020 ArSysOp and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.preferences;

import java.util.*;
import org.eclipse.core.internal.preferences.PrefsMessages;
import org.eclipse.osgi.util.NLS;

/**
 * The preference store implementation that uses OSGi preference node as an
 * enclosed storage.
 *
 * @see IEclipsePreferences
 *
 * @since 3.8
 */
public final class OsgiPreferenceMetadataStore implements IPreferenceMetadataStore {

	private static final Set<Class<?>> CLASSES = Collections.unmodifiableSet(//
			new HashSet<>(Arrays.asList(//
					Boolean.class, //
					byte[].class, //
					Double.class, //
					Float.class, //
					Integer.class, //
					Long.class, //
					String.class)));

	private final IEclipsePreferences preferences;

	/**
	 *
	 * @param preferences the OSGi preference node, must not be <code>null</code>
	 */
	public OsgiPreferenceMetadataStore(IEclipsePreferences preferences) {
		Objects.requireNonNull(preferences, PrefsMessages.OsgiPreferenceMetadataStore_e_null_preference_node);
		this.preferences = preferences;
	}

	@Override
	public <V> boolean handles(Class<V> valueType) {
		return CLASSES.contains(valueType);
	}

	@Override
	public <V> V load(PreferenceMetadata<V> preference) {
		Class<V> valueClass = preference.valueClass();
		String identifer = preference.identifer();
		V defaultValue = preference.defaultValue();
		if (String.class.equals(valueClass)) {
			return valueClass.cast(preferences.get(identifer, String.class.cast(defaultValue)));
		} else if (Boolean.class.equals(valueClass)) {
			return valueClass.cast(preferences.getBoolean(identifer, Boolean.class.cast(defaultValue)));
		} else if (byte[].class.equals(valueClass)) {
			return valueClass.cast(preferences.getByteArray(identifer, byte[].class.cast(defaultValue)));
		} else if (Double.class.equals(valueClass)) {
			return valueClass.cast(preferences.getDouble(identifer, Double.class.cast(defaultValue)));
		} else if (Float.class.equals(valueClass)) {
			return valueClass.cast(preferences.getFloat(identifer, Float.class.cast(defaultValue)));
		} else if (Integer.class.equals(valueClass)) {
			return valueClass.cast(preferences.getInt(identifer, Integer.class.cast(defaultValue)));
		} else if (Long.class.equals(valueClass)) {
			return valueClass.cast(preferences.getLong(identifer, Long.class.cast(defaultValue)));
		}
		String message = PrefsMessages.PreferenceStorage_e_load_unsupported;
		throw new UnsupportedOperationException(NLS.bind(message, preference, valueClass));
	}

	@Override
	public <V> void save(V value, PreferenceMetadata<V> preference) {
		Class<V> valueClass = preference.valueClass();
		String identifer = preference.identifer();
		if (String.class.equals(valueClass)) {
			preferences.put(identifer, String.class.cast(value));
		} else if (Boolean.class.equals(valueClass)) {
			preferences.putBoolean(identifer, Boolean.class.cast(value));
		} else if (byte[].class.equals(valueClass)) {
			preferences.putByteArray(identifer, byte[].class.cast(value));
		} else if (Double.class.equals(valueClass)) {
			preferences.putDouble(identifer, Double.class.cast(value));
		} else if (Float.class.equals(valueClass)) {
			preferences.putFloat(identifer, Float.class.cast(value));
		} else if (Integer.class.equals(valueClass)) {
			preferences.putInt(identifer, Integer.class.cast(value));
		} else if (Long.class.equals(valueClass)) {
			preferences.putLong(identifer, Long.class.cast(value));
		} else {
			String message = PrefsMessages.PreferenceStorage_e_save_unsupported;
			throw new UnsupportedOperationException(NLS.bind(message, preference, valueClass));
		}
	}

}
