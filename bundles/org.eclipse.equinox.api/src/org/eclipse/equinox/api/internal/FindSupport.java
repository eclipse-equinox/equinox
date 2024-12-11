/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - use parameterized types (bug 442021)
 *******************************************************************************/
package org.eclipse.equinox.api.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

// This class provides implements the find* methods exposed on Platform.
// It does the lookup in bundles and fragments and does the variable replacement.
// Can only be used if OSGi is available.
public class FindSupport {
	// OSGI system properties
	public static final String PROP_NL = "osgi.nl"; //$NON-NLS-1$
	public static final String PROP_OS = "osgi.os"; //$NON-NLS-1$
	public static final String PROP_WS = "osgi.ws"; //$NON-NLS-1$
	public static final String PROP_ARCH = "osgi.arch"; //$NON-NLS-1$

	private static final String[] NL_JAR_VARIANTS = buildNLVariants(APISupport.getProperty(PROP_NL));

	private static String[] buildNLVariants(String nl) {
		ArrayList<String> result = new ArrayList<>();
		if (nl != null) {
			IPath base = IPath.fromOSString("nl"); //$NON-NLS-1$

			IPath path = IPath.fromOSString(nl.replace('_', '/'));
			while (path.segmentCount() > 0) {
				result.add(base.append(path).toString());
				// for backwards compatibility only, don't replace the slashes
				if (path.segmentCount() > 1)
					result.add(base.append(path.toString().replace('/', '_')).toString());
				path = path.removeLastSegments(1);
			}
		}

		return result.toArray(new String[result.size()]);
	}

//	/**
//	 * See doc on {@link FileLocator#find(Bundle, IPath, Map)}
//	 */
	public static URL find(Bundle bundle, IPath path) {
		return find(bundle, path, null);
	}

//	/**
//	 * See doc on {@link FileLocator#find(Bundle, IPath, Map)}
//	 */
	public static URL find(Bundle b, IPath path, Map<String, String> override) {
		return find(b, path, override, null);
	}

//	/**
//	 * See doc on {@link FileLocator#findEntries(Bundle, IPath)}
//	 */
	public static URL[] findEntries(Bundle bundle, IPath path) {
		return findEntries(bundle, path, null);
	}

//	/**
//	 * See doc on {@link FileLocator#findEntries(Bundle, IPath, Map)}
//	 */
	public static URL[] findEntries(Bundle bundle, IPath path, Map<String, String> override) {
		ArrayList<URL> results = new ArrayList<>(1);
		find(bundle, path, override, results);
		return results.toArray(new URL[results.size()]);
	}

	private static URL find(Bundle b, IPath path, Map<String, String> override, ArrayList<URL> multiple) {
		if (path == null)
			return null;

		URL result = null;

		// Check for the empty or root case first
		if (path.isEmpty() || path.isRoot()) {
			// Watch for the root case. It will produce a new
			// URL which is only the root directory (and not the
			// root of this plugin).
			result = findInPlugin(b, IPath.EMPTY, multiple);
			if (result == null || multiple != null)
				result = findInFragments(b, IPath.EMPTY, multiple);
			return result;
		}

		// Now check for paths without variable substitution
		String first = path.segment(0);
		if (first.charAt(0) != '$') {
			result = findInPlugin(b, path, multiple);
			if (result == null || multiple != null)
				result = findInFragments(b, path, multiple);
			return result;
		}

		// Worry about variable substitution
		IPath rest = path.removeFirstSegments(1);
		if (first.equalsIgnoreCase("$nl$")) //$NON-NLS-1$
			return findNL(b, rest, override, multiple);
		if (first.equalsIgnoreCase("$os$")) //$NON-NLS-1$
			return findOS(b, rest, override, multiple);
		if (first.equalsIgnoreCase("$ws$")) //$NON-NLS-1$
			return findWS(b, rest, override, multiple);
		if (first.equalsIgnoreCase("$files$")) //$NON-NLS-1$
			return null;

		return null;
	}

	private static URL findOS(Bundle b, IPath path, Map<String, String> override, ArrayList<URL> multiple) {
		String os = null;
		if (override != null)
			try {
				// check for override
				os = override.get("$os$"); //$NON-NLS-1$
			} catch (ClassCastException e) {
				// just in case
			}
		if (os == null)
			// use default
			os = APISupport.getProperty(PROP_OS);
		if (os.length() == 0)
			return null;

		// Now do the same for osarch
		String osArch = null;
		if (override != null)
			try {
				// check for override
				osArch = override.get("$arch$"); //$NON-NLS-1$
			} catch (ClassCastException e) {
				// just in case
			}
		if (osArch == null)
			// use default
			osArch = APISupport.getProperty(PROP_ARCH);
		if (osArch.length() == 0)
			return null;

		URL result = null;
		IPath base = IPath.fromOSString("os").append(os).append(osArch); //$NON-NLS-1$
		// Keep doing this until all you have left is "os" as a path
		while (base.segmentCount() != 1) {
			IPath filePath = base.append(path);
			result = findInPlugin(b, filePath, multiple);
			if (result != null && multiple == null)
				return result;
			result = findInFragments(b, filePath, multiple);
			if (result != null && multiple == null)
				return result;
			base = base.removeLastSegments(1);
		}
		// If we get to this point, we haven't found it yet.
		// Look in the plugin and fragment root directories
		result = findInPlugin(b, path, multiple);
		if (result != null && multiple == null)
			return result;
		return findInFragments(b, path, multiple);
	}

