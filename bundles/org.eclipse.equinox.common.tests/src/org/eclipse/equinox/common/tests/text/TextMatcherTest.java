/*******************************************************************************
 * Copyright (c) 2020, 2024 Thomas Wolf<thomas.wolf@paranor.ch> and others.
 *
 * This program and the accompanying materials
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

/**
 * Tests for {@link StringMatcher}.
 */
public class TextMatcherTest {

	@Test
	public void testEmpty() {
		assertTrue(new StringMatcher("", false, false).matchWords(""));
		assertFalse(new StringMatcher("", false, false).matchWords("foo"));
		assertFalse(new StringMatcher("", false, false).matchWords("foo bar baz"));
		assertTrue(new StringMatcher("", false, true).matchWords(""));
		assertFalse(new StringMatcher("", false, true).matchWords("foo"));
		assertFalse(new StringMatcher("", false, true).matchWords("foo bar baz"));
	}

	@Test
	public void testSuffixes() {
		assertFalse(new StringMatcher("fo*ar", false, false).matchWords("foobar_123"));
		assertFalse(new StringMatcher("fo*ar", false, false).matchWords("foobar_baz"));
	}

	@Test
	public void testChinese() {
		assertTrue(new StringMatcher("喜欢", false, false).matchWords("我 喜欢 吃 苹果。"));
		// This test would work only if word-splitting used the ICU BreakIterator.
		// "Words" are as shown above.
		// assertTrue(new StringMatcher("喜欢", false).matchWords("我喜欢吃苹果。"));
	}

	@Test
	public void testSingleWords() {
		assertTrue(new StringMatcher("huhn", false, false).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("h?hner", false, false).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("h*hner", false, false).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("hühner", false, false).matchWords("hahn henne hühner küken huhn"));
		// Full pattern must match word fully
		assertFalse(new StringMatcher("h?hner", false, false).matchWords("hahn henne hühnerhof küken huhn"));
		assertFalse(new StringMatcher("h*hner", false, false).matchWords("hahn henne hühnerhof küken huhn"));
		assertFalse(new StringMatcher("hühner", false, false).matchWords("hahn henne hühnerhof küken huhn"));

		assertTrue(new StringMatcher("huhn", false, true).matchWords("hahn henne hühner küken huhn"));
		assertFalse(new StringMatcher("h?hner", false, true).matchWords("hahn henne hühner küken huhn"));
		assertFalse(new StringMatcher("h*hner", false, true).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("hühner", false, true).matchWords("hahn henne hühner küken huhn"));
		// Full pattern must match word fully
		assertFalse(new StringMatcher("h?hner", false, true).matchWords("hahn henne hühnerhof küken huhn"));
		assertFalse(new StringMatcher("h*hner", false, true).matchWords("hahn henne hühnerhof küken huhn"));
		assertFalse(new StringMatcher("hühner", false, true).match("hahn henne hühnerhof küken huhn"));
	}

	@Test
	public void testMultipleWords() {
		assertTrue(new StringMatcher("huhn h?hner", false, false).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("huhn h?hner", false, false).matchWords("hahn henne hühnerhof küken huhn"));
		assertFalse(new StringMatcher("huhn h?hner", false, true).matchWords("hahn henne hühner küken huhn"));
		assertFalse(new StringMatcher("huhn h?hner", false, true).matchWords("hahn henne hühnerhof küken huhn"));
		assertTrue(new StringMatcher("huhn h*hner", false, false).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("huhn h*hner", false, false).matchWords("hahn henne hühnerhof küken huhn"));
		assertFalse(new StringMatcher("huhn h*hner", false, true).matchWords("hahn henne hühner küken huhn"));
		assertFalse(new StringMatcher("huhn h*hner", false, true).matchWords("hahn henne hühnerhof küken huhn"));
		assertTrue(new StringMatcher("huhn hühner", false, false).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("huhn hühner", false, false).matchWords("hahn henne hühnerhof küken huhn"));
		assertTrue(new StringMatcher("huhn hühner", false, true).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("huhn hühner", false, true).matchWords("hahn henne hühnerhof küken huhn"));
	}

	@Test
	public void testCaseInsensitivity() {
		assertTrue(new StringMatcher("Huhn HÜHNER", true, false).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("Huhn HÜHNER", true, false).matchWords("hahn henne hühnerhof küken huhn"));
		assertTrue(new StringMatcher("Huhn HÜHNER", true, true).matchWords("hahn henne hühner küken huhn"));
		assertTrue(new StringMatcher("Huhn HÜHNER", true, true).matchWords("hahn henne hühnerhof küken huhn"));
		assertTrue(new StringMatcher("HüHnEr", true, false).matchWords("hahn henne hühner küken huhn"));
		assertFalse(new StringMatcher("HüHnEr", true, false).matchWords("hahn henne hühnerhof küken huhn"));
		assertTrue(new StringMatcher("HüHnEr", true, true).matchWords("hahn henne hühner küken huhn"));
		assertFalse(new StringMatcher("HüHnEr", true, true).matchWords("hahn henne hühnerhof küken huhn"));
	}
}
