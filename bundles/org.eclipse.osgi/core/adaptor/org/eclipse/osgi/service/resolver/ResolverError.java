/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

/**
 * ResolverErrors represent a single error that prevents a bundle from resolving
 * in a <code>State</code> object.
 */
public interface ResolverError {
	/**
	 * Error type constant (bit mask) indicating that an Import-Package could 
	 * not be resolved.
	 * @see ResolverError#getType()
	 */
	public static final int MISSING_IMPORT_PACKAGE = 0x0001;
	/**
	 * Error type constant (bit mask) indicating that a Require-Bundle could
	 * not be resolved.
	 * @see ResolverError#getType()
	 */
	public static final int MISSING_REQUIRE_BUNDLE = 0x0002;
	/**
	 * Error type constant (bit mask) indicating that a Fragment-Host could
	 * not be resolved.
	 * @see ResolverError#getType()
	 */
	public static final int MISSING_FRAGMENT_HOST = 0x0004;
	/**
	 * Error type constant (bit mask) indicating that the bundle could not
	 * be resolved because another singleton bundle was selected.
	 * @see ResolverError#getType()
	 */
	public static final int SINGLETON_SELECTION = 0x0008;
	/**
	 * Error type constant (bit mask) indicating that the bundle fragment
	 * could not be resolved because a constraint conflict with a host.
	 * @see ResolverError#getType()
	 */
	public static final int FRAGMENT_CONFLICT = 0x0010;
	/**
	 * Error type constant (bit mask) indicating that an Import-Package could
	 * not be resolved because of a uses directive conflict. 
	 * @see ResolverError#getType()
	 */
	public static final int IMPORT_PACKAGE_USES_CONFLICT = 0x0020;
	/**
	 * Error type constant (bit mask) indicating that a Require-Bundle could
	 * not be resolved because of a uses directive conflict.
	 * @see ResolverError#getType()
	 */
	public static final int REQUIRE_BUNDLE_USES_CONFLICT = 0x0040;
	/**
	 * Error type constant (bit mask) indicating that an Import-Package could
	 * not be resolved because the importing bundle does not have the correct
	 * permissions to import the package.
	 * @see ResolverError#getType()
	 */
	public static final int IMPORT_PACKAGE_PERMISSION = 0x0080;
	/**
	 * Error type constant (bit mask) indicating that an Import-Package could
	 * not be resolved because no exporting bundle has the correct
	 * permissions to export the package.
	 * @see ResolverError#getType()
	 */
	public static final int EXPORT_PACKAGE_PERMISSION = 0x0100;
	/**
	 * Error type constant (bit mask) indicating that a Require-Bundle could
	 * not be resolved because the requiring bundle does not have the correct
	 * permissions to require the bundle.
	 * @see ResolverError#getType()
	 */
	public static final int REQUIRE_BUNDLE_PERMISSION = 0x0200;
	/**
	 * Error type constant (bit mask) indicating that a Require-Bundle could
	 * not be resolved because no bundle with the required symbolic name has 
	 * the correct permissions to provied the required symbolic name.
	 * @see ResolverError#getType()
	 */
	public static final int PROVIDE_BUNDLE_PERMISSION = 0x0400;
	/**
	 * Error type constant (bit mask) indicating that a Fragment-Host could
	 * not be resolved because no bundle with the required symbolic name has 
	 * the correct permissions to host a fragment.
	 * @see ResolverError#getType()
	 */
	public static final int HOST_BUNDLE_PERMISSION = 0x0800;
	/**
	 * Error type constant (bit mask) indicating that a Fragment-Host could
	 * not be resolved because the fragment bundle does not have the correct
	 * permissions to be a fragment.
	 * @see ResolverError#getType()
	 */
	public static final int FRAGMENT_BUNDLE_PERMISSION = 0x1000;
	/**
	 * Error type constant (bit mask) indicating that a bundle could not be
	 * resolved because a platform filter did not match the runtime environment.
	 * @see ResolverError#getType()
	 */
	public static final int PLATFORM_FILTER = 0x2000;

	/**
	 * Returns the bundle which this ResolverError is for
	 * @return the bundle which this ResolverError is for
	 */
	public BundleDescription getBundle();

	/**
	 * Returns the type of ResolverError this is
	 * @return the type of ResolverError this is
	 */
	public int getType();

	/**
	 * Returns non-translatable data associated with this ResolverError.
	 * For example, the data for a ResolverError of type MISSING_IMPORT_PACKAGE
	 * could be the Import-Package manifest statement which did not resolve.
	 * @return non-translatable data associated with this ResolverError
	 */
	public String getData();
}