	private static URL findWS(Bundle b, IPath path, Map<String, String> override, ArrayList<URL> multiple) {
		String ws = null;
		if (override != null)
			try {
				// check for override
				ws = override.get("$ws$"); //$NON-NLS-1$
			} catch (ClassCastException e) {
				// just in case
			}
		if (ws == null)
			// use default
			ws = APISupport.getProperty(PROP_WS);
		IPath filePath = IPath.fromOSString("ws").append(ws).append(path); //$NON-NLS-1$
		// We know that there is only one segment to the ws path
		// e.g. ws/win32
		URL result = findInPlugin(b, filePath, multiple);
		if (result != null && multiple == null)
			return result;
		result = findInFragments(b, filePath, multiple);
		if (result != null && multiple == null)
			return result;
		// If we get to this point, we haven't found it yet.
		// Look in the plugin and fragment root directories
		result = findInPlugin(b, path, multiple);
		if (result != null && multiple == null)
			return result;
		return findInFragments(b, path, multiple);
	}

	private static URL findNL(Bundle b, IPath path, Map<String, String> override, ArrayList<URL> multiple) {
		String nl = null;
		String[] nlVariants = null;
		if (override != null)
			try {
				// check for override
				nl = override.get("$nl$"); //$NON-NLS-1$
			} catch (ClassCastException e) {
				// just in case
			}
		nlVariants = nl == null ? NL_JAR_VARIANTS : buildNLVariants(nl);
		if (nl != null && nl.length() == 0)
			return null;

		URL result = null;
		for (String nlVariant : nlVariants) {
			IPath filePath = IPath.fromOSString(nlVariant).append(path);
			result = findInPlugin(b, filePath, multiple);
			if (result != null && multiple == null)
				return result;
			result = findInFragments(b, filePath, multiple);
			if (result != null && multiple == null)
				return result;
		}
		// If we get to this point, we haven't found it yet.
		// Look in the plugin and fragment root directories
		result = findInPlugin(b, path, multiple);
		if (result != null && multiple == null)
			return result;
		return findInFragments(b, path, multiple);
	}

	private static URL findInPlugin(Bundle b, IPath filePath, ArrayList<URL> multiple) {
		URL result = b.getEntry(filePath.toString());
		if (result != null && multiple != null)
			multiple.add(result);
		return result;
	}

	private static URL findInFragments(Bundle b, IPath filePath, ArrayList<URL> multiple) {
		Bundle[] fragments = APISupport.getFragments(b);
		if (fragments == null)
			return null;

		if (multiple != null)
			multiple.ensureCapacity(fragments.length + 1);

		for (Bundle fragment : fragments) {
			URL fileURL = fragment.getEntry(filePath.toString());
			if (fileURL != null) {
				if (multiple == null)
					return fileURL;
				multiple.add(fileURL);
			}
		}
		return null;
	}

//	/**
//	 * See doc on {@link FileLocator#openStream(Bundle, IPath, boolean)}
//	 */
	public static final InputStream openStream(Bundle bundle, IPath file, boolean substituteArgs) throws IOException {
		URL url = null;
		if (!substituteArgs) {
			url = findInPlugin(bundle, file, null);
			if (url == null)
				url = findInFragments(bundle, file, null);
		} else {
			url = FindSupport.find(bundle, file);
		}
		if (url != null)
			return url.openStream();
		throw new IOException("Cannot find " + file.toString()); //$NON-NLS-1$
	}

	/**
	 * See doc on {@link FileLocator#find(URL)}
	 */
	public static URL find(URL url) {
		// if !platform/plugin | fragment URL return
		if (!"platform".equalsIgnoreCase(url.getProtocol())) //$NON-NLS-1$
			return null;

		// call a helper method to get the bundle object and rest of the path
		String spec = url.getFile().trim();
		Object[] obj = null;
		try {
			Class<?> clz = APISupport.equinoxCommonBundle
					.loadClass("org.eclipse.core.internal.runtime.PlatformURLPluginConnection");
			Method method = clz.getMethod("parse", String.class, URL.class);
			obj = (Object[]) method.invoke(null, spec, url);
		} catch (Exception e) {
			APISupport.log(new Status(IStatus.ERROR, APISupport.PI_RUNTIME, "Can't parse input url:" + url, e)); //$NON-NLS-1$
			return null;
		}
		Bundle bundle = (Bundle) obj[0];
		String path = (String) obj[1];

		// use FileLocator.find(bundle, path, null) to look for the file
		if ("/".equals(path)) //$NON-NLS-1$
			return bundle.getEntry(path);
		return find(bundle, IPath.fromOSString(path), null);
	}
}
