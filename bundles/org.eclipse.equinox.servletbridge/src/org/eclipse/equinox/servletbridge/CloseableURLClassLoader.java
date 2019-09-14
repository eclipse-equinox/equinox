/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
 *******************************************************************************/

/**
 * The java.net.URLClassLoader class allows one to load resources from arbitrary URLs and in particular is optimized to handle
 * "jar" URLs. Unfortunately for jar files this optimization ends up holding the file open which ultimately prevents the file from
 * being deleted or update until the VM is shutdown.
 *
 * The CloseableURLClassLoader is meant to replace the URLClassLoader and provides an additional method to allow one to "close" any
 * resources left open. In the current version the CloseableURLClassLoader will only ensure the closing of jar file resources. The
 * jar handling behavior in this class will also provides a construct to allow one to turn off jar file verification in performance
 * sensitive situations where the verification us not necessary.
 *
 * also see https://bugs.eclipse.org/bugs/show_bug.cgi?id=190279
 */

package org.eclipse.equinox.servletbridge;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.jar.Attributes.Name;

public class CloseableURLClassLoader extends URLClassLoader {
	private static final boolean CLOSEABLE_REGISTERED_AS_PARALLEL;
	static {
		boolean registeredAsParallel;
		try {
			Method parallelCapableMetod = ClassLoader.class.getDeclaredMethod("registerAsParallelCapable", (Class[]) null); //$NON-NLS-1$
			parallelCapableMetod.setAccessible(true);
			registeredAsParallel = ((Boolean) parallelCapableMetod.invoke(null, (Object[]) null)).booleanValue();
		} catch (Throwable e) {
			// must do everything to avoid failing in clinit
			registeredAsParallel = true;
		}
		CLOSEABLE_REGISTERED_AS_PARALLEL = registeredAsParallel;
	}
	static final String DOT_CLASS = ".class"; //$NON-NLS-1$
	static final String BANG_SLASH = "!/"; //$NON-NLS-1$
	static final String JAR = "jar"; //$NON-NLS-1$
	private static final String UNC_PREFIX = "//"; //$NON-NLS-1$
	private static final String SCHEME_FILE = "file"; //$NON-NLS-1$

	// @GuardedBy("loaders")
	final ArrayList<CloseableJarFileLoader> loaders = new ArrayList<>(); // package private to avoid synthetic access.
	// @GuardedBy("loaders")
	private final ArrayList<URL> loaderURLs = new ArrayList<>(); // note: protected by loaders
	// @GuardedBy("loaders")
	boolean closed = false; // note: protected by loaders, package private to avoid synthetic access.

	private final AccessControlContext context;
	private final boolean verifyJars;
	private final boolean registeredAsParallel;

	private static class CloseableJarURLConnection extends JarURLConnection {
		private final JarFile jarFile;
		// @GuardedBy("this")
		private JarEntry entry;

		public CloseableJarURLConnection(URL url, JarFile jarFile) throws MalformedURLException {
			super(url);
			this.jarFile = jarFile;
		}

		@Override
		public void connect() throws IOException {
			internalGetEntry();
		}

		private synchronized JarEntry internalGetEntry() throws IOException {
			if (entry != null)
				return entry;
			entry = jarFile.getJarEntry(getEntryName());
			if (entry == null)
				throw new FileNotFoundException(getEntryName());
			return entry;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return jarFile.getInputStream(internalGetEntry());
		}

		/**
		 * @throws IOException
		 * Documented to avoid warning
		 */
		@Override
		public JarFile getJarFile() throws IOException {
			return jarFile;
		}

		@Override
		public JarEntry getJarEntry() throws IOException {
			return internalGetEntry();
		}
	}

	private static class CloseableJarURLStreamHandler extends URLStreamHandler {
		private final JarFile jarFile;

		public CloseableJarURLStreamHandler(JarFile jarFile) {
			this.jarFile = jarFile;
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new CloseableJarURLConnection(u, jarFile);
		}

		@Override
		protected void parseURL(URL u, String spec, int start, int limit) {
			setURL(u, JAR, null, 0, null, null, spec.substring(start, limit), null, null);
		}
	}

	private static class CloseableJarFileLoader {
		private final JarFile jarFile;
		private final Manifest manifest;
		private final CloseableJarURLStreamHandler jarURLStreamHandler;
		private final String jarFileURLPrefixString;

		public CloseableJarFileLoader(File file, boolean verify) throws IOException {
			this.jarFile = new JarFile(file, verify);
			this.manifest = jarFile.getManifest();
			this.jarURLStreamHandler = new CloseableJarURLStreamHandler(jarFile);
			this.jarFileURLPrefixString = file.toURL().toString() + BANG_SLASH;
		}

		public URL getURL(String name) {
			if (jarFile.getEntry(name) != null)
				try {
					return new URL(JAR, null, -1, jarFileURLPrefixString + name, jarURLStreamHandler);
				} catch (MalformedURLException e) {
					// ignore
				}
			return null;
		}

		public Manifest getManifest() {
			return manifest;
		}

