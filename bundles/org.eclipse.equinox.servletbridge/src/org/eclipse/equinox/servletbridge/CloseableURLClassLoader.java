/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.servletbridge;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.jar.Attributes.Name;

public class CloseableURLClassLoader extends URLClassLoader {
	static final String DOT_CLASS = ".class"; //$NON-NLS-1$
	static final String BANG_SLASH = "!/"; //$NON-NLS-1$
	static final String JAR = "jar"; //$NON-NLS-1$

	final ArrayList loaders = new ArrayList(); // package private to avoid synthetic access.
	private final ArrayList loaderURLs = new ArrayList(); // note: protected by loaders
	boolean closed = false; // note: protected by loaders, package private to avoid synthetic access.

	private final AccessControlContext context;
	private boolean verifyJars;

	public static class CloseableJarURLConnection extends JarURLConnection {
		private final JarFile jarFile;
		private JarEntry entry;

		public CloseableJarURLConnection(URL url, JarFile jarFile) throws MalformedURLException {
			super(url);
			this.jarFile = jarFile;
		}

		/**
		 * @throws IOException
		 * Documented to avoid warning 
		 */
		public synchronized void connect() throws IOException {
			if (entry != null)
				return;
			entry = jarFile.getJarEntry(getEntryName());
		}

		public InputStream getInputStream() throws IOException {
			connect();
			return jarFile.getInputStream(entry);
		}

		/**
		 * @throws IOException
		 * Documented to avoid warning 
		 */
		public JarFile getJarFile() throws IOException {
			return jarFile;
		}

		public JarEntry getJarEntry() throws IOException {
			connect();
			return entry;
		}
	}

	public static class CloseableJarURLStreamHandler extends URLStreamHandler {
		private final JarFile jarFile;

		public CloseableJarURLStreamHandler(JarFile jarFile) {
			this.jarFile = jarFile;
		}

		protected URLConnection openConnection(URL u) throws IOException {
			return new CloseableJarURLConnection(u, jarFile);
		}

		protected void parseURL(URL u, String spec, int start, int limit) {
			setURL(u, JAR, null, 0, null, null, spec.substring(start, limit), null, null);
		}
	}

	public static class CloseableJarFileLoader {
		private final JarFile jarFile;
		private final Manifest manifest;
		private final CloseableJarURLStreamHandler jarURLStreamHandler;
		private final String jarFileURLPrefixString;
		private final Collection entries = new HashSet();

		public CloseableJarFileLoader(File file, boolean verify) throws IOException {
			this.jarFile = new JarFile(file, verify);
			this.manifest = jarFile.getManifest();
			this.jarURLStreamHandler = new CloseableJarURLStreamHandler(jarFile);
			this.jarFileURLPrefixString = file.toURL().toString() + BANG_SLASH;
			Enumeration e = jarFile.entries();
			while (e.hasMoreElements()) {
				JarEntry entry = (JarEntry) e.nextElement();
				if (!entry.isDirectory())
					entries.add(entry.getName());
			}
		}

