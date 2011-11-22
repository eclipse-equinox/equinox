/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.metatype;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;

/**
 * A {@link MetaTypeService} providing {@link MetaTypeInformation} instances 
 * with {@link Extendable extendable} versions of {@link 
 * EquinoxObjectClassDefinition object class definitions}.
 * @since 1.2
 *
 */
public interface EquinoxMetaTypeService extends MetaTypeService {
	/**
	 * Returns a {@link MetaTypeInformation} that provides {@link Extendable 
	 * extendable} versions of {@link EquinoxObjectClassDefinition object class
	 * definitions}.
	 */
	EquinoxMetaTypeInformation getMetaTypeInformation(Bundle bundle);
}
