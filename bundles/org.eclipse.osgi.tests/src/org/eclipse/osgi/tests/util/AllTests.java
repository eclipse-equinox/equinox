/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.osgi.tests.util;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {

	// Test suite to run all text processor session tests
	public static Test suite() {
		return new AllTests();
	}

	public AllTests() {
		addTest(new TestSuite(TestCaseinsensitiveMap.class));
		addTest(new TestSuite(ObjectPoolTestCase.class));
		addTest(new JUnit4TestAdapter(ManifestElementTestCase.class));
		addTest(new TestSuite(NLSTestCase.class));
		addTest(new TestSuite(StorageUtilTestCase.class));
		addBidiTests();
		addLatinTests();
	}

	private void addBidiTests() {
		addTest(new TextProcessorSessionTest("org.eclipse.osgi.tests", BidiTextProcessorTestCase.class, "iw"));
	}

	private void addLatinTests() {
		addTest(new TextProcessorSessionTest("org.eclipse.osgi.tests", LatinTextProcessorTestCase.class, "en"));
	}
}
