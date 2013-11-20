/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container.namespaces;

import org.osgi.resource.Namespace;

/**
 * Equinox Namespace for fragment capabilities.
 * 
 * <p>
 * This class defines the names for the attributes and directives for this
 * namespace. All unspecified capability attributes are of type {@code String}
 * and are used as arbitrary matching attributes for the capability. The values
 * associated with the specified directive and attribute keys are of type
 * {@code String}, unless otherwise indicated.
 * 
 * @Immutable
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 3.10
 */
public class EquinoxFragmentNamespace extends Namespace {

	/**
	/**
	 * Namespace name for fragment capabilities and requirements.
	 * 
	 * <p>
	 * Also, the capability attribute used to specify the symbolic name of the host the
	 * resource is providing a fragment for.
	 */
	public static final String FRAGMENT_NAMESPACE = "equinox.fragment"; //$NON-NLS-1$
}
