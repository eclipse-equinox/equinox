/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.equinox.plurl.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.eclipse.equinox.plurl.Plurl;
import org.eclipse.equinox.plurl.PlurlContentHandlerFactory;
import org.eclipse.equinox.plurl.PlurlStreamHandlerBase;
import org.eclipse.equinox.plurl.PlurlStreamHandlerFactory;
import org.eclipse.equinox.plurl.impl.PlurlImpl;

@SuppressWarnings("nls")
public class PlurlTestHandlers {
	public static abstract class TestFactory {
		protected final List<String> TYPES;
		protected final AtomicBoolean shouldHandle = new AtomicBoolean(false);
		protected final Set<Class<?>> shouldHandleClasses = new HashSet<>();

		public TestFactory(List<String> protocols, Class<?>... shouldHandleClasses) {
			this.TYPES = protocols;
			this.shouldHandleClasses.addAll(Arrays.asList(shouldHandleClasses));
		}

		protected boolean shouldHandleImpl(Class<?> clazz) {
			if (shouldHandle.get()) {
				return true;
			}
			return shouldHandleClasses.contains(clazz);
		}

		protected boolean supports(String type) {
			return TYPES.contains(type);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + '@' + System.identityHashCode(this) + '[' + TYPES + ','
					+ shouldHandle + ','
					+ shouldHandleClasses.stream().map(Class::getSimpleName).collect(Collectors.toList()) + ']';
		}
	}

	public static abstract class TestContentHandlerFactory extends TestFactory implements ContentHandlerFactory {
		public TestContentHandlerFactory(List<String> protocols, Class<?>... shouldHandleClasses) {
			super(protocols, shouldHandleClasses);
		}

		@Override
		public ContentHandler createContentHandler(String mimetype) {
			if (supports(mimetype)) {
				return createContentHandlerImpl(mimetype);
			}
			return null;
		}

		protected abstract ContentHandler createContentHandlerImpl(String mimetype);
	}

	static class TestNotPlurlContentHandlerFactory extends TestContentHandlerFactory implements ContentHandlerFactory {

		public TestNotPlurlContentHandlerFactory(List<String> protocols, Class<?>... shouldHandleClasses) {
			super(protocols, shouldHandleClasses);
		}

		@Override
		protected ContentHandler createContentHandlerImpl(String mimetype) {
			return new TestContentHandler(mimetype);
		}

		public boolean hasAuthority(Class<?> clazz) {
			return shouldHandleImpl(clazz);
		}
	}

	public static class TestPlurlContentHandlerFactory extends TestContentHandlerFactory
			implements PlurlContentHandlerFactory {

		public TestPlurlContentHandlerFactory(List<String> mimetypes, Class<?>... shouldHandleClasses) {
			super(mimetypes, shouldHandleClasses);
		}

		@Override
		protected ContentHandler createContentHandlerImpl(String mimetype) {
			return new TestContentHandler(mimetype);
		}

		@Override
		public boolean shouldHandle(Class<?> clazz) {
			return shouldHandleImpl(clazz);
		}
	}

	public static class TestContentHandler extends ContentHandler {
		private final String mimetype;

		public TestContentHandler(String mimetype) {
			this.mimetype = mimetype;
		}
		@Override
		public Object getContent(URLConnection u) throws IOException {
			// for testing just return the mimetype
			return mimetype;
		}
	}

	public static abstract class TestURLStreamHandlerFactory extends TestFactory implements URLStreamHandlerFactory {
		public TestURLStreamHandlerFactory(List<String> protocols, Class<?>... shouldHandleClasses) {
			super(protocols, shouldHandleClasses);
		}

		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			if (supports(protocol)) {
				return createURLStreamHandlerImpl(protocol);
			}
			return null;
		}

