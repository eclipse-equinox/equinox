/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.perf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.osgi.service.resolver.State;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class StatePerformanceTest extends BasePerformanceTest {

	@Rule
	public TestName testName = new TestName();

	@SuppressWarnings("deprecation") // writeState
	State storeAndRetrieve(State toStore) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		toStore.getFactory().writeState(toStore, baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return toStore.getFactory().readState(bais);
	}

	@Test
	public void testCreation() throws Exception {
		final int stateSize = 5000;
		new PerformanceTestRunner() {
			protected void test() {
				buildRandomState(stateSize);
			}
		}.run(getClass(), testName.getMethodName(), 10, 10);
	}

	private void testResolution(int stateSize, int repetitions, String degradation) throws Exception {
		final State originalState = buildRandomState(stateSize);
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() {
				originalState.resolve(false);
			}
		};
		runner.setRegressionReason(degradation);
		runner.run(getClass(), testName.getMethodName(), 10, repetitions);
	}

	@Test
	public void testResolution100() throws Exception {
		testResolution(100, 500, AllTests.DEGRADATION_RESOLUTION);
	}

	@Test
	public void testResolution1000() throws Exception {
		testResolution(1000, 15, null);
	}

	@Test
	public void testResolution500() throws Exception {
		testResolution(500, 50, AllTests.DEGRADATION_RESOLUTION);
	}

	@Test
	public void testResolution5000() throws Exception {
		testResolution(5000, 1, AllTests.DEGRADATION_RESOLUTION);
	}

	private static final String DEGREDATION_STORE_RETRIEVE = "Performance decrease caused by additional fuctionality required for generic capabilities/requirements in OSGi R4.3 specification. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=324753 for details.";

	@Test
	public void testStoreAndRetrieve() throws Exception {
		int stateSize = 5000;
		final State originalState = buildRandomState(stateSize);
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() throws IOException {
				storeAndRetrieve(originalState);
			}
		};
		runner.setRegressionReason(DEGREDATION_STORE_RETRIEVE);
		runner.run(getClass(), testName.getMethodName(), 10, 10);
	}
}
