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

import java.util.Hashtable;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.osgi.service.resolver.State;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class StateUsesPerformanceTest extends BasePerformanceTest {

	@Rule
	public TestName testName = new TestName();

	private void doUsesResolution(int stateSize, int repetitions, String degradation) throws Exception {
		final State originalState = buildRandomState(stateSize);
		addUsesBundles(originalState);
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() {
				originalState.resolve(false);
			}
		};
		runner.setRegressionReason(degradation);
		runner.run(getClass(), testName.getMethodName(), 10, repetitions);

	}

	@Test
	public void testUsesResolution00100() throws Exception {
		doUsesResolution(100, 100, AllTests.DEGRADATION_RESOLUTION);
	}

	@Test
	public void testUsesResolution00500() throws Exception {
		doUsesResolution(500, 10, AllTests.DEGRADATION_RESOLUTION);
	}

	@Test
	public void testUsesResolution01000() throws Exception {
		doUsesResolution(1000, 10, AllTests.DEGRADATION_RESOLUTION);
	}

	@Test
	public void testUsesResolution05000() throws Exception {
		doUsesResolution(5000, 1, AllTests.DEGRADATION_RESOLUTION);
	}

	private void addUsesBundles(State state) throws BundleException {
		int id = state.getBundles().length + 500;
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "3.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "4.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "5.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[1.0.0,2.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[1.0.0,2.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "3.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[1.0.0,2.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "4.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[1.0.0,2.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "5.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[1.0.0,2.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[2.0.0,3.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[2.0.0,3.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "3.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[2.0.0,3.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "4.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[2.0.0,3.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "5.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=a");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[2.0.0,3.0.0)\"; visibility:=reexport");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "f; uses:=b");
		manifest.put(Constants.REQUIRE_BUNDLE, "A, B, C");
		state.addBundle(state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION), id++));

	}
}
