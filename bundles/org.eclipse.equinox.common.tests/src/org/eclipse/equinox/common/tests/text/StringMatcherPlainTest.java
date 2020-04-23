/*******************************************************************************
 * Copyright (C) 2020, Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.common.tests.text;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;

import org.eclipse.core.text.StringMatcher;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class StringMatcherPlainTest extends AbstractStringMatcherTestBase {

	@Parameters(name = "{0}")
	public static TestData[] data() throws IOException {
		return getTestData("StringMatcherPlainTest.txt");
	}

	@Test
	public void testStringMatch() throws Exception {
		assumeFalse("Test not applicable", data.caseInsensitive);
		StringMatcher matcher = new StringMatcher(data.pattern, false, true);
		assertTrue("Unexpected result", data.expected == matcher.match(data.text));
	}

	@Test
	public void testStringMatchCaseInsensitive() throws Exception {
		StringMatcher matcher = new StringMatcher(data.pattern, true, true);
		assertTrue("Unexpected result", data.expected == matcher.match(data.text));
	}
}
