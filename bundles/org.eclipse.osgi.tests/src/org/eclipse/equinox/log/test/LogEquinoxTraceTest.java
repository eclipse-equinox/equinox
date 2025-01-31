/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.eclipse.osgi.tests.bundles.SystemBundleTests;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.condition.Condition;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;
import org.osgi.service.startlevel.StartLevel;

@SuppressWarnings("deprecation") // LogService
public class LogEquinoxTraceTest extends AbstractBundleTests {

	static class TestListener implements SynchronousLogListener {
		List<LogEntry> logs = new ArrayList<>();

		@Override
		public void logged(LogEntry entry) {
			synchronized (logs) {
				Bundle b = entry.getBundle();
				if (b != null && b.getBundleId() == 0) {
					logs.add(entry);
				}
			}
		}

		List<LogEntry> getLogs() {
			synchronized (logs) {
				List<LogEntry> results = new ArrayList(logs);
				logs.clear();
				System.out.println("DEBUG TEST LOGS: count=" + results.size());
				results.stream().forEach((l) -> System.out.println("   DEBUG TEST LOG: " + l.getMessage()));
				return results;
			}
		}
	}

	private LoggerAdmin loggerAdmin = null;
	private TestListener testListener = null;
	private Equinox equinox = null;
	private DebugOptions debugOptions = null;

	@Override
	public void setUp() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		equinox = new Equinox(configuration);
		equinox.start();
		loggerAdmin = equinox.getBundleContext()
				.getService(equinox.getBundleContext().getServiceReference(LoggerAdmin.class));
		LogReaderService logReader = equinox.getBundleContext()
				.getService(equinox.getBundleContext().getServiceReference(LogReaderService.class));

		testListener = new TestListener();
		logReader.addLogListener(testListener);

