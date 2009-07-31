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

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.framework.debug.FrameworkDebugTraceEntry;
import org.eclipse.osgi.service.debug.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class DebugOptionsTestCase extends CoreTest {
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
			String tracingClass = this.getClass().getName();
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

	public void testTraceFile01() {
		if (debugOptions.isDebugEnabled())
			return; // cannot test
		debugOptions.setDebugEnabled(true);
		assertTrue("Debug is not enabled", debugOptions.isDebugEnabled()); //$NON-NLS-1$
		debugOptions.setOption(getName() + "/debug", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		File traceFile = OSGiTestsActivator.getContext().getDataFile(getName() + ".trace"); //$NON-NLS-1$
		debugOptions.setFile(traceFile);

		DebugTrace wrapped = debugOptions.newDebugTrace(getName(), TestDebugTrace.class);
		TestDebugTrace debugTrace = new TestDebugTrace(wrapped);
		debugTrace.trace("/debug", "testing 1"); //$NON-NLS-1$ //$NON-NLS-2$
		debugTrace.trace("/debug", "testing 2"); //$NON-NLS-1$ //$NON-NLS-2$
		debugTrace.trace("/notset", "testing 3"); //$NON-NLS-1$ //$NON-NLS-2$
		String[][] traceOutput = readTraceFile(traceFile);
		assertEquals("Wrong number of trace entries", 2, traceOutput.length); //$NON-NLS-1$
		assertEquals("Wrong entry length", 8, traceOutput[0].length); //$NON-NLS-1$
		assertEquals("Wrong message", "testing 1", traceOutput[0][7]); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Wrong entry length", 8, traceOutput[1].length); //$NON-NLS-1$
		assertEquals("Wrong message", "testing 2", traceOutput[1][7]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String[][] readTraceFile(File traceFile) {
		ArrayList result = new ArrayList();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(traceFile));
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				if (line.startsWith("#")) //$NON-NLS-1$
					continue;
				StringTokenizer st = new StringTokenizer(line, "|"); //$NON-NLS-1$
				int count = st.countTokens();
				String[] entry = new String[count];
				for (int i = 0; i < entry.length; i++)
					entry[i] = st.nextToken().trim();
				result.add(entry);
			}
		} catch (IOException e) {
			fail("Failed to read trace file", e); //$NON-NLS-1$
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					// nothing;
				}
		}
		return (String[][]) result.toArray(new String[result.size()][]);
	}

	static class TestDebugTrace implements DebugTrace {
		private final DebugTrace wrapped;

		public TestDebugTrace(DebugTrace wrapped) {
			this.wrapped = wrapped;
		}

		public void trace(String option, String message) {
			wrapped.trace(option, message);
		}

		public void trace(String option, String message, Throwable error) {
			wrapped.trace(option, message, error);
		}

		public void traceDumpStack(String option) {
			wrapped.traceDumpStack(option);
		}

		public void traceEntry(String option) {
			wrapped.traceEntry(option);
		}

		public void traceEntry(String option, Object methodArgument) {
			wrapped.traceEntry(option, methodArgument);
		}

		public void traceEntry(String option, Object[] methodArguments) {
			wrapped.traceEntry(option, methodArguments);
		}

		public void traceExit(String option) {
			wrapped.traceExit(option);
		}

		public void traceExit(String option, Object result) {
			wrapped.traceExit(option, result);
		}
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
