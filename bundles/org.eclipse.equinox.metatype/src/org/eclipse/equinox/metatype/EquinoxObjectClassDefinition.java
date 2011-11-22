/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.metatype;

import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * An {@link Extendable extendable} version of {@link ObjectClassDefinition 
 * object class definition} allowing access to any {@link 
 * Extendable#getExtensionAttributes(String) extension attributes} provided by
 * third parties.
 * <p/>
 * For example, an Equinox object class definition will contain all XML 
 * attributes specified as part of the &lt;OCD/&gt; element with namespaces
 * other than the metatype namespace as extension attributes.
 * 
 * @since 1.2
 *
 */
public interface EquinoxObjectClassDefinition extends ObjectClassDefinition, Extendable {
	/**
	 * Returns {@link Extendable extendable} versions of {@link 
	 * EquinoxAttributeDefinition attribute definitions}.
	 */
	EquinoxAttributeDefinition[] getAttributeDefinitions(int filter);
}
