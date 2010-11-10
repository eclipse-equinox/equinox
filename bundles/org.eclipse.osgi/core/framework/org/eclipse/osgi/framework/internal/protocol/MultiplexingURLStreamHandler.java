/*******************************************************************************
 * Copyright (c) 2006, 2010 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.protocol;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.*;
import org.eclipse.osgi.framework.internal.core.Framework;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

public class MultiplexingURLStreamHandler extends URLStreamHandler {
	private static Method openConnectionMethod;
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
	private StreamHandlerFactory factory;

	private static synchronized void initializeMethods(StreamHandlerFactory factory) {
		if (methodsInitialized)
			return;
		try {
			openConnectionMethod = URLStreamHandler.class.getDeclaredMethod("openConnection", new Class[] {URL.class}); //$NON-NLS-1$
			openConnectionMethod.setAccessible(true);

			equalsMethod = URLStreamHandler.class.getDeclaredMethod("equals", new Class[] {URL.class, URL.class}); //$NON-NLS-1$
			equalsMethod.setAccessible(true);

			getDefaultPortMethod = URLStreamHandler.class.getDeclaredMethod("getDefaultPort", (Class[]) null); //$NON-NLS-1$
			getDefaultPortMethod.setAccessible(true);

			getHostAddressMethod = URLStreamHandler.class.getDeclaredMethod("getHostAddress", new Class[] {URL.class}); //$NON-NLS-1$
			getHostAddressMethod.setAccessible(true);

			hashCodeMethod = URLStreamHandler.class.getDeclaredMethod("hashCode", new Class[] {URL.class}); //$NON-NLS-1$
			hashCodeMethod.setAccessible(true);

			hostsEqualMethod = URLStreamHandler.class.getDeclaredMethod("hostsEqual", new Class[] {URL.class, URL.class}); //$NON-NLS-1$
			hostsEqualMethod.setAccessible(true);

			parseURLMethod = URLStreamHandler.class.getDeclaredMethod("parseURL", new Class[] {URL.class, String.class, Integer.TYPE, Integer.TYPE}); //$NON-NLS-1$
			parseURLMethod.setAccessible(true);

			sameFileMethod = URLStreamHandler.class.getDeclaredMethod("sameFile", new Class[] {URL.class, URL.class}); //$NON-NLS-1$
			sameFileMethod.setAccessible(true);

			setURLMethod = URLStreamHandler.class.getDeclaredMethod("setURL", new Class[] {URL.class, String.class, String.class, Integer.TYPE, String.class, String.class, String.class, String.class, String.class}); //$NON-NLS-1$
			setURLMethod.setAccessible(true);

			toExternalFormMethod = URLStreamHandler.class.getDeclaredMethod("toExternalForm", new Class[] {URL.class}); //$NON-NLS-1$
			toExternalFormMethod.setAccessible(true);

			try {
				handlerField = URL.class.getDeclaredField("handler"); //$NON-NLS-1$
			} catch (NoSuchFieldException e) {
				handlerField = Framework.getField(URL.class, URLStreamHandler.class, true);
				if (handlerField == null)
					throw e;
			}
			handlerField.setAccessible(true);
		} catch (Exception e) {
			factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "initializeMethods", 0, e, null)); //$NON-NLS-1$
			throw new RuntimeException(e.getMessage(), e);
		}
		methodsInitialized = true;
	}

	public MultiplexingURLStreamHandler(String protocol, StreamHandlerFactory factory) {
		this.protocol = protocol;
		this.factory = factory;
		initializeMethods(factory);
	}

	protected URLConnection openConnection(URL url) throws IOException {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return (URLConnection) openConnectionMethod.invoke(handler, new Object[] {url});
			} catch (InvocationTargetException e) {
				if (e.getTargetException() instanceof IOException)
					throw (IOException) e.getTargetException();
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "openConnection", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new MalformedURLException();
	}

	protected boolean equals(URL url1, URL url2) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Boolean) equalsMethod.invoke(handler, new Object[] {url1, url2})).booleanValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "equals", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	protected int getDefaultPort() {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Integer) getDefaultPortMethod.invoke(handler, (Object[]) null)).intValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "getDefaultPort", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	protected InetAddress getHostAddress(URL url) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return (InetAddress) getHostAddressMethod.invoke(handler, new Object[] {url});
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "hashCode", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	protected int hashCode(URL url) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Integer) hashCodeMethod.invoke(handler, new Object[] {url})).intValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "hashCode", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	protected boolean hostsEqual(URL url1, URL url2) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Boolean) hostsEqualMethod.invoke(handler, new Object[] {url1, url2})).booleanValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "hostsEqual", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	protected void parseURL(URL arg0, String arg1, int arg2, int arg3) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				// set the real handler for the URL
				handlerField.set(arg0, handler);
				parseURLMethod.invoke(handler, new Object[] {arg0, arg1, new Integer(arg2), new Integer(arg3)});
				return;
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "parseURL", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	protected boolean sameFile(URL url1, URL url2) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return ((Boolean) sameFileMethod.invoke(handler, new Object[] {url1, url2})).booleanValue();
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "sameFile", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	protected void setURL(URL arg0, String arg1, String arg2, int arg3, String arg4, String arg5, String arg6, String arg7, String arg8) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				// set the real handler for the URL
				handlerField.set(arg0, handler);
				setURLMethod.invoke(handler, new Object[] {arg0, arg1, arg2, new Integer(arg3), arg4, arg5, arg6, arg7, arg8});
				return;
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "setURL", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

	protected String toExternalForm(URL url) {
		URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
		if (handler != null) {
			try {
				return (String) toExternalFormMethod.invoke(handler, new Object[] {url});
			} catch (InvocationTargetException e) {
				throw (RuntimeException) e.getTargetException();
			} catch (Exception e) {
				factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), FrameworkLogEntry.ERROR, 0, "toExternalForm", 0, e, null)); //$NON-NLS-1$
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException();
	}

}
