/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.util.Hashtable;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServiceRegistryBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(ServiceRegistryBundleTests.class);
	}

	public void testServiceTracker01() {
		// simple ServiceTracker test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put("testServiceTracker01", Boolean.TRUE); //$NON-NLS-1$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
		ServiceTracker testTracker = null;
		try {
			final boolean[] results = new boolean[] {false, false, false};
			ServiceTrackerCustomizer testCustomizer = new ServiceTrackerCustomizer() {
				public Object addingService(ServiceReference reference) {
					results[0] = true;
					return reference;
				}

				public void modifiedService(ServiceReference reference, Object service) {
					results[1] = true;
				}

				public void removedService(ServiceReference reference, Object service) {
					results[2] = true;
				}
			};
			try {
				testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), FrameworkUtil.createFilter("(&(objectClass=java.lang.Runnable)(testServiceTracker01=true))"), testCustomizer); //$NON-NLS-1$
			} catch (InvalidSyntaxException e) {
				fail("filter error", e); //$NON-NLS-1$
			}
			testTracker.open();
			assertTrue("Did not call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertTrue("Did not call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testServiceTracker01", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertTrue("Did not call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testChangeProp", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props back to match
			props.put("testServiceTracker01", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertTrue("Did not call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

		} finally {
			if (reg != null)
				reg.unregister();
			if (testTracker != null)
				testTracker.close();
		}
	}

	public void testServiceTracker02() {
		// simple ServiceTracker test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put("testServiceTracker02", Boolean.FALSE); //$NON-NLS-1$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
		ServiceTracker testTracker = null;
		try {
			final boolean[] results = new boolean[] {false, false, false};
			ServiceTrackerCustomizer testCustomizer = new ServiceTrackerCustomizer() {
				public Object addingService(ServiceReference reference) {
					results[0] = true;
					return reference;
				}

				public void modifiedService(ServiceReference reference, Object service) {
					results[1] = true;
				}

				public void removedService(ServiceReference reference, Object service) {
					results[2] = true;
				}
			};
			try {
				testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), FrameworkUtil.createFilter("(&(objectClass=java.lang.Runnable)(testServiceTracker02=true))"), testCustomizer); //$NON-NLS-1$
			} catch (InvalidSyntaxException e) {
				fail("filter error", e); //$NON-NLS-1$
			}
			testTracker.open();
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to match
			props.put("testServiceTracker02", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertTrue("Did not call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertTrue("Did not call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testServiceTracker02", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertTrue("Did not call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

		} finally {
			if (reg != null)
				reg.unregister();
			if (testTracker != null)
				testTracker.close();
		}
	}

	public void testServiceListener01() {
		// simple ServiceListener test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final boolean[] results = new boolean[] {false, false, false, false};
		ServiceListener testListener = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				switch (event.getType()) {
					case ServiceEvent.REGISTERED :
						results[0] = true;
						break;
					case ServiceEvent.MODIFIED :
						results[1] = true;
						break;
					case ServiceEvent.MODIFIED_ENDMATCH :
						results[2] = true;
						break;
					case ServiceEvent.UNREGISTERING :
						results[3] = true;
						break;
				}
			}
		};
		try {
			OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectClass=java.lang.Runnable)(testServiceListener01=true))"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("filter error", e); //$NON-NLS-1$
		}
		ServiceRegistration reg = null;
		try {
			// register service which matches
			Hashtable props = new Hashtable();
			props.put("testServiceListener01", Boolean.TRUE); //$NON-NLS-1$
			reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
			assertTrue("Did not get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testServiceListener01", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testChangeProp", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props back to match
			props.put("testServiceListener01", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// unregister
			reg.unregister();
			reg = null;
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);
		} finally {
			OSGiTestsActivator.getContext().removeServiceListener(testListener);
			if (reg != null)
				reg.unregister();
		}
	}

	public void testServiceListener02() {
		// simple ServiceListener test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final boolean[] results = new boolean[] {false, false, false, false};
		ServiceListener testListener = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				switch (event.getType()) {
					case ServiceEvent.REGISTERED :
						results[0] = true;
						break;
					case ServiceEvent.MODIFIED :
						results[1] = true;
						break;
					case ServiceEvent.MODIFIED_ENDMATCH :
						results[2] = true;
						break;
					case ServiceEvent.UNREGISTERING :
						results[3] = true;
						break;
				}
			}
		};
		try {
			OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectClass=java.lang.Runnable)(testServiceListener02=true))"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("filter error", e); //$NON-NLS-1$
		}
		ServiceRegistration reg = null;
		try {
			// register service which does not match
			Hashtable props = new Hashtable();
			props.put("testServiceListener02", Boolean.FALSE); //$NON-NLS-1$
			reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to still not match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to match
			props.put("testServiceListener02", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testServiceListener02", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// unregister
			reg.unregister();
			reg = null;
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);
		} finally {
			OSGiTestsActivator.getContext().removeServiceListener(testListener);
			if (reg != null)
				reg.unregister();
		}
	}

	public void testServiceException01() {
		// test a service factory which returns wrong object types
		ServiceExceptionServiceFactory wrongObjectFactory = new ServiceExceptionServiceFactory("A String"); //$NON-NLS-1$
		Hashtable props = new Hashtable();
		props.put("name", "testServiceException01"); //$NON-NLS-1$ //$NON-NLS-2$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), wrongObjectFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getContext().getBundle(), null, ServiceException.FACTORY_ERROR);
		OSGiTestsActivator.getContext().addFrameworkListener(listener);
		try {
			ServiceReference[] refs = null;
			try {
				refs = OSGiTestsActivator.getContext().getServiceReferences(Runnable.class.getName(), "(name=testServiceException01)"); //$NON-NLS-1$
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
		// test a service factory which returns null objects
		ServiceExceptionServiceFactory nullObjectFactory = new ServiceExceptionServiceFactory(null);
		Hashtable props = new Hashtable();
		props.put("name", "testServiceException02"); //$NON-NLS-1$ //$NON-NLS-2$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), nullObjectFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getContext().getBundle(), null, ServiceException.FACTORY_ERROR);
		OSGiTestsActivator.getContext().addFrameworkListener(listener);
		try {
			ServiceReference[] refs = null;
			try {
				refs = OSGiTestsActivator.getContext().getServiceReferences(Runnable.class.getName(), "(name=testServiceException02)"); //$NON-NLS-1$
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
		// test a service factory which throws a RuntimeException
		RuntimeException cause = new RuntimeException("testServiceException03"); //$NON-NLS-1$ 
		ServiceExceptionServiceFactory runtimeExceptionFactory = new ServiceExceptionServiceFactory(cause);
		Hashtable props = new Hashtable();
		props.put("name", "testServiceException03"); //$NON-NLS-1$ //$NON-NLS-2$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runtimeExceptionFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getContext().getBundle(), cause, ServiceException.FACTORY_EXCEPTION);
		OSGiTestsActivator.getContext().addFrameworkListener(listener);
		try {
			ServiceReference[] refs = null;
			try {
				refs = OSGiTestsActivator.getContext().getServiceReferences(Runnable.class.getName(), "(name=testServiceException03)"); //$NON-NLS-1$
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
		// test a service factory which throws an Error
		Error cause = new Error("testServiceException04"); //$NON-NLS-1$ 
		ServiceExceptionServiceFactory errorFactory = new ServiceExceptionServiceFactory(cause);
		Hashtable props = new Hashtable();
		props.put("name", "testServiceException03"); //$NON-NLS-1$ //$NON-NLS-2$
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), errorFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getContext().getBundle(), cause, ServiceException.FACTORY_EXCEPTION);
		OSGiTestsActivator.getContext().addFrameworkListener(listener);
		try {
			ServiceReference[] refs = null;
			try {
				refs = OSGiTestsActivator.getContext().getServiceReferences(Runnable.class.getName(), "(name=testServiceException03)"); //$NON-NLS-1$
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

	private void clearResults(boolean[] results) {
		for (int i = 0; i < results.length; i++)
			results[i] = false;
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
