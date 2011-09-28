/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.impl;

import java.net.URL;
import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

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
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null)
			return null;
		String directory = "/"; //$NON-NLS-1$
		String file = "*"; //$NON-NLS-1$
		int index = path.lastIndexOf(MetaTypeProviderImpl.DIRECTORY_SEP);
		switch (index) {
			case -1 :
				file = path;
				break;
			case 0 :
				if (path.length() > 1)
					file = path.substring(1);
				break;
			default :
				directory = path.substring(0, index);
				file = path.substring(index + 1);
		}
		List<URL> entries = wiring.findEntries(directory, file, 0);
		if (entries == null)
			return null;
		return entries.toArray(new URL[entries.size()]);
	}
}
