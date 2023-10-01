/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation.
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
package org.eclipse.equinox.metatype.impl;

import java.net.URL;
import java.util.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;

/*
 * Fragment Utilities
 */
public class FragmentUtils {

	/*
	 * 
	 */
	public static boolean isFragment(Bundle bundle) {
		return (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	/*
	 * Find all the URLs to entries for the bundle and its fragments.
	 */
	public static URL[] findEntries(Bundle bundle, String path) {
		String directory = "/"; //$NON-NLS-1$
		String file = "*"; //$NON-NLS-1$
		int index = path.lastIndexOf(MetaTypeProviderImpl.DIRECTORY_SEP);
		switch (index) {
		case -1:
			file = path;
			break;
		case 0:
			if (path.length() > 1)
				file = path.substring(1);
			break;
		default:
			directory = path.substring(0, index);
			file = path.substring(index + 1);
		}
		Enumeration<URL> entries = bundle.findEntries(directory, file, false);
		if (entries == null)
			return null;
		List<URL> list = Collections.list(entries);
		return list.toArray(new URL[list.size()]);
	}
}
