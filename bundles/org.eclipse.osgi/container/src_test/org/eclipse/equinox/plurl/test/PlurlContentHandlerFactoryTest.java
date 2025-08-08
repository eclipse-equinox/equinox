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

import static org.eclipse.equinox.plurl.test.PlurlStreamHandlerFactoryTest.checkProtocol;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.canReflect;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.createTestContentHandlerFactory;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.createTestURLStreamHandlerFactory;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.equinox.plurl.test.PlurlStreamHandlerFactoryTest.TesterClass;
import org.eclipse.equinox.plurl.test.PlurlStreamHandlerFactoryTest.TesterClass1;
import org.eclipse.equinox.plurl.test.PlurlStreamHandlerFactoryTest.TesterClass2;
import org.eclipse.equinox.plurl.test.PlurlTestHandlers.TestContentHandlerFactory;
import org.eclipse.equinox.plurl.test.PlurlTestHandlers.TestFactoryType;
import org.eclipse.equinox.plurl.test.PlurlTestHandlers.TestURLStreamHandlerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("nls")
public class PlurlContentHandlerFactoryTest
{
	private static PlurlTestHandlers plurlTestHandlers;

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
	public void testBuiltinImageProducer() throws IOException {
		URL testImage = getClass().getResource("debug.gif");
		Object content = testImage.getContent();

		// Doing String check for interface to avoid requiring read access to
		// ImageProducer class
		// for testing
		assertTrue("Wrong content type: " + content.getClass(),
				checkInterface(content, "java.awt.image.ImageProducer"));
	}

