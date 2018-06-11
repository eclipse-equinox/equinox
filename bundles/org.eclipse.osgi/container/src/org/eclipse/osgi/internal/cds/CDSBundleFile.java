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

import com.ibm.oti.shared.SharedClassURLHelper;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;

/**
 * Wraps an actual BundleFile object for purposes of loading classes from the
 * shared classes cache. 
 */
public class CDSBundleFile extends BundleFileWrapper {
	private URL url; // the URL to the content of the real bundle file
	private SharedClassURLHelper urlHelper; // the url helper set by the classloader
	private boolean primed = false;

	/**
	 * The constructor
	 * @param wrapped the real bundle file
	 */
	public CDSBundleFile(BundleFile wrapped) {
		super(wrapped);
		// get the url to the content of the real bundle file
		try {
			this.url = new URL("file", "", wrapped.getBaseFile().getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
			// do nothing
		}
	}

	public CDSBundleFile(BundleFile bundleFile, SharedClassURLHelper urlHelper) {
		this(bundleFile);
		this.urlHelper = urlHelper;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.storage.bundlefile.BundleFile#getEntry(java.lang.String)
	 * 
	 * If path is not for a class then just use the wrapped bundle file to answer the call. 
	 * If the path is for a class, it returns a CDSBundleEntry object.
	 * If the path is for a class, it will look for the magic cookie in the 
	 * shared classes cache. If found, the bytes representing the magic cookie are stored in CDSBundleEntry object.
	 */
	public BundleEntry getEntry(String path) {
		String classFileExt = ".class"; //$NON-NLS-1$
		BundleEntry wrappedEntry = super.getEntry(path);
		if (wrappedEntry == null) {
			return null;
		}
		if ((false == primed) || (false == path.endsWith(classFileExt))) {
			return wrappedEntry;
		}

		byte[] classbytes = getClassBytes(path.substring(0, path.length() - classFileExt.length()));
		BundleEntry be = new CDSBundleEntry(path, classbytes, wrappedEntry);
		return be;
	}

	/**
	 * Returns the file url to the content of the actual bundle file 
	 * @return the file url to the content of the actual bundle file
	 */
	URL getURL() {
		return url;
	}

	/**
	 * Returns the url helper for this bundle file.  This is set by the 
	 * class loading hook
	 * @return the url helper for this bundle file
	 */
	SharedClassURLHelper getURLHelper() {
		return urlHelper;
	}

	/**
	 * Sets the url helper for this bundle file.  This is called by the 
	 * class loading hook.
	 * @param urlHelper the url helper
	 */
	void setURLHelper(SharedClassURLHelper urlHelper) {
		this.urlHelper = urlHelper;
		this.primed = false; // always unprime when a new urlHelper is set
	}

	/**
	 * Sets the primed flag for the bundle file.  This is called by the 
	 * class loading hook after the first class has been loaded from disk for 
	 * this bundle file.
	 * @param primed the primed flag
	 */
	void setPrimed(boolean primed) {
		this.primed = primed;
	}

	/**
	 * Searches in the shared classes cache for the specified class name.
	 * @param name the name of the class
	 * @return the magic cookie to the shared class or null if the class is not in the cache.
	 */
	private byte[] getClassBytes(String name) {
		if (urlHelper == null || url == null)
			return null;
		return urlHelper.findSharedClass(null, url, name);
	}
}
