/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.metatype;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeService;

/**
 * 
 * @since 1.2
 *
 */
public interface EquinoxMetaTypeService extends MetaTypeService {
	EquinoxMetaTypeInformation getMetaTypeInformation(Bundle bundle);
}
