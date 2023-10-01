/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sergey Prigogin (Google) - use parameterized types (bug 442021)
 *******************************************************************************/
package org.eclipse.core.internal.boot;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.core.internal.runtime.CommonMessages;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

/**
 * Platform URL support
 */
public abstract class PlatformURLConnection extends URLConnection {

	// URL access
	private boolean isInCache = false;
	private boolean isJar = false;

	// protected URL url; // declared in super (platform: URL)
	private URL resolvedURL = null; // resolved file URL (e.g. http: URL)
	private URL cachedURL = null; // file URL in cache (file: URL)

	private URLConnection connection = null; // actual connection

	// local cache
	private static Properties cacheIndex = new Properties();
	private static String cacheLocation;
	private static String indexName;
	private static String filePrefix;

	// constants
	private static final Object NOT_FOUND = new Object(); // marker
	private static final String CACHE_PROP = ".cache.properties"; //$NON-NLS-1$
	private static final String CACHE_LOCATION_PROP = "location"; //$NON-NLS-1$
	private static final String CACHE_INDEX_PROP = "index"; //$NON-NLS-1$
	private static final String CACHE_PREFIX_PROP = "prefix"; //$NON-NLS-1$
	private static final String CACHE_INDEX = ".index.properties"; //$NON-NLS-1$
	private static final String CACHE_DIR = ".eclipse-" + PlatformURLHandler.PROTOCOL + File.separator; //$NON-NLS-1$

	// debug tracing
	private static final String OPTION_DEBUG = "org.eclipse.core.runtime/url/debug"; //$NON-NLS-1$ ;
	private static final String OPTION_DEBUG_CONNECT = OPTION_DEBUG + "/connect"; //$NON-NLS-1$ ;
	private static final String OPTION_DEBUG_CACHE_LOOKUP = OPTION_DEBUG + "/cachelookup"; //$NON-NLS-1$ ;
	private static final String OPTION_DEBUG_CACHE_COPY = OPTION_DEBUG + "/cachecopy"; //$NON-NLS-1$ ;

	public final static boolean DEBUG;
	public final static boolean DEBUG_CONNECT;
	public final static boolean DEBUG_CACHE_LOOKUP;
	public final static boolean DEBUG_CACHE_COPY;

	static {
		Activator activator = Activator.getDefault();
		if (activator == null) {
			DEBUG = DEBUG_CONNECT = DEBUG_CACHE_LOOKUP = DEBUG_CACHE_COPY = false;
		} else {
			DebugOptions debugOptions = activator.getDebugOptions();
			if (debugOptions != null) {
				DEBUG = debugOptions.getBooleanOption(OPTION_DEBUG, false);
				DEBUG_CONNECT = debugOptions.getBooleanOption(OPTION_DEBUG_CONNECT, true);
				DEBUG_CACHE_LOOKUP = debugOptions.getBooleanOption(OPTION_DEBUG_CACHE_LOOKUP, true);
				DEBUG_CACHE_COPY = debugOptions.getBooleanOption(OPTION_DEBUG_CACHE_COPY, true);
			} else
				DEBUG = DEBUG_CONNECT = DEBUG_CACHE_LOOKUP = DEBUG_CACHE_COPY = false;
		}
	}

	protected PlatformURLConnection(URL url) {
		super(url);
	}

	protected boolean allowCaching() {
		return false;
	}

	@Override
	public void connect() throws IOException {
		connect(false);
	}

	private synchronized void connect(boolean asLocal) throws IOException {
		if (connected)
			return;

		if (shouldCache(asLocal)) {
			try {
				URL inCache = getURLInCache();
				if (inCache != null)
					connection = inCache.openConnection();
			} catch (IOException e) {
				// failed to cache ... will use resolved URL instead
			}
		}

		// use resolved URL
		if (connection == null)
			connection = resolvedURL.openConnection();
		connected = true;
		if (DEBUG && DEBUG_CONNECT)
			debug("Connected as " + connection.getURL()); //$NON-NLS-1$
	}

