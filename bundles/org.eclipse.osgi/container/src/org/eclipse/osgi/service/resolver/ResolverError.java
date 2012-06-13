/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
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
 *  * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
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
	 * the correct permissions to provide the required symbolic name.
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
	 * Error type constant (bit mask) indicating that a bundle could not be
	 * resolved because the required execution enviroment did not match the runtime
	 * environment.
	 * @see ResolverError#getType()
	 */
	public static final int MISSING_EXECUTION_ENVIRONMENT = 0x4000;

	/**
	 * Error type constant (bit mask) indicating that a bundle could not be 
	 * resolved because the required generic capability could not be resolved.
	 */
	public static final int MISSING_GENERIC_CAPABILITY = 0x8000;

	/**
	 * Error type constant (bit mask) indicating that a bundle could not be
	 * resolved because no match was found for the  native code specification.
	 * @since 3.4
	 */
	public static final int NO_NATIVECODE_MATCH = 0x10000;

	/**
	 * Error type constant (bit mask) indicating that a bundle could not be
	 * resolved because the matching native code paths are invalid.
	 * @since 3.4
	 */
	public static final int INVALID_NATIVECODE_PATHS = 0x20000;

	/**
	 * Error type constant (bit mask) indicating that a bundle could not be
	 * resolved because the bundle was disabled
	 * @since 3.4
	 */
	public static final int DISABLED_BUNDLE = 0x40000;

	/**
	 * Error type constant (bit mask) indicating that a Require-Capability could
	 * not be resolved because the requiring bundle does not have the correct
	 * permissions to require the capability.
	 * @see ResolverError#getType()
	 * @since 3.7
	 */
	public static final int REQUIRE_CAPABILITY_PERMISSION = 0x80000;

	/**
	 * Error type constant (bit mask) indicating that a Require-Bundle could
	 * not be resolved because no bundle with the required symbolic name has 
	 * the correct permissions to provide the required symbolic name.
	 * @see ResolverError#getType()
	 * @since 3.7
	 */
	public static final int PROVIDE_CAPABILITY_PERMISSION = 0x100000;

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

	/**
	 * Returns the unsatisfied constraint if this ResolverError occurred 
	 * because of an unsatisfied constraint; otherwise <code>null</code> 
	 * is returned.
	 * @return the unsatisfied constraint or <code>null</code>.
	 */
	public VersionConstraint getUnsatisfiedConstraint();
}
