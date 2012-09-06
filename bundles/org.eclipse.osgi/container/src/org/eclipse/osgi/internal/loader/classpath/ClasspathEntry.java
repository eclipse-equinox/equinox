/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.loader.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;
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
	private final BundleFile bundlefile;
	private final ProtectionDomain domain;
	private final Manifest manifest;
	private KeyedHashSet userObjects = null;

	// TODO Note that PDE has internal dependency on this field type/name (bug 267238)
	//private volatile BaseData data;

	/**
	 * Constructs a ClasspathElement with the specified bundlefile and domain
	 * @param bundlefile A BundleFile object which acts as a source
	 * @param domain the protection domain
	 */
	public ClasspathEntry(BundleFile bundlefile, ProtectionDomain domain, Generation generation) {
		this.bundlefile = bundlefile;
		this.domain = domain;
		this.manifest = getManifest(bundlefile, generation);
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
	public Object getUserObject(Object key) {
		if (userObjects == null)
			return null;
		synchronized (userObjects) {
			return userObjects.getByKey(key);
		}
	}

	/**
	 * Adds a user object
	 * @param userObject the user object to add
	 */
	public synchronized void addUserObject(KeyedElement userObject) {
		if (userObjects == null)
			userObjects = new KeyedHashSet(5, false);
		synchronized (userObjects) {
			userObjects.add(userObject);
		}
	}

	private static Manifest getManifest(BundleFile cpBundleFile, Generation generation) {
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

	public Manifest getManifest() {
		return this.manifest;
	}

}
