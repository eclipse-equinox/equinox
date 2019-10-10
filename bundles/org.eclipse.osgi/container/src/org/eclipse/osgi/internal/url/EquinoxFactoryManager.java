/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.osgi.internal.url;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.util.Hashtable;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.osgi.framework.BundleContext;

public class EquinoxFactoryManager {
	private final EquinoxContainer container;
	// we need to hold these so that we can unregister them at shutdown
	private volatile URLStreamHandlerFactoryImpl urlStreamHandlerFactory;
	private volatile ContentHandlerFactoryImpl contentHandlerFactory;

	public EquinoxFactoryManager(EquinoxContainer container) {
		this.container = container;
	}

	public void installHandlerFactories(BundleContext context) {
		installURLStreamHandlerFactory(context);
		installContentHandlerFactory(context);
	}

	private void installURLStreamHandlerFactory(BundleContext context) {
		URLStreamHandlerFactoryImpl shf = new URLStreamHandlerFactoryImpl(context, container);
		try {
			// first try the standard way
			URL.setURLStreamHandlerFactory(shf);
		} catch (Error err) {
			try {
				// ok we failed now use more drastic means to set the factory
				forceURLStreamHandlerFactory(shf);
			} catch (Throwable ex) {
				container.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, ex.getMessage(), ex);
				urlStreamHandlerFactory = null;
				return;
			}
		}
		urlStreamHandlerFactory = shf;
	}

	private static void forceURLStreamHandlerFactory(URLStreamHandlerFactoryImpl shf) throws Exception {
		Field factoryField = getField(URL.class, URLStreamHandlerFactory.class, false);
		if (factoryField == null)
			throw new Exception("Could not find URLStreamHandlerFactory field"); //$NON-NLS-1$
		// look for a lock to synchronize on
		Object lock = getURLStreamHandlerFactoryLock();
		synchronized (lock) {
			URLStreamHandlerFactory factory = (URLStreamHandlerFactory) factoryField.get(null);
			// doing a null check here just in case, but it would be really strange if it was null, 
			// because we failed to set the factory normally!!
			if (factory != null) {
				try {
					factory.getClass().getMethod("isMultiplexing", (Class[]) null); //$NON-NLS-1$
					Method register = factory.getClass().getMethod("register", new Class[] {Object.class}); //$NON-NLS-1$
					register.invoke(factory, new Object[] {shf});
				} catch (NoSuchMethodException e) {
					// current factory does not support multiplexing, ok we'll wrap it
					shf.setParentFactory(factory);
					factory = shf;
				}
			}
			factoryField.set(null, null);
			// always attempt to clear the handlers cache
			// This allows an optimization for the single framework use-case
			resetURLStreamHandlers();
			URL.setURLStreamHandlerFactory(factory);
		}
	}

	private static void resetURLStreamHandlers() throws IllegalAccessException {
		Field handlersField = getField(URL.class, Hashtable.class, false);
		if (handlersField != null) {
			@SuppressWarnings("rawtypes")
			Hashtable<?, ?> handlers = (Hashtable) handlersField.get(null);
			if (handlers != null)
				handlers.clear();
		}
	}

	private static Object getURLStreamHandlerFactoryLock() throws IllegalAccessException {
		Object lock;
		try {
			Field streamHandlerLockField = URL.class.getDeclaredField("streamHandlerLock"); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(streamHandlerLockField);
			lock = streamHandlerLockField.get(null);
		} catch (NoSuchFieldException noField) {
			// could not find the lock, lets sync on the class object
			lock = URL.class;
		}
		return lock;
	}

	private void installContentHandlerFactory(BundleContext context) {
		ContentHandlerFactoryImpl chf = new ContentHandlerFactoryImpl(context, container);
		try {
			// first try the standard way
			URLConnection.setContentHandlerFactory(chf);
		} catch (Error err) {
			// ok we failed now use more drastic means to set the factory
			try {
				forceContentHandlerFactory(chf);
			} catch (Throwable ex) {
				// this is unexpected, log the exception and throw the original error
				container.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, ex.getMessage(), ex);
				contentHandlerFactory = null;
				return;
			}
		}
		contentHandlerFactory = chf;
	}

	private static void forceContentHandlerFactory(ContentHandlerFactoryImpl chf) throws Exception {
		Field factoryField = getField(URLConnection.class, java.net.ContentHandlerFactory.class, false);
		if (factoryField == null)
			throw new Exception("Could not find ContentHandlerFactory field"); //$NON-NLS-1$
		synchronized (URLConnection.class) {
			java.net.ContentHandlerFactory factory = (java.net.ContentHandlerFactory) factoryField.get(null);
			// doing a null check here just in case, but it would be really strange if it was null, 
			// because we failed to set the factory normally!!

			if (factory != null) {
				try {
					factory.getClass().getMethod("isMultiplexing", (Class[]) null); //$NON-NLS-1$
					Method register = factory.getClass().getMethod("register", new Class[] {Object.class}); //$NON-NLS-1$
					register.invoke(factory, new Object[] {chf});
				} catch (NoSuchMethodException e) {
					// current factory does not support multiplexing, ok we'll wrap it
					chf.setParentFactory(factory);
					factory = chf;
				}
			}
			// null out the field so that we can successfully call setContentHandlerFactory			
			factoryField.set(null, null);
			// always attempt to clear the handlers cache
			// This allows an optimization for the single framework use-case
			resetContentHandlers();
			URLConnection.setContentHandlerFactory(factory);
		}
	}

	private static void resetContentHandlers() throws IllegalAccessException {
		Field handlersField = getField(URLConnection.class, Hashtable.class, false);
		if (handlersField != null) {
			@SuppressWarnings("rawtypes")
			Hashtable<?, ?> handlers = (Hashtable) handlersField.get(null);
			if (handlers != null)
				handlers.clear();
		}
	}

	public void uninstallHandlerFactories() {
		uninstallURLStreamHandlerFactory();
		uninstallContentHandlerFactory();
	}

	private void uninstallURLStreamHandlerFactory() {
		if (urlStreamHandlerFactory == null) {
			return; // didn't succeed in setting the factory at launch
		}
		try {
			Field factoryField = getField(URL.class, URLStreamHandlerFactory.class, false);
			if (factoryField == null)
				return; // oh well, we tried
			Object lock = getURLStreamHandlerFactoryLock();
			synchronized (lock) {
				URLStreamHandlerFactory factory = (URLStreamHandlerFactory) factoryField.get(null);
				if (factory == urlStreamHandlerFactory) {
					factory = (URLStreamHandlerFactory) urlStreamHandlerFactory.designateSuccessor();
				} else {
					Method unregister = factory.getClass().getMethod("unregister", new Class[] {Object.class}); //$NON-NLS-1$
					unregister.invoke(factory, new Object[] {urlStreamHandlerFactory});
				}
				factoryField.set(null, null);
				// always attempt to clear the handlers cache
				// This allows an optimization for the single framework use-case
				// Note that the call to setURLStreamHandlerFactory below may clear this cache
				// but we want to be sure to clear it here just in case the parent is null.
				// In this case the call below would not occur.
				resetURLStreamHandlers();
				if (factory != null)
					URL.setURLStreamHandlerFactory(factory);
			}
		} catch (Throwable e) {
			// ignore and continue closing the framework
		}
	}

	private void uninstallContentHandlerFactory() {
		if (contentHandlerFactory == null) {
			return; // didn't succeed in setting the factory at launch
		}
		try {
			Field factoryField = getField(URLConnection.class, java.net.ContentHandlerFactory.class, false);
			if (factoryField == null)
				return; // oh well, we tried.
			synchronized (URLConnection.class) {
				java.net.ContentHandlerFactory factory = (java.net.ContentHandlerFactory) factoryField.get(null);

				if (factory == contentHandlerFactory) {
					factory = (java.net.ContentHandlerFactory) contentHandlerFactory.designateSuccessor();
				} else {
					Method unregister = factory.getClass().getMethod("unregister", new Class[] {Object.class}); //$NON-NLS-1$
					unregister.invoke(factory, new Object[] {contentHandlerFactory});
				}
				// null out the field so that we can successfully call setContentHandlerFactory									
				factoryField.set(null, null);
				// always attempt to clear the handlers cache
				// This allows an optomization for the single framework use-case
				// Note that the call to setContentHandlerFactory below may clear this cache
				// but we want to be sure to clear it here just incase the parent is null.
				// In this case the call below would not occur.
				// Also it appears most java libraries actually do not clear the cache
				// when setContentHandlerFactory is called, go figure!!
				resetContentHandlers();
				if (factory != null)
					URLConnection.setContentHandlerFactory(factory);
			}
		} catch (Throwable e) {
			// ignore and continue closing the framework
		}
	}

	public static Field getField(Class<?> clazz, Class<?> type, boolean instance) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			boolean isStatic = Modifier.isStatic(field.getModifiers());
			if (instance != isStatic && field.getType().equals(type)) {
				MultiplexingFactory.setAccessible(field);
				return field;
			}
		}
		return null;
	}
}