	private boolean checkInterface(Object content, String iName) {
		Class<?> clazz = content.getClass();
		while (clazz != null) {
			for (Class<?> i : clazz.getInterfaces()) {
				if (iName.equals(i.getName())) {
					return true;
				}
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	@Test
	public void testAddRemovePlurlFactory() throws IOException {
		doAddRemoveFactory(TestFactoryType.PLURL_FACTORY);
	}

	@Test
	public void testAddRemoveNotPlurlFactory() throws IOException {
		doAddRemoveFactory(TestFactoryType.NOT_PLURL_FACTORY);
	}

	@Test
	public void testAddRemoveLegacyFactory() throws IOException {
		doAddRemoveFactory(TestFactoryType.LEGACY_FACTORY);
	}

	@Test
	public void testAddRemoveProxyFactory() throws IOException {
		doAddRemoveFactory(TestFactoryType.PLURL_PROXY_FACTORY);
	}

	private void doAddRemoveFactory(TestFactoryType type) throws IOException {
		assumeThat(canReflect(type), is(String.valueOf(true)));

		final String root = "root";
		final String P1 = "p1";
		final String P2 = "p2";

		// install the handler factory that uses the content mime types to test
		TestURLStreamHandlerFactory testPlurlFactory = createTestURLStreamHandlerFactory(type,
				Arrays.asList(root, P1, P2));
		testPlurlFactory.shouldHandle.set(true);
		plurlTestHandlers.add(type, testPlurlFactory);
		checkProtocol(testPlurlFactory.TYPES, true);

		// install the root content factory
		TestContentHandlerFactory rootContent = createTestContentHandlerFactory(type, root);
		rootContent.shouldHandle.set(false);
		plurlTestHandlers.add(type, rootContent);
		checkContent(rootContent.TYPES, true);

		TestContentHandlerFactory testContent1 = createTestContentHandlerFactory(type, P1);
		plurlTestHandlers.add(type, testContent1);

		TestContentHandlerFactory testContent2 = createTestContentHandlerFactory(type, P2);
		plurlTestHandlers.add(type, testContent2);

		testContent1.shouldHandle.set(false);
		testContent2.shouldHandle.set(false);
		checkContent(testContent1.TYPES, false);
		checkContent(testContent2.TYPES, false);

		testContent1.shouldHandle.set(true);
		testContent2.shouldHandle.set(false);
		checkContent(testContent1.TYPES, true);
		checkContent(testContent2.TYPES, false);

		testContent1.shouldHandle.set(false);
		testContent2.shouldHandle.set(true);
		checkContent(testContent1.TYPES, false);
		checkContent(testContent2.TYPES, true);

		// enabled both handlers but then remove them
		testContent1.shouldHandle.set(true);
		testContent2.shouldHandle.set(true);

		plurlTestHandlers.remove(type, testContent1);
		plurlTestHandlers.remove(type, testContent2);

		checkContent(testContent1.TYPES, false);
		checkContent(testContent2.TYPES, false);
		// root content should still work
		checkContent(rootContent.TYPES, true);
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

		TestURLStreamHandlerFactory rootPlurlFactory = createTestURLStreamHandlerFactory(type,
				"gctest");
		rootPlurlFactory.shouldHandle.set(false);
		plurlTestHandlers.add(type, rootPlurlFactory);
		checkProtocol(rootPlurlFactory.TYPES, true);

		TestContentHandlerFactory testGCFactory = createTestContentHandlerFactory(type, rootPlurlFactory.TYPES);
		testGCFactory.shouldHandle.set(true);
		plurlTestHandlers.add(type, testGCFactory);
		checkContent(testGCFactory.TYPES, true);

		// null out factory and hopefully GC it
		plurlTestHandlers.unpin(testGCFactory);
		testGCFactory = null;

		System.gc();
		System.gc();
		System.gc();

		// rootfactory protocol should no longer work
		checkContent(rootPlurlFactory.TYPES, false);
	}

	@Test
	public void testContentPlurlFactory() throws IOException {
		doTestContentContext(TestFactoryType.PLURL_FACTORY, plurlTestHandlers, null);
	}

	@Test
	public void testContentNotPlurlFactory() throws IOException {
		doTestContentContext(TestFactoryType.NOT_PLURL_FACTORY, plurlTestHandlers, null);
	}

	@Test
	public void testContentLegacyFactory() throws IOException {
		doTestContentContext(TestFactoryType.LEGACY_FACTORY, plurlTestHandlers, null);
	}

	@Test
	public void testContentProxyFactory() throws IOException {
		doTestContentContext(TestFactoryType.PLURL_PROXY_FACTORY, plurlTestHandlers, null);
	}

	static void doTestContentContext(TestFactoryType type, PlurlTestHandlers handlersToUse,
			PlurlTestHandlers handlersToUninstall) throws IOException {
		assumeThat(canReflect(type), is(String.valueOf(true)));
		final String TEST_PROTOCOL1 = "testprotocol1";
		final String TEST_PROTOCOL2 = "testprotocol2";
		final String TEST_PROTOCOL3 = "testprotocol3";

		// install the handler factory that uses the content mime types to test
		PlurlTestHandlers.TestURLStreamHandlerFactory urlStreamHandlerFactory = createTestURLStreamHandlerFactory(type,
				Arrays.asList(TEST_PROTOCOL1, TEST_PROTOCOL2, TEST_PROTOCOL3));
		handlersToUse.add(type, urlStreamHandlerFactory);

		TestContentHandlerFactory testContent1 = createTestContentHandlerFactory(type,
				Arrays.asList(TEST_PROTOCOL1, TEST_PROTOCOL2), TesterClass1.class);
		handlersToUse.add(type, testContent1);

		TestContentHandlerFactory testContent2 = createTestContentHandlerFactory(type,
				Arrays.asList(TEST_PROTOCOL2, TEST_PROTOCOL3), TesterClass2.class);
		handlersToUse.add(type, testContent2);

		if (handlersToUninstall != null) {
			handlersToUninstall.uninstall(false);
		}

		TesterClass t1 = new TesterClass1();
		TesterClass t2 = new TesterClass2();

		checkContent(t1, testContent1.TYPES, true);
		checkContent(t1, Collections.singletonList(TEST_PROTOCOL3), false);
		checkContent(t2, testContent2.TYPES, true);
		checkContent(t2, Collections.singletonList(TEST_PROTOCOL1), false);
	}

	static void checkContent(List<String> mimetypes, boolean expectSuccess) throws IOException {
		for (String mimetype : mimetypes) {
			checkURLContent(mimetype + "://test/it", expectSuccess);
		}
	}

	static void checkContent(TesterClass testerClass, List<String> mimetypes, boolean expectSuccess)
			throws IOException {
		for (String mimetype : mimetypes) {
			checkURLContent(testerClass, mimetype + "://test/it", expectSuccess);
		}
	}

	private static void checkURLContent(String spec, boolean expectSuccess) throws IOException {
		checkURLContent(new URL(spec), expectSuccess);
	}

	private static void checkURLContent(TesterClass testerClass, String spec, boolean expectSuccess)
			throws IOException {
		checkURLContent(testerClass, testerClass.createURL(spec), expectSuccess);
	}

	private static void checkURLContent(URL u, boolean expectSuccess) throws IOException {
		if (expectSuccess) {
			assertEquals("Wrong content value", u.getProtocol(), u.getContent());
		} else {
			assertNotEquals("Wrong content value", u.getProtocol(), u.getContent());
		}
	}

	private static void checkURLContent(TesterClass testerClass, URL u, boolean expectSuccess) throws IOException {
		if (expectSuccess) {
			assertEquals("Wrong content value", u.getProtocol(), testerClass.getContent(u));
		} else {
			assertNotEquals("Wrong content value", u.getProtocol(), testerClass.getContent(u));
		}
	}
}

