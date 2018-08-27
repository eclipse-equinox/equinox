/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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

package org.eclipse.osgi.storage;

import java.io.File;
import java.util.*;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleRevision;

public class NativeCodeFinder {
	public static final String REQUIREMENT_NATIVE_PATHS_ATTRIBUTE = "native.paths"; //$NON-NLS-1$
	private static final String[] EMPTY_STRINGS = new String[0];
	public static final String EXTERNAL_LIB_PREFIX = "external:"; //$NON-NLS-1$
	private final Generation generation;
	private final Debug debug;
	// This is only used to keep track of when the same native library is loaded more than once
	private final Collection<String> loadedNativeCode = new ArrayList<>(1);

	public NativeCodeFinder(Generation generation) {
		this.generation = generation;
		this.debug = generation.getBundleInfo().getStorage().getConfiguration().getDebug();
	}

	/*
	 * Maps an already mapped library name to additional library file extensions.
	 * This is needed on platforms like AIX where .a and .so can be used as library file
	 * extensions, but System.mapLibraryName only returns a single string.
	 */
	public String[] mapLibraryNames(String mappedLibName) {
		int extIndex = mappedLibName.lastIndexOf('.');
		List<String> LIB_EXTENSIONS = generation.getBundleInfo().getStorage().getConfiguration().LIB_EXTENSIONS;
		if (LIB_EXTENSIONS.isEmpty() || extIndex < 0)
			return EMPTY_STRINGS;
		String libNameBase = mappedLibName.substring(0, extIndex);
		String[] results = new String[LIB_EXTENSIONS.size()];
		for (int i = 0; i < results.length; i++)
			results[i] = libNameBase + LIB_EXTENSIONS.get(i);
		return results;
	}

	String findLibrary(String libname) {
		String path = findLibrary0(libname);
		if (path != null) {
			synchronized (loadedNativeCode) {
				if (loadedNativeCode.contains(path) || generation.getBundleInfo().getStorage().getConfiguration().COPY_NATIVES) {
					// we must copy the library to a temp space to allow another class loader to load the library
					String temp = generation.getBundleInfo().getStorage().copyToTempLibrary(generation, path);
					if (temp != null)
						path = temp;
				} else {
					loadedNativeCode.add(path);
				}
			}
		}
		return path;
	}

	private String findLibrary0(String libname) {
		String path = null;
		List<ClassLoaderHook> hooks = generation.getBundleInfo().getStorage().getConfiguration().getHookRegistry().getClassLoaderHooks();
		for (ClassLoaderHook hook : hooks) {
			path = hook.findLocalLibrary(generation, libname);
			if (path != null) {
				return path;
			}
		}
		String mappedName = System.mapLibraryName(libname);
		String[] altMappedNames = mapLibraryNames(mappedName);

		// first check Bundle-NativeCode header
		path = findBundleNativeCode(libname, mappedName, altMappedNames);
		// next check eclipse specific support
		return path != null ? path : findEclipseNativeCode(libname, mappedName, altMappedNames);
	}

	private String findEclipseNativeCode(String libname, String mappedName, String[] altMappedNames) {
		if (libname.length() == 0)
			return null;
		if (libname.charAt(0) == '/' || libname.charAt(0) == '\\')
			libname = libname.substring(1);
		String result = searchEclipseVariants(mappedName);
		if (result != null)
			return result;
		for (int i = 0; i < altMappedNames.length && result == null; i++)
			result = searchEclipseVariants(altMappedNames[i]);
		return result;
	}

