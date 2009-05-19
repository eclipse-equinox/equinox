/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import java.io.*;
import java.util.Hashtable;
import junit.framework.TestCase;
import org.eclipse.equinox.log.internal.ExtendedLogReaderServiceFactory;
import org.osgi.framework.*;
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
		Activator.getBundle("org.eclipse.equinox.log").start(); //$NON-NLS-1$
		logReference = Activator.getBundleContext().getServiceReference(LogService.class.getName());
		readerReference = Activator.getBundleContext().getServiceReference(LogReaderService.class.getName());

		log = (LogService) Activator.getBundleContext().getService(logReference);
		reader = (LogReaderService) Activator.getBundleContext().getService(readerReference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(logReference);
		Activator.getBundleContext().ungetService(readerReference);
		Activator.getBundle("org.eclipse.equinox.log").stop(); //$NON-NLS-1$
	}

	public void testaddListener() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info"); //$NON-NLS-1$
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testaddListenerTwice() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		reader.addLogListener(listener);
		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info"); //$NON-NLS-1$
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
				throw new RuntimeException("Expected error for testBadListener."); //$NON-NLS-1$
			}
		};
		reader.addLogListener(listener);

		ExtendedLogReaderServiceFactory.setErrorStream(new PrintStream(new OutputStream() {
			public void write(int arg0) throws IOException {

			}
		}));

		synchronized (listener) {
			log.log(LogService.LOG_INFO, "info"); //$NON-NLS-1$
			listener.wait();
		}
	}

	public void testLogEntry() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		long timeBeforeLog = System.currentTimeMillis();
		synchronized (listener) {
			log.log(logReference, LogService.LOG_INFO, "info", new Throwable("test")); //$NON-NLS-1$ //$NON-NLS-2$
			listener.wait();
		}
		assertTrue(listener.getEntry().getBundle() == Activator.getBundleContext().getBundle());
		assertTrue(listener.getEntry().getMessage().equals("info")); //$NON-NLS-1$
		assertTrue(listener.getEntry().getException().getMessage().equals("test")); //$NON-NLS-1$
		assertTrue(listener.getEntry().getServiceReference() == logReference);
		assertTrue(listener.getEntry().getTime() >= timeBeforeLog);
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testLogBundleEventInfo() throws Exception {

		// this is just a bundle that is harmless to start/stop
		Bundle servicesBundle = Activator.getBundle("org.eclipse.osgi.services"); //$NON-NLS-1$
		if (servicesBundle == null)
			return; // ignore

		servicesBundle.stop();

		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			servicesBundle.start();
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testLogServiceEventInfo() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			Activator.getBundleContext().registerService(Object.class.getName(), new Object(), null);
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testLogServiceEventDebug() throws Exception {
		ServiceRegistration registration = Activator.getBundleContext().registerService(Object.class.getName(), new Object(), null);

		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			registration.setProperties(new Hashtable());
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_DEBUG);
	}

	public void testLogFrameworkEvent() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			Activator.refreshPackages();
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}
}
