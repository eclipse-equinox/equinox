package org.eclipse.equinox.log.test;

import java.io.*;
import junit.framework.TestCase;
import org.eclipse.equinox.log.*;
import org.eclipse.equinox.log.internal.ExtendedLogReaderServiceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.*;

public class ExtendedLogReaderServiceTest extends TestCase {

	private ExtendedLogService log;
	private ServiceReference logReference;
	private ExtendedLogReaderService reader;
	private ServiceReference readerReference;
	private boolean called;

	public ExtendedLogReaderServiceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.log").start();
		logReference = Activator.getBundleContext().getServiceReference(ExtendedLogService.class.getName());
		readerReference = Activator.getBundleContext().getServiceReference(ExtendedLogReaderService.class.getName());

		log = (ExtendedLogService) Activator.getBundleContext().getService(logReference);
		reader = (ExtendedLogReaderService) Activator.getBundleContext().getService(readerReference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(logReference);
		Activator.getBundleContext().ungetService(readerReference);
		Activator.getBundle("org.eclipse.equinox.log").stop();
	}

	public void testaddFilteredListener() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener, new LogFilter() {
			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				return true;
			}
		});
		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info");
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testaddNullFilterr() throws Exception {
		TestListener listener = new TestListener();

		try {
			reader.addLogListener(listener, null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testaddFilteredListenerTwice() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener, new LogFilter() {
			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				return false;
			}
		});

		if (log.isLoggable(LogService.LOG_INFO))
			fail();

		reader.addLogListener(listener, new LogFilter() {
			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				return true;
			}
		});
		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info");
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testaddNullListener() throws Exception {
		try {
			reader.addLogListener(null);
		} catch (IllegalArgumentException t) {
			return;
		}
		fail();
	}

	public void testBadFilter() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener, new LogFilter() {
			public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
				throw new RuntimeException("Expected error for testBadFilter.");
			}
		});
		ExtendedLogReaderServiceFactory.setErrorStream(new PrintStream(new OutputStream() {
			public void write(int arg0) throws IOException {

			}
		}));

		if (log.isLoggable(LogService.LOG_INFO))
			fail();
	}

	public void testSynchronousLogListener() throws Exception {
		final Thread loggerThread = Thread.currentThread();
		called = false;
		LogListener listener = new SynchronousLogListener() {
			public void logged(LogEntry entry) {
				assertTrue(Thread.currentThread() == loggerThread);
				called = true;
			}
		};
		reader.addLogListener(listener);
		log.log(LogService.LOG_INFO, "info");
		assertTrue(called);
	}

	public void testExtendedLogEntry() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		long timeBeforeLog = System.currentTimeMillis();
		String threadName = Thread.currentThread().getName();
		long threadId = Thread.currentThread().getId();
		synchronized (listener) {
			log.getLogger("test").log(logReference, LogService.LOG_INFO, "info", new Throwable("test"));
			listener.wait();
		}
		long sequenceNumberBefore = listener.getEntryX().getSequenceNumber();

		synchronized (listener) {
			log.getLogger("test").log(logReference, LogService.LOG_INFO, "info", new Throwable("test"));
			listener.wait();
		}
		assertTrue(listener.getEntryX().getBundle() == Activator.getBundleContext().getBundle());
		assertTrue(listener.getEntryX().getMessage().equals("info"));
		assertTrue(listener.getEntryX().getException().getMessage().equals("test"));
		assertTrue(listener.getEntryX().getServiceReference() == logReference);
		assertTrue(listener.getEntryX().getTime() >= timeBeforeLog);
		assertTrue(listener.getEntryX().getLevel() == LogService.LOG_INFO);

		assertTrue(listener.getEntryX().getLoggerName().equals("test"));
		assertTrue(listener.getEntryX().getThreadName().equals(threadName));
		assertTrue(listener.getEntryX().getThreadId() == threadId);
		assertTrue(listener.getEntryX().getContext() == logReference);
		assertTrue(listener.getEntryX().getSequenceNumber() > sequenceNumberBefore);
	}
}
