/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import java.util.Dictionary;
import org.osgi.framework.Version;

/**
 * A description of a generic capability.
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface GenericDescription extends BaseDescription {
	/**
	 * The default type of generic capability.
	 */
	public static String DEFAULT_TYPE = "generic"; //$NON-NLS-1$

	/**
	 * Returns the arbitrary attributes for this description
	 * @return the arbitrary attributes for this description
	 */
	public Dictionary<String, Object> getAttributes();

	/**
	 * Returns the type of generic description capability
	 * @return the type of generic description capability
	 */
	public String getType();

	/**
	 * This method is deprecated.  Capabilities do not always have a 
	 * name associated with them.  All matching attributes associated
	 * with a capability are available in the attributes of a 
	 * capability.  This method will return the value of the 
	 * attribute with the same key as this capabilities type.
	 * If this attribute's value is not a String then null is
	 * returned.
	 * @deprecated matching should only be done against a capability's 
	 * attributes.
	 */
	public String getName();

	/**
	 * This method is deprecated.  Capabilities do not always have a
	 * version associated with them. All matching attributes associated
	 * with a capability are available in the attributes of a
	 * capability.  This method will return the value of the 
	 * attribute with the key <code>"version"</code>.
	 * If this attribute's value is not a {@link Version} then null is
	 * returned.
	 * @deprecated matching should only be done against a capability's
	 * attributes.
	 */
	public Version getVersion();
}
