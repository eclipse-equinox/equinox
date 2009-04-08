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

package org.eclipse.core.runtime.internal.adaptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.loader.*;
import org.eclipse.osgi.framework.util.KeyedElement;

public class ClasspathManifest implements KeyedElement {
	public static final Object KEY = new Object();
	public static final int HASHCODE = KEY.hashCode();

	private Manifest manifest;
	private boolean initialized = false;

	public int getKeyHashCode() {
		return HASHCODE;
	}

	public boolean compare(KeyedElement other) {
		return other.getKey() == KEY;
	}

	public Object getKey() {
		return KEY;
	}

	public synchronized Manifest getManifest(ClasspathEntry cpEntry, ClasspathManager loader) {
		if (initialized)
			return manifest;
		if (!hasPackageInfo(cpEntry, loader)) {
			initialized = true;
			manifest = null;
			return manifest;
		}
		BundleEntry mfEntry = cpEntry.getBundleFile().getEntry(org.eclipse.osgi.framework.internal.core.Constants.OSGI_BUNDLE_MANIFEST);
		if (mfEntry != null) {
			InputStream manIn = null;
			try {
				try {
					manIn = mfEntry.getInputStream();
					manifest = new Manifest(manIn);
				} finally {
					if (manIn != null)
						manIn.close();
				}
			} catch (IOException e) {
				// do nothing
			}
		}
		initialized = true;
		return manifest;
	}

	private boolean hasPackageInfo(ClasspathEntry cpEntry, ClasspathManager loader) {
		BaseData bundledata = null;
		if (cpEntry.getBundleFile() == loader.getBaseData().getBundleFile())
			bundledata = loader.getBaseData();
		if (bundledata == null) {
			FragmentClasspath[] fragCPs = loader.getFragmentClasspaths();
			if (fragCPs != null)
				for (int i = 0; i < fragCPs.length; i++)
					if (cpEntry.getBundleFile() == fragCPs[i].getBundleData().getBundleFile()) {
						bundledata = fragCPs[i].getBundleData();
						break;
					}
		}
		if (bundledata == null)
			return true;
		EclipseStorageHook storageHook = (EclipseStorageHook) bundledata.getStorageHook(EclipseStorageHook.KEY);
		return storageHook == null ? true : storageHook.hasPackageInfo();
	}

}
