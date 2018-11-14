/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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

package org.eclipse.osgi.internal.loader.classpath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.storage.BundleInfo;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.Storage;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;

/**
 * A ClasspathEntry contains a single <code>BundleFile</code> which is used as 
 * a source to load classes and resources from, and a single 
 * <code>ProtectionDomain</code> which is used as the domain to define classes 
 * loaded from this ClasspathEntry.
 * @since 3.2
 */
public class ClasspathEntry {
	static final class PDEData {
		final String fileName;
		final String symbolicName;

		PDEData(File baseFile, String symbolicName) {
			this.fileName = baseFile == null ? null : baseFile.getAbsolutePath();
			this.symbolicName = symbolicName;
		}
	}

	private final BundleFile bundlefile;
	private final ProtectionDomain domain;
	private final ManifestPackageAttributes mainManifestPackageAttributes;
	private final Map<String, ManifestPackageAttributes> perPackageManifestAttributes;
	private final List<BundleFile> mrBundleFiles;
	private HashMap<Object, KeyedElement> userObjects = null;

	// TODO Note that PDE has internal dependency on this field type/name (bug 267238)
	@SuppressWarnings("unused")
	private final PDEData data;

	/**
	 * Constructs a ClasspathElement with the specified bundlefile and domain
	 * @param bundlefile A BundleFile object which acts as a source
	 * @param domain the protection domain
	 */
	public ClasspathEntry(BundleFile bundlefile, ProtectionDomain domain, Generation generation) {
		this.bundlefile = bundlefile;
		this.domain = domain;
		this.data = new PDEData(generation.getBundleFile().getBaseFile(), generation.getRevision().getSymbolicName());
		final Manifest manifest = loadManifest(bundlefile, generation);
		if (manifest != null && generation.getBundleInfo().getStorage().getConfiguration().DEFINE_PACKAGE_ATTRIBUTES) {
			mainManifestPackageAttributes = manifestPackageAttributesFor(manifest.getMainAttributes(), null);
			perPackageManifestAttributes = manifestPackageAttributesMapFor(manifest.getEntries().entrySet(), mainManifestPackageAttributes);
		} else {
			mainManifestPackageAttributes = ManifestPackageAttributes.NONE;
			perPackageManifestAttributes = null;
		}

		boolean isMRJar;
		if (bundlefile == generation.getBundleFile()) {
			// this is the root bundle file
			isMRJar = generation.isMRJar();
		} else {
			isMRJar = manifest != null ? Boolean.parseBoolean(manifest.getMainAttributes().getValue(BundleInfo.MULTI_RELEASE_HEADER)) : false;
		}
		if (isMRJar) {
			mrBundleFiles = getMRBundleFiles(bundlefile, generation);
		} else {
			mrBundleFiles = Collections.emptyList();
		}
	}

	private static List<BundleFile> getMRBundleFiles(BundleFile bundlefile, Generation generation) {
		Storage storage = generation.getBundleInfo().getStorage();
		if (storage.getRuntimeVersion().getMajor() < 9) {
			return Collections.emptyList();
		}
		List<BundleFile> mrBundleFiles = new ArrayList<>();
		for (int i = storage.getRuntimeVersion().getMajor(); i > 8; i--) {
			String versionPath = BundleInfo.MULTI_RELEASE_VERSIONS + i + '/';
			BundleEntry versionEntry = bundlefile.getEntry(versionPath);
			if (versionEntry != null) {
				mrBundleFiles.add(storage.createNestedBundleFile(versionPath, bundlefile, generation, BundleInfo.MULTI_RELEASE_FILTER_PREFIXES));
			}
		}
		return Collections.unmodifiableList(mrBundleFiles);
	}

