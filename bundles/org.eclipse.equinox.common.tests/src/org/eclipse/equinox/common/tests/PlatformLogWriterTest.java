/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package org.eclipse.equinox.common.tests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;

public class PlatformLogWriterTest {

	public static class TestILogListener implements ILogListener {
		final AtomicReference<CountDownLatch> expected = new AtomicReference<>();
		final List<IStatus> statuses = new CopyOnWriteArrayList<>();

		@Override
		public void logging(IStatus status, String plugin) {
			CountDownLatch current = expected.get();
			if (current != null) {
				current.countDown();
			}
			statuses.add(status);
		}

		List<IStatus> getAllExpectedStatus() throws InterruptedException {
			CountDownLatch current = expected.get();
			if (current != null) {
				current.await(10, TimeUnit.SECONDS);
			}
			return new ArrayList<>(statuses);
		}

		void setExpected(int expected) {
			statuses.clear();
			this.expected.set(new CountDownLatch(expected));
		}
	}

	LogService logService;
	final TestILogListener listener = new TestILogListener();

	@Before
	public void setUp() {
		Bundle thisBundle = FrameworkUtil.getBundle(getClass());
		BundleContext context = thisBundle.getBundleContext();
		logService = context.getService(context.getServiceReference(LogService.class));
		RuntimeLog.addLogListener(listener);
	}

	@After
	public void tearDown() throws Exception {
		RuntimeLog.removeLogListener(listener);
	}

	@Test
	public void testLogServiceLevels() throws InterruptedException {
		listener.setExpected(6);
		Logger logger = logService.getLogger("org.eclipse.equinox.logger");
		logger.audit("audit");
		logger.error("error");
		logger.warn("warn");
		logger.info("info");
		logger.debug("debug");
		logger.trace("trace");
		List<IStatus> allStatus = listener.getAllExpectedStatus();
		assertEquals("Wrong number of status.", 6, allStatus.size());
		assertStatus(allStatus.get(0), "audit", IStatus.OK);
		assertStatus(allStatus.get(1), "error", IStatus.ERROR);
		assertStatus(allStatus.get(2), "warn", IStatus.WARNING);
		assertStatus(allStatus.get(3), "info", IStatus.INFO);
		assertStatus(allStatus.get(4), "debug", IStatus.OK);
		assertStatus(allStatus.get(5), "trace", IStatus.OK);
	}

	private void assertStatus(IStatus status, String message, int severity) {
		assertEquals("Wrong message.", message, status.getMessage());
		assertEquals("Wrong severity.", severity, status.getSeverity());
	}
}
