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

import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.canReflect;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.createTestURLStreamHandlerFactory;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.equinox.plurl.test.PlurlTestHandlers.TestFactoryType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("nls")
public class PlurlStreamHandlerFactoryTest
{
	protected static PlurlTestHandlers plurlTestHandlers;

	@Before
	public synchronized void installPlurl() {
		if (plurlTestHandlers == null) {
			plurlTestHandlers = new PlurlTestHandlers();
		}
	}
	@After
	public void cleanupHandlers() {
		plurlTestHandlers.cleanupHandlers();
	}

	@AfterClass
	public static void uninstallPlurl() {
		plurlTestHandlers.uninstall(true);
		plurlTestHandlers = null;
	}

	@Test
	public void testAddRemovePlurlFactory() throws IOException {
		doAddRemoveFactory(TestFactoryType.PLURL_FACTORY, plurlTestHandlers, null);
	}

	@Test
	public void testAddRemoveNotPlurlFactory() throws IOException {
		doAddRemoveFactory(TestFactoryType.NOT_PLURL_FACTORY, plurlTestHandlers, null);
	}

	@Test
	public void testAddRemoveLegacyFactory() throws IOException {
		doAddRemoveFactory(TestFactoryType.LEGACY_FACTORY, plurlTestHandlers, null);
	}

	@Test
	public void testAddRemoveProxyFactory() throws IOException {
		doAddRemoveFactory(TestFactoryType.PLURL_PROXY_FACTORY, plurlTestHandlers, null);
	}

	static void doAddRemoveFactory(TestFactoryType type, PlurlTestHandlers handlersToUse,
			PlurlTestHandlers handlersToUninstall) throws IOException {
		assumeThat(canReflect(type), is(String.valueOf(true)));

		final String ROOT_FACTORY = "rootfactory";
		final String TEST_PROTOCOL1 = "testprotocol1";
		final String TEST_PROTOCOL2 = "testprotocol2";

		// install the root handler factory
		PlurlTestHandlers.TestURLStreamHandlerFactory rootPlurlFactory = createTestURLStreamHandlerFactory(type, ROOT_FACTORY);
		rootPlurlFactory.shouldHandle.set(false);
		handlersToUse.add(type, rootPlurlFactory);
		checkProtocol(rootPlurlFactory.TYPES, true);

		PlurlTestHandlers.TestURLStreamHandlerFactory testProtocol1 = createTestURLStreamHandlerFactory(type, TEST_PROTOCOL1);
		handlersToUse.add(type, testProtocol1);

		PlurlTestHandlers.TestURLStreamHandlerFactory testProtocol2 = createTestURLStreamHandlerFactory(type, TEST_PROTOCOL2);
		handlersToUse.add(type, testProtocol2);

		PlurlTestHandlers.TestURLStreamHandlerFactory catchAll = new PlurlTestHandlers.CatchAllPlurlFactory(
				ROOT_FACTORY,
				TEST_PROTOCOL1, TEST_PROTOCOL2);
		handlersToUse.add(type, catchAll);

		if (handlersToUninstall != null) {
			handlersToUninstall.uninstall(false);
		}

		testProtocol1.shouldHandle.set(false);
		testProtocol2.shouldHandle.set(false);
		checkProtocol(testProtocol1.TYPES, false);
		checkProtocol(testProtocol2.TYPES, false);

		testProtocol1.shouldHandle.set(true);
		testProtocol2.shouldHandle.set(false);
		checkProtocol(testProtocol1.TYPES, true);
		checkProtocol(testProtocol2.TYPES, false);

		testProtocol1.shouldHandle.set(false);
		testProtocol2.shouldHandle.set(true);
		checkProtocol(testProtocol1.TYPES, false);
		checkProtocol(testProtocol2.TYPES, true);

		// enabled both handlers but then remove them
		testProtocol1.shouldHandle.set(true);
		testProtocol2.shouldHandle.set(true);

		handlersToUse.remove(type, testProtocol1);
		handlersToUse.remove(type, testProtocol2);
		handlersToUse.remove(type, catchAll);

		checkProtocol(testProtocol1.TYPES, false);
		checkProtocol(testProtocol2.TYPES, false);
		// root should still work
		checkProtocol(rootPlurlFactory.TYPES, true);
	}

	@Test
	public void testURLContextPlurlFactory() throws IOException {
		doTestURLContext(TestFactoryType.PLURL_FACTORY, plurlTestHandlers, null);
	}

