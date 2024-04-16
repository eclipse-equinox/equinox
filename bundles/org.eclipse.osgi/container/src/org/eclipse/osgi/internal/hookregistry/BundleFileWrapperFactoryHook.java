/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.hookregistry;

import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;

/**
 * A factory that wraps bundle file objects.
 */
public interface BundleFileWrapperFactoryHook {
	/**
	 * Wraps a bundle file for the given content and base data. If the specified
	 * bundle file should not be wrapped then null is returned
	 * 
	 * @param bundleFile the bundle file to be wrapped
	 * @param generation the generation the bundle file is for
	 * @param base       true if the content is for the base bundle (not an inner
	 *                   jar, directory etc.)
	 * @return a wrapped bundle file for the specified content, or null if the
	 *         bundle content is not wrapped.
	 */
	BundleFileWrapper wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base);

}