	// TODO consider refactoring this method... it is too long
	// TODO avoid cryptic identifiers such as ix, tgt, tmp, srcis, tgtos...
	private void copyToCache() throws IOException {

		if (isInCache | cachedURL == null)
			return;
		String tmp;
		int ix;

		// cache entry key
		String key;
		if (isJar) {
			tmp = url.getFile();
			ix = tmp.lastIndexOf(PlatformURLHandler.JAR_SEPARATOR);
			if (ix != -1)
				tmp = tmp.substring(0, ix);
			key = tmp;
		} else
			key = url.getFile();

		// source url
		URL src;
		if (isJar) {
			tmp = resolvedURL.getFile();
			ix = tmp.lastIndexOf(PlatformURLHandler.JAR_SEPARATOR);
			if (ix != -1)
				tmp = tmp.substring(0, ix);
			src = new URL(tmp);
		} else
			src = resolvedURL;

		// cache target
		String tgt;
		if (isJar) {
			tmp = cachedURL.getFile();
			ix = tmp.indexOf(PlatformURLHandler.PROTOCOL_SEPARATOR);
			if (ix != -1)
				tmp = tmp.substring(ix + 1);
			ix = tmp.lastIndexOf(PlatformURLHandler.JAR_SEPARATOR);
			if (ix != -1)
				tmp = tmp.substring(0, ix);
			tgt = tmp;
		} else
			tgt = cachedURL.getFile();
		boolean error = false;
		long total = 0;

		try (InputStream srcis = src.openStream()) {
			if (DEBUG && DEBUG_CACHE_COPY) {
				if (isJar)
					debug("Caching jar as " + tgt); //$NON-NLS-1$
				else
					debug("Caching as " + tgt); //$NON-NLS-1$
			}

			File tgtFile = new File(tgt);
			try (FileOutputStream tgtos = new FileOutputStream(tgtFile)) {
				srcis.transferTo(tgtos);
				tgtos.flush();
				tgtos.getFD().sync();
			}
			// add cache entry
			cacheIndex.put(key, tgt);
			isInCache = true;
		} catch (IOException e) {
			error = true;
			cacheIndex.put(key, NOT_FOUND);
			// mark cache entry for this execution
			if (DEBUG && DEBUG_CACHE_COPY)
				debug("Failed to cache due to " + e); //$NON-NLS-1$
			throw e;
		} finally {
			if (!error && DEBUG && DEBUG_CACHE_COPY)
				debug(total + " bytes copied"); //$NON-NLS-1$
		}
	}

