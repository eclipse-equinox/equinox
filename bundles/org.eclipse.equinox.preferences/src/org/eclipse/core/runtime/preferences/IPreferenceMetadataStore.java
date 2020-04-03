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

/**
 * Provides metadata-based access to a preference store.
 *
 * @since 3.8
 */
public interface IPreferenceMetadataStore {

	/**
	 * Checks if this value type can be handled by this preference store.
	 *
	 * @param <V> the value type for the preference
	 * @param valueType the value type to be checked
	 *
	 * @return true if this value type can be handled by this preference store and false otherwise
	 */
	<V> boolean handles(Class<V> valueType);

	/**
	 * Loads the value of specified preference from an enclosed storage.
	 * If the value is not found returns the preference default value.
	 *
	 * @param <V> the value type for the preference
	 * @param preference the preference metadata, must not be <code>null</code>.
	 *
	 * @return the preference value or default value if preference is unknown
	 * @throws UnsupportedOperationException for unsupported preference value types
	 *
	 * @see #handles(Class)
	 * @see PreferenceMetadata
	 */
	<V> V load(PreferenceMetadata<V> preference);

	/**
	 * Saves the value of specified preference to the enclosed storage.
	 *
	 * @param <V> the value type for the preference
	 * @param value to be saved, must not be <code>null</code>.
	 * @param preference the preference metadata, must not be <code>null</code>.
	 *
	 * @throws UnsupportedOperationException for unsupported preference value types
	 *
	 * @see #handles(Class)
	 * @see PreferenceMetadata
	 */
	<V> void save(V value, PreferenceMetadata<V> preference);

}