	private static ManifestPackageAttributes manifestPackageAttributesFor(Attributes attributes, ManifestPackageAttributes defaultAttributes) {
		return ManifestPackageAttributes.of(attributes.getValue(Attributes.Name.SPECIFICATION_TITLE), //
				attributes.getValue(Attributes.Name.SPECIFICATION_VERSION), //
				attributes.getValue(Attributes.Name.SPECIFICATION_VENDOR), //
				attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE), //
				attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION), //
				attributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR), //
				defaultAttributes);
	}

	private static Map<String, ManifestPackageAttributes> manifestPackageAttributesMapFor(Set<Entry<String, Attributes>> entries, ManifestPackageAttributes defaultAttributes) {
		Map<String, ManifestPackageAttributes> result = null;
		for (Entry<String, Attributes> entry : entries) {
			String name = entry.getKey();
			Attributes attributes = entry.getValue();
			if (name != null && name.endsWith("/")) { //$NON-NLS-1$
				String packageName = name.substring(0, name.length() - 1).replace('/', '.');
				if (result == null) {
					result = new HashMap<>(4);
				}
				result.put(packageName, manifestPackageAttributesFor(attributes, defaultAttributes));
			}
		}
		return result;
	}

	/**
	 * Returns the source BundleFile for this classpath entry
	 * @return the source BundleFile for this classpath entry
	 */
	public BundleFile getBundleFile() {
		return bundlefile;
	}

	/**
	 * Returns the ProtectionDomain for this classpath entry
	 * @return the ProtectionDomain for this classpath entry
	 */
	public ProtectionDomain getDomain() {
		return domain;
	}

	/**
	 * Returns a user object which is keyed by the specified key
	 * @param key the key of the user object to get
	 * @return a user object which is keyed by the specified key
	 */
	public synchronized Object getUserObject(Object key) {
		if (userObjects == null)
			return null;
		return userObjects.get(key);

	}

	/**
	 * Adds a user object
	 * @param userObject the user object to add
	 */
	public synchronized void addUserObject(KeyedElement userObject) {
		if (userObjects == null)
			userObjects = new HashMap<>(5);
		if (!userObjects.containsKey(userObject.getKey())) {
			userObjects.put(userObject.getKey(), userObject);
		}
	}

	/**
	 * Finds the entry with the specified path.
	 * This handles Multi-Release searching also.
	 * @param path the path to find
	 * @return the entry with the specified path.
	 */
	public BundleEntry findEntry(String path) {
		for (BundleFile mrFile : mrBundleFiles) {
			BundleEntry mrEntry = mrFile.getEntry(path);
			if (mrEntry != null) {
				return mrEntry;
			}
		}
		return bundlefile.getEntry(path);
	}

	/**
	 * Finds the resource wiht the specified name.
	 * This handles Multi-Release searching also.
	 * @param name the resource name
	 * @param m the module this classpath entry is for
	 * @param index the index this classpath entry.
	 * @return the resource URL or {@code null} if the resource does not exist.
	 */
	public URL findResource(String name, Module m, int index) {
		for (BundleFile mrFile : mrBundleFiles) {
			URL mrURL = mrFile.getResourceURL(name, m, index);
			if (mrURL != null) {
				return mrURL;
			}
		}
		return bundlefile.getResourceURL(name, m, index);
	}

	/**
	 * Adds the BundleFile objects for this classpath in the proper order
	 * for searching for resources. This handles Multi-Release ordering also.
	 * @param bundlefiles
	 */
	public void addBundleFiles(List<BundleFile> bundlefiles) {
		bundlefiles.addAll(mrBundleFiles);
		bundlefiles.add(bundlefile);
	}

	private static Manifest loadManifest(BundleFile cpBundleFile, Generation generation) {
		if (!generation.hasPackageInfo() && generation.getBundleFile() == cpBundleFile) {
			return null;
		}
		BundleEntry mfEntry = cpBundleFile.getEntry(BundleInfo.OSGI_BUNDLE_MANIFEST);
		if (mfEntry != null) {
			InputStream manIn = null;
			try {
				try {
					manIn = mfEntry.getInputStream();
					return new Manifest(manIn);
				} finally {
					if (manIn != null)
						manIn.close();
				}
			} catch (IOException e) {
				// do nothing
			}
		}
		return null;
	}

	ManifestPackageAttributes manifestPackageAttributesFor(String packageName) {
		ManifestPackageAttributes perPackage = perPackageManifestAttributes == null ? null : perPackageManifestAttributes.get(packageName);
		if (perPackage != null) {
			return perPackage;
		}
		return mainManifestPackageAttributes;
	}

	public void close() throws IOException {
		bundlefile.close();
		for (BundleFile bf : mrBundleFiles) {
			bf.close();
		}
	}
}
