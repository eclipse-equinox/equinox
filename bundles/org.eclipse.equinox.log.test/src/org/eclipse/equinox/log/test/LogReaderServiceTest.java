package org.eclipse.equinox.log.test;

import java.io.*;
import junit.framework.TestCase;
import org.eclipse.equinox.log.internal.ExtendedLogReaderServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.*;

public class LogReaderServiceTest extends TestCase {

	private LogService log;
	private ServiceReference logReference;
	private LogReaderService reader;
	private ServiceReference readerReference;

	public LogReaderServiceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.log").start();
		logReference = Activator.getBundleContext().getServiceReference(LogService.class.getName());
		readerReference = Activator.getBundleContext().getServiceReference(LogReaderService.class.getName());

		log = (LogService) Activator.getBundleContext().getService(logReference);
		reader = (LogReaderService) Activator.getBundleContext().getService(readerReference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(logReference);
		Activator.getBundleContext().ungetService(readerReference);
		Activator.getBundle("org.eclipse.equinox.log").stop();
	}

	public void testaddListener() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info");
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testaddListenerTwice() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		reader.addLogListener(listener);
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

	public void testBadListener() throws Exception {
		LogListener listener = new LogListener() {
			public synchronized void logged(LogEntry entry) {
				notifyAll();
				throw new RuntimeException("Expected error for testBadListener.");
			}
		};
		reader.addLogListener(listener);

		ExtendedLogReaderServiceFactory.setErrorStream(new PrintStream(new OutputStream() {
			public void write(int arg0) throws IOException {

			}
		}));

		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info");
			listener.wait();
		}
	}

	public void testLogEntry() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		long timeBeforeLog = System.currentTimeMillis();
		synchronized (listener) {
			log.log(logReference, LogService.LOG_INFO, "info", new Throwable("test"));
			listener.wait();
		}
		assertTrue(listener.getEntry().getBundle() == Activator.getBundleContext().getBundle());
		assertTrue(listener.getEntry().getMessage().equals("info"));
		assertTrue(listener.getEntry().getException().getMessage().equals("test"));
		assertTrue(listener.getEntry().getServiceReference() == logReference);
		assertTrue(listener.getEntry().getTime() >= timeBeforeLog);
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}
}
