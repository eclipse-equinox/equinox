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
package org.eclipse.osgi.tests.serviceregistry;

import java.util.*;
import junit.framework.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.*;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.PublishHook;

public class ServiceHookTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(ServiceHookTests.class);
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

	public void testPublishHook01() {
		final String testMethodName = "testPublishHook01"; //$NON-NLS-1$
		// test the FindHook is called and can remove a reference from the results
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = OSGiTestsActivator.getContext();

		final int[] hookCalled = new int[] {0, 0};
		final AssertionFailedError[] hookError = new AssertionFailedError[] {null};
		final List events = new ArrayList();

		final ServiceListener sl = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				synchronized (events) {
					events.add(event);
				}
			}
		};

		final String filterString = "(&(name=" + testMethodName + ")(objectClass=java.lang.Runnable))"; //$NON-NLS-1$ //$NON-NLS-2$
		Filter tmpFilter = null;
		try {
			tmpFilter = testContext.createFilter(filterString);
			testContext.addServiceListener(sl, filterString);
		} catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e); //$NON-NLS-1$
		}

		final Filter filter = tmpFilter;
		PublishHook hook1 = new PublishHook() {
			public void event(ServiceEvent event, Collection contexts) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertTrue("does not contain test context", contexts.contains(testContext)); //$NON-NLS-1$

					try {
						contexts.add(testContext.getBundle(0).getBundleContext());
						fail("add to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
					try {
						contexts.addAll(Arrays.asList(new BundleContext[] {testContext.getBundle(0).getBundleContext()}));
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
		};
		PublishHook hook2 = new PublishHook() {
			public void event(ServiceEvent event, Collection contexts) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertTrue("does not contain test context", contexts.contains(testContext)); //$NON-NLS-1$
					contexts.remove(testContext);
					try {
						contexts.add(testContext.getBundle(0).getBundleContext());
						fail("add to collection succeeded"); //$NON-NLS-1$
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e); //$NON-NLS-1$
					}
					try {
						contexts.addAll(Arrays.asList(new BundleContext[] {testContext.getBundle(0).getBundleContext()}));
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
		};

		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		// register publish hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "publish hook 1"); //$NON-NLS-1$
		ServiceRegistration regHook = testContext.registerService(PublishHook.class.getName(), hook1, props);

		ServiceRegistration reg1 = null;
		try {
			props.put(Constants.SERVICE_DESCRIPTION, "service 1"); //$NON-NLS-1$
			synchronized (events) {
				events.clear();
			}
			reg1 = testContext.registerService(Runnable.class.getName(), runIt, props);
			assertEquals("all hooks not called", 1, hookCalled[0]); //$NON-NLS-1$
			assertEquals("hook 1 not called first", 1, hookCalled[1]); //$NON-NLS-1$
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}
			synchronized (events) {
				assertEquals("listener not called once", 1, events.size()); //$NON-NLS-1$
				Iterator iter = events.iterator();
				while (iter.hasNext()) {
					ServiceEvent event = (ServiceEvent) iter.next();
					assertEquals("type not registered", ServiceEvent.REGISTERED, event.getType()); //$NON-NLS-1$
					assertEquals("wrong service", reg1.getReference(), event.getServiceReference()); //$NON-NLS-1$
				}
			}

			regHook.unregister();
			regHook = null;

			synchronized (events) {
				events.clear();
			}
			hookCalled[0] = 0;
			props.put(Constants.SERVICE_DESCRIPTION, "service 2"); //$NON-NLS-1$
			reg1.setProperties(props);
			synchronized (events) {
				assertEquals("listener not called once", 1, events.size()); //$NON-NLS-1$
				Iterator iter = events.iterator();
				while (iter.hasNext()) {
					ServiceEvent event = (ServiceEvent) iter.next();
					assertEquals("type not registered", ServiceEvent.MODIFIED, event.getType()); //$NON-NLS-1$
					assertEquals("wrong service", reg1.getReference(), event.getServiceReference()); //$NON-NLS-1$
				}
			}
			assertEquals("hooks called", 0, hookCalled[0]); //$NON-NLS-1$

			props.put(Constants.SERVICE_DESCRIPTION, "publish hook 2"); //$NON-NLS-1$
			regHook = testContext.registerService(PublishHook.class.getName(), hook2, props);

			synchronized (events) {
				events.clear();
			}
			hookCalled[0] = 0;
			reg1.unregister();
			reg1 = null;
			synchronized (events) {
				assertEquals("listener called", 0, events.size()); //$NON-NLS-1$
			}
			assertEquals("all hooks not called", 1, hookCalled[0]); //$NON-NLS-1$
			assertEquals("hook 1 not called first", 1, hookCalled[1]); //$NON-NLS-1$
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}

		} finally {
			// unregister hook and services
			if (regHook != null)
				regHook.unregister();
			if (reg1 != null)
				reg1.unregister();
			if (sl != null)
				testContext.removeServiceListener(sl);
		}
	}
}