	protected void debug(String s) {
		System.out.println("URL " + getURL() + "^" + Integer.toHexString(Thread.currentThread().hashCode()) + " " + s); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private static void debugStartup(String s) {
		System.out.println("URL " + s); //$NON-NLS-1$
	}

	@Override
	public synchronized InputStream getInputStream() throws IOException {
		if (!connected)
			connect();
		return connection.getInputStream();
	}

	public URL getResolvedURL() {
		return resolvedURL;
	}

	public URL getURLAsLocal() throws IOException {
		connect(true); // connect and force caching if necessary
		URL u = connection.getURL();
		String up = u.getProtocol();
		if (!up.equals(PlatformURLHandler.FILE) && !up.equals(PlatformURLHandler.JAR)
				&& !up.startsWith(PlatformURLHandler.BUNDLE))
			throw new IOException(NLS.bind(CommonMessages.url_noaccess, up));
		return u;
	}

	// TODO consider refactoring this method... it is too long
	private URL getURLInCache() throws IOException {

		if (!allowCaching())
			return null; // target should not be cached

		if (isInCache)
			return cachedURL;

		if (cacheLocation == null | cacheIndex == null)
			return null; // not caching

		// check if we are dealing with a .jar/ .zip
		String file = ""; //$NON-NLS-1$
		String jarEntry = null;
		if (isJar) {
			file = url.getFile();
			int ix = file.lastIndexOf(PlatformURLHandler.JAR_SEPARATOR);
			if (ix != -1) {
				jarEntry = file.substring(ix + PlatformURLHandler.JAR_SEPARATOR.length());
				file = file.substring(0, ix);
			}
		} else {
			file = url.getFile();
		}

		// check for cached entry
		String tmp = (String) cacheIndex.get(file);

		// check for "not found" marker
		if (tmp != null && tmp == NOT_FOUND)
			throw new IOException();

		// validate cache entry
		if (tmp != null && !(new File(tmp)).exists()) {
			tmp = null;
			cacheIndex.remove(url.getFile());
		}

		// found in cache
		if (tmp != null) {
			if (isJar) {
				if (DEBUG && DEBUG_CACHE_LOOKUP)
					debug("Jar located in cache as " + tmp); //$NON-NLS-1$
				tmp = PlatformURLHandler.FILE + PlatformURLHandler.PROTOCOL_SEPARATOR + tmp
						+ PlatformURLHandler.JAR_SEPARATOR + jarEntry;
				cachedURL = new URL(PlatformURLHandler.JAR, null, -1, tmp);
			} else {
				if (DEBUG && DEBUG_CACHE_LOOKUP)
					debug("Located in cache as " + tmp); //$NON-NLS-1$
				cachedURL = new URL(PlatformURLHandler.FILE, null, -1, tmp);
			}
			isInCache = true;
		} else {
			// attempt to cache
			int ix = file.lastIndexOf('/');
			tmp = file.substring(ix + 1);
			tmp = cacheLocation + filePrefix + (new java.util.Date()).getTime() + "_" + tmp; //$NON-NLS-1$
			tmp = tmp.replace(File.separatorChar, '/');
			if (isJar) {
				tmp = PlatformURLHandler.FILE + PlatformURLHandler.PROTOCOL_SEPARATOR + tmp
						+ PlatformURLHandler.JAR_SEPARATOR + jarEntry;
				cachedURL = new URL(PlatformURLHandler.JAR, null, -1, tmp);
			} else
				cachedURL = new URL(PlatformURLHandler.FILE, null, -1, tmp);
			copyToCache();
		}

		return cachedURL;
	}

	/*
	 * to be implemented by subclass
	 * 
	 * @return URL resolved URL
	 */
	protected URL resolve() throws IOException {
		// TODO throw UnsupportedOperationException instead - this is a bug in subclass,
		// not an actual failure
		throw new IOException();
	}

	protected static String getId(String spec) {
		String id = (String) parse(spec)[0];
		return id == null ? spec : id;
	}

	protected static String getVersion(String spec) {
		Version version = (Version) parse(spec)[1];
		return version == null ? "" : version.toString(); //$NON-NLS-1$
	}

	private static Object[] parse(String spec) {
		String bsn = null;
		Version version = null;
		int underScore = spec.indexOf('_');
		while (underScore >= 0) {
			try {
				version = Version.parseVersion(spec.substring(underScore + 1));
			} catch (IllegalArgumentException iae) {
				// continue to next underscore
				underScore = spec.indexOf('_', underScore + 1);
				continue;
			}
			bsn = spec.substring(0, underScore);
			break;
		}
		return new Object[] { bsn, version };
	}

	void setResolvedURL(URL url) throws IOException {
		if (url == null)
			throw new IOException();
		if (resolvedURL != null)
			return;
		int ix = url.getFile().lastIndexOf(PlatformURLHandler.JAR_SEPARATOR);
		isJar = -1 != ix;
		// Resolved URLs containing !/ separator are assumed to be jar URLs.
		// If the resolved protocol is not jar, new jar URL is created.
		if (isJar && !url.getProtocol().equals(PlatformURLHandler.JAR))
			url = new URL(PlatformURLHandler.JAR, "", -1, url.toExternalForm()); //$NON-NLS-1$
		resolvedURL = url;
	}

	private boolean shouldCache(boolean asLocal) {

		// don't cache files that are known to be local
		String rp = resolvedURL.getProtocol();
		String rf = resolvedURL.getFile();
		if (rp.equals(PlatformURLHandler.FILE))
			return false;
		if (rp.equals(PlatformURLHandler.JAR) && (rf.startsWith(PlatformURLHandler.FILE)))
			return false;

		// for other files force caching if local connection was requested
		if (asLocal)
			return true;

		// for now cache all files
		// XXX: add cache policy support
		return true;
	}

	static void shutdown() {
		if (indexName != null && cacheLocation != null) {
			// weed out "not found" entries
			Enumeration<Object> keys = cacheIndex.keys();
			String key;
			Object value;
			while (keys.hasMoreElements()) {
				key = (String) keys.nextElement();
				value = cacheIndex.get(key);
				if (value == NOT_FOUND)
					cacheIndex.remove(key);
			}
			// if the cache index is empty we don't need to save it
			if (cacheIndex.size() == 0)
				return;
			try {
				// try to save cache index
				FileOutputStream fos = null;
				fos = new FileOutputStream(cacheLocation + indexName);
				try {
					cacheIndex.store(fos, null);
					fos.flush();
					fos.getFD().sync();
				} finally {
					fos.close();
				}
			} catch (IOException e) {
				// failed to store cache index ... ignore
			}
		}
	}

	// TODO consider splitting this method into two or more steps - it is too long
	static void startup(String location, String os, String ws, String nl) {
		verifyLocation(location); // check for platform location, ignore errors
		String cacheProps = location.trim();
		if (!cacheProps.endsWith(File.separator))
			cacheProps += File.separator;
		cacheProps += CACHE_PROP;
		File cachePropFile = new File(cacheProps);
		Properties props = null;
		FileInputStream fis;

		if (cachePropFile.exists()) {
			// load existing properties
			try {
				props = new Properties();
				fis = new FileInputStream(cachePropFile);
				try {
					props.load(fis);
				} finally {
					fis.close();
				}
			} catch (IOException e) {
				props = null;
			}
		}

		if (props == null) {
			// first time up, or failed to load previous settings
			props = new Properties();

			String tmp = System.getProperty("user.home"); //$NON-NLS-1$
			if (!tmp.endsWith(File.separator))
				tmp += File.separator;
			tmp += CACHE_DIR;
			props.put(CACHE_LOCATION_PROP, tmp);

			tmp = Long.toString((new java.util.Date()).getTime());
			props.put(CACHE_PREFIX_PROP, tmp);

			tmp += CACHE_INDEX;
			props.put(CACHE_INDEX_PROP, tmp);

			// save for next time around
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(cachePropFile);
				try {
					props.store(fos, null);
					fos.flush();
					fos.getFD().sync();
				} finally {
					fos.close();
				}
			} catch (IOException e) {
				// failed to store cache location metadata ... ignore
			}
		}

		// remember settings for shutdown processing
		filePrefix = (String) props.get(CACHE_PREFIX_PROP);
		indexName = (String) props.get(CACHE_INDEX_PROP);
		cacheLocation = (String) props.get(CACHE_LOCATION_PROP);

		if (DEBUG) {
			debugStartup("Cache location: " + cacheLocation); //$NON-NLS-1$
			debugStartup("Cache index: " + indexName); //$NON-NLS-1$
			debugStartup("Cache file prefix: " + filePrefix); //$NON-NLS-1$
		}

		// create cache directory structure if needed
		if (!verifyLocation(cacheLocation)) {
			indexName = null;
			cacheLocation = null;
			if (DEBUG)
				debugStartup("Failed to create cache directory structure. Caching suspended"); //$NON-NLS-1$
			return;
		}

		// attempt to initialize cache index
		if (cacheLocation != null && indexName != null) {
			try {
				fis = new FileInputStream(cacheLocation + indexName);
				try {
					cacheIndex.load(fis);
				} finally {
					fis.close();
				}
			} catch (IOException e) {
				if (DEBUG)
					debugStartup("Failed to initialize cache"); //$NON-NLS-1$
			}
		}
	}

	private static boolean verifyLocation(String location) {
		// verify cache directory exists. Create if needed
		File cacheDir = new File(location);
		if (cacheDir.exists())
			return true;
		return cacheDir.mkdirs();
	}
}
