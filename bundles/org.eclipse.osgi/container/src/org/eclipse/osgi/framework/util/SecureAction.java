/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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

package org.eclipse.osgi.framework.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.eclipse.osgi.container.Module;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utility class to execute common privileged code.
 * 
 * @since 3.1
 */
public class SecureAction {
	// make sure we use the correct controlContext;
	private AccessControlContext controlContext;

	// uses initialization-on-demand holder idiom to do fast lazy loading
	private static class BootClassLoaderHolder {
		// This ClassLoader is used in loadSystemClass if System.getClassLoader()
		// returns null
		static final ClassLoader bootClassLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
			@Override
			public ClassLoader run() {
				return new ClassLoader(Object.class.getClassLoader()) {
					/* boot class loader */};
			}
		});

	}

	/*
	 * Package privaet constructor a new SecureAction object. The constructed
	 * SecureAction object uses the caller's AccessControlContext to perform
	 * security checks
	 */
	SecureAction() {
		// save the control context to be used.
		this.controlContext = AccessController.getContext();
	}

	/**
	 * Creates a privileged action that can be used to construct a SecureAction
	 * object. The recommended way to construct a SecureAction object is the
	 * following:
	 * 
	 * <pre>
	 * SecureAction secureAction = (SecureAction) AccessController.doPrivileged(SecureAction.createSecureAction());
	 * </pre>
	 * 
	 * @return a privileged action object that can be used to construct a
	 *         SecureAction object.
	 */
	public static PrivilegedAction<SecureAction> createSecureAction() {
		return new PrivilegedAction<SecureAction>() {
			@Override
			public SecureAction run() {
				return new SecureAction();
			}
		};
	}

	/**
	 * Returns a system property. Same as calling System.getProperty(String).
	 * 
	 * @param property the property key.
	 * @return the value of the property or null if it does not exist.
	 */
	public String getProperty(final String property) {
		if (System.getSecurityManager() == null)
			return System.getProperty(property);
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			@Override
			public String run() {
				return System.getProperty(property);
			}
		}, controlContext);
	}

	/**
	 * Returns a system properties. Same as calling System.getProperties().
	 * 
	 * @return the system properties.
	 */
	public Properties getProperties() {
		if (System.getSecurityManager() == null)
			return System.getProperties();
		return AccessController.doPrivileged(new PrivilegedAction<Properties>() {
			@Override
			public Properties run() {
				return System.getProperties();
			}
		}, controlContext);
	}

	/**
	 * Creates a FileInputStream from a File. Same as calling new
	 * FileInputStream(File).
	 * 
	 * @param file the File to craete a FileInputStream from.
	 * @return The FileInputStream.
	 * @throws FileNotFoundException if the File does not exist.
	 */
	public FileInputStream getFileInputStream(final File file) throws FileNotFoundException {
		if (System.getSecurityManager() == null)
			return new FileInputStream(file);
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
				@Override
				public FileInputStream run() throws FileNotFoundException {
					return new FileInputStream(file);
				}
			}, controlContext);
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof FileNotFoundException)
				throw (FileNotFoundException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	/**
	 * Creates a FileInputStream from a File. Same as calling new
	 * FileOutputStream(File,boolean).
	 * 
	 * @param file   the File to create a FileOutputStream from.
	 * @param append indicates if the OutputStream should append content.
	 * @return The FileOutputStream.
	 * @throws FileNotFoundException if the File does not exist.
	 */
	public FileOutputStream getFileOutputStream(final File file, final boolean append) throws FileNotFoundException {
		if (System.getSecurityManager() == null)
			return new FileOutputStream(file.getAbsolutePath(), append);
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<FileOutputStream>() {
				@Override
				public FileOutputStream run() throws FileNotFoundException {
					return new FileOutputStream(file.getAbsolutePath(), append);
				}
			}, controlContext);
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof FileNotFoundException)
				throw (FileNotFoundException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	/**
	 * Returns the length of a file. Same as calling file.length().
	 * 
	 * @param file a file object
	 * @return the length of a file.
	 */
	public long length(final File file) {
		if (System.getSecurityManager() == null)
			return file.length();
		return AccessController.doPrivileged(new PrivilegedAction<Long>() {
			@Override
			public Long run() {
				return Long.valueOf(file.length());
			}
		}, controlContext).longValue();
	}

	/**
	 * Returns the canonical path of a file. Same as calling
	 * file.getCanonicalPath().
	 * 
	 * @param file a file object
	 * @return the canonical path of a file.
	 * @throws IOException on error
	 */
	public String getCanonicalPath(final File file) throws IOException {
		if (System.getSecurityManager() == null)
			return file.getCanonicalPath();
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
				@Override
				public String run() throws IOException {
					return file.getCanonicalPath();
				}
			}, controlContext);
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof IOException)
				throw (IOException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	/**
	 * Returns the absolute file. Same as calling file.getAbsoluteFile().
	 * 
	 * @param file a file object
	 * @return the absolute file.
	 */
	public File getAbsoluteFile(final File file) {
		if (System.getSecurityManager() == null)
			return file.getAbsoluteFile();
		return AccessController.doPrivileged(new PrivilegedAction<File>() {
			@Override
			public File run() {
				return file.getAbsoluteFile();
			}
		}, controlContext);
	}

	/**
	 * Returns the canonical file. Same as calling file.getCanonicalFile().
	 * 
	 * @param file a file object
	 * @return the canonical file.
	 */
	public File getCanonicalFile(final File file) throws IOException {
		if (System.getSecurityManager() == null)
			return file.getCanonicalFile();
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {
				@Override
				public File run() throws IOException {
					return file.getCanonicalFile();
				}
			}, controlContext);
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof IOException)
				throw (IOException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	/**
	 * Returns true if a file exists, otherwise false is returned. Same as calling
	 * file.exists().
	 * 
	 * @param file a file object
	 * @return true if a file exists, otherwise false
	 */
	public boolean exists(final File file) {
		if (System.getSecurityManager() == null)
			return file.exists();
		return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			@Override
			public Boolean run() {
				return file.exists() ? Boolean.TRUE : Boolean.FALSE;
			}
		}, controlContext).booleanValue();
	}

	public boolean mkdirs(final File file) {
		if (System.getSecurityManager() == null)
			return file.mkdirs();
		return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			@Override
			public Boolean run() {
				return file.mkdirs() ? Boolean.TRUE : Boolean.FALSE;
			}
		}, controlContext).booleanValue();
	}

	/**
	 * Returns true if a file is a directory, otherwise false is returned. Same as
	 * calling file.isDirectory().
	 * 
	 * @param file a file object
	 * @return true if a file is a directory, otherwise false
	 */
	public boolean isDirectory(final File file) {
		if (System.getSecurityManager() == null)
			return file.isDirectory();
		return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			@Override
			public Boolean run() {
				return file.isDirectory() ? Boolean.TRUE : Boolean.FALSE;
			}
		}, controlContext).booleanValue();
	}

	/**
	 * Returns a file's last modified stamp. Same as calling file.lastModified().
	 * 
	 * @param file a file object
	 * @return a file's last modified stamp.
	 */
	public long lastModified(final File file) {
		if (System.getSecurityManager() == null)
			return file.lastModified();
		return AccessController.doPrivileged(new PrivilegedAction<Long>() {
			@Override
			public Long run() {
				return Long.valueOf(file.lastModified());
			}
		}, controlContext).longValue();
	}

	/**
	 * Returns a file's list. Same as calling file.list().
	 * 
	 * @param file a file object
	 * @return a file's list.
	 */
	public String[] list(final File file) {
		if (System.getSecurityManager() == null)
			return file.list();
		return AccessController.doPrivileged(new PrivilegedAction<String[]>() {
			@Override
			public String[] run() {
				return file.list();
			}
		}, controlContext);
	}

	/**
	 * Returns a ZipFile. Same as calling new ZipFile(file)
	 * 
	 * @param file   the file to get a ZipFile for
	 * @param verify whether or not to verify the zip file if it is signed.
	 * @return a ZipFile
	 * @throws IOException if an error occured
	 */
	public ZipFile getZipFile(final File file, final boolean verify) throws IOException {
		try {
			if (System.getSecurityManager() == null)
				return new ZipFile(file);
			try {
				return AccessController.doPrivileged(new PrivilegedExceptionAction<ZipFile>() {
					@Override
					public ZipFile run() throws IOException {
						return verify ? new JarFile(file) : new ZipFile(file);
					}
				}, controlContext);
			} catch (PrivilegedActionException e) {
				if (e.getException() instanceof IOException)
					throw (IOException) e.getException();
				throw (RuntimeException) e.getException();
			}
		} catch (ZipException e) {
			ZipException zipNameException = new ZipException("Exception in opening zip file: " + file.getPath()); //$NON-NLS-1$
			zipNameException.initCause(e);
			throw zipNameException;
		} catch (IOException e) {
			throw new IOException("Exception in opening zip file: " + file.getPath(), e); //$NON-NLS-1$
		}
	}

	/**
	 * Gets a URL. Same a calling
	 * {@link URL#URL(java.lang.String, java.lang.String, int, java.lang.String, java.net.URLStreamHandler)}
	 * 
	 * @param protocol the protocol
	 * @param host     the host
	 * @param port     the port
	 * @param file     the file
	 * @param handler  the URLStreamHandler
	 * @return a URL
	 */
	public URL getURL(final String protocol, final String host, final int port, final String file,
			final URLStreamHandler handler) throws MalformedURLException {
		if (System.getSecurityManager() == null)
			return new URL(protocol, host, port, file, handler);
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
				@Override
				public URL run() throws MalformedURLException {
					return new URL(protocol, host, port, file, handler);
				}
			}, controlContext);
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof MalformedURLException)
				throw (MalformedURLException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	/**
	 * Creates a new Thread from a Runnable. Same as calling new
	 * Thread(target,name).setContextClassLoader(contextLoader).
	 * 
	 * @param target        the Runnable to create the Thread from.
	 * @param name          The name of the Thread.
	 * @param contextLoader the context class loader for the thread
	 * @return The new Thread
	 */
	public Thread createThread(final Runnable target, final String name, final ClassLoader contextLoader) {
		if (System.getSecurityManager() == null)
			return createThread0(target, name, contextLoader);
		return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
			@Override
			public Thread run() {
				return createThread0(target, name, contextLoader);
			}
		}, controlContext);
	}

	Thread createThread0(Runnable target, String name, ClassLoader contextLoader) {
		Thread result = new Thread(target, name);
		if (contextLoader != null)
			result.setContextClassLoader(contextLoader);
		return result;
	}

	/**
	 * Gets a service object. Same as calling context.getService(reference)
	 * 
	 * @param reference the ServiceReference
	 * @param context   the BundleContext
	 * @return a service object
	 */
	public <S> S getService(final ServiceReference<S> reference, final BundleContext context) {
		if (System.getSecurityManager() == null)
			return context.getService(reference);
		return AccessController.doPrivileged(new PrivilegedAction<S>() {
			@Override
			public S run() {
				return context.getService(reference);
			}
		}, controlContext);
	}

	/**
	 * Returns a Class. Same as calling Class.forName(name)
	 * 
	 * @param name the name of the class.
	 * @return a Class
	 */
	public Class<?> forName(final String name) throws ClassNotFoundException {
		if (System.getSecurityManager() == null)
			return Class.forName(name);
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
				@Override
				public Class<?> run() throws Exception {
					return Class.forName(name);
				}
			}, controlContext);
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof ClassNotFoundException)
				throw (ClassNotFoundException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	/**
	 * Returns a Class. Tries to load a class from the System ClassLoader or if that
	 * doesn't exist tries the boot ClassLoader
	 * 
	 * @param name the name of the class.
	 * @return a Class
	 */
	public Class<?> loadSystemClass(final String name) throws ClassNotFoundException {
		if (System.getSecurityManager() == null) {
			ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
			return (systemClassLoader != null) ? systemClassLoader.loadClass(name)
					: BootClassLoaderHolder.bootClassLoader.loadClass(name);
		}
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
				@Override
				public Class<?> run() throws Exception {
					ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
					return (systemClassLoader != null) ? systemClassLoader.loadClass(name)
							: BootClassLoaderHolder.bootClassLoader.loadClass(name);
				}
			}, controlContext);
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof ClassNotFoundException)
				throw (ClassNotFoundException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	/**
	 * Opens a ServiceTracker. Same as calling tracker.open()
	 * 
	 * @param tracker the ServiceTracker to open.
	 */
	public void open(final ServiceTracker<?, ?> tracker) {
		if (System.getSecurityManager() == null) {
			tracker.open();
			return;
		}
		AccessController.doPrivileged(new PrivilegedAction<Void>() {
			@Override
			public Void run() {
				tracker.open();
				return null;
			}
		}, controlContext);
	}

	/**
	 * Starts a module.
	 * 
	 * @param module  the module to start
	 * @param options the start options
	 */
	public void start(final Module module, final Module.StartOptions... options) throws BundleException {
		if (System.getSecurityManager() == null) {
			module.start(options);
			return;
		}
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws BundleException {
					module.start(options);
					return null;
				}
			}, controlContext);
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof BundleException)
				throw (BundleException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	public BundleContext getContext(final Bundle bundle) {
		if (System.getSecurityManager() == null) {
			return bundle.getBundleContext();
		}
		return AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
			@Override
			public BundleContext run() {
				return bundle.getBundleContext();
			}
		}, controlContext);
	}

	public String getLocation(final Bundle bundle) {
		if (System.getSecurityManager() == null) {
			return bundle.getLocation();
		}
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			@Override
			public String run() {
				return bundle.getLocation();
			}
		}, controlContext);
	}
}
