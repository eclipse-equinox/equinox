/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import static org.eclipse.equinox.plurl.test.PlurlContentHandlerFactoryTest.checkContent;
import static org.eclipse.equinox.plurl.test.PlurlStreamHandlerFactoryTest.checkProtocol;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.createTestContentHandlerFactory;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.createTestURLStreamHandlerFactory;
import static org.eclipse.equinox.plurl.test.PlurlTestHandlers.TestFactoryType.PLURL_FACTORY;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.equinox.plurl.test.PlurlTestHandlers.TestContentHandlerFactory;
import org.eclipse.equinox.plurl.test.PlurlTestHandlers.TestURLStreamHandlerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class PlurlConcurrencyTest {
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

	private static final int CONCURRENCY_TEST_ITERATIONS = 100;
	private static final int CONCURRENT_THREAD_COUNT = 10;
	@Test
	public void testConcurrentGetContentCalls() throws InterruptedException, IOException {
		// Note that if the test fails it only fails the first iteration because of an
		// implementation detail in ServiceLoader.
		for (int i = 0; i < CONCURRENCY_TEST_ITERATIONS; i++) {
			// install the URL handler, unique to this iteration
			TestURLStreamHandlerFactory testPlurlFactory = createTestURLStreamHandlerFactory(PLURL_FACTORY,
					"getcontent" + i); //$NON-NLS-1$
			testPlurlFactory.shouldHandle.set(true);
			plurlTestHandlers.add(PLURL_FACTORY, testPlurlFactory);
			checkProtocol(testPlurlFactory.TYPES, true);

			// install the content factory, unique to this iteration
			TestContentHandlerFactory testContentFactory = createTestContentHandlerFactory(PLURL_FACTORY,
					"getcontent" + i); //$NON-NLS-1$
			testContentFactory.shouldHandle.set(true);
			plurlTestHandlers.add(PLURL_FACTORY, testContentFactory);

			List<Thread> threads = new ArrayList<>();
			List<AtomicReference<Throwable>> errors = new ArrayList<>();

			for (int j = 0; j < CONCURRENT_THREAD_COUNT; j++) {
				AtomicReference<Throwable> error = new AtomicReference<>();
				errors.add(error);

				Thread thread = new Thread(() -> {
					try {
						checkContent(testContentFactory.TYPES, true);
					} catch (Throwable t) {
						error.set(t);
					}
				});
				threads.add(thread);
				thread.start();
			}

			for (Thread thread : threads) {
				thread.join();
			}

			plurlTestHandlers.remove(PLURL_FACTORY, testContentFactory);
			plurlTestHandlers.remove(PLURL_FACTORY, testPlurlFactory);

			for (int j = 0; j < errors.size(); j++) {
				Throwable t = errors.get(j).get();
				assertNull("Thread threw an error: " + j, t); //$NON-NLS-1$
			}
		}
	}
}
