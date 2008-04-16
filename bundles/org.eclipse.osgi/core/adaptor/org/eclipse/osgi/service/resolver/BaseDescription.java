/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import org.osgi.framework.Version;

/**
 * This class represents a base description object for a state.  All description
 * objects in a state have a name and a version.
 * <p>
 * This interface is not intended to be implemented by clients.  The
 * {@link StateObjectFactory} should be used to construct instances.
 * </p>
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface BaseDescription {
	/**
	 * Returns the name.
	 * @return the name
	 */
	public String getName();

	/**
	 * Returns the version.
	 * @return the version
	 */
	public Version getVersion();

	/**
	 * Returns the bundle which supplies this base description
	 * @return the bundle which supplies this base description
	 * @since 3.2
	 */
	public BundleDescription getSupplier();
}
