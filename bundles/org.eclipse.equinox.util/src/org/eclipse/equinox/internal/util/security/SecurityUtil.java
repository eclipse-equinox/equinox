/*******************************************************************************
 * Copyright (c) 1997-2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util.security;

import java.io.*;
import java.util.Dictionary;
import org.eclipse.equinox.internal.util.UtilActivator;
import org.osgi.framework.*;

/**
 * Utility class to execute common privileged code.
 * 
 * @author Valentin Valchev
 * @author Pavlin Dobrev
 * @version 1.0
 */

public final class SecurityUtil implements PrivilegedRunner.PrivilegedDispatcher {

	private Object controlContext;

	private static final int SYSTEM_GET_PROPERTY = 41;
	private static final int CREATE_THREAD = 42;
	private static final int CLASS_FOR_NAME = 43;
	private static final int SYSTEM_SET_PROPERTY = 44;

	private static final int FILE_BASE = 50;
	private static final int FILE_GET_INPUT_STREAM = FILE_BASE + 0;
	private static final int FILE_GET_OUTPUT_STREAM = FILE_BASE + 1;
	private static final int FILE_LENGTH = FILE_BASE + 2;
	private static final int FILE_EXISTS = FILE_BASE + 3;
	private static final int FILE_ISDIR = FILE_BASE + 4;
	private static final int FILE_LAST_MODIFIED = FILE_BASE + 5;
	private static final int FILE_LIST = FILE_BASE + 6;
	private static final int FILE_DELETE = FILE_BASE + 7;
	private static final int FILE_RENAME = FILE_BASE + 8;
	private static final int FILE_GET_RANDOM_ACCESS_FILE = FILE_BASE + 9;

	private static final int SERVICE_BASE = 60;
	private static final int SERVICE_GET_REFERENCE = SERVICE_BASE + 0;
	private static final int SERVICE_GET_SERVICE = SERVICE_BASE + 1;
	private static final int SERVICE_REG_CLASS = SERVICE_BASE + 2;
	private static final int SERVICE_REG_CLASSES = SERVICE_BASE + 3;

	private static final int BUNDLE_BASE = 70;
	private static final int BUNDLE_GET_LOCATION = BUNDLE_BASE + 0;
	private static final int BUNDLE_GET_HEADERS = BUNDLE_BASE + 1;
	private static final int BUNDLE_START = BUNDLE_BASE + 2;
	private static final int BUNDLE_STOP = BUNDLE_BASE + 3;
	private static final int BUNDLE_UNINSTALL = BUNDLE_BASE + 4;
	private static final int BUNDLE_UPDATE = BUNDLE_BASE + 5;
	private static final int BUNDLE_UPDATE_IS = BUNDLE_BASE + 6;

