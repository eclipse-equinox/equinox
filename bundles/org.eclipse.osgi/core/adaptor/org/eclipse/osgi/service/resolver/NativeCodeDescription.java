/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

import org.osgi.framework.Filter;

/**
 * This class represents a native code description.
 * <p>
 * This interface is not intended to be implemented by clients.  The
 * {@link StateObjectFactory} should be used to construct instances.
 * </p>
 * @since 3.4
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface NativeCodeDescription extends BaseDescription, Comparable {
	/**
	 * Returns the paths to the native code libraries.
	 * @return the paths to the native code libraries.
	 */
	public String[] getNativePaths();

	/**
	 * Returns the processors supported by the native code.
	 * @return the processors supported by the native code.  An 
	 * empty array is returned if no processors are supported.
	 */
	public String[] getProcessors();

	/**
	 * Returns the operating system names supported by the native code.
	 * @return the operating system names supported by the native code.
	 * An empty array is returned if no operating systems are supported.
	 */
	public String[] getOSNames();

	/**
	 * Returns the operating system version ranges supported by the native code.
	 * @return the operating system version ranges supported by the native code.
	 * An empty array is returned if all versions are supported.
	 */
	public VersionRange[] getOSVersions();

	/**
	 * Returns the languages supported by the native code.
	 * @return the languages supported by the native code.  An empty array is 
	 * returned if all languages are supported.
	 */
	public String[] getLanguages();

	/**
	 * Returns the selection filter used to select the native code.
	 * @return the selection filter used to select the native code.
	 */
	public Filter getFilter();

	/**
	 * Native code descriptions are sorted in by the following preferences
	 * <ul>
	 * <li>The minimum version of the os version ranges</li>
	 * <li>The language<li>
	 * </ul>
	 * @param other
	 * @return
	 */
	public int compareTo(Object other);

	/**
	 * Indicates if this native code description has invalid native code paths.  Native
	 * code paths are invalid if they can not be found in the bundle content. 
	 * @return true if the native code paths are invalid; otherwise false is returned.
	 */
	public boolean hasInvalidNativePaths();
}
