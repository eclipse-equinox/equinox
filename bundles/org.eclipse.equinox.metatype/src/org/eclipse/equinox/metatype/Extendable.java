/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.metatype;

import java.util.Map;
import java.util.Set;

/**
 * An interface marking an object as possibly having extension attributes. An
 * attribute is a property that describes the object. An extension attribute is
 * a custom property added by a third party.
 * 
 * The use case for this interface was to expose third party attributes
 * included in the metadata XML on elements that support <anyAttribute/>
 * according to the metatype schema, but it is not strictly limited to this use.
 * 
 * @since 1.2
 *
 */
public interface Extendable {
	/**
	 * Returns a {@link Map map} containing the extension attributes for the
	 * specified {@link #getExtensionUris() URI}. The map key is the attribute
	 * name, and the map value is the attribute value. If the specified URI has
	 * no extension attributes, the map will be {@code null}.
	 * 
	 * @param uri - The URI for which extension attributes are desired.
	 * @return A map containing the extension attributes for the specified URI,
	 *         or {@code null} if there are none.
	 * @see #getExtensionUris()
	 */
	Map<String, String> getExtensionAttributes(String uri);

	/**
	 * Returns the {@link Set set} of URIs for which {@link 
	 * #getExtensionAttributes(String) extension attributes} exist. It is
	 * guaranteed that there is at least one extension attribute for each URI
	 * in the set. 
	 * @return A set containing the URIs for which extension attributes exist.
	 * @see #getExtensionAttributes(String)
	 */
	Set<String> getExtensionUris();
}
