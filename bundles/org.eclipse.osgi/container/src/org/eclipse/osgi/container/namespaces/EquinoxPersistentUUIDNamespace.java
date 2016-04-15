/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container.namespaces;

import org.osgi.framework.BundleContext;
import org.osgi.resource.Namespace;

/**
 * Equinox Namespace for the persistent UUID of the system bundle.
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
 * @since 3.12
 */
public class EquinoxPersistentUUIDNamespace extends Namespace {

	/**
	 * Namespace name for the persistent uuid capabilities and requirements.
	 * 
	 * <p>
	 * Also, the capability attribute used to specify the persistent UUID of the system bundle.  Also used
	 * as the {@link BundleContext#getProperty(String) context property} key to hold the
	 * persistent UUID.
	 */
	public static final String PERSISTENT_UUID_NAMESPACE = "org.eclipse.equinox.persistent.uuid"; //$NON-NLS-1$
}
