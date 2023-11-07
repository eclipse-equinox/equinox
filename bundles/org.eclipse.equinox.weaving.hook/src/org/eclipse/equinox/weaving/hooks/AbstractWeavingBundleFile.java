/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *   Martin Lippert            caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import org.eclipse.equinox.weaving.adaptors.IWeavingAdaptor;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;

public abstract class AbstractWeavingBundleFile extends BundleFileWrapper {

	private final BundleAdaptorProvider adaptorProvider;

	protected BundleFile delegate;

	public AbstractWeavingBundleFile(final BundleAdaptorProvider adaptorProvider, final BundleFile bundleFile) {
		super(bundleFile);
		this.adaptorProvider = adaptorProvider;
		this.delegate = bundleFile;
	}

	public IWeavingAdaptor getAdaptor() {
		return this.adaptorProvider.getAdaptor();
	}
}
