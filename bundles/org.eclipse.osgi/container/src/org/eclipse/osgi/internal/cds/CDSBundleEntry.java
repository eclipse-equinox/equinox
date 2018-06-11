/***********************************************************************
 * IBM Confidential 
 * OCO Source Materials 
 *
 * (C) Copyright IBM Corp. 2006, 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 ************************************************************************/

package org.eclipse.osgi.internal.cds;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.osgi.storage.bundlefile.BundleEntry;

/**
 * A bundle entry for a class that is found in the shared classes cache
 */
public class CDSBundleEntry extends BundleEntry {
	String path;
	byte[] classbytes;
	BundleEntry wrapped;

	/**
	 * The constructor
	 * @param path the path to the class file
	 * @param classbytes the magic cookie bytes for the class in the shared cache
	 * @param wrapped the actual bundleEntry where the class comes from
	 */
	public CDSBundleEntry(String path, byte[] classbytes, BundleEntry wrapped) {
		super();
		this.path = path;
		this.classbytes = classbytes;
		this.wrapped = wrapped;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getFileURL()
	 * uses the wrapped bundle file to get the actual file url to the content of
	 * the class on disk.
	 * 
	 * This should is likely never to be called.
	 */
	public URL getFileURL() {
		return wrapped.getFileURL();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getInputStream()
	 * wraps the classbytes into a ByteArrayInputStream.  This should not be used
	 * by classloading.
	 */
	public InputStream getInputStream() throws IOException {
		// someone is trying to get the real bytes of the class file!!
		// just return the entry from the wrapped file instead of the magic cookie
		return wrapped.getInputStream();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getBytes()
	 * if classbytes is not null, it returns the magic cookie for the shared class.  This is used to define 
	 * the class during class loading.
	 * if classbytes is null, it gets the contents from actual BundleEntry and caches it in classbytes.
	 */
	public byte[] getBytes() throws IOException {
		if (classbytes == null) {
			classbytes = wrapped.getBytes();
		}
		return classbytes;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getLocalURL()
	 * uses the wrapped bundle file to get the actual local url to the content of
	 * the class on disk.
	 * 
	 * This should is likely never to be called.
	 */
	public URL getLocalURL() {
		return wrapped.getLocalURL();
	}

	public String getName() {
		return path;
	}

	public long getSize() {
		return wrapped.getSize();
	}

	public long getTime() {
		return wrapped.getTime();
	}
}
