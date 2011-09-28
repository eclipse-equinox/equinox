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
 * 
 * @since 1.2
 *
 */
public interface Extendable {
	Map<String, String> getExtensionAttributes(String uri);

	Set<String> getExtensionUris();
}
