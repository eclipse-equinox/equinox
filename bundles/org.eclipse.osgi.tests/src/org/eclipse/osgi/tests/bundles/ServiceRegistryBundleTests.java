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

import java.util.*;
import junit.framework.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServiceRegistryBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(ServiceRegistryBundleTests.class);
	}

	public void testServiceTracker01() {
		final String testMethodName = "testServiceTracker01"; //$NON-NLS-1$
		// simple ServiceTracker test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put(testMethodName, Boolean.TRUE);
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
				testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), FrameworkUtil.createFilter("(&(objectClass=java.lang.Runnable)(" + testMethodName + "=true))"), testCustomizer); //$NON-NLS-1$ //$NON-NLS-2$
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
			props.put(testMethodName, Boolean.FALSE);
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
			props.put(testMethodName, Boolean.TRUE);
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
		final String testMethodName = "testServiceTracker02"; //$NON-NLS-1$
		// simple ServiceTracker test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put(testMethodName, Boolean.FALSE);
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
				testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), FrameworkUtil.createFilter("(&(objectClass=java.lang.Runnable)(" + testMethodName + "=true))"), testCustomizer); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("filter error", e); //$NON-NLS-1$
			}
			testTracker.open();
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to match
			props.put(testMethodName, Boolean.TRUE);
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
			props.put(testMethodName, Boolean.FALSE);
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
		final String testMethodName = "testServiceListener01"; //$NON-NLS-1$
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
			OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectClass=java.lang.Runnable)(" + testMethodName + "=true))"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InvalidSyntaxException e) {
			fail("filter error", e); //$NON-NLS-1$
		}
		ServiceRegistration reg = null;
		try {
			// register service which matches
			Hashtable props = new Hashtable();
			props.put(testMethodName, Boolean.TRUE);
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
			props.put(testMethodName, Boolean.FALSE);
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
			props.put(testMethodName, Boolean.TRUE);
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
		final String testMethodName = "testServiceListener02"; //$NON-NLS-1$
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
			OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectClass=java.lang.Runnable)(" + testMethodName + "=true))"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InvalidSyntaxException e) {
			fail("filter error", e); //$NON-NLS-1$
		}
		ServiceRegistration reg = null;
		try {
			// register service which does not match
			Hashtable props = new Hashtable();
			props.put(testMethodName, Boolean.FALSE);
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
			props.put(testMethodName, Boolean.TRUE);
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
			props.put(testMethodName, Boolean.FALSE);
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
		final String testMethodName = "testServiceException01"; //$NON-NLS-1$
		// test a service factory which returns wrong object types
		ServiceExceptionServiceFactory wrongObjectFactory = new ServiceExceptionServiceFactory("A String"); //$NON-NLS-1$
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$ 
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), wrongObjectFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getContext().getBundle(), null, ServiceException.FACTORY_ERROR);
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
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getContext().getBundle(), null, ServiceException.FACTORY_ERROR);
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
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getContext().getBundle(), cause, ServiceException.FACTORY_EXCEPTION);
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
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(OSGiTestsActivator.getContext().getBundle(), cause, ServiceException.FACTORY_EXCEPTION);
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

	public void testServiceOrdering01() {
		final String testMethodName = "testServiceOrdering01"; //$NON-NLS-1$
		// test that getServiceReference returns the proper service
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$ 
		props.put(Constants.SERVICE_DESCRIPTION, "min value"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
		ServiceRegistration reg1 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value first"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		ServiceRegistration reg2 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value second"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		ServiceRegistration reg3 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		try {
			ServiceReference ref = null;
			ref = OSGiTestsActivator.getContext().getServiceReference(Runnable.class.getName());
			assertNotNull("service ref is null", ref); //$NON-NLS-1$
			assertEquals("Wrong references", reg2.getReference(), ref); //$NON-NLS-1$
		} finally {
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
			if (reg3 != null)
				reg3.unregister();
		}
	}

	public void testFindHook01() {
		final String testMethodName = "testFindHook01"; //$NON-NLS-1$
		// test the FindHook is called and can remove a reference from the results
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = OSGiTestsActivator.getContext();
		// register services
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "service 1"); //$NON-NLS-1$
		final ServiceRegistration reg1 = testContext.registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "service 2"); //$NON-NLS-1$
		final ServiceRegistration reg2 = testContext.registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "service 3"); //$NON-NLS-1$
		final ServiceRegistration reg3 = testContext.registerService(Runnable.class.getName(), runIt, props);

		final int[] hookCalled = new int[] {0, 0, 0, 0, 0};
		final AssertionFailedError[] hookError = new AssertionFailedError[] {null, null, null, null};

		// register find hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "find hook 1"); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "min value"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
		ServiceRegistration regHook1 = testContext.registerService(FindHook.class.getName(), new FindHook() {
			public void find(BundleContext context, String name, String filter, boolean allServices, Collection references) {
				try {
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertEquals("wrong context in hook", testContext, context); //$NON-NLS-1$
					assertEquals("wrong name in hook", Runnable.class.getName(), name); //$NON-NLS-1$
					assertEquals("wrong filter in hook", "(name=" + testMethodName + ")", filter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					assertEquals("wrong allservices in hook", false, allServices); //$NON-NLS-1$
					assertEquals("wrong number of services in hook", 1, references.size()); //$NON-NLS-1$
					Iterator iter = references.iterator();
					while (iter.hasNext()) {
						ServiceReference ref = (ServiceReference) iter.next();
						if (ref.equals(reg1.getReference())) {
							fail("service 1 is present"); //$NON-NLS-1$
						}
						if (ref.equals(reg2.getReference())) {
							fail("service 2 is present"); //$NON-NLS-1$
						}
					}

					try {
						references.add(reg1.getReference());
						fail("add to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
					try {
						references.addAll(Arrays.asList(new ServiceReference[] {reg1.getReference()}));
						fail("addAll to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
				} catch (AssertionFailedError a) {
					hookError[0] = a;
					return;
				}
			}
		}, props);

		// register find hook 2
		props.put(Constants.SERVICE_DESCRIPTION, "find hook 2"); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "max value first"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		ServiceRegistration regHook2 = testContext.registerService(FindHook.class.getName(), new FindHook() {
			public void find(BundleContext context, String name, String filter, boolean allServices, Collection references) {
				try {
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 2;
					}
					assertEquals("wrong context in hook", testContext, context); //$NON-NLS-1$
					assertEquals("wrong name in hook", Runnable.class.getName(), name); //$NON-NLS-1$
					assertEquals("wrong filter in hook", "(name=" + testMethodName + ")", filter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					assertEquals("wrong allservices in hook", false, allServices); //$NON-NLS-1$
					assertEquals("wrong number of services in hook", 3, references.size()); //$NON-NLS-1$
					Iterator iter = references.iterator();
					while (iter.hasNext()) {
						ServiceReference ref = (ServiceReference) iter.next();
						if (ref.equals(reg2.getReference())) {
							iter.remove();
						}
					}

					try {
						references.add(reg2.getReference());
						fail("add to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
					try {
						references.addAll(Arrays.asList(new ServiceReference[] {reg2.getReference()}));
						fail("addAll to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
				} catch (AssertionFailedError a) {
					hookError[1] = a;
					return;
				}
			}
		}, props);

		// register find hook 3
		props.put(Constants.SERVICE_DESCRIPTION, "find hook 3"); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "max value second"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		ServiceRegistration regHook3 = testContext.registerService(FindHook.class.getName(), new FindHook() {
			public void find(BundleContext context, String name, String filter, boolean allServices, Collection references) {
				try {
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 3;
					}
					assertEquals("wrong context in hook", testContext, context); //$NON-NLS-1$
					assertEquals("wrong name in hook", Runnable.class.getName(), name); //$NON-NLS-1$
					assertEquals("wrong filter in hook", "(name=" + testMethodName + ")", filter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					assertEquals("wrong allservices in hook", false, allServices); //$NON-NLS-1$
					assertEquals("wrong number of services in hook", 2, references.size()); //$NON-NLS-1$
					Iterator iter = references.iterator();
					while (iter.hasNext()) {
						ServiceReference ref = (ServiceReference) iter.next();
						if (ref.equals(reg2.getReference())) {
							fail("service 2 is present"); //$NON-NLS-1$
						}
					}

					try {
						references.add(reg2.getReference());
						fail("add to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
					try {
						references.addAll(Arrays.asList(new ServiceReference[] {reg2.getReference()}));
						fail("addAll to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
				} catch (AssertionFailedError a) {
					hookError[2] = a;
					return;
				}
				// throw an exception from the hook to test that the next hooks are called.
				throw new RuntimeException(testMethodName);
			}
		}, props);

		// register find hook 4
		props.put(Constants.SERVICE_DESCRIPTION, "find hook 4"); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "max value third"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		ServiceRegistration regHook4 = testContext.registerService(FindHook.class.getName(), new FindHook() {
			public void find(BundleContext context, String name, String filter, boolean allServices, Collection references) {
				try {
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 4;
					}
					assertEquals("wrong context in hook", testContext, context); //$NON-NLS-1$
					assertEquals("wrong name in hook", Runnable.class.getName(), name); //$NON-NLS-1$
					assertEquals("wrong filter in hook", "(name=" + testMethodName + ")", filter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					assertEquals("wrong allservices in hook", false, allServices); //$NON-NLS-1$
					assertEquals("wrong number of services in hook", 2, references.size()); //$NON-NLS-1$
					Iterator iter = references.iterator();
					while (iter.hasNext()) {
						ServiceReference ref = (ServiceReference) iter.next();
						if (ref.equals(reg1.getReference())) {
							iter.remove();
						}
						if (ref.equals(reg2.getReference())) {
							fail("service 2 is present"); //$NON-NLS-1$
						}
					}

					try {
						references.add(reg2.getReference());
						fail("add to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
					try {
						references.addAll(Arrays.asList(new ServiceReference[] {reg2.getReference()}));
						fail("addAll to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
				} catch (AssertionFailedError a) {
					hookError[3] = a;
					return;
				}
			}
		}, props);

		// get reference and hook removes some services
		try {
			ServiceReference[] refs = null;
			try {
				refs = testContext.getServiceReferences(Runnable.class.getName(), "(name=" + testMethodName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertEquals("all hooks not called", 4, hookCalled[0]); //$NON-NLS-1$
			assertEquals("hook 2 not called first", 2, hookCalled[1]); //$NON-NLS-1$
			assertEquals("hook 3 not called second", 3, hookCalled[2]); //$NON-NLS-1$
			assertEquals("hook 4 not called third", 4, hookCalled[3]); //$NON-NLS-1$
			assertEquals("hook 1 not called fourth ", 1, hookCalled[4]); //$NON-NLS-1$
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}
			assertNotNull("service refs is null", refs); //$NON-NLS-1$
			assertEquals("Wrong number of references", 1, refs.length); //$NON-NLS-1$

			// test removed services are not in the result
			List refList = Arrays.asList(refs);
			assertFalse("contains service 1", refList.contains(reg1.getReference())); //$NON-NLS-1$
			assertFalse("contains service 2", refList.contains(reg2.getReference())); //$NON-NLS-1$
			assertTrue("missing service 3", refList.contains(reg3.getReference())); //$NON-NLS-1$

			// remove the hooks
			regHook1.unregister();
			regHook1 = null;
			regHook2.unregister();
			regHook2 = null;
			regHook3.unregister();
			regHook3 = null;
			regHook4.unregister();
			regHook4 = null;

			// get services and make sure none are filtered
			refs = null;
			hookCalled[0] = 0;

			try {
				refs = testContext.getServiceReferences(Runnable.class.getName(), "(name=" + testMethodName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertEquals("hooks called", 0, hookCalled[0]); //$NON-NLS-1$
			assertNotNull("service refs is null", refs); //$NON-NLS-1$
			assertEquals("Wrong number of references", 3, refs.length); //$NON-NLS-1$

			// test result contains all expected services
			refList = Arrays.asList(refs);
			assertTrue("missing service 1", refList.contains(reg1.getReference())); //$NON-NLS-1$
			assertTrue("missing service 2", refList.contains(reg2.getReference())); //$NON-NLS-1$
			assertTrue("missing service 3", refList.contains(reg3.getReference())); //$NON-NLS-1$
		} finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			if (regHook2 != null)
				regHook2.unregister();
			if (regHook3 != null)
				regHook3.unregister();
			if (regHook4 != null)
				regHook4.unregister();
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
			if (reg3 != null)
				reg3.unregister();
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