		public void close() {
			try {
				jarFile.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/**
	 * @param urls the array of URLs to use for loading resources
	 * @see URLClassLoader
	 */
	public CloseableURLClassLoader(URL[] urls) {
		this(urls, ClassLoader.getSystemClassLoader(), true);
	}

	/**
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader used for delegation
	 * @see URLClassLoader
	 */
	public CloseableURLClassLoader(URL[] urls, ClassLoader parent) {
		this(excludeFileJarURLS(urls), parent, true);
	}

	/**
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader used for delegation
	 * @param verifyJars flag to determine if jar file verification should be performed
	 * @see URLClassLoader
	 */
	public CloseableURLClassLoader(URL[] urls, ClassLoader parent, boolean verifyJars) {
		super(excludeFileJarURLS(urls), parent);
		this.registeredAsParallel = CLOSEABLE_REGISTERED_AS_PARALLEL && this.getClass() == CloseableURLClassLoader.class;
		this.context = AccessController.getContext();
		this.verifyJars = verifyJars;
		for (URL url : urls) {
			if (isFileJarURL(url)) {
				loaderURLs.add(url);
				safeAddLoader(url);
			}
		}
	}

	// @GuardedBy("loaders")
	private boolean safeAddLoader(URL url) {
		//assume all illegal characters have been properly encoded, so use URI class to unencode
		try {
			File file = new File(toURI(url));
			if (file.exists()) {
				try {
					loaders.add(new CloseableJarFileLoader(file, verifyJars));
					return true;
				} catch (IOException e) {
					// ignore
				}
			}
		} catch (URISyntaxException e1) {
			// ignore
		}

		return false;
	}

	private static URI toURI(URL url) throws URISyntaxException {
		if (!SCHEME_FILE.equals(url.getProtocol())) {
			throw new IllegalArgumentException("bad prototcol: " + url.getProtocol()); //$NON-NLS-1$
		}
		//URL behaves differently across platforms so for file: URLs we parse from string form
		String pathString = url.toExternalForm().substring(5);
		//ensure there is a leading slash to handle common malformed URLs such as file:c:/tmp
		if (pathString.indexOf('/') != 0)
			pathString = '/' + pathString;
		else if (pathString.startsWith(UNC_PREFIX) && !pathString.startsWith(UNC_PREFIX, 2)) {
			//URL encodes UNC path with two slashes, but URI uses four (see bug 207103)
			pathString = ensureUNCPath(pathString);
		}
		return new URI(SCHEME_FILE, null, pathString, null);
	}

	/**
	 * Ensures the given path string starts with exactly four leading slashes.
	 */
	private static String ensureUNCPath(String path) {
		int len = path.length();
		StringBuilder result = new StringBuilder(len);
		for (int i = 0; i < 4; i++) {
			//	if we have hit the first non-slash character, add another leading slash
			if (i >= len || result.length() > 0 || path.charAt(i) != '/')
				result.append('/');
		}
		result.append(path);
		return result.toString();
	}

	private static URL[] excludeFileJarURLS(URL[] urls) {
		ArrayList<URL> urlList = new ArrayList<>();
		for (URL url : urls) {
			if (!isFileJarURL(url)) {
				urlList.add(url);
			}
		}
		return urlList.toArray(new URL[urlList.size()]);
	}

	private static boolean isFileJarURL(URL url) {
		if (!url.getProtocol().equals("file")) //$NON-NLS-1$
			return false;

		String path = url.getPath();
		if (path != null && path.endsWith("/")) //$NON-NLS-1$
			return false;

		return true;
	}

	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		try {
			Class<?> clazz = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
				@Override
				public Class<?> run() throws ClassNotFoundException {
					String resourcePath = name.replace('.', '/') + DOT_CLASS;
					CloseableJarFileLoader loader = null;
					URL resourceURL = null;
					synchronized (loaders) {
						if (closed)
							return null;
						for (Iterator<CloseableJarFileLoader> iterator = loaders.iterator(); iterator.hasNext();) {
							loader = iterator.next();
							resourceURL = loader.getURL(resourcePath);
							if (resourceURL != null)
								break;
						}
					}
					if (resourceURL != null) {
						try {
							return defineClass(name, resourceURL, loader.getManifest());
						} catch (IOException e) {
							throw new ClassNotFoundException(name, e);
						}
					}
					return null;
				}
			}, context);
			if (clazz != null)
				return clazz;
		} catch (PrivilegedActionException e) {
			throw (ClassNotFoundException) e.getException();
		}
		return super.findClass(name);
	}

