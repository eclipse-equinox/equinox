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
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.osgi.service.resolver.State;

public class StatePerformanceTest extends BasePerformanceTest {

	public StatePerformanceTest(String name) {
		super(name);
	}

	State storeAndRetrieve(State toStore) throws IOException {
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

	private void testResolution(int stateSize, int repetitions, String localName, String degradation) {
		final State originalState = buildRandomState(stateSize);
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() {
				originalState.resolve(false);
			}
		};
		runner.setRegressionReason(degradation);
		runner.run(this, localName, 10, repetitions);
	}

	public void testResolution100() {
		testResolution(100, 500, null, AllTests.DEGRADATION_RESOLUTION);
	}

	public void testResolution1000() {
		testResolution(1000, 15, "State Resolution", null);
	}

	public void testResolution500() {
		testResolution(500, 50, null, AllTests.DEGRADATION_RESOLUTION);
	}

	public void testResolution5000() {
		testResolution(5000, 1, null, AllTests.DEGRADATION_RESOLUTION);
	}

	private static final String DEGREDATION_STORE_RETRIEVE = "Performance decrease caused by additional fuctionality required for generic capabilities/requirements in OSGi R4.3 specification. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=324753 for details.";

	public void testStoreAndRetrieve() {
		int stateSize = 5000;
		final State originalState = buildRandomState(stateSize);
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() {
				try {
					storeAndRetrieve(originalState);
				} catch (IOException e) {
					CoreTest.fail("", e);
				}
			}
		};
		runner.setRegressionReason(DEGREDATION_STORE_RETRIEVE);
		runner.run(this, 10, 10);
	}
}
