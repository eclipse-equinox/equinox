/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.protocol.bundleentry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.BundleResourceHandler;

/**
 * URLStreamHandler the bundleentry protocol.
 */

public class Handler extends BundleResourceHandler {

	/**
	 * Constructor for a bundle protocol resource URLStreamHandler.
	 */
	public Handler() {
		super();
	}

	public Handler(BundleEntry bundleEntry, BaseAdaptor adaptor) {
		super(bundleEntry, adaptor);
	}

	protected BundleEntry findBundleEntry(URL url, AbstractBundle bundle) throws IOException {
		BaseData bundleData = (BaseData) bundle.getBundleData();
		BundleEntry entry = bundleData.getBundleFile().getEntry(url.getPath());
		if (entry == null)
			throw new FileNotFoundException(url.getPath());
		return entry;

	}

}