	// package private to avoid synthetic access.
	Class<?> defineClass(String name, URL resourceURL, Manifest manifest) throws IOException {
		JarURLConnection connection = (JarURLConnection) resourceURL.openConnection();
		int lastDot = name.lastIndexOf('.');
		if (lastDot != -1) {
			String packageName = name.substring(0, lastDot);
			synchronized (pkgLock) {
				Package pkg = getPackage(packageName);
				if (pkg != null) {
					checkForSealedPackage(pkg, packageName, manifest, connection.getJarFileURL());
				} else {
					definePackage(packageName, manifest, connection.getJarFileURL());
				}
			}

		}
		JarEntry entry = connection.getJarEntry();
		byte[] bytes = new byte[(int) entry.getSize()];
		DataInputStream is = null;
		try {
			is = new DataInputStream(connection.getInputStream());
			is.readFully(bytes, 0, bytes.length);
			CodeSource cs = new CodeSource(connection.getJarFileURL(), entry.getCertificates());
			if (isRegisteredAsParallel()) {
				boolean initialLock = lockClassName(name);
				try {
					Class<?> clazz = findLoadedClass(name);
					if (clazz != null) {
						return clazz;
					}
					return defineClass(name, bytes, 0, bytes.length, cs);
				} finally {
					if (initialLock) {
						unlockClassName(name);
					}
				}
			}
			return defineClass(name, bytes, 0, bytes.length, cs);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	private void checkForSealedPackage(Package pkg, String packageName, Manifest manifest, URL jarFileURL) {
		if (pkg.isSealed()) {
			// previously sealed case
			if (!pkg.isSealed(jarFileURL)) {
				// this URL does not seal; ERROR
				throw new SecurityException("The package '" + packageName + "' was previously loaded and is already sealed."); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			// previously unsealed case
			String entryPath = packageName.replace('.', '/') + "/"; //$NON-NLS-1$
			Attributes entryAttributes = manifest.getAttributes(entryPath);
			String sealed = null;
			if (entryAttributes != null)
				sealed = entryAttributes.getValue(Name.SEALED);

			if (sealed == null) {
				Attributes mainAttributes = manifest.getMainAttributes();
				if (mainAttributes != null)
					sealed = mainAttributes.getValue(Name.SEALED);
			}
			if (Boolean.valueOf(sealed).booleanValue()) {
				// this manifest attempts to seal when package defined previously unsealed; ERROR
				throw new SecurityException("The package '" + packageName + "' was previously loaded unsealed. Cannot seal package."); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	@Override
	public URL findResource(final String name) {
		URL url = AccessController.doPrivileged(new PrivilegedAction<URL>() {
			@Override
			public URL run() {
				synchronized (loaders) {
					if (closed)
						return null;
					for (CloseableJarFileLoader loader : loaders) {
						URL resourceURL = loader.getURL(name);
						if (resourceURL != null)
							return resourceURL;
					}
				}
				return null;
			}
		}, context);
		if (url != null)
			return url;
		return super.findResource(name);
	}

	@Override
	public Enumeration<URL> findResources(final String name) throws IOException {
		final List<URL> resources = new ArrayList<>();
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				synchronized (loaders) {
					if (closed)
						return null;
					for (CloseableJarFileLoader loader : loaders) {
						URL resourceURL = loader.getURL(name);
						if (resourceURL != null)
							resources.add(resourceURL);
					}
				}
				return null;
			}
		}, context);
		Enumeration<URL> e = super.findResources(name);
		while (e.hasMoreElements())
			resources.add(e.nextElement());

		return Collections.enumeration(resources);
	}

	/**
	 * The "close" method is called when the class loader is no longer needed and we should close any open resources.
	 * In particular this method will close the jar files associated with this class loader.
	 */
	@Override
	public void close() {
		synchronized (loaders) {
			if (closed)
				return;
			for (CloseableJarFileLoader loader : loaders) {
				loader.close();
			}
			closed = true;
		}
	}

	@Override
	protected void addURL(URL url) {
		synchronized (loaders) {
			if (isFileJarURL(url)) {
				if (closed)
					throw new IllegalStateException("Cannot add url. CloseableURLClassLoader is closed."); //$NON-NLS-1$
				loaderURLs.add(url);
				if (safeAddLoader(url))
					return;
			}
		}
		super.addURL(url);
	}

	@Override
	public URL[] getURLs() {
		List<URL> result = new ArrayList<>();
		synchronized (loaders) {
			result.addAll(loaderURLs);
		}
		result.addAll(Arrays.asList(super.getURLs()));
		return result.toArray(new URL[result.size()]);
	}

	private final Map<String, Thread> classNameLocks = new HashMap<>(5);
	private final Object pkgLock = new Object();

	private boolean lockClassName(String classname) {
		synchronized (classNameLocks) {
			Object lockingThread = classNameLocks.get(classname);
			Thread current = Thread.currentThread();
			if (lockingThread == current)
				return false;
			boolean previousInterruption = Thread.interrupted();
			try {
				while (true) {
					if (lockingThread == null) {
						classNameLocks.put(classname, current);
						return true;
					}

					classNameLocks.wait();
					lockingThread = classNameLocks.get(classname);
				}
			} catch (InterruptedException e) {
				current.interrupt();
				throw (LinkageError) new LinkageError(classname).initCause(e);
			} finally {
				if (previousInterruption) {
					current.interrupt();
				}
			}
		}
	}

	private void unlockClassName(String classname) {
		synchronized (classNameLocks) {
			classNameLocks.remove(classname);
			classNameLocks.notifyAll();
		}
	}

	protected boolean isRegisteredAsParallel() {
		return registeredAsParallel;
	}
}
