/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.serviceregistry;

import java.util.Hashtable;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class ServiceExceptionTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(ServiceExceptionTests.class);
	}

	public void testServiceException01() {
		final String testMethodName = "testServiceException01"; //$NON-NLS-1$
		// test a service factory which returns wrong object types
		ServiceExceptionServiceFactory wrongObjectFactory = new ServiceExceptionServiceFactory("A String"); //$NON-NLS-1$
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), wrongObjectFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getBundle(), null, ServiceException.FACTORY_ERROR);
		OSGiTestsActivator.getContext().addFrameworkListener(listener);
		try {
			ServiceReference[] refs = null;
			try {
				refs = OSGiTestsActivator.getContext().getServiceReferences(Runnable.class.getName(), "(name=" + testMethodName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertNotNull("service refs is null", refs); //$NON-NLS-1$
			assertEquals("Wrong number of references", 1, refs.length); //$NON-NLS-1$
			Runnable service = null;
			try {
				service = (Runnable) OSGiTestsActivator.getContext().getService(refs[0]);
			} catch (ClassCastException e) {
				fail("Unexpected cast exception", e); //$NON-NLS-1$
			}
			assertNull("service is not null", service); //$NON-NLS-1$
			listener.waitForEvent("Failed to fire ServiceException"); //$NON-NLS-1$
			OSGiTestsActivator.getContext().ungetService(refs[0]);
			Error error = wrongObjectFactory.getUngetFailure();
			if (error != null)
				throw error;
		} finally {
			if (reg != null)
				reg.unregister();
			if (listener != null)
				OSGiTestsActivator.getContext().removeFrameworkListener(listener);
		}
	}

	public void testServiceException02() {
		final String testMethodName = "testServiceException02"; //$NON-NLS-1$
		// test a service factory which returns null objects
		ServiceExceptionServiceFactory nullObjectFactory = new ServiceExceptionServiceFactory(null);
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), nullObjectFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getBundle(), null, ServiceException.FACTORY_ERROR);
		OSGiTestsActivator.getContext().addFrameworkListener(listener);
		try {
			ServiceReference[] refs = null;
			try {
				refs = OSGiTestsActivator.getContext().getServiceReferences(Runnable.class.getName(), "(name=" + testMethodName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertNotNull("service refs is null", refs); //$NON-NLS-1$
			assertEquals("Wrong number of references", 1, refs.length); //$NON-NLS-1$
			Runnable service = null;
			try {
				service = (Runnable) OSGiTestsActivator.getContext().getService(refs[0]);
			} catch (ClassCastException e) {
				fail("Unexpected cast exception", e); //$NON-NLS-1$
			}
			assertNull("service is not null", service); //$NON-NLS-1$
			listener.waitForEvent("Failed to fire ServiceException"); //$NON-NLS-1$
			OSGiTestsActivator.getContext().ungetService(refs[0]);
			Error error = nullObjectFactory.getUngetFailure();
			if (error != null)
				throw error;
		} finally {
			if (reg != null)
				reg.unregister();
			if (listener != null)
				OSGiTestsActivator.getContext().removeFrameworkListener(listener);
		}
	}

	public void testServiceException03() {
		final String testMethodName = "testServiceException03"; //$NON-NLS-1$
		// test a service factory which throws a RuntimeException
		RuntimeException cause = new RuntimeException(testMethodName);
		ServiceExceptionServiceFactory runtimeExceptionFactory = new ServiceExceptionServiceFactory(cause);
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runtimeExceptionFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getBundle(), cause, ServiceException.FACTORY_EXCEPTION);
		OSGiTestsActivator.getContext().addFrameworkListener(listener);
		try {
			ServiceReference[] refs = null;
			try {
				refs = OSGiTestsActivator.getContext().getServiceReferences(Runnable.class.getName(), "(name=" + testMethodName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertNotNull("service refs is null", refs); //$NON-NLS-1$
			assertEquals("Wrong number of references", 1, refs.length); //$NON-NLS-1$
			Runnable service = null;
			try {
				service = (Runnable) OSGiTestsActivator.getContext().getService(refs[0]);
			} catch (ClassCastException e) {
				fail("Unexpected cast exception", e); //$NON-NLS-1$
			}
			assertNull("service is not null", service); //$NON-NLS-1$
			listener.waitForEvent("Failed to fire ServiceException"); //$NON-NLS-1$
			OSGiTestsActivator.getContext().ungetService(refs[0]);
			Error error = runtimeExceptionFactory.getUngetFailure();
			if (error != null)
				throw error;
		} finally {
			if (reg != null)
				reg.unregister();
			if (listener != null)
				OSGiTestsActivator.getContext().removeFrameworkListener(listener);
		}
	}

	public void testServiceException04() {
		final String testMethodName = "testServiceException04"; //$NON-NLS-1$
		// test a service factory which throws an Error
		Error cause = new Error(testMethodName);
		ServiceExceptionServiceFactory errorFactory = new ServiceExceptionServiceFactory(cause);
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), errorFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getBundle(), cause, ServiceException.FACTORY_EXCEPTION);
		OSGiTestsActivator.getContext().addFrameworkListener(listener);
		try {
			ServiceReference[] refs = null;
			try {
				refs = OSGiTestsActivator.getContext().getServiceReferences(Runnable.class.getName(), "(name=" + testMethodName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertNotNull("service refs is null", refs); //$NON-NLS-1$
			assertEquals("Wrong number of references", 1, refs.length); //$NON-NLS-1$
			Runnable service = null;
			try {
				service = (Runnable) OSGiTestsActivator.getContext().getService(refs[0]);
			} catch (ClassCastException e) {
				fail("Unexpected cast exception", e); //$NON-NLS-1$
			}
			assertNull("service is not null", service); //$NON-NLS-1$
			listener.waitForEvent("Failed to fire ServiceException"); //$NON-NLS-1$
			OSGiTestsActivator.getContext().ungetService(refs[0]);
			Error error = errorFactory.getUngetFailure();
			if (error != null)
				throw error;
		} finally {
			if (reg != null)
				reg.unregister();
			if (listener != null)
				OSGiTestsActivator.getContext().removeFrameworkListener(listener);
		}
	}

	class ServiceExceptionServiceFactory implements ServiceFactory {
		private final Object serviceOrThrowable;
		private Error ungetFailure;

		public ServiceExceptionServiceFactory(Object serviceOrThrowable) {
			this.serviceOrThrowable = serviceOrThrowable;
		}

		public Object getService(Bundle bundle, ServiceRegistration registration) {
			if (serviceOrThrowable instanceof RuntimeException)
				throw (RuntimeException) serviceOrThrowable;
			if (serviceOrThrowable instanceof Error)
				throw (Error) serviceOrThrowable;
			return serviceOrThrowable;
		}

		public synchronized void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			try {
				if (serviceOrThrowable instanceof RuntimeException)
					fail("Unexpected call to ungetService: " + serviceOrThrowable); //$NON-NLS-1$
				if (serviceOrThrowable instanceof Error)
					fail("Unexpected call to ungetService: " + serviceOrThrowable); //$NON-NLS-1$
			} catch (Error error) {
				ungetFailure = error;
			}
		}

		public Error getUngetFailure() {
			return ungetFailure;
		}
	}

	class ServiceExceptionFrameworkListener implements FrameworkListener {
		private final Bundle registrationBundle;
		private final Throwable exception;
		private final int exceptionType;
		private boolean waitForEvent = true;

		public ServiceExceptionFrameworkListener(Bundle registrationBundle, Throwable exception, int exceptionType) {
			this.registrationBundle = registrationBundle;
			this.exception = exception;
			this.exceptionType = exceptionType;
		}

		public void frameworkEvent(FrameworkEvent event) {
			if (event.getBundle() != registrationBundle)
				return;
			if (!(event.getThrowable() instanceof ServiceException))
				return;
			if (((ServiceException) event.getThrowable()).getCause() != exception)
				return;
			if (((ServiceException) event.getThrowable()).getType() != exceptionType)
				return;
			notifyWaiter();
		}

		private synchronized void notifyWaiter() {
			waitForEvent = false;
			notifyAll();
		}

		public synchronized void waitForEvent(String failMessage) {
			if (waitForEvent) {
				try {
					wait(10000);
				} catch (InterruptedException e) {
					fail("unexpected interuption", e); //$NON-NLS-1$
				}
				// still waiting for event; we now fail
				if (waitForEvent)
					fail(failMessage);
			}
		}
	}
}
