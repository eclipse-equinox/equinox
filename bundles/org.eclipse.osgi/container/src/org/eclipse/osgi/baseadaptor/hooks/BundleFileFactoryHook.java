/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor.hooks;

import java.io.IOException;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;

/**
 * A factory that creates bundle file objects.
 * @see BaseAdaptor#createBundleFile(Object, BaseData)
 * @since 3.2
 */
public interface BundleFileFactoryHook {
	/**
	 * Creates a bundle file for the given content and base data. 
	 * @param content The object which contains the content of a bundle file.
	 * @param data The base data associated with the content
	 * @param base true if the content is for the base bundle (not an inner jar, directory etc.)
	 * @return a new bundle file for the specified content, or null if this factory cannot
	 * create a bundle file for the specified content.
	 * @throws IOException if an IO error occurs
	 */
	BundleFile createBundleFile(Object content, BaseData data, boolean base) throws IOException;
}
