/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import java.util.Map;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;

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

	/**
	 * Returns the directives declared with the description.
	 * This will return all known directives for the type of description.
	 * The set of directives differs for each description type.
	 * @return the known directives declared with the description
	 * @since 3.7
	 */
	public Map<String, String> getDeclaredDirectives();

	/**
	 * Returns the attributes declared with the description.
	 * This will return all known attributes for the type of description.
	 * The set of attributes differs for each description type.
	 * @return the attributes declared with the description
	 * @since 3.7
	 */
	public Map<String, Object> getDeclaredAttributes();

	/**
	 * Returns the capability represented by this description.
	 * Some descriptions types may not be able to represent 
	 * a capability.  In such cases <code>null</code> is
	 * returned.
	 * @return the capability represented by this base description
	 * @since 3.7
	 */
	public BundleCapability getCapability();
}
