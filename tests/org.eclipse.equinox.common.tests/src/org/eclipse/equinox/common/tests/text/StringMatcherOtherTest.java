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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.text.StringMatcher;
import org.junit.Test;

public class StringMatcherOtherTest {

	@Test
	public void testEmptyNoWildcard() {
		StringMatcher m = new StringMatcher("", false, true);
		assertTrue(m.match(""));
		assertFalse(m.match("foo"));
		assertFalse(m.match("foo bar"));
	}

	@Test
	public void testEmptyWildcard() {
		StringMatcher m = new StringMatcher("", false, false);
		assertTrue(m.match(""));
		assertFalse(m.match("foo"));
		assertFalse(m.match("foo bar"));
	}
}
