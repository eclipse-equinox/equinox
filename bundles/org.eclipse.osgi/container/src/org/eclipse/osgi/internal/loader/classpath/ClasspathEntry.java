/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.loader.classpath;

import java.io.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.framework.util.KeyedHashSet;
import org.eclipse.osgi.storage.BundleInfo;
import org.eclipse.osgi.storage.BundleInfo.Generation;
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
	private KeyedHashSet userObjects = null;

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
		return userObjects.getByKey(key);

	}

	/**
	 * Adds a user object
	 * @param userObject the user object to add
	 */
	public synchronized void addUserObject(KeyedElement userObject) {
		if (userObjects == null)
			userObjects = new KeyedHashSet(5, false);
		userObjects.add(userObject);
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

}