	private String searchEclipseVariants(String path) {
		List<String> ECLIPSE_LIB_VARIANTS = generation.getBundleInfo().getStorage().getConfiguration().ECLIPSE_LIB_VARIANTS;
		for (String variant : ECLIPSE_LIB_VARIANTS) {
			BundleFile baseBundleFile = generation.getBundleFile();
			BundleEntry libEntry = baseBundleFile.getEntry(variant + path);
			if (libEntry != null) {
				File libFile = baseBundleFile.getFile(variant + path, true);
				if (libFile == null)
					return null;
				// see bug 88697 - HP requires libraries to have executable permissions
				if (org.eclipse.osgi.service.environment.Constants.OS_HPUX.equals(generation.getBundleInfo().getStorage().getConfiguration().getOS())) {
					try {
						// use the string array method in case there is a space in the path
						Runtime.getRuntime().exec(new String[] {"chmod", "755", libFile.getAbsolutePath()}).waitFor(); //$NON-NLS-1$ //$NON-NLS-2$
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return libFile.getAbsolutePath();
			}
		}
		return null;
	}

	private String findBundleNativeCode(String libname, String mappedName, String[] altMappedNames) {
		String path = null;
		if (debug.DEBUG_LOADER)
			Debug.println("  mapped library name: " + mappedName); //$NON-NLS-1$
		List<String> nativePaths = getNativePaths();
		if (nativePaths.isEmpty()) {
			return null;
		}
		path = findNativePath(nativePaths, mappedName);
		if (path == null) {
			for (int i = 0; i < altMappedNames.length && path == null; i++)
				path = findNativePath(nativePaths, altMappedNames[i]);
		}
		if (path == null) {
			if (debug.DEBUG_LOADER)
				Debug.println("  library does not exist: " + mappedName); //$NON-NLS-1$
			path = findNativePath(nativePaths, libname);
		}
		if (debug.DEBUG_LOADER)
			Debug.println("  returning library: " + path); //$NON-NLS-1$
		return path;
	}

	private List<String> getNativePaths() {
		ModuleRevision revision = generation.getRevision();
		ModuleWiring wiring = revision.getWiring();
		if (wiring == null) {
			// unresolved?  should not be possible
			return Collections.emptyList();
		}
		if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
			List<ModuleWire> hosts = wiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
			if (hosts == null) {
				// unresolved or invalid?  should not be possible
				return Collections.emptyList();
			}
			if (!hosts.isEmpty()) {
				// just use the first host wiring
				wiring = hosts.get(0).getProviderWiring();
			}
		}

		List<ModuleWire> nativeCode = wiring.getRequiredModuleWires(NativeNamespace.NATIVE_NAMESPACE);
		if (nativeCode.isEmpty()) {
			return Collections.emptyList();
		}

		// just taking the first paths for the revision, we sorted correctly when transforming to the requirement
		for (ModuleWire moduleWire : nativeCode) {
			if (moduleWire.getRequirement().getRevision().equals(revision)) {
				@SuppressWarnings("unchecked")
				List<String> result = (List<String>) nativeCode.get(0).getRequirement().getAttributes().get(REQUIREMENT_NATIVE_PATHS_ATTRIBUTE);
				if (result != null)
					return result;
				// this must be a multi-clause Bundle-NativeCode header, need to check for the correct one in the index
				try {
					FilterImpl filter = FilterImpl.newInstance(moduleWire.getRequirement().getDirectives().get(NativeNamespace.REQUIREMENT_FILTER_DIRECTIVE));
					int index = -1;
					Map<String, Object> capabilityAttrs = moduleWire.getCapability().getAttributes();
					for (FilterImpl child : filter.getChildren()) {
						index++;
						if (child.matches(capabilityAttrs)) {
							break;
						}
					}
					if (index != -1) {
						@SuppressWarnings("unchecked")
						List<String> indexResult = (List<String>) nativeCode.get(0).getRequirement().getAttributes().get(REQUIREMENT_NATIVE_PATHS_ATTRIBUTE + '.' + index);
						if (indexResult != null)
							return indexResult;
					}
				} catch (InvalidSyntaxException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return Collections.emptyList();
	}

	private String findNativePath(List<String> nativePaths, String libname) {
		int slash = libname.lastIndexOf('/');
		if (slash >= 0)
			libname = libname.substring(slash + 1);
		for (String nativePath : nativePaths) {
			slash = nativePath.lastIndexOf('/');
			String path = slash < 0 ? nativePath : nativePath.substring(slash + 1);
			if (path.equals(libname)) {
				if (nativePath.startsWith(NativeCodeFinder.EXTERNAL_LIB_PREFIX)) {
					// references an external library; do variable substitution
					String externalPath = generation.getBundleInfo().getStorage().getConfiguration().substituteVars(nativePath.substring(NativeCodeFinder.EXTERNAL_LIB_PREFIX.length()));
					File nativeFile = new File(externalPath);
					return nativeFile.getAbsolutePath();
				}
				// this is a normal library contained within the bundle
				File nativeFile = generation.getBundleFile().getFile(nativePath, true);
				if (nativeFile != null)
					return nativeFile.getAbsolutePath();
			}
		}
		return null;
	}

}
