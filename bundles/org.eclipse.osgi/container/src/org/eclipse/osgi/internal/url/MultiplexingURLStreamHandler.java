/*******************************************************************************
 * Copyright (c) 2006, 2016 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.osgi.internal.url;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.*;
import java.net.Proxy;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

public class MultiplexingURLStreamHandler extends URLStreamHandler {
	private static Method openConnectionMethod;
	private static Method openConnectionProxyMethod;
	private static Method equalsMethod;
	private static Method getDefaultPortMethod;
	private static Method getHostAddressMethod;
	private static Method hashCodeMethod;
	private static Method hostsEqualMethod;
	private static Method parseURLMethod;
	private static Method sameFileMethod;
	private static Method setURLMethod;
	private static Method toExternalFormMethod;
	private static Field handlerField;
	private static boolean methodsInitialized = false;

	private String protocol;
	private URLStreamHandlerFactoryImpl factory;
	private final URLStreamHandler authorized;

	private static synchronized void initializeMethods(URLStreamHandlerFactoryImpl factory) {
		if (methodsInitialized)
			return;
		try {
			openConnectionMethod = URLStreamHandler.class.getDeclaredMethod("openConnection", new Class[] {URL.class}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(openConnectionMethod);

			openConnectionProxyMethod = URLStreamHandler.class.getDeclaredMethod("openConnection", new Class[] {URL.class, Proxy.class}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(openConnectionProxyMethod);

			equalsMethod = URLStreamHandler.class.getDeclaredMethod("equals", new Class[] {URL.class, URL.class}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(equalsMethod);

			getDefaultPortMethod = URLStreamHandler.class.getDeclaredMethod("getDefaultPort", (Class[]) null); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(getDefaultPortMethod);

			getHostAddressMethod = URLStreamHandler.class.getDeclaredMethod("getHostAddress", new Class[] {URL.class}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(getHostAddressMethod);

			hashCodeMethod = URLStreamHandler.class.getDeclaredMethod("hashCode", new Class[] {URL.class}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(hashCodeMethod);

			hostsEqualMethod = URLStreamHandler.class.getDeclaredMethod("hostsEqual", new Class[] {URL.class, URL.class}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(hostsEqualMethod);

			parseURLMethod = URLStreamHandler.class.getDeclaredMethod("parseURL", new Class[] {URL.class, String.class, Integer.TYPE, Integer.TYPE}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(parseURLMethod);

			sameFileMethod = URLStreamHandler.class.getDeclaredMethod("sameFile", new Class[] {URL.class, URL.class}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(sameFileMethod);

			setURLMethod = URLStreamHandler.class.getDeclaredMethod("setURL", new Class[] {URL.class, String.class, String.class, Integer.TYPE, String.class, String.class, String.class, String.class, String.class}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(setURLMethod);

			toExternalFormMethod = URLStreamHandler.class.getDeclaredMethod("toExternalForm", new Class[] {URL.class}); //$NON-NLS-1$
			MultiplexingFactory.setAccessible(toExternalFormMethod);

			try {
				handlerField = URL.class.getDeclaredField("handler"); //$NON-NLS-1$
			} catch (NoSuchFieldException e) {
				handlerField = EquinoxFactoryManager.getField(URL.class, URLStreamHandler.class, true);
				if (handlerField == null)
					throw e;
			}
			MultiplexingFactory.setAccessible(handlerField);
		} catch (Exception e) {
			factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "initializeMethods", e); //$NON-NLS-1$
			throw new RuntimeException(e.getMessage(), e);
		}
		methodsInitialized = true;
	}

	public MultiplexingURLStreamHandler(String protocol, URLStreamHandlerFactoryImpl factory, URLStreamHandler authorized) {
		this.protocol = protocol;
		this.factory = factory;
		this.authorized = authorized;
		initializeMethods(factory);
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return (URLConnection) openConnectionMethod.invoke(handler, new Object[] {url});
			} catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof IOException)
					throw (IOException) e.getTargetException();
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "openConnection", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new MalformedURLException();
	}

	@Override
	protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return (URLConnection) openConnectionProxyMethod.invoke(handler, new Object[] {url, proxy});
			} catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof IOException)
					throw (IOException) e.getTargetException();
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "openConnection", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new MalformedURLException();
	}

	@Override
	protected boolean equals(URL url1, URL url2) {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Boolean) equalsMethod.invoke(handler, new Object[] {url1, url2})).booleanValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "equals", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	@Override
	protected int getDefaultPort() {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Integer) getDefaultPortMethod.invoke(handler, (Object[]) null)).intValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "getDefaultPort", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	@Override
	protected InetAddress getHostAddress(URL url) {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return (InetAddress) getHostAddressMethod.invoke(handler, new Object[] {url});
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "hashCode", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	@Override
	protected int hashCode(URL url) {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Integer) hashCodeMethod.invoke(handler, new Object[] {url})).intValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "hashCode", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	@Override
	protected boolean hostsEqual(URL url1, URL url2) {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Boolean) hostsEqualMethod.invoke(handler, new Object[] {url1, url2})).booleanValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "hostsEqual", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	@Override
	protected void parseURL(URL arg0, String arg1, int arg2, int arg3) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				// set the real handler for the URL
				handlerField.set(arg0, handler);
				parseURLMethod.invoke(handler, new Object[] {arg0, arg1, Integer.valueOf(arg2), Integer.valueOf(arg3)});
				return;
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "parseURL", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	@Override
	protected boolean sameFile(URL url1, URL url2) {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Boolean) sameFileMethod.invoke(handler, new Object[] {url1, url2})).booleanValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "sameFile", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	@Override
	protected void setURL(URL arg0, String arg1, String arg2, int arg3, String arg4, String arg5, String arg6, String arg7, String arg8) {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				// set the real handler for the URL
				handlerField.set(arg0, handler);
				setURLMethod.invoke(handler, new Object[] {arg0, arg1, arg2, Integer.valueOf(arg3), arg4, arg5, arg6, arg7, arg8});
				return;
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "setURL", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	@Override
	protected String toExternalForm(URL url) {
		URLStreamHandler handler = findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return (String) toExternalFormMethod.invoke(handler, new Object[] {url});
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.container.getLogServices().log(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, "toExternalForm", e); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	private URLStreamHandler findAuthorizedURLStreamHandler(String requested) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(requested);
		return handler == null ? authorized : handler;
	}
}
