/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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

import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.tests.OSGiTest;

public class CaseMapPerformanceTest extends OSGiTest {
	static final String[] KEYS;
	static final Object VALUE = new Object();
	static {
		KEYS = new String[1000];
		for (int i = 0; i < 1000; i++) {
			KEYS[i] = "Header-" + i + "-Key";
		}
	}

	public static Test suite() {
		return new TestSuite(CaseMapPerformanceTest.class);
	}

	public CaseMapPerformanceTest(String name) {
		super(name);
	}

	public void testHeaders004() {
		final Map<String, Object> headers = new Headers<String, Object>(4);
		doTestMap(headers, 4);
	}

	public void testHeaders005() {
		final Map<String, Object> headers = new Headers<String, Object>(5);
		doTestMap(headers, 5);
	}

	public void testHeaders006() {
		final Map<String, Object> headers = new Headers<String, Object>(6);
		doTestMap(headers, 6);
	}

	public void testHeaders010() {
		final Map<String, Object> headers = new Headers<String, Object>(10);
		doTestMap(headers, 10);
	}

	public void testHeaders020() {
		final Map<String, Object> headers = new Headers<String, Object>(20);
		doTestMap(headers, 20);
	}

	public void testHeaders100() {
		final Map<String, Object> headers = new Headers<String, Object>(100);
		doTestMap(headers, 100);
	}

	public void testXCaseMap004() {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<String, Object>(4);
		doTestMap(headers, 4);
	}

	public void testXCaseMap005() {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<String, Object>(5);
		doTestMap(headers, 5);
	}

	public void testXCaseMap006() {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<String, Object>(6);
		doTestMap(headers, 6);
	}

	public void testXCaseMap010() {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<String, Object>(10);
		doTestMap(headers, 10);
	}

	public void testXCaseMap020() {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<String, Object>(20);
		doTestMap(headers, 10);
	}

	public void testXCaseMap100() {
		final Map<String, Object> headers = new CaseInsensitiveDictionaryMap<String, Object>(100);
		doTestMap(headers, 100);
	}

	private void doTestMap(final Map<String, Object> map, final int numKeys) {
		fillMap(map, numKeys);
		new PerformanceTestRunner() {
			protected void test() {
				doMapGet(map, numKeys);
			}

		}.run(this, 10, 10000);
	}

	static void fillMap(Map<String, Object> map, int numKeys) {
		map.clear();
		for (int i = 0; i < numKeys; i++) {
			map.put(KEYS[i], VALUE);
		}
	}

	static void doMapGet(Map<String, Object> map, int numKeys) {
		for (int i = 0; i < numKeys; i++) {
			map.get(KEYS[i]);
		}
	}

}
