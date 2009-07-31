/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.osgi.tests.debugoptions;

import java.util.*;
import java.util.Map.Entry;
import junit.framework.*;
import org.eclipse.osgi.framework.debug.FrameworkDebugTraceEntry;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class DebugOptionsTestCase extends TestCase {
	public static Test suite() {
		return new TestSuite(DebugOptionsTestCase.class);
	}

	DebugOptions debugOptions;
	ServiceReference ref;
	Dictionary props = null;
	TestDebugOptionsListener listener = null;
	ServiceRegistration reg = null;

	protected void setUp() throws Exception {
		ref = OSGiTestsActivator.getContext().getServiceReference(DebugOptions.class.getName());
		assertNotNull("DebugOptions service is not available", ref); //$NON-NLS-1$
		debugOptions = (DebugOptions) OSGiTestsActivator.getContext().getService(ref);
		assertNotNull("DebugOptions service is not available", debugOptions); //$NON-NLS-1$
		props = new Hashtable();
		props.put(DebugOptions.LISTENER_SYMBOLICNAME, getName());
		listener = new TestDebugOptionsListener();
		reg = OSGiTestsActivator.getContext().registerService(DebugOptionsListener.class.getName(), listener, props);
	}

	protected void tearDown() throws Exception {
		if (debugOptions == null)
			return;
		debugOptions.setDebugEnabled(false);
		debugOptions = null;
		OSGiTestsActivator.getContext().ungetService(ref);
		if (reg != null)
			reg.unregister();
	}

	public void testRegistration01() {
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
	}

	/**
	 * Test that a new {@link FrameworkDebugTraceEntry} object created without a trace class
	 * has 'org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase' as the class name and
	 * 'testTracingEntry01' as the method name that it determined as the caller of it.
	 * 
	 * This test mimics the tracing framework to ensure that the correct class name and method name
	 * are returned and written to the trace file.
	 */
	public void testTracingEntry01() {

		String bundleName = OSGiTestsActivator.getContext().getBundle().getSymbolicName();
		String optionPath = "/debug"; //$NON-NLS-1$
		String message = "Test message"; //$NON-NLS-1$
		FrameworkDebugTraceEntry traceEntry = new FrameworkDebugTraceEntry(bundleName, optionPath, message, null);
		String correctClassName = "org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase"; //$NON-NLS-1$
		String correctMethodName = "testTracingEntry01"; //$NON-NLS-1$
		assertEquals("The class calling the trace API does not match the expected value.", correctClassName, traceEntry.getClassName()); //$NON-NLS-1$  
		assertEquals("The method calling the trace API does not match the expected value.", correctMethodName, traceEntry.getMethodName()); //$NON-NLS-1$
	}

	/**
	 * Test that a new {@link FrameworkDebugTraceEntry} object created with a trace class
	 * of 'org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase' has the correct class name and
	 * method name of the caller.
	 * 
	 * This test mimics the tracing framework to ensure that the correct class name and method name
	 * are returned and written to the trace file.
	 */
	public void testTracingEntry02() {

		String correctClassName = Runner1.class.getName();
		String correctMethodName = "run"; //$NON-NLS-1$
		FrameworkDebugTraceEntry traceEntry = new Runner1().run();
		assertEquals("The class calling the trace API does not match the expected value.", correctClassName, traceEntry.getClassName()); //$NON-NLS-1$  
		assertEquals("The method calling the trace API does not match the expected value.", correctMethodName, traceEntry.getMethodName()); //$NON-NLS-1$
	}

	static class Runner1 {
		public FrameworkDebugTraceEntry run() {
			return new Runner2().run();
		}
	}

	static class Runner2 {
		public FrameworkDebugTraceEntry run() {
			String bundleName = OSGiTestsActivator.getContext().getBundle().getSymbolicName();
			String optionPath = "/debug"; //$NON-NLS-1$
			String message = "Test message"; //$NON-NLS-1$
			Class tracingClass = this.getClass();
			return new FrameworkDebugTraceEntry(bundleName, optionPath, message, tracingClass);
		}
	}

	public void testDyanmicEnablement01() {
		listener.clear();
		if (debugOptions.isDebugEnabled())
			return; // cannot test
		debugOptions.setDebugEnabled(true);
		assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$
		Map checkValues = new HashMap();
		checkValues.put(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		listener.setCheckValues(checkValues);
		debugOptions.setOption(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertNull("Found bad value: " + listener.getIncorrectValue(), listener.getIncorrectValue()); //$NON-NLS-1$
	}

	public void testDyanmicEnablement02() {
		listener.clear();
		if (debugOptions.isDebugEnabled())
			return; // cannot test
		debugOptions.setDebugEnabled(true);
		assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$
		Map checkValues = new HashMap();
		checkValues.put(getName() + "/debug", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		listener.setCheckValues(checkValues);
		debugOptions.setOption(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertNotNull("Should find bad value: " + listener.getIncorrectValue(), listener.getIncorrectValue()); //$NON-NLS-1$
	}

	public void testDyanmicEnablement03() {
		listener.clear();
		if (debugOptions.isDebugEnabled())
			return; // cannot test
		TestDebugOptionsListener anotherListener = new TestDebugOptionsListener();
		Dictionary anotherProps = new Hashtable();
		anotherProps.put(DebugOptions.LISTENER_SYMBOLICNAME, "anotherListener"); //$NON-NLS-1$
		ServiceRegistration anotherReg = OSGiTestsActivator.getContext().registerService(DebugOptionsListener.class.getName(), anotherListener, anotherProps);
		assertTrue("Not called", anotherListener.gotCalled()); //$NON-NLS-1$
		anotherListener.clear();

		debugOptions.setDebugEnabled(true);
		assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$

		Map checkValues = new HashMap();
		checkValues.put(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		listener.setCheckValues(checkValues);
		debugOptions.setOption(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("Should not call wrong listener", anotherListener.gotCalled()); //$NON-NLS-1$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertNull("Found bad value: " + listener.getIncorrectValue(), listener.getIncorrectValue()); //$NON-NLS-1$
		listener.clear();
		anotherListener.clear();
		debugOptions.setOption("anotherListener/test", "blah"); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("Listener should not have been called", listener.gotCalled()); //$NON-NLS-1$
		assertTrue("Another listener should have been called", anotherListener.gotCalled()); //$NON-NLS-1$

		listener.clear();
		anotherListener.clear();
		anotherProps.put(DebugOptions.LISTENER_SYMBOLICNAME, getName());
		anotherReg.setProperties(anotherProps);
		debugOptions.setOption(getName() + "/debug", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Listener did not get called", listener.gotCalled()); //$NON-NLS-1$
		assertTrue("Another listener did not get called", anotherListener.gotCalled()); //$NON-NLS-1$

		anotherReg.unregister();
	}

	class TestDebugOptionsListener implements DebugOptionsListener {
		boolean called = false;
		String incorrectValue;
		Map checkValues;

		public void optionsChanged(DebugOptions options) {
			called = true;
			if (checkValues == null)
				return;
			for (Iterator entries = checkValues.entrySet().iterator(); entries.hasNext();) {
				Map.Entry entry = (Entry) entries.next();
				String debugValue = options.getOption((String) entry.getKey());
				String error = "Value is inccorect for key: " + entry.getKey() + " " + debugValue; //$NON-NLS-1$//$NON-NLS-2$
				if (debugValue == null) {
					if (entry.getValue() != null) {
						incorrectValue = error;
						return;
					}
					continue;
				}
				if (!debugValue.equals(entry.getValue())) {
					incorrectValue = error;
					return;
				}
			}
		}

		public boolean gotCalled() {
			return called;
		}

		public void clear() {
			called = false;
			checkValues = null;
			incorrectValue = null;
		}

		public void setCheckValues(Map checkValues) {
			this.checkValues = checkValues;
		}

		public String getIncorrectValue() {
			return incorrectValue;
		}
	}
}
