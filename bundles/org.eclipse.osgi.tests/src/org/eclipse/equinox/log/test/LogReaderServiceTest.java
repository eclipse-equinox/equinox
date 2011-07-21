/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import java.util.Hashtable;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.*;
import org.osgi.service.log.*;

public class LogReaderServiceTest extends AbstractBundleTests {

	private LogService log;
	private ServiceReference logReference;
	private LogReaderService reader;
	private ServiceReference readerReference;

	public LogReaderServiceTest(String name) {
		setName(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		logReference = OSGiTestsActivator.getContext().getServiceReference(LogService.class.getName());
		readerReference = OSGiTestsActivator.getContext().getServiceReference(LogReaderService.class.getName());

		log = (LogService) OSGiTestsActivator.getContext().getService(logReference);
		reader = (LogReaderService) OSGiTestsActivator.getContext().getService(readerReference);
	}

	protected void tearDown() throws Exception {
		OSGiTestsActivator.getContext().ungetService(logReference);
		OSGiTestsActivator.getContext().ungetService(readerReference);
		super.tearDown();
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
		assertTrue(listener.getEntry().getBundle() == OSGiTestsActivator.getContext().getBundle());
		assertTrue(listener.getEntry().getMessage().equals("info")); //$NON-NLS-1$
		assertTrue(listener.getEntry().getException().getMessage().equals("test")); //$NON-NLS-1$
		assertTrue(listener.getEntry().getServiceReference() == logReference);
		assertTrue(listener.getEntry().getTime() >= timeBeforeLog);
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testLogBundleEventInfo() throws Exception {

		// this is just a bundle that is harmless to start/stop
		Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			testBundle.start();
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testLogServiceEventInfo() throws Exception {
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			OSGiTestsActivator.getContext().registerService(Object.class.getName(), new Object(), null);
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}

	public void testLogServiceEventDebug() throws Exception {
		ServiceRegistration registration = OSGiTestsActivator.getContext().registerService(Object.class.getName(), new Object(), null);

		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			registration.setProperties(new Hashtable());
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_DEBUG);
	}

	public void testLogFrameworkEvent() throws Exception {
		Bundle testBundle = installer.installBundle("test.logging.a"); //$NON-NLS-1$
		TestListener listener = new TestListener();
		reader.addLogListener(listener);
		synchronized (listener) {
			installer.refreshPackages(new Bundle[] {testBundle});
			listener.wait();
		}
		assertTrue(listener.getEntry().getLevel() == LogService.LOG_INFO);
	}
}
