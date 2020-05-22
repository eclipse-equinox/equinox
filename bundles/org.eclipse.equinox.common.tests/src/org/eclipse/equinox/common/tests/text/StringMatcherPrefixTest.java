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

public class StringMatcherPrefixTest {

	@Test
	public void testNoPrefix() {
		StringMatcher m = new StringMatcher("foo", false, true);
		assertTrue(m.match("foo"));
		assertFalse(m.match("foobar"));
		assertFalse(m.match("foo bar"));
		assertFalse(m.match("bar foo"));
		assertFalse(m.match("bar foo bar"));
	}

	@Test
	public void testEmptyNoWildcard() {
		StringMatcher m = new StringMatcher("", false, true);
		m.usePrefixMatch();
		assertTrue(m.match(""));
		assertFalse(m.match("foo"));
		assertFalse(m.match("foo bar"));
	}

	@Test
	public void testEmptyWildcard() {
		StringMatcher m = new StringMatcher("", false, false);
		m.usePrefixMatch();
		assertTrue(m.match(""));
		assertTrue(m.match("foo"));
		assertTrue(m.match("foo bar"));
	}

	@Test
	public void testPrefixNoWildcards() {
		StringMatcher m = new StringMatcher("foo", false, true);
		m.usePrefixMatch();
		assertTrue(m.match("foo"));
		assertTrue(m.match("foobar"));
		assertTrue(m.match("foo bar"));
		assertFalse(m.match("bar foo"));
		assertFalse(m.match("bar foo bar"));
	}

	@Test
	public void testPrefixWildcards() {
		StringMatcher m = new StringMatcher("f?o", false, false);
		m.usePrefixMatch();
		assertTrue(m.match("foo"));
		assertTrue(m.match("foobar"));
		assertTrue(m.match("foo bar"));
		assertFalse(m.match("bar foo"));
		assertFalse(m.match("bar foo bar"));
	}

	@Test
	public void testPrefixWildcardsOffSingle() {
		StringMatcher m = new StringMatcher("f?o", false, true);
		m.usePrefixMatch();
		assertFalse(m.match("foo"));
		assertTrue(m.match("f?o"));
		assertFalse(m.match("foobar"));
		assertTrue(m.match("f?obar"));
		assertTrue(m.match("f?o bar"));
		assertFalse(m.match("bar f?o"));
		assertFalse(m.match("bar f?o bar"));
	}

	@Test
	public void testPrefixWildcardsOffMulti() {
		StringMatcher m = new StringMatcher("foo*bar", false, true);
		m.usePrefixMatch();
		assertFalse(m.match("foobar"));
		assertFalse(m.match("foobazbar"));
		assertTrue(m.match("foo*bar"));
		assertFalse(m.match("foobarbaz"));
		assertTrue(m.match("foo*barbaz"));
		assertFalse(m.match("bar foo*bar"));
		assertFalse(m.match("bar foo*barbaz"));
	}
}
