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

import static org.eclipse.equinox.plurl.test.PlurlContentHandlerFactoryTest.doTestContentContext;
import static org.eclipse.equinox.plurl.test.PlurlStreamHandlerFactoryTest.doAddRemoveFactory;
import static org.eclipse.equinox.plurl.test.PlurlStreamHandlerFactoryTest.doTestURLContext;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.createTestURLStreamHandlerFactory;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.forceContentHandlerFactory;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.forceURLStreamHandlerFactory;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.TestFactoryType.PLURL_FACTORY;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.equinox.plurl.Plurl;
import org.eclipse.equinox.plurl.test.PlurlTestHandlers.TestFactoryType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("nls")
public class MultiplePlurlInstallWithParentsTests {
	static final List<PlurlTestHandlers> listOfPlurlTestHandlers = new ArrayList<>();

	@BeforeClass
	public static void setupMultiplePlurls() throws Exception {
		if (!Boolean.valueOf(PlurlTestHandlers.CAN_REFLECT_ON_URL)) {
			return;
		}
		forceContentHandlerFactory(
				new PlurlTestHandlers.TestNotPlurlContentHandlerFactory(Collections.singletonList("forceparent")));
		forceURLStreamHandlerFactory(
				new PlurlTestHandlers.TestNotPlurlStreamHandlerFactory(Collections.singletonList("forceparent")));

		// create multiple Plurl implementations; needs to be at least more than the
		// number of test
		for (int i = 0; i < 50; i++) {
			// every other one we add uses a proxy instance to test install reflection works
			listOfPlurlTestHandlers.add(new PlurlTestHandlers(i % 2 == 0));
		}
	}

	@Before
	public void checkReflection() {
		assumeThat(PlurlTestHandlers.CAN_REFLECT_ON_URL, is(String.valueOf(true)));
	}

	@After
	public void cleanupHandlers() {
		if (!listOfPlurlTestHandlers.isEmpty()) {
			listOfPlurlTestHandlers.get(listOfPlurlTestHandlers.size() - 1).cleanupHandlers();
		}
	}

	@Test
	public void testAddRemovePlurlFactory() throws IOException {
		doTestMultiplePlurlsAddRemoveFactory(TestFactoryType.PLURL_FACTORY);
	}

	@Test
	public void testAddRemoveNotPlurlFactory() throws IOException {
		doTestMultiplePlurlsAddRemoveFactory(TestFactoryType.NOT_PLURL_FACTORY);
	}

	@Test
	public void testAddRemoveLegacyFactory() throws IOException {
		doTestMultiplePlurlsAddRemoveFactory(TestFactoryType.LEGACY_FACTORY);
	}

	@Test
	public void testAddRemoveProxyFactory() throws IOException {
		doTestMultiplePlurlsAddRemoveFactory(TestFactoryType.PLURL_PROXY_FACTORY);
	}

	private static void doTestMultiplePlurlsAddRemoveFactory(TestFactoryType type) throws IOException {
		doAddRemoveFactory(type, //
				listOfPlurlTestHandlers.get(listOfPlurlTestHandlers.size() - 1), //
				listOfPlurlTestHandlers.remove(0));
	}

	@Test
	public void testURLContextPlurlFactory() throws IOException {
		doTestMultiplePlurlsTestURLContext(TestFactoryType.PLURL_FACTORY);
	}

	@Test
	public void testURLContextNotPlurlFactory() throws IOException {
		doTestMultiplePlurlsTestURLContext(TestFactoryType.NOT_PLURL_FACTORY);
	}

	@Test
	public void testURLContextLegacyFactory() throws IOException {
		doTestMultiplePlurlsTestURLContext(TestFactoryType.LEGACY_FACTORY);
	}

	@Test
	public void testURLContextProxyFactory() throws IOException {
		doTestMultiplePlurlsTestURLContext(TestFactoryType.PLURL_PROXY_FACTORY);
	}

	private static void doTestMultiplePlurlsTestURLContext(TestFactoryType type) throws IOException {
		doTestURLContext(type, //
				listOfPlurlTestHandlers.get(listOfPlurlTestHandlers.size() - 1), //
				listOfPlurlTestHandlers.remove(0));
	}

	@Test
	public void testContentPlurlFactory() throws IOException {
		doTestMultiplePlurlsContent(TestFactoryType.PLURL_FACTORY);
	}

	@Test
	public void testContentNotPlurlFactory() throws IOException {
		doTestMultiplePlurlsContent(TestFactoryType.NOT_PLURL_FACTORY);
	}

	@Test
	public void testContentLegacyFactory() throws IOException {
		doTestMultiplePlurlsContent(TestFactoryType.LEGACY_FACTORY);
	}

	@Test
	public void testContentProxyFactory() throws IOException {
		doTestMultiplePlurlsContent(TestFactoryType.PLURL_PROXY_FACTORY);
	}

	private static void doTestMultiplePlurlsContent(TestFactoryType type) throws IOException {
		doTestContentContext(type, //
				listOfPlurlTestHandlers.get(listOfPlurlTestHandlers.size() - 1), //
				listOfPlurlTestHandlers.remove(0));
	}

	@Test
	public void testParentDelegation() throws IOException {
		PlurlTestHandlers plurlTestHandlers = listOfPlurlTestHandlers.get(listOfPlurlTestHandlers.size() - 1);
		// register some plurl protocols
		PlurlTestHandlers.TestURLStreamHandlerFactory testProtocol1 = createTestURLStreamHandlerFactory(PLURL_FACTORY,
				"p1");
		plurlTestHandlers.add(PLURL_FACTORY, testProtocol1);

		PlurlTestHandlers.TestURLStreamHandlerFactory testProtocol2 = createTestURLStreamHandlerFactory(PLURL_FACTORY,
				"p2");
		plurlTestHandlers.add(PLURL_FACTORY, testProtocol2);

		// make sure the original parent factories work
		assertEquals("Wrong content for forceParent test", "forceparent",
				new URL("forceparent://test/it").openConnection().getContent());
	}
	@AfterClass
	public static void resetFactories() throws Exception {
		if (!Boolean.valueOf(PlurlTestHandlers.CAN_REFLECT_ON_URL)) {
			return;
		}

		for (PlurlTestHandlers plurlTestHandlers : listOfPlurlTestHandlers) {
			plurlTestHandlers.uninstall(false);
		}
		// make sure the original parent factories got restored and still work
		assertEquals("Wrong content for forceParent test", "forceparent",
				new URL("forceparent://test/it").openConnection().getContent());

		boolean plurlStillAvailable = false;
		try {
			// make sure the plurl protocol no longer works
			new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP, Plurl.PLURL_ADD_URL_STREAM_HANDLER_FACTORY);
			plurlStillAvailable = true;
		} catch (MalformedURLException e) {
			// expected
		}

		forceContentHandlerFactory(null);
		forceURLStreamHandlerFactory(null);
		if (plurlStillAvailable) {
			Assert.fail("Expected the plurl protocol to no longer be available.");
		}
	}
}
