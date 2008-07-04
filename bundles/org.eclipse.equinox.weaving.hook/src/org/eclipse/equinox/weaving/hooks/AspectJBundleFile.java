/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.IOException;
import java.net.URL;

import org.eclipse.equinox.weaving.adaptors.Debug;
import org.eclipse.equinox.weaving.adaptors.IAspectJAdaptor;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;

public class AspectJBundleFile extends AbstractAJBundleFile {

	private URL url;
	
	public AspectJBundleFile (IAspectJAdaptor aa, BundleFile bundleFile) throws IOException {
		super(aa,bundleFile);
		this.url = delegate.getBaseFile().toURL();
	}
	
	public URL getURL () {
		return url;
	}

	public BundleEntry getEntry(String path) {
		if (Debug.DEBUG_BUNDLE) Debug.println("> AspectJBundleFile.getEntry() path=" + path + ", url=" + url);
		BundleEntry entry;
		if (path.endsWith(".class")) {
			int offset = path.lastIndexOf('.');
			String name = path.substring(0,offset).replace('/','.');
			byte[] bytes = adaptor.findClass(name,url);
			if (bytes == null) {
				entry = delegate.getEntry(path);
				if (entry != null) {
					entry = new AspectJBundleEntry(adaptor,entry, url);
					if (Debug.DEBUG_BUNDLE) Debug.println("- AspectJBundleFile.getEntry() path=" + path + ", entry=" + entry);
				}
			}
			else entry = new AspectJBundleEntry(adaptor,delegate.getEntry(path), path,bytes, url);
		}
		else {
			entry = delegate.getEntry(path);
		}

		if (Debug.DEBUG_BUNDLE) Debug.println("< AspectJBundleFile.getEntry() entry=" + entry);
		return entry;
	}

}