		BundleContext systemContext = equinox.getBundleContext();
		debugOptions = systemContext.getService(systemContext.getServiceReference(DebugOptions.class));
	}

	@Override
	public void tearDown() {
		if (equinox != null) {
			try {
				equinox.stop();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Test
	public void testEnableRootLoggerContextTrace() {
		doTestEnableTrace(null);
	}

	@Test
	public void testEnableFrameworkLoggerContextTrace() {
		doTestEnableTrace(equinox.getBundleContext().getBundle().getSymbolicName());
	}

	private void doTestEnableTrace(String loggerContextName) {
		assertFalse("Expected debug options to be disabled.", debugOptions.isDebugEnabled());
		assertEquals("Expected no debug Options.", 0, debugOptions.getOptions().size());

		LoggerContext rootContext = loggerAdmin.getLoggerContext(loggerContextName);
		Map<String, LogLevel> rootLogLevels = rootContext.getLogLevels();

		// enable service trace
		rootLogLevels.put(Debug.EQUINOX_TRACE, LogLevel.TRACE);
		rootLogLevels.put(Debug.OPTION_DEBUG_SERVICES, LogLevel.TRACE);
		rootContext.setLogLevels(rootLogLevels);

		assertTrue("Expected debug to be enabled.", debugOptions.isDebugEnabled());
		assertEquals("Expected 1 debug Option.", 1, debugOptions.getOptions().size());
		assertTrue("Expected trace option to be enabled.",
				debugOptions.getBooleanOption(Debug.OPTION_DEBUG_SERVICES, false));
		assertFalse("Expected trace option to be disabled.",
				debugOptions.getBooleanOption(Debug.OPTION_DEBUG_STARTLEVEL, false));

		// get some service to generate trace
		ServiceReference<StartLevel> ref = equinox.getBundleContext().getServiceReference(StartLevel.class);
		List<LogEntry> traceLogs = testListener.getLogs();
		assertNotEquals("Expected to have some trace logs.", 0, traceLogs.size());
		for (LogEntry logEntry : traceLogs) {
			assertEquals("Wrong logger name", Debug.OPTION_DEBUG_SERVICES,
					logEntry.getLoggerName());
		}

		// disable service trace, enable startlevel trace
		rootLogLevels.remove(Debug.OPTION_DEBUG_SERVICES);
		rootLogLevels.put(Debug.OPTION_DEBUG_STARTLEVEL, LogLevel.TRACE);
		rootContext.setLogLevels(rootLogLevels);

		assertTrue("Expected debug to be enabled.", debugOptions.isDebugEnabled());
		assertEquals("Expected 1 debug Option.", 1, debugOptions.getOptions().size());
		assertTrue("Expected trace option to be enabled.",
				debugOptions.getBooleanOption(Debug.OPTION_DEBUG_STARTLEVEL, false));
		assertFalse("Expected trace option to be disabled.",
				debugOptions.getBooleanOption(Debug.OPTION_DEBUG_SERVICES, false));

		// Get the StartLevel service to generate service logs (should be disabled)
		// Use the StartLevel service to generate startlevel logs (should be enabled)
		StartLevel startLevel = equinox.getBundleContext().getService(ref);
		startLevel.setStartLevel(20);

		traceLogs = testListener.getLogs();
		assertNotEquals("Expected to have some trace logs.", 0, traceLogs.size());
		for (LogEntry logEntry : traceLogs) {
			assertEquals("Wrong logger name", Debug.OPTION_DEBUG_STARTLEVEL,
					logEntry.getLoggerName());
		}

		// Setting the EQUINOX.TRACE to anything but trace should disable
		rootLogLevels.put(Debug.EQUINOX_TRACE, LogLevel.AUDIT);
		rootContext.setLogLevels(rootLogLevels);
		assertFalse("Expected debug to be disabled.", debugOptions.isDebugEnabled());
		assertEquals("Expected no debug Options.", 0, debugOptions.getOptions().size());
	}

	@Test
	public void testRootLoggerNameTrace() {
		LoggerContext rootContext = loggerAdmin.getLoggerContext(null);
		Map<String, LogLevel> rootLogLevels = rootContext.getLogLevels();

		// make sure that enabling the root logger trace does not enable all Equinox
		// trace
		rootLogLevels.put(Logger.ROOT_LOGGER_NAME, LogLevel.TRACE);
		// enable some unused trace for equinox
		rootLogLevels.put(Debug.EQUINOX_TRACE, LogLevel.TRACE);
		rootLogLevels.put(Debug.OPTION_DEBUG_STARTLEVEL, LogLevel.TRACE);
		rootContext.setLogLevels(rootLogLevels);

		// get some service to generate trace (which should not be logged)
		equinox.getBundleContext().getServiceReference(Condition.class);
		List<LogEntry> traceLogs = testListener.getLogs();
		assertEquals("Expected to have no trace logs.", 0, traceLogs.size());
	}

	@Test
	public void testSeparateLoggerContext() throws IOException, BundleException {
		// install another bundle and get a logger context that applies to it
		File baseBundlesDir = OSGiTestsActivator.getContext().getDataFile(getName());

		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());

		baseBundlesDir.mkdirs();

		File bundleFile = SystemBundleTests.createBundle(baseBundlesDir, getName(), headers);
		Bundle b = equinox.getBundleContext().installBundle(bundleFile.toURI().toASCIIString());
		LoggerContext testBundleLoggerContext = loggerAdmin.getLoggerContext(b.getSymbolicName());

		// Try setting EQUINOX TRACE using a logger context that does not apply to the
		// framework
		Map<String, LogLevel> testBundleLogLevels = testBundleLoggerContext.getLogLevels();
		// enable service trace
		testBundleLogLevels.put(Debug.EQUINOX_TRACE, LogLevel.TRACE);
		testBundleLogLevels.put(Debug.OPTION_DEBUG_SERVICES, LogLevel.TRACE);
		testBundleLoggerContext.setLogLevels(testBundleLogLevels);
		// should not enable trace
		assertFalse("Expected debug to be disabled.", debugOptions.isDebugEnabled());
		assertEquals("Expected no debug Options.", 0, debugOptions.getOptions().size());

		// get some service to generate trace
		ServiceReference<StartLevel> ref = equinox.getBundleContext().getServiceReference(StartLevel.class);
		List<LogEntry> traceLogs = testListener.getLogs();
		assertEquals("Expected to have no trace logs.", 0, traceLogs.size());
	}
}
