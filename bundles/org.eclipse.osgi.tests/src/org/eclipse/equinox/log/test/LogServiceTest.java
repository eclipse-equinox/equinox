/*******************************************************************************
s
s This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import junit.framework.TestCase;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

@SuppressWarnings("deprecation")
public class LogServiceTest extends TestCase {

	private LogService log;
	private ServiceReference logReference;
	private LogReaderService reader;
	private ServiceReference readerReference;
	private TestListener listener;
	private ServiceReference<LoggerAdmin> loggerAdminReference;
	private LoggerAdmin loggerAdmin;
	LoggerContext rootLoggerContext;
	Map<String, LogLevel> rootLogLevels;

	public LogServiceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		logReference = OSGiTestsActivator.getContext().getServiceReference(LogService.class.getName());
		readerReference = OSGiTestsActivator.getContext().getServiceReference(LogReaderService.class.getName());
		loggerAdminReference = OSGiTestsActivator.getContext().getServiceReference(LoggerAdmin.class);

		log = (LogService) OSGiTestsActivator.getContext().getService(logReference);
		reader = (LogReaderService) OSGiTestsActivator.getContext().getService(readerReference);
		loggerAdmin = OSGiTestsActivator.getContext().getService(loggerAdminReference);

		listener = new TestListener();
		reader.addLogListener(listener);

		rootLoggerContext = loggerAdmin.getLoggerContext(null);
		rootLogLevels = rootLoggerContext.getLogLevels();

		Map<String, LogLevel> copyLogLevels = new HashMap<>(rootLogLevels);
		copyLogLevels.put(Logger.ROOT_LOGGER_NAME, LogLevel.TRACE);
		rootLoggerContext.setLogLevels(copyLogLevels);
	}

	protected void tearDown() throws Exception {
		rootLoggerContext.setLogLevels(rootLogLevels);
		reader.removeLogListener(listener);
		OSGiTestsActivator.getContext().ungetService(loggerAdminReference);
		OSGiTestsActivator.getContext().ungetService(logReference);
		OSGiTestsActivator.getContext().ungetService(readerReference);
	}

	public void testLogDebug() throws Exception {
		log.log(LogService.LOG_DEBUG, "debug"); //$NON-NLS-1$
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_DEBUG);
	}

	public void testLogError() throws Exception {
		log.log(LogService.LOG_ERROR, "error"); //$NON-NLS-1$
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_ERROR);
	}

	public void testLogInfo() throws Exception {
		log.log(LogService.LOG_INFO, "info"); //$NON-NLS-1$
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_INFO);
	}

	public void testLogWarning() throws Exception {
		log.log(LogService.LOG_WARNING, "warning"); //$NON-NLS-1$
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_WARNING);
	}

	public void testLogZeroLevel() throws Exception {
		log.log(0, "zero"); //$NON-NLS-1$
		assertTrue(listener.getEntryX().getLevel() == 0);
	}

	public void testLogNegativeLevel() throws Exception {
		log.log(-1, "negative"); //$NON-NLS-1$
		assertTrue(listener.getEntryX().getLevel() == -1);
	}

	public void testLogMessage() throws Exception {
		log.log(LogService.LOG_INFO, "message"); //$NON-NLS-1$
		assertTrue(listener.getEntryX().getMessage().equals("message")); //$NON-NLS-1$
	}

	public void testLogNullMessage() throws Exception {
		log.log(LogService.LOG_INFO, null);
		assertTrue(listener.getEntryX().getMessage() == null);
	}

	public void testLogThrowable() throws Exception {
		Throwable t = new Throwable("throwable"); //$NON-NLS-1$
		log.log(LogService.LOG_INFO, null, t);
		assertTrue(listener.getEntryX().getException().getMessage().equals(t.getMessage()));
	}

	public void testLogNullThrowable() throws Exception {
		log.log(LogService.LOG_INFO, null, null);
		assertTrue(listener.getEntryX().getException() == null);
	}

	public void testLogServiceReference() throws Exception {
		log.log(logReference, LogService.LOG_INFO, null);
		assertTrue(listener.getEntryX().getServiceReference().equals(logReference));
	}

	public void testNullLogServiceReference() throws Exception {
		log.log(null, LogService.LOG_INFO, null);
		assertTrue(listener.getEntryX().getServiceReference() == null);
	}

	public void testLogServiceReferenceWithNullThrowable() throws Exception {
		log.log(logReference, LogService.LOG_INFO, null, null);
		assertTrue(listener.getEntryX().getServiceReference().equals(logReference));
	}

	public void testLogNull1() throws Exception {
		log.log(0, null);
		LogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	public void testLogNull2() throws Exception {
		log.log(0, null, null);
		LogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	public void testLogNull3() throws Exception {
		log.log(null, 0, null);
		LogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	public void testLogNull4() throws Exception {
		log.log(null, 0, null, null);
		LogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == 0);
		assertTrue(entry.getMessage() == null);
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	public void testLogFull1() throws Exception {
		String message = "test"; //$NON-NLS-1$
		log.log(LogService.LOG_INFO, message);
		LogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == null);
	}

	public void testLogFull2() throws Exception {
		String message = "test"; //$NON-NLS-1$
		Throwable t = new Throwable("test"); //$NON-NLS-1$
		log.log(LogService.LOG_INFO, message, t);
		LogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException().getMessage().equals(t.getMessage()));
		assertTrue(entry.getServiceReference() == null);
	}

	public void testLogFull3() throws Exception {
		String message = "test"; //$NON-NLS-1$
		log.log(logReference, LogService.LOG_INFO, message);
		LogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException() == null);
		assertTrue(entry.getServiceReference() == logReference);
	}

	public void testLogFull4() throws Exception {
		String message = "test"; //$NON-NLS-1$
		Throwable t = new Throwable("test"); //$NON-NLS-1$
		log.log(logReference, LogService.LOG_INFO, message, t);
		LogEntry entry = listener.getEntryX();
		assertTrue(entry.getLevel() == LogService.LOG_INFO);
		assertTrue(entry.getMessage().equals(message));
		assertTrue(entry.getException().getMessage().equals(t.getMessage()));
		assertTrue(entry.getServiceReference() == logReference);
	}

	public void testServiceEventLog() throws InterruptedException {
		BundleContext context = OSGiTestsActivator.getContext();
		ServiceRegistration<Object> reg = context.registerService(Object.class, new Object(), null);
		ServiceReference<Object> ref = reg.getReference();
		String expectedLoggerName = "Events.Service" + "." + context.getBundle().getSymbolicName();

		LogEntry entry = listener.getEntryX();
		assertEquals("Wrong logger name.", expectedLoggerName, entry.getLoggerName());
		assertEquals("Wrong event log level.", LogLevel.INFO, entry.getLogLevel());
		assertEquals("Wrong bundle.", context.getBundle(), entry.getBundle());
		assertNull("Wrong exception.", entry.getException());
		assertEquals("Wrong service reference.", ref, entry.getServiceReference());
		assertEquals("Wrong message.", "ServiceEvent REGISTERED", entry.getMessage());

		reg.setProperties(new Hashtable(Collections.singletonMap("key1", "value1")));

		entry = listener.getEntryX();
		assertEquals("Wrong logger name.", expectedLoggerName, entry.getLoggerName());
		assertEquals("Wrong event log level.", LogLevel.DEBUG, entry.getLogLevel());
		assertEquals("Wrong bundle.", context.getBundle(), entry.getBundle());
		assertNull("Wrong exception.", entry.getException());
		assertEquals("Wrong service reference.", ref, entry.getServiceReference());
		assertEquals("Wrong message.", "ServiceEvent MODIFIED", entry.getMessage());

		reg.unregister();
		entry = listener.getEntryX();
		assertEquals("Wrong logger name.", expectedLoggerName, entry.getLoggerName());
		assertEquals("Wrong event log level.", LogLevel.INFO, entry.getLogLevel());
		assertEquals("Wrong bundle.", context.getBundle(), entry.getBundle());
		assertNull("Wrong exception.", entry.getException());
		assertEquals("Wrong service reference.", ref, entry.getServiceReference());
		assertEquals("Wrong message.", "ServiceEvent UNREGISTERING", entry.getMessage());
	}
}