	/**
	 * Constructs a new SecureAction object. The constructed SecureAction object
	 * uses the caller's AccessControlContext to perform security checks
	 */
	public SecurityUtil() {
		// save the control context to be used.
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			controlContext = sm.getSecurityContext();
		}
	}

	/**
	 * Creates a new Thread from a Runnable. Same as calling new
	 * Thread(target,name).
	 * 
	 * @param target
	 *            the Runnable to create the Thread from.
	 * @param name
	 *            The name of the Thread.
	 * @return The new Thread
	 */
	public Thread createThread(final Runnable target, final String name) {
		try {
			return (Thread) PrivilegedRunner.doPrivileged(controlContext, this, CREATE_THREAD, target, name, null, null);
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Returns a Class. Same as calling Class.forName(name)
	 * 
	 * @param name
	 *            the name of the class.
	 * @return a Class
	 * @throws ClassNotFoundException
	 */
	public Class forName(final String name) throws ClassNotFoundException {
		try {
			return (Class) PrivilegedRunner.doPrivileged(controlContext, this, CLASS_FOR_NAME, name, null, null, null);
		} catch (ClassNotFoundException t) {
			throw t;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Returns a system property. Same as calling System.getProperty(String).
	 * 
	 * @param property
	 *            the property key.
	 * @return the value of the property or null if it does not exist.
	 */
	public String getProperty(final String property) {
		if (property == null) {
			throw new NullPointerException("property is null");
		}
		String ret;
		try {
			ret = (String) PrivilegedRunner.doPrivileged(controlContext, this, SYSTEM_GET_PROPERTY, property, null, null, null);
		} catch (Exception t) {
			ret = null;
		}
		return ret;
	}

	/**
	 * Returns a system property. Same as calling
	 * System.getProperty(String,String).
	 * 
	 * @param property
	 *            the property key.
	 * @param def
	 *            the default value if the property key does not exist.
	 * @return the value of the property or the default value if the property
	 *         does not exist.
	 */
	public String getProperty(final String property, final String def) {
		String ret = getProperty(property);
		return ret != null ? ret : def;
	}

	/**
	 * Returns a boolean system property. Same as calling
	 * Boolean.getBoolean(String).
	 * 
	 * @param property
	 *            the property key.
	 * @return the value of the property or <code>false</code>, if not set
	 */
	public boolean getBooleanProperty(final String property) {
		String ret = getProperty(property);
		return ret != null ? Boolean.valueOf(ret).booleanValue() : false;
	}

	/**
	 * Sets a system property. Same as System.setProperty()
	 * 
	 * @param key
	 *            the name of the property
	 * @param value
	 *            the value of the system property
	 * @return the old value of the property, or null
	 */
	public String setProperty(final String key, final String value) {
		if (key == null) {
			throw new NullPointerException("key is null");
		}
		if (value == null) {
			throw new NullPointerException("key is null");
		}
		try {
			return (String) PrivilegedRunner.doPrivileged(controlContext, this, SYSTEM_SET_PROPERTY, key, value, null, null);
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Creates a FileInputStream from a File. Same as calling new
	 * FileInputStream(File).
	 * 
	 * @param file
	 *            the File to create a FileInputStream from.
	 * @return The FileInputStream.
	 * @throws FileNotFoundException
	 *             if the File does not exist.
	 */
	public FileInputStream getFileInputStream(final File file) throws FileNotFoundException {
		try {
			return (FileInputStream) PrivilegedRunner.doPrivileged(controlContext, this, FILE_GET_INPUT_STREAM, file, null, null, null);
		} catch (FileNotFoundException t) {
			throw t;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Creates a random access file
	 * 
	 * @param file
	 *            the file object
	 * @param mode
	 *            the open mode
	 * @return the random seekable file object
	 * @throws FileNotFoundException
	 *             if the File does not exist
	 */
	public RandomAccessFile getRandomAccessFile(final File file, final String mode) throws FileNotFoundException {
		try {
			return (RandomAccessFile) PrivilegedRunner.doPrivileged(controlContext, this, FILE_GET_RANDOM_ACCESS_FILE, file, mode, null, null);
		} catch (FileNotFoundException t) {
			throw t;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Creates a FileInputStream from a File. Same as calling new
	 * FileOutputStream(File,boolean).
	 * 
	 * @param file
	 *            the File to create a FileOutputStream from.
	 * @param append
	 *            indicates if the OutputStream should append content.
	 * @return The FileOutputStream.
	 * @throws FileNotFoundException
	 *             if the File does not exist.
	 */
	public FileOutputStream getFileOutputStream(final File file, final boolean append) throws FileNotFoundException {
		try {
			return (FileOutputStream) PrivilegedRunner.doPrivileged(//
					controlContext, this, FILE_GET_OUTPUT_STREAM, file, //
					append ? Boolean.TRUE : Boolean.FALSE, null, null);
		} catch (FileNotFoundException t) {
			throw t;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Returns true if a file exists, otherwise false is returned. Same as
	 * calling file.exists().
	 * 
	 * @param file
	 *            a file object
	 * @return true if a file exists, otherwise false
	 */
	public boolean exists(final File file) {
		try {
			return ((Boolean) PrivilegedRunner.doPrivileged(controlContext, this, FILE_EXISTS, file, null, null, null)).booleanValue();
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Returns true if a file is a directory, otherwise false is returned. Same
	 * as calling file.isDirectory().
	 * 
	 * @param file
	 *            a file object
	 * @return true if a file is a directory, otherwise false
	 */
	public boolean isDirectory(final File file) {
		try {
			return ((Boolean) PrivilegedRunner.doPrivileged(controlContext, this, FILE_ISDIR, file, null, null, null)).booleanValue();
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Returns the length of a file. Same as calling file.length().
	 * 
	 * @param file
	 *            a file object
	 * @return the length of a file or -1 if file doesn't exists
	 */
	public long length(final File file) {
		try {
			return ((Long) PrivilegedRunner.doPrivileged(controlContext, this, FILE_LENGTH, file, null, null, null)).longValue();
		} catch (Throwable t) {
			return -1L;
		}
	}

	/**
	 * Returns a file's last modified stamp. Same as calling
	 * file.lastModified().
	 * 
	 * @param file
	 *            a file object
	 * @return a file's last modified stamp or -1 if file doesn't exists
	 */
	public long lastModified(final File file) {
		try {
			return ((Long) PrivilegedRunner.doPrivileged(controlContext, this, FILE_LAST_MODIFIED, file, null, null, null)).longValue();
		} catch (Exception t) {
			return -1L;
		}
	}

	/**
	 * Returns a file's list. Same as calling file.list().
	 * 
	 * @param file
	 *            a file object
	 * @return a file's list.
	 */
	public String[] list(final File file) {
		try {
			return (String[]) PrivilegedRunner.doPrivileged(controlContext, this, FILE_LIST, file, null, null, null);
		} catch (Exception t) {
			return null;
		}
	}

	/**
	 * Deletes the specified file. Same as File.delete()
	 * 
	 * @param file
	 *            the file object
	 * @return if delete succeeded
	 */
	public boolean delete(final File file) {
		try {
			return ((Boolean) PrivilegedRunner.doPrivileged(controlContext, this, FILE_DELETE, file, null, null, null)).booleanValue();
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Renames the source file to the target name. Same as File.renameTo(File)
	 * 
	 * @param source
	 *            the file object, that will be renamed
	 * @param target
	 *            the target file name
	 * @return if rename succeeded
	 */
	public boolean renameTo(final File source, final File target) {
		try {
			return ((Boolean) PrivilegedRunner.doPrivileged(controlContext, this, FILE_RENAME, source, target, null, null)).booleanValue();
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Gets a service object. Same as calling context.getService(reference)
	 * 
	 * @param reference
	 *            the ServiceReference
	 * @param context
	 *            the BundleContext
	 * @return a service object
	 */
	public Object getService(final ServiceReference reference, final BundleContext context) {
		if (context == null) {
			throw new NullPointerException("Context is null");
		}
		if (reference == null) {
			throw new NullPointerException("Reference is null");
		}
		try {
			return PrivilegedRunner.doPrivileged(controlContext, this, SERVICE_GET_SERVICE, context, reference, null, null);
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Gets a reference for the specified service(s). Same as calling
	 * context.getServiceReferences(class, filter)
	 * 
	 * @param clazz
	 *            the name of the requested service class
	 * @param filter
	 *            an LDAP filter
	 * @param context
	 *            the BundleContext
	 * @return a list of reference or <code>null</code>
	 * @throws InvalidSyntaxException
	 *             if filter is not correct
	 */
	public ServiceReference[] getServiceReferences(String clazz, String filter, BundleContext context) throws InvalidSyntaxException {
		if (context == null) {
			throw new NullPointerException("Context is null");
		}
		if (clazz == null && filter == null) {
			throw new NullPointerException("Either filter or clazz parameter should not be null");
		}
		try {
			return (ServiceReference[]) PrivilegedRunner.doPrivileged(controlContext, this, SERVICE_GET_REFERENCE, context, clazz, filter, null);
		} catch (InvalidSyntaxException e) {
			throw e;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Registers a service. Same as BundleContext.register(clazz, service,
	 * properties);
	 * 
	 * @param clazz
	 *            the class name of the service
	 * @param service
	 *            the service instance
	 * @param properties
	 *            the properties.
	 * @param context
	 *            the bundle context
	 * @return a service registration
	 */
	public ServiceRegistration registerService(String clazz, Object service, Dictionary properties, BundleContext context) {
		if (context == null) {
			throw new NullPointerException("Context is null");
		}
		if (service == null) {
			throw new NullPointerException("Service is null");
		}
		if (clazz == null) {
			throw new NullPointerException("Class name is null");
		}
		try {
			return (ServiceRegistration) PrivilegedRunner.doPrivileged(controlContext, this, SERVICE_REG_CLASS, context, clazz, service, properties);
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Registers a instance that implements many services. Same as
	 * BundleContext.register(clases, service, properties);
	 * 
	 * @param classes
	 *            the class names of the service
	 * @param service
	 *            the service instance
	 * @param properties
	 *            the properties.
	 * @param context
	 *            the bundle context
	 * @return a service registration
	 */
	public ServiceRegistration registerService(String[] classes, Object service, Dictionary properties, BundleContext context) {
		if (context == null) {
			throw new NullPointerException("Context is null");
		}
		if (service == null) {
			throw new NullPointerException("Service is null");
		}
		if (classes == null) {
			throw new NullPointerException("Class names are null");
		}
		try {
			return (ServiceRegistration) PrivilegedRunner.doPrivileged(controlContext, this, SERVICE_REG_CLASSES, context, classes, service, properties);
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Gets the location of the bundle. Same as Bundle.getLocation().
	 * 
	 * @param bundle
	 *            the bundle
	 * @return the bundle location
	 */
	public String getLocation(Bundle bundle) {
		try {
			return (String) PrivilegedRunner.doPrivileged(controlContext, this, BUNDLE_GET_LOCATION, bundle, null, null, null);
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Gets the bundle headers. Same as Bundle.getHeaders().
	 * 
	 * @param bundle
	 *            the bundle
	 * @return the bundle location
	 */
	public Dictionary getHeaders(Bundle bundle) {
		try {
			return (Dictionary) PrivilegedRunner.doPrivileged(controlContext, this, BUNDLE_GET_HEADERS, bundle, null, null, null);
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Starts the bundle. Same as Bundle.start()
	 * 
	 * @param bundle
	 *            the bundle
	 * @throws BundleException
	 */
	public void start(Bundle bundle) throws BundleException {
		try {
			PrivilegedRunner.doPrivileged(controlContext, this, BUNDLE_START, bundle, null, null, null);
		} catch (BundleException t) {
			throw t;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Stops the bundle. Same as Bundle.stop()
	 * 
	 * @param bundle
	 *            the bundle
	 * @throws BundleException
	 */
	public void stop(Bundle bundle) throws BundleException {
		try {
			PrivilegedRunner.doPrivileged(controlContext, this, BUNDLE_STOP, bundle, null, null, null);
		} catch (BundleException t) {
			throw t;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Un-installs the bundle. Same as Bundle.uninstall()
	 * 
	 * @param bundle
	 *            the bundle
	 * @throws BundleException
	 */
	public void uninstall(Bundle bundle) throws BundleException {
		try {
			PrivilegedRunner.doPrivileged(controlContext, this, BUNDLE_UNINSTALL, bundle, null, null, null);
		} catch (BundleException t) {
			throw t;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Updates the bundle. Same as Bundle.update()
	 * 
	 * @param bundle
	 *            the bundle
	 * @throws BundleException
	 */
	public void update(Bundle bundle) throws BundleException {
		try {
			PrivilegedRunner.doPrivileged(controlContext, this, BUNDLE_UPDATE, bundle, null, null, null);
		} catch (BundleException t) {
			throw t;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Updates the bundle from stream. Same as Bundle.update(stream)
	 * 
	 * @param bundle
	 *            the bundle
	 * @param is
	 *            the stream
	 * @throws BundleException
	 */
	public void update(Bundle bundle, InputStream is) throws BundleException {
		try {
			PrivilegedRunner.doPrivileged(controlContext, this, BUNDLE_UPDATE_IS, bundle, is, null, null);
		} catch (BundleException t) {
			throw t;
		} catch (Exception t) {
			throw new RuntimeException(t.getMessage());
		}
	}

	/**
	 * Performs a privileged action <b>using the current security context</b>.
	 * The method calls the dispatcher inside the privileged call passing it the
	 * same parameters that were passed to this method.
	 * 
	 * @param dispatcher
	 *            the dispatcher which should be called
	 * @param type
	 *            the type of the action - used in the dispatcher
	 * @param arg1
	 *            a parameter received by the dispatcher
	 * @param arg2
	 *            a parameter received by the dispatcher
	 * @param arg3
	 *            a parameter received by the dispatcher
	 * @param arg4
	 *            a parameter received by the dispatcher
	 * @return the object returned from the execution
	 * @throws Exception
	 *             if the dispatcher fails
	 * @see PrivilegedRunner#doPrivileged(Object,
	 *      org.eclipse.equinox.internal.util.security.PrivilegedRunner.PrivilegedDispatcher,
	 *      int, Object, Object, Object, Object)
	 */
	public Object doPrivileged(PrivilegedRunner.PrivilegedDispatcher dispatcher, int type, Object arg1, Object arg2, Object arg3, Object arg4) throws Exception {
		return PrivilegedRunner.doPrivileged(controlContext, dispatcher, type, arg1, arg2, arg3, arg4);
	}

	/**
	 * Performs a privileged action <b>using the current security context</b>.
	 * The method calls the dispatcher inside the privileged call passing it the
	 * same parameters that were passed to this method.
	 * 
	 * @param dispatcher
	 *            the dispatcher which should be called
	 * @param type
	 *            the type of the action - used in the dispatcher
	 * @param arg1
	 *            a parameter received by the dispatcher
	 * @param arg2
	 *            a parameter received by the dispatcher
	 * @param arg3
	 *            a parameter received by the dispatcher
	 * @return the object returned from the execution
	 * @throws Exception
	 *             if the dispatcher fails
	 * @see PrivilegedRunner#doPrivileged(Object,
	 *      org.eclipse.equinox.internal.util.security.PrivilegedRunner.PrivilegedDispatcher,
	 *      int, Object, Object, Object, Object)
	 */
	public Object doPrivileged(PrivilegedRunner.PrivilegedDispatcher dispatcher, int type, Object arg1, Object arg2, Object arg3) throws Exception {
		return PrivilegedRunner.doPrivileged(controlContext, dispatcher, type, arg1, arg2, arg3, null);
	}

	/**
	 * Performs a privileged action <b>using the current security context</b>.
	 * The method calls the dispatcher inside the privileged call passing it the
	 * same parameters that were passed to this method.
	 * 
	 * @param dispatcher
	 *            the dispatcher which should be called
	 * @param type
	 *            the type of the action - used in the dispatcher
	 * @param arg1
	 *            a parameter received by the dispatcher
	 * @param arg2
	 *            a parameter received by the dispatcher
	 * @return the object returned from the execution
	 * @throws Exception
	 *             if the dispatcher fails
	 * @see PrivilegedRunner#doPrivileged(Object,
	 *      org.eclipse.equinox.internal.util.security.PrivilegedRunner.PrivilegedDispatcher,
	 *      int, Object, Object, Object, Object)
	 */
	public Object doPrivileged(PrivilegedRunner.PrivilegedDispatcher dispatcher, int type, Object arg1, Object arg2) throws Exception {
		return PrivilegedRunner.doPrivileged(controlContext, dispatcher, type, arg1, arg2, null, null);
	}

	/**
	 * Performs a privileged action <b>using the current security context</b>.
	 * The method calls the dispatcher inside the privileged call passing it the
	 * same parameters that were passed to this method.
	 * 
	 * @param dispatcher
	 *            the dispatcher which should be called
	 * @param type
	 *            the type of the action - used in the dispatcher
	 * @param arg1
	 *            a parameter received by the dispatcher
	 * @return the object returned from the execution
	 * @throws Exception
	 *             if the dispatcher fails
	 * @see PrivilegedRunner#doPrivileged(Object,
	 *      org.eclipse.equinox.internal.util.security.PrivilegedRunner.PrivilegedDispatcher,
	 *      int, Object, Object, Object, Object)
	 */
	public Object doPrivileged(PrivilegedRunner.PrivilegedDispatcher dispatcher, int type, Object arg1) throws Exception {
		return PrivilegedRunner.doPrivileged(controlContext, dispatcher, type, arg1, null, null, null);
	}

	/**
	 * @see org.eclipse.equinox.internal.util.security.PrivilegedRunner.PrivilegedDispatcher#dispatchPrivileged(int,
	 *      java.lang.Object, java.lang.Object, java.lang.Object,
	 *      java.lang.Object)
	 */
	public Object dispatchPrivileged(int type, Object arg1, Object arg2, Object arg3, Object arg4) throws Exception {
		switch (type) {
			case SYSTEM_GET_PROPERTY :
				return UtilActivator.bc.getProperty((String) arg1);
			case CREATE_THREAD :
				return new Thread((Runnable) arg1, (String) arg2);
			case CLASS_FOR_NAME :
				return Class.forName((String) arg1);
			case SYSTEM_SET_PROPERTY :
				return System.getProperties().put(arg1, arg2);
			case FILE_GET_INPUT_STREAM :
				return new FileInputStream((File) arg1);
			case FILE_GET_OUTPUT_STREAM :
				return new FileOutputStream(((File) arg1).getAbsolutePath(), ((Boolean) arg2).booleanValue());
			case FILE_LENGTH :
				return new Long(((File) arg1).length());
			case FILE_EXISTS :
				return ((File) arg1).exists() ? Boolean.TRUE : Boolean.FALSE;
			case FILE_ISDIR :
				return ((File) arg1).isDirectory() ? Boolean.TRUE : Boolean.FALSE;
			case FILE_LAST_MODIFIED :
				return new Long(((File) arg1).lastModified());
			case FILE_LIST :
				return ((File) arg1).list();
			case FILE_DELETE :
				return ((File) arg1).delete() ? Boolean.TRUE : Boolean.FALSE;
			case FILE_RENAME :
				return ((File) arg1).renameTo(((File) arg2)) ? Boolean.TRUE : Boolean.FALSE;
			case FILE_GET_RANDOM_ACCESS_FILE :
				return new RandomAccessFile((File) arg1, (String) arg2);
			case SERVICE_GET_REFERENCE :
				return ((BundleContext) arg1).getServiceReferences((String) arg2, (String) arg3);
			case SERVICE_GET_SERVICE :
				return ((BundleContext) arg1).getService((ServiceReference) arg2);
			case SERVICE_REG_CLASS :
				return ((BundleContext) arg1).registerService((String) arg2, arg3, (Dictionary) arg4);
			case SERVICE_REG_CLASSES :
				return ((BundleContext) arg1).registerService((String[]) arg2, arg3, (Dictionary) arg4);
			case BUNDLE_GET_LOCATION :
				return ((Bundle) arg1).getLocation();
			case BUNDLE_GET_HEADERS :
				return ((Bundle) arg1).getHeaders();
			case BUNDLE_START :
				((Bundle) arg1).start();
				break;
			case BUNDLE_STOP :
				((Bundle) arg1).stop();
				break;
			case BUNDLE_UNINSTALL :
				((Bundle) arg1).uninstall();
				break;
			case BUNDLE_UPDATE :
				((Bundle) arg1).update();
				break;
			case BUNDLE_UPDATE_IS :
				((Bundle) arg1).update((InputStream) arg2);
				break;
		}
		return null;
	}

}