		protected abstract URLStreamHandler createURLStreamHandlerImpl(String protocol);

	}


	static class TestNotPlurlStreamHandlerFactory extends TestURLStreamHandlerFactory implements URLStreamHandlerFactory {
		public TestNotPlurlStreamHandlerFactory(List<String> protocols, Class<?>... shouldHandleClasses) {
			super(protocols, shouldHandleClasses);
		}
	
		protected URLStreamHandler createURLStreamHandlerImpl(String protocol) {
			return new TestNotPlurlStreamHandler();
		}

		public boolean hasAuthority(Class<?> clazz) {
			return shouldHandleImpl(clazz);
		}
	}


	public static class TestPlurlStreamHandlerFactory extends TestURLStreamHandlerFactory implements PlurlStreamHandlerFactory {
		public TestPlurlStreamHandlerFactory(List<String> protocols, Class<?>... shouldHandleClasses) {
			super(protocols, shouldHandleClasses);
		}
		@Override
		protected URLStreamHandler createURLStreamHandlerImpl(String protocol) {
			return new TestPlurlStreamHandler(false);
		}

		@Override
		public boolean shouldHandle(Class<?> clazz) {
			return shouldHandleImpl(clazz);
		}
	}

	static class CatchAllPlurlFactory extends TestURLStreamHandlerFactory implements PlurlStreamHandlerFactory {
		CatchAllPlurlFactory(String... protos) {
			super(Arrays.asList(protos));
			shouldHandle.set(true);
		}
	
		@Override
		protected URLStreamHandler createURLStreamHandlerImpl(String protocol) {
			return new TestPlurlStreamHandler(true);
		}

		@Override
		public boolean shouldHandle(Class<?> clazz) {
			return shouldHandleImpl(clazz);
		}
	}

	static class TestNotPlurlStreamHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new URLConnection(u) {
				@Override
				public String getContentType() {
					// for testing purposes just use the test protocol name
					return u.getProtocol();
				}

				@Override
				public void connect() throws IOException {
					// do nothing
				}

				@Override
				public InputStream getInputStream() throws IOException {
					// just testing
					return new ByteArrayInputStream(u.getProtocol().getBytes());
				}
			};
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + '@' + System.identityHashCode(this);
		}
	}


	static class TestPlurlStreamHandler extends PlurlStreamHandlerBase {
		private final boolean unsupported;
	
		public TestPlurlStreamHandler() {
			this(false);
		}
	
		public TestPlurlStreamHandler(boolean unsupported) {
			this.unsupported = unsupported;
		}
	
		@Override
		public URLConnection openConnection(URL u) throws IOException {
			if (unsupported) {
				throw new UnsupportedOperationException();
			}
			return new URLConnection(u) {
				@Override
				public String getContentType() {
					// for testing purposes just use the test protocol name
					return u.getProtocol();
				}

				@Override
				public void connect() throws IOException {
					// do nothing
				}

				@Override
				public InputStream getInputStream() throws IOException {
					// just testing
					return new ByteArrayInputStream(u.getProtocol().getBytes());
				}
			};
		}
	
		@Override
		public void parseURL(PlurlSetter setter, URL u, String spec, int start, int limit) {
			if (unsupported) {
				throw new UnsupportedOperationException();
			}
			super.parseURL(setter, u, spec, start, limit);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + '@' + System.identityHashCode(this);
		}
	}

	enum TestFactoryType {
		PLURL_FACTORY, PLURL_PROXY_FACTORY, NOT_PLURL_FACTORY, LEGACY_FACTORY
	}
	static final boolean CAN_REFLECT_SET_URL_HANDLER;
	static final String CAN_REFLECT_ON_URL_STREAM_HANDLER;
	static final String CAN_REFLECT_ON_URL;

	static final ClassLoader PROXY_LOADER;
	static {
		boolean canReflectSetUrlHandler = false;
		Field f = null;
		try {
			f = URL.class.getDeclaredField("handler"); //$NON-NLS-1$
		} catch (Exception e) {
			Field[] fields = URL.class.getDeclaredFields();
			for (Field field : fields) {
				boolean isStatic = Modifier.isStatic(field.getModifiers());
				if (!isStatic && field.getType().equals(URLStreamHandler.class)) {
					f = field;
					break;
				}
			}
		}
		if (f != null) {
			try {
				f.setAccessible(true);
				canReflectSetUrlHandler = true;
			} catch (Exception e) {
				// ignore
			}
		}
		CAN_REFLECT_SET_URL_HANDLER = canReflectSetUrlHandler;
		String canReflectOnURLStreamHandler;
		try {
			URLStreamHandler.class.getDeclaredMethod("getDefaultPort").setAccessible(true);
			canReflectOnURLStreamHandler = String.valueOf(true);
		} catch (Exception e) {
			canReflectOnURLStreamHandler = e.getMessage();
		}
		CAN_REFLECT_ON_URL_STREAM_HANDLER = canReflectOnURLStreamHandler;

		String canReflectOnURL;
		try {
			getStaticField(URL.class, URLStreamHandlerFactory.class, true);
			canReflectOnURL = String.valueOf(true);
		} catch (Exception e) {
			canReflectOnURL = e.getMessage();
		}
		CAN_REFLECT_ON_URL = canReflectOnURL;

		URL apiLocation = Plurl.class.getProtectionDomain().getCodeSource().getLocation();
		URL testLocation = PlurlTestHandlers.class.getProtectionDomain().getCodeSource().getLocation();
		PROXY_LOADER = new URLClassLoader(new URL[] { apiLocation, testLocation },
				PlurlTestHandlers.class.getClassLoader()) {
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				Class<?> loadedClass = findLoadedClass(name);
				if (loadedClass != null) {
					return loadedClass;
				}
				if (name.startsWith("org.eclipse.equinox.plurl.")
						&& !name.equals(TestContentHandlerFactory.class.getName())
						&& !name.equals(TestURLStreamHandlerFactory.class.getName())
						&& !name.equals(TestFactory.class.getName())) {
					try {
						Class<?> childFirst = findClass(name);
						if (resolve) {
							resolveClass(childFirst);
						}
						return childFirst;
					} catch (ClassNotFoundException e) {
						// ignore
					}
				}
				return super.loadClass(name, resolve);
			}
		};
	}

	static TestURLStreamHandlerFactory createTestURLStreamHandlerFactory(TestFactoryType type, String protocol,
			Class<?>... shouldHandleClasses) {
		return createTestURLStreamHandlerFactory(type, Collections.singletonList(protocol), shouldHandleClasses);
	}

	static TestURLStreamHandlerFactory createTestURLStreamHandlerFactory(TestFactoryType type, List<String> protocols,
			Class<?>... shouldHandleClasses) {
		switch (type) {
		case PLURL_FACTORY:
			return new TestPlurlStreamHandlerFactory(protocols, shouldHandleClasses);
		case NOT_PLURL_FACTORY:
		case LEGACY_FACTORY:
			return new TestNotPlurlStreamHandlerFactory(protocols, shouldHandleClasses);
		case PLURL_PROXY_FACTORY:
			return createProxyURLHandlerFactory(protocols, shouldHandleClasses);
		default:
			throw new UnsupportedOperationException("Unknown type: " + type);
		}
	}

	private static TestURLStreamHandlerFactory createProxyURLHandlerFactory(List<String> protocols,
			Class<?>[] shouldHandleClasses) {
		try {
			Class<?> testFactoryClass = PROXY_LOADER.loadClass(TestPlurlStreamHandlerFactory.class.getName());
			return (TestURLStreamHandlerFactory) testFactoryClass.getConstructor(List.class, Class[].class)
					.newInstance(protocols, shouldHandleClasses);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	static TestContentHandlerFactory createTestContentHandlerFactory(TestFactoryType type, String mimetype,
			Class<?>... shouldHandleClasses) {
		return createTestContentHandlerFactory(type, Collections.singletonList(mimetype), shouldHandleClasses);
	}

	static TestContentHandlerFactory createTestContentHandlerFactory(TestFactoryType type, List<String> mimetypes,
			Class<?>... shouldHandleClasses) {
		switch (type) {
		case PLURL_FACTORY:
			return new TestPlurlContentHandlerFactory(mimetypes, shouldHandleClasses);
		case NOT_PLURL_FACTORY:
		case LEGACY_FACTORY:
			return new TestNotPlurlContentHandlerFactory(mimetypes, shouldHandleClasses);
		case PLURL_PROXY_FACTORY:
			return createProxyContentFactory(mimetypes, shouldHandleClasses);
		default:
			throw new UnsupportedOperationException("Unknown type: " + type);
		}
	}

	private static TestContentHandlerFactory createProxyContentFactory(List<String> mimetypes,
			Class<?>[] shouldHandleClasses) {
		try {
			Class<?> testFactoryClass = PROXY_LOADER.loadClass(TestPlurlContentHandlerFactory.class.getName());
			return (TestContentHandlerFactory) testFactoryClass.getConstructor(List.class, Class[].class)
					.newInstance(mimetypes, shouldHandleClasses);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final Object plurl;
	private final Set<URLStreamHandlerFactory> urlHandlers = Collections.newSetFromMap(new WeakHashMap<>());
	private final Set<URLStreamHandlerFactory> legacyUrlHandlers = Collections.newSetFromMap(new WeakHashMap<>());
	private final Set<URLStreamHandlerFactory> pinnedUrlHandlers = new HashSet<>();
	private final Set<ContentHandlerFactory> contentHandlers = Collections.newSetFromMap(new WeakHashMap<>());
	private final Set<ContentHandlerFactory> legacyContentHandlers = Collections.newSetFromMap(new WeakHashMap<>());
	private final Set<ContentHandlerFactory> pinnedContentHandlers = new HashSet<>();

	PlurlTestHandlers() {
		this(false);
	}
	PlurlTestHandlers(boolean useProxyLoader) {
		// TODO should use service loader
		if (useProxyLoader) {
			plurl = createPlurlImplFromProxyLoader(Plurl.PLURL_FORBID_NOTHING);
		} else {
			PlurlImpl impl = new PlurlImpl();
			impl.install(Plurl.PLURL_FORBID_NOTHING);
			plurl = impl;
		}
	}

	private Object createPlurlImplFromProxyLoader(String... forbidden) {
		try {
			Class<?> plurlImplClass = PROXY_LOADER.loadClass(PlurlImpl.class.getName());
			Object impl = plurlImplClass.getConstructor().newInstance();
			impl.getClass().getMethod("install", String[].class).invoke(impl, (Object) forbidden);
			return impl;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void uninstall(boolean nullOutJVMFactories) {
		try {
			plurl.getClass().getMethod("uninstall").invoke(plurl);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (nullOutJVMFactories) {
			try {
				// try to null out factories for next tests
				forceContentHandlerFactory(null);
				forceURLStreamHandlerFactory(null);
			} catch (Exception e) {
				// don't fail on forcing null; may be blocked by JVM
			}
		}
	}

	void cleanupHandlers() {
		urlHandlers.forEach((h) -> {
			try {
				plurlOp(Plurl.PLURL_REMOVE_URL_STREAM_HANDLER_FACTORY, h);
			} catch (IOException e) {
				// ignore
			}
		});
		urlHandlers.clear();
		legacyUrlHandlers.forEach((h) -> {
			try {
				plurlOp(Plurl.PLURL_REMOVE_URL_STREAM_HANDLER_FACTORY, h);
			} catch (IOException e) {
				// ignore
			}
		});
		legacyUrlHandlers.clear();

		contentHandlers.forEach((h) -> {
			try {
				plurlOp(Plurl.PLURL_REMOVE_CONTENT_HANDLER_FACTORY, h);
			} catch (IOException e) {
				// ignore
			}
		});
		contentHandlers.clear();

		legacyContentHandlers.forEach((h) -> {
			try {
				plurlOp(Plurl.PLURL_REMOVE_CONTENT_HANDLER_FACTORY, h);
			} catch (IOException e) {
				// ignore
			}
		});
		legacyContentHandlers.clear();

		pinnedUrlHandlers.clear();
		pinnedContentHandlers.clear();
	}

	void unpin(URLStreamHandlerFactory f) {
		pinnedUrlHandlers.remove(f);
	}

	void unpin(ContentHandlerFactory f) {
		pinnedContentHandlers.remove(f);
	}

	void add(TestFactoryType type, URLStreamHandlerFactory f) throws IOException {
		pinnedUrlHandlers.add(f);
		if (type == TestFactoryType.LEGACY_FACTORY) {
			legacyUrlHandlers.add(f);
			forceLegacyURLStreamHandlerFactory(f);
		} else {
			urlHandlers.add(f);
			if (f instanceof PlurlStreamHandlerFactory) {
				Plurl.add((PlurlStreamHandlerFactory) f);
			} else {
				plurlOp(Plurl.PLURL_ADD_URL_STREAM_HANDLER_FACTORY, f);
			}
		}
	}

	void remove(TestFactoryType type, URLStreamHandlerFactory f) throws IOException {
		if (type == TestFactoryType.LEGACY_FACTORY) {
			legacyUrlHandlers.remove(f);
			unregisterLegacyURLStreamHandlerFactory(f);
		} else {
			urlHandlers.remove(f);
			if (f instanceof PlurlStreamHandlerFactory) {
				Plurl.remove((PlurlStreamHandlerFactory) f);
			} else {
				plurlOp(Plurl.PLURL_REMOVE_URL_STREAM_HANDLER_FACTORY, f);
			}
		}
	}

	void add(TestFactoryType type, ContentHandlerFactory f) throws IOException {
		pinnedContentHandlers.add(f);
		if (type == TestFactoryType.LEGACY_FACTORY) {
			legacyContentHandlers.add(f);
			forceLegacyContentHandlerFactory(f);
		} else {
			contentHandlers.add(f);
			if (f instanceof PlurlContentHandlerFactory) {
				Plurl.add((PlurlContentHandlerFactory) f);
			} else {
				plurlOp(Plurl.PLURL_ADD_CONTENT_HANDLER_FACTORY, f);
			}
		}
	}

	void remove(TestFactoryType type, ContentHandlerFactory f) throws IOException {
		if (type == TestFactoryType.LEGACY_FACTORY) {
			legacyContentHandlers.remove(f);
			unregisterLegacyContentHandlerFactory(f);
		} else {
			contentHandlers.remove(f);
			if (f instanceof PlurlContentHandlerFactory) {
				Plurl.remove((PlurlContentHandlerFactory) f);
			} else {
				plurlOp(Plurl.PLURL_REMOVE_CONTENT_HANDLER_FACTORY, f);
			}
		}
	}

	void plurlOp(String op, URLStreamHandlerFactory factory) throws IOException {
		URL addURLStreamHandler = new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP, op);
		@SuppressWarnings("unchecked")
		Consumer<URLStreamHandlerFactory> addFactory = (Consumer<URLStreamHandlerFactory>) addURLStreamHandler
				.openConnection().getContent();
		addFactory.accept(factory);
	}

	void plurlOp(String op, ContentHandlerFactory factory) throws IOException {
		URL addContentHandler = new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP, op);
		@SuppressWarnings("unchecked")
		Consumer<ContentHandlerFactory> addFactory = (Consumer<ContentHandlerFactory>) addContentHandler
				.openConnection().getContent();
		addFactory.accept(factory);
	}

	private static void forceLegacyURLStreamHandlerFactory(URLStreamHandlerFactory f) throws IOException {
		// this mimics what a legacy framework would have done
		Field factoryField = getField(URL.class, URLStreamHandlerFactory.class, false);
		if (factoryField == null) {
			throw new IOException("Could not find URLStreamHandlerFactory field"); //$NON-NLS-1$
		}
		try {
			URLStreamHandlerFactory factory = (URLStreamHandlerFactory) factoryField.get(null);
			factory.getClass().getMethod("isMultiplexing", (Class[]) null); //$NON-NLS-1$
			Method register = factory.getClass().getMethod("register", new Class[] { Object.class }); //$NON-NLS-1$
			register.invoke(factory, new Object[] { f });
			factoryField.set(null, null);
			URL.setURLStreamHandlerFactory(factory);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private static void forceLegacyContentHandlerFactory(ContentHandlerFactory f) throws IOException {
		Field factoryField = getField(URLConnection.class, ContentHandlerFactory.class, false);
		if (factoryField == null) {
			throw new IOException("Could not find ContentHandlerFactory field"); //$NON-NLS-1$
		}
		// this mimics what a legacy framework would have done
		try {
			ContentHandlerFactory factory = (ContentHandlerFactory) factoryField.get(null);
			factory.getClass().getMethod("isMultiplexing", (Class[]) null); //$NON-NLS-1$
			Method register = factory.getClass().getMethod("register", new Class[] { Object.class }); //$NON-NLS-1$
			register.invoke(factory, new Object[] { f });
			factoryField.set(null, null);
			URLConnection.setContentHandlerFactory(factory);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private static void unregisterLegacyURLStreamHandlerFactory(URLStreamHandlerFactory f) throws IOException {
		// this mimics what a legacy framework would have done
		Field factoryField = getField(URL.class, URLStreamHandlerFactory.class, false);
		if (factoryField == null) {
			throw new IOException("Could not find URLStreamHandlerFactory field"); //$NON-NLS-1$
		}
		try {
			URLStreamHandlerFactory factory = (URLStreamHandlerFactory) factoryField.get(null);
			factory.getClass().getMethod("isMultiplexing", (Class[]) null); //$NON-NLS-1$
			Method unregister = factory.getClass().getMethod("unregister", new Class[] { Object.class }); //$NON-NLS-1$
			unregister.invoke(factory, new Object[] { f });
			factoryField.set(null, null);
			URL.setURLStreamHandlerFactory(factory);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private static void unregisterLegacyContentHandlerFactory(ContentHandlerFactory f) throws IOException {
		// this mimics what a legacy framework would have done
		Field factoryField = getField(URLConnection.class, ContentHandlerFactory.class, false);
		if (factoryField == null) {
			throw new IOException("Could not find URLStreamHandlerFactory field"); //$NON-NLS-1$
		}
		try {
			ContentHandlerFactory factory = (ContentHandlerFactory) factoryField.get(null);
			factory.getClass().getMethod("isMultiplexing", (Class[]) null); //$NON-NLS-1$
			Method unregister = factory.getClass().getMethod("unregister", new Class[] { Object.class }); //$NON-NLS-1$
			unregister.invoke(factory, new Object[] { f });
			factoryField.set(null, null);
			URLConnection.setContentHandlerFactory(factory);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	static Field getField(Class<?> clazz, Class<?> type, boolean instance) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			boolean isStatic = Modifier.isStatic(field.getModifiers());
			if (instance != isStatic && field.getType().equals(type)) {
				field.setAccessible(true);
				return field;
			}
		}
		return null;
	}

	static String canReflect(TestFactoryType type) {
		if (type == TestFactoryType.PLURL_FACTORY || type == TestFactoryType.PLURL_PROXY_FACTORY) {
			return String.valueOf(true);
		}
		return PlurlTestHandlers.CAN_REFLECT_ON_URL_STREAM_HANDLER;
	}

	static void forceURLStreamHandlerFactory(URLStreamHandlerFactory forceFactory) throws Exception {
		try {
			URL.setURLStreamHandlerFactory(forceFactory);
			return;
		} catch (Error e) {
			// try to force set it with reflection
		}
		Field factoryField = getStaticField(URL.class, URLStreamHandlerFactory.class);
		if (factoryField == null) {
			// throw new Exception("Could not find URLStreamHandlerFactory field");
			// //$NON-NLS-1$
			return;
		}
		// look for a lock to synchronize on
		Object lock = getURLStreamHandlerFactoryLock();
		synchronized (lock) {
			factoryField.set(null, null);
			resetURLStreamHandlers();
			URL.setURLStreamHandlerFactory(forceFactory);
		}
	}

	private static Object getURLStreamHandlerFactoryLock() throws IllegalAccessException {
		Object lock;
		try {
			Field streamHandlerLockField = URL.class.getDeclaredField("streamHandlerLock"); //$NON-NLS-1$
			streamHandlerLockField.setAccessible(true);
			lock = streamHandlerLockField.get(null);
		} catch (NoSuchFieldException noField) {
			// could not find the lock, lets sync on the class object
			lock = URL.class;
		}
		return lock;
	}

	private static void resetURLStreamHandlers() throws IllegalAccessException {
		Field handlersField = getStaticField(URL.class, Hashtable.class);
		if (handlersField != null) {
			@SuppressWarnings("rawtypes")
			Hashtable<?, ?> handlers = (Hashtable) handlersField.get(null);
			if (handlers != null) {
				handlers.clear();
			}
		}
	}

	static void forceContentHandlerFactory(ContentHandlerFactory forceFactory) throws Exception {
		try {
			URLConnection.setContentHandlerFactory(forceFactory);
			return;
		} catch (Error e) {
			// try to force set it with reflection
		}
		Field factoryField = getStaticField(URLConnection.class, java.net.ContentHandlerFactory.class);
		if (factoryField == null) {
			// throw new Exception("Could not find ContentHandlerFactory field");
			// //$NON-NLS-1$
			return;
		}

		// null out the field so that we can successfully call setContentHandlerFactory
		factoryField.set(null, null);
		// always attempt to clear the handlers cache
		// This allows an optimization for the single framework use-case
		resetContentHandlers();
		URLConnection.setContentHandlerFactory(forceFactory);
	}

	private static void resetContentHandlers() throws IllegalAccessException {
		Field handlersField = getStaticField(URLConnection.class, Hashtable.class);
		if (handlersField != null) {
			@SuppressWarnings("rawtypes")
			Hashtable<?, ?> handlers = (Hashtable) handlersField.get(null);
			if (handlers != null) {
				handlers.clear();
			}
		}
	}

	private static Field getStaticField(Class<?> clazz, Class<?> type) {
		return getStaticField(clazz, type, false);
	}

	private static Field getStaticField(Class<?> clazz, Class<?> type, boolean throwError) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			boolean isStatic = Modifier.isStatic(field.getModifiers());
			if (isStatic && field.getType().equals(type)) {
				try {
					field.setAccessible(true);
				} catch (RuntimeException e) {
					if (throwError) {
						throw e;
					}
					return null;
				}
				return field;
			}
		}
		return null;
	}
}
