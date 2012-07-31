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

package org.eclipse.osgi.internal.hookregistry;

import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;

/**
 * A factory that wraps bundle file objects.
 */
public interface BundleFileWrapperFactoryHook {
	/**
	 * Wraps a bundle file for the given content and base data.  If the 
	 * specified bundle file should not be wrapped then null is returned 
	 * @param bundleFile the bundle file to be wrapped
	 * @param generation the generation the bundle file is for
	 * @param base true if the content is for the base bundle (not an inner jar, directory etc.)
	 * @return a wrapped bundle file for the specified content, or null if the bundle content
	 * is not wrapped.
	 */
	BundleFile wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base);

}