	@Test
	public void testURLContextNotPlurlFactory() throws IOException {
		doTestURLContext(TestFactoryType.NOT_PLURL_FACTORY, plurlTestHandlers, null);
	}

	@Test
	public void testURLContextLegacyFactory() throws IOException {
		doTestURLContext(TestFactoryType.LEGACY_FACTORY, plurlTestHandlers, null);
	}

	@Test
	public void testURLContextProxyFactory() throws IOException {
		doTestURLContext(TestFactoryType.PLURL_PROXY_FACTORY, plurlTestHandlers, null);
	}

	static void doTestURLContext(TestFactoryType type, PlurlTestHandlers handlersToUse,
			PlurlTestHandlers handlersToUninstall) throws IOException {
		assumeThat(canReflect(type), is(String.valueOf(true)));
		final String TEST_PROTOCOL1 = "testprotocol1";
		final String TEST_PROTOCOL2 = "testprotocol2";
		final String TEST_PROTOCOL3 = "testprotocol3";

		PlurlTestHandlers.TestURLStreamHandlerFactory testFactory1 = createTestURLStreamHandlerFactory(type,
				Arrays.asList(TEST_PROTOCOL1, TEST_PROTOCOL2), TesterClass1.class);
		handlersToUse.add(type, testFactory1);

		PlurlTestHandlers.TestURLStreamHandlerFactory testFactory2 = createTestURLStreamHandlerFactory(type,
				Arrays.asList(TEST_PROTOCOL2, TEST_PROTOCOL3), TesterClass2.class);
		handlersToUse.add(type, testFactory2);

		PlurlTestHandlers.TestURLStreamHandlerFactory catchAll = new PlurlTestHandlers.CatchAllPlurlFactory(
				TEST_PROTOCOL1,
				TEST_PROTOCOL2, TEST_PROTOCOL3);
		handlersToUse.add(type, catchAll);

		if (handlersToUninstall != null) {
			handlersToUninstall.uninstall(false);
		}

		TesterClass t1 = new TesterClass1();
		TesterClass t2 = new TesterClass2();

		checkProtocol(t1, testFactory1.TYPES, true);
		checkProtocol(t1, Collections.singletonList(TEST_PROTOCOL3), false);
		checkProtocol(t2, testFactory2.TYPES, true);
		checkProtocol(t2, Collections.singletonList(TEST_PROTOCOL1), false);

		checkProtocolContext(t1, t2, testFactory1.TYPES, testFactory2.TYPES);
	}

	static void checkProtocolContext(TesterClass t1, TesterClass t2, List<String> p1, List<String> p2)
			throws MalformedURLException {
		swapContext(t1, t2, p1);
		swapContext(t2, t1, p2);
	}

	static void swapContext(TesterClass t1, TesterClass t2, List<String> protocols) throws MalformedURLException {
		for (String p : protocols) {
			URL u = t1.createURL(p + "://test/it");
			t1.createURL(u, "new/path", true);
			// Make sure the URL can be used in another context.
			// Here we assume if the handler toExternalForm works then the other URL methods
			// will work
			t2.toExternalForm(u);

			boolean expectSuccess = false;
			try {
				t2.createURL(p + "://test/it");
				// If the other context can create this protocol then
				// we expect the context URL constructor to work
				expectSuccess = true;
			} catch (MalformedURLException e) {
				// Otherwise we don't because the root proxy
				// will not be able to figure out the original context.
				// But if we can reflect on URL to set the handler then we can
				expectSuccess = PlurlTestHandlers.CAN_REFLECT_SET_URL_HANDLER;
			}
			t2.createURL(u, p, expectSuccess);
		}
	}

