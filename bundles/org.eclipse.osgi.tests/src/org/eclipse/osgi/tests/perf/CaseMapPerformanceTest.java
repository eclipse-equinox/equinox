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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.framework.util.Headers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.osgi.framework.Constants;

@SuppressWarnings("deprecation") // Headers
public class CaseMapPerformanceTest {
	@Rule
	public TestName testName = new TestName();

	static final String[] KEYS;
	static final Object VALUE = new Object();
	static {
		KEYS = new String[1000];
		for (int i = 0; i < 1000; i++) {
			KEYS[i] = "Header-" + i + "-Key";
		}
	}

	public void testHeaders004() throws Exception {
		final Map<String, Object> headers = new Headers<>(4);
		doTestMap(headers, 4);
	}

	public void testHeaders005() throws Exception {
		final Map<String, Object> headers = new Headers<>(5);
		doTestMap(headers, 5);
	}

	public void testHeaders006() throws Exception {
		final Map<String, Object> headers = new Headers<>(6);
		doTestMap(headers, 6);
	}

	public void testHeaders010() throws Exception {
		final Map<String, Object> headers = new Headers<>(10);
		doTestMap(headers, 10);
	}

	public void testHeaders020() throws Exception {
		final Map<String, Object> headers = new Headers<>(20);
		doTestMap(headers, 20);
	}

	public void testHeaders100() throws Exception {
		final Map<String, Object> headers = new Headers<>(100);
		doTestMap(headers, 100);
	}

	public void testXCaseMap004() throws Exception {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<>(4);
		doTestMap(headers, 4);
	}

	public void testXCaseMap005() throws Exception {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<>(5);
		doTestMap(headers, 5);
	}

	public void testXCaseMap006() throws Exception {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<>(6);
		doTestMap(headers, 6);
	}

	public void testXCaseMap010() throws Exception {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<>(10);
		doTestMap(headers, 10);
	}

	public void testXCaseMap034() throws Exception {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<>(34);
		doTestMap(headers, 34);
	}

	public void testXCaseMap100() throws Exception {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<>(100);
		doTestMap(headers, 100);
	}

	private void doTestMap(final Map<String, Object> map, final int numKeys) throws Exception {
		new PerformanceTestRunner() {
			protected void test() {
				fillMap(map, numKeys);
				doMapGet(map, numKeys);
			}

		}.run(getClass(), testName.getMethodName(), 10, 10000);
	}

	public void testCommonKeyMap() throws Exception {
		final Map<String, Object> map = new CaseInsensitiveDictionaryMap<>(34);
		new PerformanceTestRunner() {
			protected void test() {
				fillCommonKeyMap(map);
				doCommonKeyMapGet(map);
			}

		}.run(getClass(), testName.getMethodName(), 10, 10000);
	}

	public void testCommonHashMap() throws Exception {
		final Map<String, Object> map = new HashMap<>(34);
		new PerformanceTestRunner() {
			protected void test() {
				fillCommonKeyMap(map);
				doCommonKeyMapGet(map);
			}

		}.run(getClass(), testName.getMethodName(), 10, 10000);
	}

	static void fillMap(Map<String, Object> map, int numKeys) {
		map.clear();
		for (int i = 0; i < numKeys; i++) {
			map.put(KEYS[i], VALUE);
		}
	}

	static void fillCommonKeyMap(Map<String, Object> map) {
		map.clear();
		for (String key : COMMON_KEY_NAMES) {
			map.put(key, VALUE);
		}
	}

	static void doMapGet(Map<String, Object> map, int numKeys) {
		for (int i = 0; i < numKeys; i++) {
			Assert.assertEquals("Wrong value found.", VALUE, map.get(KEYS[i]));
		}
	}

	static void doCommonKeyMapGet(Map<String, Object> map) {
		for (String key : COMMON_KEY_NAMES) {
			Assert.assertEquals("Wrong value found.", VALUE, map.get(key));
		}
	}

	final static String[] COMMON_KEY_NAMES = new String[] {

			// common core service property keys
			Constants.OBJECTCLASS, //
			Constants.SERVICE_BUNDLEID, //
			Constants.SERVICE_CHANGECOUNT, //
			Constants.SERVICE_DESCRIPTION, //
			Constants.SERVICE_ID, //
			Constants.SERVICE_PID, //
			Constants.SERVICE_RANKING, //
			Constants.SERVICE_SCOPE, //
			Constants.SERVICE_VENDOR, //

			// common SCR service property keys
			"component.name", //$NON-NLS-1$
			"component.id", //$NON-NLS-1$

			// common meta-type property keys
			"metatype.pid", //$NON-NLS-1$
			"metatype.factory.pid", //$NON-NLS-1$

			// event admin keys
			"event.topics", //$NON-NLS-1$
			"event.filter", //$NON-NLS-1$

			// jmx keys
			"jmx.objectname", //$NON-NLS-1$

			// common bundle manifest headers
			Constants.BUNDLE_ACTIVATIONPOLICY, //
			Constants.BUNDLE_ACTIVATOR, //
			Constants.BUNDLE_CLASSPATH, //
			Constants.BUNDLE_LICENSE, //
			Constants.BUNDLE_LOCALIZATION, //
			Constants.BUNDLE_MANIFESTVERSION, //
			Constants.BUNDLE_NAME, //
			Constants.BUNDLE_NATIVECODE, //
			Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, //
			Constants.BUNDLE_SCM, //
			Constants.BUNDLE_SYMBOLICNAME, //
			Constants.BUNDLE_VENDOR, //
			Constants.BUNDLE_VERSION, //
			Constants.EXPORT_PACKAGE, //
			Constants.FRAGMENT_HOST, //
			Constants.IMPORT_PACKAGE, //
			Constants.REQUIRE_BUNDLE, //
			Constants.REQUIRE_CAPABILITY //
	};
}