		public URL getURL(String name) {
			if (entries.contains(name))
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

	public CloseableURLClassLoader(URL[] urls) {
		this(urls, ClassLoader.getSystemClassLoader(), true);
	}

	public CloseableURLClassLoader(URL[] urls, ClassLoader parent) {
		this(excludeFileJarURLS(urls), parent, true);
	}

	public CloseableURLClassLoader(URL[] urls, ClassLoader parent, boolean verifyJars) {
		super(excludeFileJarURLS(urls), parent);
		this.context = AccessController.getContext();
		this.verifyJars = verifyJars;
		for (int i = 0; i < urls.length; i++) {
			if (isFileJarURL(urls[i])) {
				loaderURLs.add(urls[i]);
				safeAddLoader(urls[i]);
			}
		}
	}

	private void safeAddLoader(URL url) {
		String path = url.getPath();
		File file = new File(path);
		if (file.exists()) {
			try {
				loaders.add(new CloseableJarFileLoader(file, verifyJars));
			} catch (IOException e) {
				// ignore
			}
		}
	}

	private static URL[] excludeFileJarURLS(URL[] urls) {
		ArrayList urlList = new ArrayList();
		for (int i = 0; i < urls.length; i++) {
			if (!isFileJarURL(urls[i]))
				urlList.add(urls[i]);
		}
		return (URL[]) urlList.toArray(new URL[urlList.size()]);
	}

	private static boolean isFileJarURL(URL url) {
		if (!url.getProtocol().equals("file")) //$NON-NLS-1$
			return false;

		String path = url.getPath();
		if (path != null && path.endsWith("/")) //$NON-NLS-1$
			return false;

		return true;
	}

	protected Class findClass(final String name) throws ClassNotFoundException {
		try {
			Class clazz = (Class) AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws ClassNotFoundException {
					String resourcePath = name.replace('.', '/') + DOT_CLASS;
					CloseableJarFileLoader loader = null;
					URL resourceURL = null;
					synchronized (loaders) {
						if (closed)
							return null;
						for (Iterator iterator = loaders.iterator(); iterator.hasNext();) {
							loader = (CloseableJarFileLoader) iterator.next();
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
	Class defineClass(String name, URL resourceURL, Manifest manifest) throws IOException {
		JarURLConnection connection = (JarURLConnection) resourceURL.openConnection();
		int lastDot = name.lastIndexOf('.');
		if (lastDot != -1) {
			String packageName = name.substring(0, lastDot + 1);
			Package pkg = getPackage(packageName);
			if (pkg != null) {
				checkForSealedPackage(pkg, packageName, manifest, connection.getJarFileURL());
			} else {
				definePackage(packageName, manifest, connection.getJarFileURL());
			}
		}
		JarEntry entry = connection.getJarEntry();
		byte[] bytes = new byte[(int) entry.getSize()];
		DataInputStream is = null;
		try {
			is = new DataInputStream(connection.getInputStream());
			is.readFully(bytes, 0, bytes.length);
			CodeSource cs = new CodeSource(connection.getJarFileURL(), entry.getCertificates());
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
		if (pkg.isSealed() && !pkg.isSealed(jarFileURL))
			throw new SecurityException("The package '" + packageName + "' was previously loaded and is already sealed."); //$NON-NLS-1$ //$NON-NLS-2$

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
		if (Boolean.valueOf(sealed).booleanValue())
			throw new SecurityException("The package '" + packageName + "' was previously loaded unsealed. Cannot seal package."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public URL findResource(final String name) {
		URL url = (URL) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				synchronized (loaders) {
					if (closed)
						return null;
					for (Iterator iterator = loaders.iterator(); iterator.hasNext();) {
						CloseableJarFileLoader loader = (CloseableJarFileLoader) iterator.next();
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

	public Enumeration findResources(final String name) throws IOException {
		final List resources = new ArrayList();
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				synchronized (loaders) {
					if (closed)
						return null;
					for (Iterator iterator = loaders.iterator(); iterator.hasNext();) {
						CloseableJarFileLoader loader = (CloseableJarFileLoader) iterator.next();
						URL resourceURL = loader.getURL(name);
						if (resourceURL != null)
							resources.add(resourceURL);
					}
				}
				return null;
			}
		}, context);
		Enumeration e = super.findResources(name);
		while (e.hasMoreElements())
			resources.add(e.nextElement());

		return Collections.enumeration(resources);
	}

	public void close() {
		synchronized (loaders) {
			if (closed)
				return;
			for (Iterator iterator = loaders.iterator(); iterator.hasNext();) {
				CloseableJarFileLoader loader = (CloseableJarFileLoader) iterator.next();
				loader.close();
			}
			closed = true;
		}
	}

	protected void addURL(URL url) {
		synchronized (loaders) {
			if (isFileJarURL(url)) {
				if (closed)
					throw new IllegalStateException("Cannot add url. CloseableURLClassLoader is closed."); //$NON-NLS-1$
				loaderURLs.add(url);
				safeAddLoader(url);
				return;
			}
		}
		super.addURL(url);
	}

	public URL[] getURLs() {
		List result = new ArrayList();
		synchronized (loaders) {
			result.addAll(loaderURLs);
		}
		result.addAll(Arrays.asList(super.getURLs()));
		return (URL[]) result.toArray(new URL[result.size()]);
	}
}
