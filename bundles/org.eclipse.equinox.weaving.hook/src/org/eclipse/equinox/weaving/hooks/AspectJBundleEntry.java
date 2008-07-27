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
 *   Martin Lippert            minor changes and bugfixes     
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.equinox.weaving.adaptors.IAspectJAdaptor;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;

public class AspectJBundleEntry extends BundleEntry {

	private IAspectJAdaptor adaptor;
	private String name;
	private byte[] bytes;
	private URL bundleFileURL;
	private BundleEntry delegate;
    private boolean dontWeave;
	
	public AspectJBundleEntry (IAspectJAdaptor aspectjAdaptor, BundleEntry delegate, URL url, boolean dontWeave) {
		this.adaptor = aspectjAdaptor;
		this.bundleFileURL = url;
		this.delegate = delegate;
		this.dontWeave = dontWeave;
	}
	
	public AspectJBundleEntry (IAspectJAdaptor aspectjAdaptor, BundleEntry delegate, String name, byte[] bytes, URL url) {
		this(aspectjAdaptor, delegate, url, true);
		this.name = name;
		this.bytes = bytes;
	}
	
	public URL getBundleFileURL () {
		return bundleFileURL; 
	}
	
	public IAspectJAdaptor getAdaptor() {
		return adaptor;
	}
	
	public URL getFileURL() {
		if (bytes == null) return delegate.getFileURL();
		else return null;
	}

	public InputStream getInputStream() throws IOException {
		// this always returns the original stream of the delegate to
		// allow getResourceAsStream to be used even in the context of
		// caching with J9 class sharing
		//
		// class loading uses getBytes instead (where the caching is considered)
		return delegate.getInputStream();
	}
	
	public byte[] getBytes() throws IOException {
		if (bytes == null) return delegate.getBytes();
		return bytes;
	}

	public URL getLocalURL() {
		if (bytes == null) return delegate.getLocalURL();
		else return null;
	}

	public String getName() {
		if (bytes == null) return delegate.getName();
		else return name;
	}

	public long getSize() {
		if (delegate != null) return delegate.getSize();
		else return bytes.length;
	}

	public long getTime() {
		if (delegate != null) return delegate.getTime();
		else return 0;
	}
	
	public boolean dontWeave() {
	    return dontWeave;
	}

}
