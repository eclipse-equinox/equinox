/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype;

import java.net.URL;
import java.util.*;
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
	 * Find all the paths to entries for the bundle and its fragments.
	 * Returned data is got by Bundle.getEntryPaths().
	 */
	public static Enumeration<String> findEntryPaths(Bundle bundle, String path) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null)
			return null;

		Vector<String> result = new Vector<String>();
		addEntryPaths(bundle.getEntryPaths(path), result);
		List<BundleRevision> fragments = wiring.getFragmentRevisions();
		if (fragments != null) {
			for (BundleRevision fragment : fragments)
				addEntryPaths(fragment.getBundle().getEntryPaths(path), result);
		}
		return result.size() == 0 ? null : result.elements();
	}

	/*
	 * Internal method - add an path to vector, and check for duplucate.
	 */
	private static void addEntryPaths(Enumeration<String> ePaths, Vector<String> pathList) {

		if (ePaths == null)
			return;
		while (ePaths.hasMoreElements()) {
			String path = ePaths.nextElement();
			if (!pathList.contains(path))
				pathList.add(path);
		}
	}

	/*
	 * Find all the URLs to entries for the bundle and its fragments.
	 * Returned data is got by Bundle.getEntry().
	 */
	public static URL[] findEntries(Bundle bundle, String path) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null)
			return null;

		URL url = bundle.getEntry(path);
		List<BundleRevision> fragments = wiring.getFragmentRevisions();
		if (fragments == null || fragments.size() == 0)
			return url == null ? null : new URL[] {url};
		ArrayList<URL> result = new ArrayList<URL>();
		if (url != null)
			result.add(url);

		for (BundleRevision fragment : fragments) {
			URL fragUrl = fragment.getBundle().getEntry(path);
			if (fragUrl != null)
				result.add(fragUrl);
		}
		return result.size() == 0 ? null : result.toArray(new URL[result.size()]);
	}
}
