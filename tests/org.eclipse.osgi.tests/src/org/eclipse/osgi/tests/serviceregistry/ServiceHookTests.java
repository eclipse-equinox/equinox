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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import junit.framework.AssertionFailedError;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;

public class ServiceHookTests extends AbstractBundleTests {

	@Test
	public void testFindHook01() throws InvalidSyntaxException {
		final String testMethodName = "testFindHook01"; //$NON-NLS-1$
		// test the FindHook is called and can remove a reference from the results
		Runnable runIt = () -> {
			// nothing
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
		final boolean[] startTest = new boolean[] {false};
		final AssertionFailedError[] hookErrors = new AssertionFailedError[] {null, null, null, null};

		// register find hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "find hook 1"); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "min value"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
		ServiceRegistration regHook1 = testContext.registerService(FindHook.class.getName(), (FindHook) (context, name, filter, allServices, references) -> {
			try {
				synchronized (hookCalled) {
					if (!startTest[0])
						return;
					hookCalled[++hookCalled[0]] = 1;
				}
				assertEquals("wrong context in hook", testContext, context); //$NON-NLS-1$
				assertEquals("wrong name in hook", Runnable.class.getName(), name); //$NON-NLS-1$
				assertEquals("wrong filter in hook", "(name=" + testMethodName + ")", filter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				assertEquals("wrong allservices in hook", false, allServices); //$NON-NLS-1$
				assertEquals("wrong number of services in hook", 1, references.size()); //$NON-NLS-1$
				for (ServiceReference ref : references) {
					assertNotEquals("service 1 is present", reg1.getReference(), ref);
					assertNotEquals("service 2 is present", reg2.getReference(), ref);
				}

				ServiceReference<?> reference1 = reg1.getReference();
				assertThrows("add to collection succeeded", UnsupportedOperationException.class,
						() -> references.add(reference1));
				assertThrows("addAll to collection succeeded", UnsupportedOperationException.class,
						() -> references.addAll(Arrays.asList(reference1)));
			} catch (AssertionFailedError a) {
				hookErrors[0] = a;
				return;
			}
		}, props);

		// register find hook 2
		props.put(Constants.SERVICE_DESCRIPTION, "find hook 2"); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "max value first"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration regHook2 = testContext.registerService(FindHook.class.getName(), (FindHook) (context, name, filter, allServices, references) -> {
			try {
				synchronized (hookCalled) {
					if (!startTest[0])
						return;
					hookCalled[++hookCalled[0]] = 2;
				}
				assertEquals("wrong context in hook", testContext, context); //$NON-NLS-1$
				assertEquals("wrong name in hook", Runnable.class.getName(), name); //$NON-NLS-1$
				assertEquals("wrong filter in hook", "(name=" + testMethodName + ")", filter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				assertEquals("wrong allservices in hook", false, allServices); //$NON-NLS-1$
				assertEquals("wrong number of services in hook", 3, references.size()); //$NON-NLS-1$

				references.removeIf(ref -> ref.equals(reg2.getReference()));

				ServiceReference<?> reference2 = reg2.getReference();
				assertThrows("add to collection succeeded", UnsupportedOperationException.class,
						() -> references.add(reference2));
				assertThrows("addAll to collection succeeded", UnsupportedOperationException.class,
						() -> references.addAll(Arrays.asList(reference2)));
			} catch (AssertionFailedError a) {
				hookErrors[1] = a;
				return;
			}
		}, props);

		// register find hook 3
		props.put(Constants.SERVICE_DESCRIPTION, "find hook 3"); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "max value second"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration regHook3 = testContext.registerService(FindHook.class.getName(), (FindHook) (context, name, filter, allServices, references) -> {
			try {
				synchronized (hookCalled) {
					if (!startTest[0])
						return;
					hookCalled[++hookCalled[0]] = 3;
				}
				assertEquals("wrong context in hook", testContext, context); //$NON-NLS-1$
				assertEquals("wrong name in hook", Runnable.class.getName(), name); //$NON-NLS-1$
				assertEquals("wrong filter in hook", "(name=" + testMethodName + ")", filter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				assertEquals("wrong allservices in hook", false, allServices); //$NON-NLS-1$
				assertEquals("wrong number of services in hook", 2, references.size()); //$NON-NLS-1$
				for (ServiceReference<?> ref : references) {
					assertNotEquals("service 2 is present", ref, reg2.getReference());
				}

				ServiceReference<?> ref2 = reg2.getReference();
				assertThrows(UnsupportedOperationException.class, () -> references.add(ref2));
				assertThrows(UnsupportedOperationException.class, () -> references.addAll(Arrays.asList(ref2)));
			} catch (AssertionFailedError a) {
				hookErrors[2] = a;
				return;
			}
			// throw an exception from the hook to test that the next hooks are called.
			throw new RuntimeException(testMethodName);
		}, props);

		// register find hook 4
		props.put(Constants.SERVICE_DESCRIPTION, "find hook 4"); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "max value third"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration regHook4 = testContext.registerService(FindHook.class.getName(), (FindHook) (context, name, filter, allServices, references) -> {
			try {
				synchronized (hookCalled) {
					if (!startTest[0])
						return;
					hookCalled[++hookCalled[0]] = 4;
				}
				assertEquals("wrong context in hook", testContext, context); //$NON-NLS-1$
				assertEquals("wrong name in hook", Runnable.class.getName(), name); //$NON-NLS-1$
				assertEquals("wrong filter in hook", "(name=" + testMethodName + ")", filter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				assertEquals("wrong allservices in hook", false, allServices); //$NON-NLS-1$
				assertEquals("wrong number of services in hook", 2, references.size()); //$NON-NLS-1$

				references.removeIf(ref -> {
					assertNotEquals("service 2 is present", ref, reg2.getReference());
					return ref.equals(reg1.getReference());
				});

				ServiceReference<?> ref2 = reg2.getReference();
				assertThrows(UnsupportedOperationException.class, () -> references.add(ref2));
				assertThrows(UnsupportedOperationException.class, () -> references.addAll(Arrays.asList(ref2)));
			} catch (AssertionFailedError a) {
				hookErrors[3] = a;
				return;
			}
		}, props);

		startTest[0] = true;
		// get reference and hook removes some services
		try {
			ServiceReference[] refs = testContext.getServiceReferences(Runnable.class.getName(),
					"(name=" + testMethodName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("all hooks not called", 4, hookCalled[0]); //$NON-NLS-1$
			assertEquals("hook 2 not called first", 2, hookCalled[1]); //$NON-NLS-1$
			assertEquals("hook 3 not called second", 3, hookCalled[2]); //$NON-NLS-1$
			assertEquals("hook 4 not called third", 4, hookCalled[3]); //$NON-NLS-1$
			assertEquals("hook 1 not called fourth ", 1, hookCalled[4]); //$NON-NLS-1$
			for (AssertionFailedError hookError : hookErrors) {
				if (hookError != null) {
					throw hookError;
				}
			}
			assertNotNull("service refs is null", refs); //$NON-NLS-1$
			assertEquals("Wrong number of references", 1, refs.length); //$NON-NLS-1$

			// test removed services are not in the result
			List refList = Arrays.asList(refs);
			assertFalse("contains service 1", refList.contains(reg1.getReference())); //$NON-NLS-1$
			assertFalse("contains service 2", refList.contains(reg2.getReference())); //$NON-NLS-1$
			assertTrue("missing service 3", refList.contains(reg3.getReference())); //$NON-NLS-1$

			startTest[0] = false;
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

			startTest[0] = true;
			refs = testContext.getServiceReferences(Runnable.class.getName(), "(name=" + testMethodName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
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

	@Test
	public void testEventHook01() throws InvalidSyntaxException {
		final String testMethodName = "testEventHook01"; //$NON-NLS-1$
		// test the EventHook is called and can remove a reference from the results
		Runnable runIt = () -> {
			// nothing
		};
		final BundleContext testContext = OSGiTestsActivator.getContext();

		final int[] hookCalled = new int[] {0, 0};
		final AssertionFailedError[] hookErrors = new AssertionFailedError[] {null};
		final List events = new ArrayList();

		final ServiceListener sl = event -> {
			synchronized (events) {
				events.add(event);
			}
		};

		final String filterString = "(&(name=" + testMethodName + ")(objectClass=java.lang.Runnable))"; //$NON-NLS-1$ //$NON-NLS-2$
		Filter tmpFilter = testContext.createFilter(filterString);
		testContext.addServiceListener(sl, filterString);

		final Filter filter = tmpFilter;
		EventHook hook1 = (event, contexts) -> {
			try {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				synchronized (hookCalled) {
					hookCalled[++hookCalled[0]] = 1;
				}
				assertTrue("does not contain test context", contexts.contains(testContext)); //$NON-NLS-1$

				assertThrows(UnsupportedOperationException.class,
						() -> contexts.add(testContext.getBundle(0).getBundleContext()));
				assertThrows(UnsupportedOperationException.class,
						() -> contexts.addAll(Arrays.asList(testContext.getBundle(0).getBundleContext())));
			} catch (AssertionFailedError a) {
				hookErrors[0] = a;
				return;
			}
		};
		EventHook hook2 = (event, contexts) -> {
			try {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				synchronized (hookCalled) {
					hookCalled[++hookCalled[0]] = 1;
				}
				assertTrue("does not contain test context", contexts.contains(testContext)); //$NON-NLS-1$
				contexts.remove(testContext);

				assertThrows(UnsupportedOperationException.class,
						() -> contexts.add(testContext.getBundle(0).getBundleContext()));
				assertThrows(UnsupportedOperationException.class,
						() -> contexts.addAll(Arrays.asList(testContext.getBundle(0).getBundleContext())));
			} catch (AssertionFailedError a) {
				hookErrors[0] = a;
				return;
			}
		};

		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		// register event hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 1"); //$NON-NLS-1$
		ServiceRegistration regHook = testContext.registerService(EventHook.class.getName(), hook1, props);

		ServiceRegistration reg1 = null;
		try {
			props.put(Constants.SERVICE_DESCRIPTION, "service 1"); //$NON-NLS-1$
			synchronized (events) {
				events.clear();
			}
			reg1 = testContext.registerService(Runnable.class.getName(), runIt, props);
			assertEquals("all hooks not called", 1, hookCalled[0]); //$NON-NLS-1$
			assertEquals("hook 1 not called first", 1, hookCalled[1]); //$NON-NLS-1$
			for (AssertionFailedError hookError : hookErrors) {
				if (hookError != null) {
					throw hookError;
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

			props.put(Constants.SERVICE_DESCRIPTION, "event hook 2"); //$NON-NLS-1$
			regHook = testContext.registerService(EventHook.class.getName(), hook2, props);

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
			for (AssertionFailedError hookError : hookErrors) {
				if (hookError != null) {
					throw hookError;
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

	@Test
	public void testListenerHook01() throws InvalidSyntaxException {
		final String testMethodName = "testListenerHook01"; //$NON-NLS-1$
		// test the ListenerHook is called
		final BundleContext testContext = OSGiTestsActivator.getContext();
		final Collection<ListenerHook.ListenerInfo> result = new ArrayList<>();
		final int[] hookCalled = new int[] {0, 0};

		ListenerHook hook1 = new ListenerHook() {
			public void added(Collection<ListenerHook.ListenerInfo> listeners) {
				synchronized (hookCalled) {
					hookCalled[0]++;
				}
				result.addAll(listeners);
			}

			public void removed(Collection<ListenerHook.ListenerInfo> listeners) {
				synchronized (hookCalled) {
					hookCalled[1]++;
				}
				result.removeAll(listeners);
			}
		};

		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		// register listener hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "listener hook 1"); //$NON-NLS-1$
		ServiceRegistration regHook = testContext.registerService(ListenerHook.class.getName(), hook1, props);

		try {
			assertFalse("no service listeners found", result.isEmpty()); //$NON-NLS-1$
			assertEquals("added not called", 1, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed called", 0, hookCalled[1]); //$NON-NLS-1$

			int size = result.size();
			ServiceListener testSL = event -> {
				// do nothing
			};
			String filterString1 = "(foo=bar)"; //$NON-NLS-1$
			testContext.addServiceListener(testSL, filterString1);
			assertEquals("added not called", 2, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed called", 0, hookCalled[1]); //$NON-NLS-1$
			assertEquals("listener not added", size + 1, result.size()); //$NON-NLS-1$
			boolean found = false;
			for (ListenerHook.ListenerInfo info : result) {
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((c == testContext) && (filterString1.equals(f))) {
					assertFalse("found more than once", found);
					found = true;
				}
			}
			assertTrue("listener not found", found);

			String filterString2 = "(bar=foo)"; //$NON-NLS-1$
			testContext.addServiceListener(testSL, filterString2);
			assertEquals("added not called", 3, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed not called", 1, hookCalled[1]); //$NON-NLS-1$
			assertEquals("listener not removed and added", size + 1, result.size()); //$NON-NLS-1$
			found = false;
			for (ListenerHook.ListenerInfo info : result) {
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((c == testContext) && (filterString2.equals(f))) {
					assertFalse("found more than once", found);
					found = true;
				}
				assertFalse("first listener not removed", c == testContext && filterString1.equals(f));
			}
			assertTrue("listener not found", found);

			testContext.removeServiceListener(testSL);
			assertEquals("added called", 3, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed not called", 2, hookCalled[1]); //$NON-NLS-1$
			assertEquals("listener not removed", size, result.size()); //$NON-NLS-1$
			for (ListenerHook.ListenerInfo info : result) {
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				assertFalse("second listener not removed", c == testContext && filterString2.equals(f));
			}

			testContext.removeServiceListener(testSL);
			assertEquals("added called", 3, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed called", 2, hookCalled[1]); //$NON-NLS-1$
			assertEquals("listener removed", size, result.size()); //$NON-NLS-1$

		} finally {
			if (regHook != null) {
				regHook.unregister();
			}
		}
	}

	@Test
	public void testListenerHook02() throws InvalidSyntaxException {
		final String testMethodName = "testListenerHook02"; //$NON-NLS-1$
		// test the ListenerHook works with the FilteredServiceListener optimization in equinox
		final BundleContext testContext = OSGiTestsActivator.getContext();
		final Collection<ListenerHook.ListenerInfo> result = new ArrayList<>();
		final int[] hookCalled = new int[] {0, 0};

		ListenerHook hook1 = new ListenerHook() {
			public void added(Collection<ListenerHook.ListenerInfo> listeners) {
				synchronized (hookCalled) {
					hookCalled[0]++;
				}
				result.addAll(listeners);
			}

			public void removed(Collection<ListenerHook.ListenerInfo> listeners) {
				synchronized (hookCalled) {
					hookCalled[1]++;
				}
				result.removeAll(listeners);
			}
		};

		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		// register listener hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "listener hook 1"); //$NON-NLS-1$
		ServiceRegistration regHook = testContext.registerService(ListenerHook.class.getName(), hook1, props);

		try {
			assertFalse("no service listeners found", result.isEmpty()); //$NON-NLS-1$
			assertEquals("added not called", 1, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed called", 0, hookCalled[1]); //$NON-NLS-1$

			int size = result.size();
			ServiceListener testSL = event -> {
				// do nothing
			};
			String filterString1 = "(" + Constants.OBJECTCLASS + "=bar)"; //$NON-NLS-1$ //$NON-NLS-2$
			testContext.addServiceListener(testSL, filterString1);
			assertEquals("added not called", 2, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed called", 0, hookCalled[1]); //$NON-NLS-1$
			assertEquals("listener not added", size + 1, result.size()); //$NON-NLS-1$
			boolean found = false;
			for (ListenerHook.ListenerInfo info : result) {
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((c == testContext) && (filterString1.equals(f))) {
					assertFalse("found more than once", found);
					found = true;
				}
			}
			assertTrue("listener not found", found);

			String filterString2 = null;
			testContext.addServiceListener(testSL);
			assertEquals("added not called", 3, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed not called", 1, hookCalled[1]); //$NON-NLS-1$
			assertEquals("listener not removed and added", size + 1, result.size()); //$NON-NLS-1$
			found = false;
			for (ListenerHook.ListenerInfo info : result) {
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((c == testContext) && (f == filterString2)) {
					assertFalse("found more than once", found);
					found = true;
				}
				assertFalse("first listener not removed", c == testContext && filterString1.equals(f));
			}
			assertTrue("listener not found", found);

			testContext.removeServiceListener(testSL);
			assertEquals("added called", 3, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed not called", 2, hookCalled[1]); //$NON-NLS-1$
			assertEquals("listener not removed", size, result.size()); //$NON-NLS-1$
			for (ListenerHook.ListenerInfo info : result) {
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				assertFalse("second listener not removed", c == testContext && f == filterString2);
			}

			testContext.removeServiceListener(testSL);
			assertEquals("added called", 3, hookCalled[0]); //$NON-NLS-1$
			assertEquals("removed called", 2, hookCalled[1]); //$NON-NLS-1$
			assertEquals("listener removed", size, result.size()); //$NON-NLS-1$

		} finally {
			if (regHook != null) {
				regHook.unregister();
			}
		}
	}
}
