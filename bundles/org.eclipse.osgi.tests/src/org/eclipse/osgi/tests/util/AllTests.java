/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.osgi.tests.util;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {

	// Test suite to run all text processor session tests
	public static Test suite() {
		return new AllTests();
	}

	public AllTests() {
		addTest(new TestSuite(ObjectPoolTestCase.class));
		addTest(new TestSuite(ManifestElementTestCase.class));
		addTest(new TestSuite(NLSTestCase.class));
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
