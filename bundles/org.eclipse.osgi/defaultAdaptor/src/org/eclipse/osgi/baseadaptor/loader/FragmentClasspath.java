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

import java.io.IOException;
import java.security.ProtectionDomain;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.osgi.framework.FrameworkEvent;

/**
 * A FragmentClasspath contains all the <code>ClasspathEntry</code> objects for a fragment
 * <code>BaseData</code>.
 * @since 3.2
 */
public class FragmentClasspath {
	private BaseData bundledata;
	// Note that PDE has internal dependency on this field type/name (bug 267238)
	private ClasspathEntry[] entries;
	private ProtectionDomain domain;

	public FragmentClasspath(BaseData bundledata, ClasspathEntry[] entries, ProtectionDomain domain) {
		this.bundledata = bundledata;
		this.entries = entries;
		this.domain = domain;
	}

	/**
	 * Returns the fragment BaseData for this FragmentClasspath
	 * @return the fragment BaseData for this FragmentClasspath
	 */
	public BaseData getBundleData() {
		return bundledata;
	}

	/**
	 * Returns the fragment domain for this FragmentClasspath
	 * @return the fragment domain for this FragmentClasspath
	 */
	public ProtectionDomain getDomain() {
		return domain;
	}

	/**
	 * Returns the fragment classpath entries for this FragmentClasspath
	 * @return the fragment classpath entries for this FragmentClasspath
	 */
	public ClasspathEntry[] getEntries() {
		return entries;
	}

	/**
	 * Closes all the classpath entry resources for this FragmentClasspath.
	 *
	 */
	public void close() {
		for (int i = 0; i < entries.length; i++) {
			try {
				entries[i].getBundleFile().close();
			} catch (IOException e) {
				bundledata.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, bundledata.getBundle(), e);
			}
		}
	}

}
