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

package org.eclipse.osgi.baseadaptor.loader;

import java.security.ProtectionDomain;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.framework.util.KeyedHashSet;

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
	private KeyedHashSet userObjects = null;
	// Note that PDE has internal dependency on this field type/name (bug 267238)
	private volatile BaseData data;

	/**
	 * Constructs a ClasspathElement with the specified bundlefile and domain
	 * @param bundlefile A BundleFile object which acts as a source
	 * @param domain the ProtectDomain for all code loaded from this classpath element
	 */
	public ClasspathEntry(BundleFile bundlefile, ProtectionDomain domain) {
		this.bundlefile = bundlefile;
		this.domain = domain;
	}

	/**
	 * Returns the source BundleFile for this classpath entry
	 * @return the source BundleFile for this classpath entry
	 */
	public BundleFile getBundleFile() {
		return bundlefile;
	}

	/**
	 * Returns the base data which this entry is associated with.  This can be
	 * either a host or fragment base data.
	 */
	public BaseData getBaseData() {
		return data;
	}

	void setBaseData(BaseData data) {
		this.data = data;
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
}