	private String canCreate(String clazz) {
		try {
			Class.forName(clazz).getConstructor().newInstance();
			return String.valueOf(true);
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	@Test
	public void testGCPlurlFactory() throws IOException {
		doTestGCURLFactory(TestFactoryType.PLURL_FACTORY);
	}

	@Test
	public void testGCNotPlurlFactory() throws IOException {
		doTestGCURLFactory(TestFactoryType.NOT_PLURL_FACTORY);
	}

	@Test
	public void testGCLegacyFactory() throws IOException {
		doTestGCURLFactory(TestFactoryType.LEGACY_FACTORY);
	}

	@Test
	public void testGCProxyFactory() throws IOException {
		doTestGCURLFactory(TestFactoryType.PLURL_PROXY_FACTORY);
	}

	private void doTestGCURLFactory(TestFactoryType type) throws IOException {
		assumeThat(canReflect(type), is(String.valueOf(true)));

		PlurlTestHandlers.TestURLStreamHandlerFactory rootPlurlFactory = createTestURLStreamHandlerFactory(type, "rootfactory");
		rootPlurlFactory.shouldHandle.set(false);
		plurlTestHandlers.add(type, rootPlurlFactory);
		checkProtocol(rootPlurlFactory.TYPES, true);

		// null out factory and hopefully GC it
		plurlTestHandlers.unpin(rootPlurlFactory);
		rootPlurlFactory = null;

		System.gc();
		System.gc();
		System.gc();

		// rootfactory protocol should no longer work
		checkProtocol(Arrays.asList("rootfactory"), false);
	}

	@Test
	public void testBuiltinURLHandlers() throws IOException {
		assumeThat(canCreate("sun.net.www.protocol.jar.Handler"), is(String.valueOf(true)));
		assumeThat(canReflect(TestFactoryType.NOT_PLURL_FACTORY), is(String.valueOf(true)));

		PlurlTestHandlers.TestURLStreamHandlerFactory catchAll = new PlurlTestHandlers.CatchAllPlurlFactory("jar");
		plurlTestHandlers.add(TestFactoryType.PLURL_FACTORY, catchAll);
		checkURL("jar:file://path/to/some.jar!/some/file.txt", false);
		plurlTestHandlers.remove(TestFactoryType.PLURL_FACTORY, catchAll);
		checkBuiltinProtocol("jar:file://path/to/some.jar!/some/file.txt");
	}

	private void checkBuiltinProtocol(String spec) throws IOException {
		URL test = new URL(spec);
		test.openConnection();
	}

	static void checkProtocol(List<String> protocols, boolean expectSuccess) throws IOException {
		for (String protocol : protocols) {
			checkURL(protocol + "://test/it", expectSuccess);
		}
	}

	static void checkProtocol(TesterClass testerClass, List<String> protocols, boolean expectSuccess)
			throws IOException {
		for (String protocol : protocols) {
			checkURL(testerClass, protocol + "://test/it", expectSuccess);
		}
	}

	private static void checkURL(String spec, boolean expectSuccess) throws IOException {
		try {
			URL url = new URL(spec);
			if (!expectSuccess) {
				fail("Expected to get MalforedURLException: " + spec + " " + url);
			}
			url.openConnection();
		} catch (MalformedURLException e) {
			if (expectSuccess) {
				throw e;
			}
		}
	}

	private static void checkURL(TesterClass testerClass, String spec, boolean expectSuccess) throws IOException {
		try {
			URL url = testerClass.createURL(spec);
			if (!expectSuccess) {
				fail("Expected to get MalforedURLException: " + spec + " " + url);
			}
			url.openConnection();
		} catch (MalformedURLException e) {
			if (expectSuccess) {
				throw e;
			}
		}
	}

	static interface TesterClass {
		URL createURL(String spec) throws MalformedURLException;

		URL createURL(URL u, String path, boolean expectSuccess) throws MalformedURLException;

		String toExternalForm(URL u);

		Object getContent(URL u) throws IOException;
	}

	static class TesterClass1 implements TesterClass {
		@Override
		public URL createURL(String spec) throws MalformedURLException {
			return new URL(spec);
		}

		@Override
		public String toExternalForm(URL u) {
			return u.toExternalForm();
		}

		@Override
		public URL createURL(URL u, String path, boolean expectSuccess) throws MalformedURLException {
			try {
				URL result = new URL(u, path);
				if (!expectSuccess) {
					fail("Expected to get MalforedURLException: " + u + " " + path);
				}
				return result;
			} catch (MalformedURLException e) {
				if (expectSuccess) {
					throw e;
				}
			}
			return null;
		}

		@Override
		public Object getContent(URL u) throws IOException {
			return u.getContent();
		}
	}

	static class TesterClass2 implements TesterClass {
		@Override
		public URL createURL(String spec) throws MalformedURLException {
			return new URL(spec);
		}

		@Override
		public String toExternalForm(URL u) {
			return u.toExternalForm();
		}

		@Override
		public URL createURL(URL u, String path, boolean expectSuccess) throws MalformedURLException {
			try {
				URL result = new URL(u, path);
				if (!expectSuccess) {
					fail("Expected to get MalforedURLException: " + u + " " + path);
				}
				return result;
			} catch (MalformedURLException e) {
				if (expectSuccess) {
					throw e;
				}
			}
			return null;
		}

		@Override
		public Object getContent(URL u) throws IOException {
			return u.getContent();
		}
	}
}

