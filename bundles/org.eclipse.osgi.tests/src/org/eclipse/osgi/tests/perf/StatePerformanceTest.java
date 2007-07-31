/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.perf;

import java.io.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.osgi.service.resolver.State;

public class StatePerformanceTest extends BasePerformanceTest {

	public static Test suite() {
		return new TestSuite(StatePerformanceTest.class);
	}

	public StatePerformanceTest(String name) {
		super(name);
	}

	private State storeAndRetrieve(State toStore) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		toStore.getFactory().writeState(toStore, baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return toStore.getFactory().readState(bais);
	}

	public void testCreation() {
		final int stateSize = 5000;
		new PerformanceTestRunner() {
			protected void test() {
				buildRandomState(stateSize);
			}
		}.run(this, 10, 10);
	}

	private void testResolution(int stateSize, int repetitions, String localName) {
		final State originalState = buildRandomState(stateSize);
		new PerformanceTestRunner() {
			protected void test() {
				originalState.resolve(false);
			}
		}.run(this, localName, 10, repetitions);
	}

	public void testResolution100() throws IOException {
		testResolution(100, 500, null);
	}

	public void testResolution1000() throws IOException {
		testResolution(1000, 15, "State Resolution");
	}

	public void testResolution500() throws IOException {
		testResolution(500, 50, null);
	}

	public void testResolution5000() throws IOException {
		testResolution(5000, 1, null);
	}

	public void testStoreAndRetrieve() {
		int stateSize = 5000;
		final State originalState = buildRandomState(stateSize);
		new PerformanceTestRunner() {
			protected void test() {
				try {
					storeAndRetrieve(originalState);
				} catch (IOException e) {
					CoreTest.fail("", e);
				}
			}
		}.run(this, 10, 10);
	}

}
