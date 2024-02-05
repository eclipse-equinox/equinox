/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.equinox.metatype;

import org.osgi.service.metatype.MetaTypeInformation;

/**
 * A {@link MetaTypeInformation} that provides {@link Extendable extendable}
 * versions of {@link EquinoxObjectClassDefinition object class definitions}.
 * 
 * @since 1.2
 */
public interface EquinoxMetaTypeInformation extends MetaTypeInformation {
	/**
	 * Returns {@link Extendable extendable} versions of
	 * {@link EquinoxObjectClassDefinition object class definitions}.
	 */
	EquinoxObjectClassDefinition getObjectClassDefinition(String id, String locale);
}
