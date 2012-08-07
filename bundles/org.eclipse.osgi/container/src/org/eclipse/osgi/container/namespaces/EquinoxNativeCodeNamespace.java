/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container.namespaces;

import org.osgi.framework.Constants;
import org.osgi.resource.Namespace;

/**
 * Eclipse Platform and Requirement Namespace.
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
public class EquinoxNativeCodeNamespace extends Namespace {

	/**
	 * Namespace name for native code.  Unlike typical name spaces
	 * this namespace is not intended to be used as an attribute.
	 */
	public static final String EQUINOX_NATIVECODE_NAMESPACE = "equinox.native.code"; //$NON-NLS-1$

	/**
	 * Specifies the supported canonical name for the environment OS.
	 * All names are lower case.
	 * The value of this attribute must be of type {@code String}.
	 */
	public static final String CAPABILITY_OS_NAME_ATTRIBUTE = "osname"; //$NON-NLS-1$

	/**
	 * Specifies the environment OS version.
	 * The value of this attribute must be of type {@code Version}.
	 */
	public static final String CAPABILITY_OS_VERSION_ATTRIBUTE = "osversion"; //$NON-NLS-1$

	/**
	 * Specifies the canonical name for the environment processor.  The value
	 * is lower case.
	 * The value of this attribute must be of type {@code String}.
	 */
	public static final String CAPABILITY_PROCESSOR_ATTRIBUTE = "processor"; //$NON-NLS-1$

	/**
	 * Specifies the language supported by the environment.  The value
	 * of this attribute must be of type {@code String}.
	 */
	public static final String CAPABILITY_LANGUAGE_ATTRIBUTE = "language"; //$NON-NLS-1$

	/**
	 * Specifies the paths to the native libraries for the native code requirement.  If this
	 * attribute is not specified then the requirement is an overall requirement for native
	 * code.  Such a requirement is used to or all the other native code requirements together
	 * to determine if the module is resolved or not.
	 * The value of this attribute must be of type {@code List<String>}.
	 */
	public static final String REQUIREMENT_NATIVE_PATHS_ATTRIBUTE = "native.paths"; //$NON-NLS-1$

	/**
	 * Specifies the arbitrary selection filter for the native code requirement.  This filter 
	 * is matched against the system properties.
	 * @see Constants#SELECTION_FILTER_ATTRIBUTE
	 */
	public static final String REQUIREMENT_SELECTION_FILTER_DIRECTIVE = "selection.filter"; //$NON-NLS-1$
}
