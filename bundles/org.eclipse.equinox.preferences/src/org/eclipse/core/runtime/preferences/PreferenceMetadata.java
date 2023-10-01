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

import java.util.Objects;
import org.eclipse.core.internal.preferences.PrefsMessages;

/**
 * The preference metadata provides the information needed to configure
 * everything about the preference except the preference value itself.
 *
 * @param <V> the value type for the preference
 *
 * @see IPreferenceMetadataStore
 *
 * @since 3.8
 */
public final class PreferenceMetadata<V> {

	private final Class<V> clazz;
	private final String identifier;
	private final V defaultValue;
	private final String name;
	private final String description;

	/**
	 * Created an instance of {@link PreferenceMetadata} using name as description
	 *
	 * @param clazz        the value type of the preference, must not be
	 *                     <code>null</code>
	 * @param identifier   the identifier of the preference, must not be
	 *                     <code>null</code>
	 * @param defaultValue the default value of the preference, must not be
	 *                     <code>null</code>
	 * @param name         the name of the preference, must not be <code>null</code>
	 *
	 * @see PreferenceMetadata#PreferenceMetadata(Class, String, Object, String,
	 *      String)
	 */
	public PreferenceMetadata(Class<V> clazz, String identifier, V defaultValue, String name) {
		this(clazz, identifier, defaultValue, name, name);
	}

	/**
	 * Created an instance of {@link PreferenceMetadata} of all the the given
	 * parameters
	 *
	 * @param clazz        the value type of the preference, must not be
	 *                     <code>null</code>
	 * @param identifier   the identifier of the preference, must not be
	 *                     <code>null</code>
	 * @param defaultValue the default value of the preference, must not be
	 *                     <code>null</code>
	 * @param name         the name of the preference, must not be <code>null</code>
	 * @param description  the description of the preference, must not be
	 *                     <code>null</code>
	 *
	 * @see PreferenceMetadata#PreferenceMetadata(Class, String, Object, String)
	 */
	public PreferenceMetadata(Class<V> clazz, String identifier, V defaultValue, String name, String description) {
		Objects.requireNonNull(clazz, PrefsMessages.PreferenceMetadata_e_null_value_type);
		Objects.requireNonNull(identifier, PrefsMessages.PreferenceMetadata_e_null_identifier);
		Objects.requireNonNull(defaultValue, PrefsMessages.PreferenceMetadata_e_null_default_value);
		Objects.requireNonNull(name, PrefsMessages.PreferenceMetadata_e_null_name);
		Objects.requireNonNull(description, PrefsMessages.PreferenceMetadata_e_null_description);
		this.clazz = clazz;
		this.identifier = identifier;
		this.defaultValue = defaultValue;
		this.name = name;
		this.description = description;
	}

	/**
	 * The preference identifier to use as a key to access the preference value.
	 * Must not be <code>null</code>.
	 *
	 * @return the identifier
	 */
	public String identifer() {
		return identifier;
	}

	/**
	 * The default value for the preference. Must not be <code>null</code>.
	 *
	 * @return the default value
	 */
	public V defaultValue() {
		return defaultValue;
	}

	/**
	 * Briefly describes the preference purpose, intended to be used in UI. Must not
	 * be <code>null</code> and should be localized. Should not be blank.
	 *
	 * @return the name
	 */
	public String name() {
		return name;
	}

	/**
	 * Widely describes the preference purpose, intended to be used in UI. Must not
	 * be <code>null</code> and should be localized. May be blank.
	 *
	 * @return the description
	 */
	public String description() {
		return description;
	}

	/**
	 * The type of preference value needed to perform type checks. Must not be
	 * <code>null</code>.
	 *
	 * @return the value class
	 */
	public Class<V> valueClass() {
		return clazz;
	}
}
