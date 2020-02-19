/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

import org.eclipse.osgi.util.TextProcessor;

/**
 * Tests for strings that use the TextProcessor and are run in a bidi locale.
 *
 * NOTE: Run these tests as a session test in order to have the correct
 *		 locale set.  Run class AllTests.
 */
public class BidiTextProcessorTestCase extends TextProcessorTestCase {

	// left to right marker
	protected static final char LRM = '\u200e';
	// left to right embedding
	protected static final char LRE = '\u202a';
	// pop directional format
	protected static final char PDF = '\u202c';

	private static String PATH_1_RESULT = LRE + PATH_1 + PDF;
	private static String PATH_2_RESULT = LRE + PATH_2 + PDF;
	private static String PATH_3_RESULT = LRE + PATH_3 + PDF;
	private static String PATH_4_RESULT = LRE + PATH_4 + PDF;
	private static String PATH_5_RESULT = LRE + "d" + ":" + "\\" + HEBREW_STRING_2 + " abcdef-" + HEBREW_STRING_3 + "\\" + "xyz" + "\\" + "abcdef" + "\\" + HEBREW_STRING_4 + LRM + "\\" + HEBREW_STRING_5 + "." + "java" + PDF;
	private static String PATH_6_RESULT = LRE + "d" + ":" + "\\" + HEBREW_STRING_2 + " abcdef-" + HEBREW_STRING_3 + "\\" + "xyz" + "\\" + "abcdef" + "\\" + HEBREW_STRING_4 + LRM + "\\" + HEBREW_STRING_5 + LRM + "." + HEBREW_STRING_6 + PDF;
	private static String PATH_7_RESULT = LRE + "d" + ":" + "\\" + HEBREW_STRING_2 + " abcdef-" + HEBREW_STRING_3 + "\\" + "xyz" + "\\" + "abcdef" + "\\" + HEBREW_STRING_4 + "\\" + "Test" + "." + "java" + PDF;
	private static String PATH_8_RESULT = LRE + PATH_8 + PDF;
	private static String PATH_9_RESULT = LRE + PATH_9 + PDF;
	private static String PATH_10_RESULT = LRE + PATH_10 + PDF;
	private static String PATH_11_RESULT = LRE + PATH_11 + PDF;
	private static String PATH_12_RESULT = PATH_12;
	private static String PATH_13_RESULT = LRE + PATH_13 + PDF;

	// additional strings
	private static String STRING_1_RESULT = STRING_1;
	private static String STRING_2_RESULT = STRING_2;
	private static String STRING_3_RESULT = LRE + STRING_3 + PDF;
	private static String STRING_4_RESULT = LRE + STRING_4 + PDF;
	private static String STRING_5_RESULT = LRE + STRING_5 + PDF;
	private static String STRING_6_RESULT = LRE + STRING_6 + PDF;
	private static String STRING_7_RESULT = STRING_7;
	private static String STRING_8_RESULT = LRE + STRING_8 + PDF;
	private static String STRING_9_RESULT = LRE + "d:\\myFolder\\" + HEBREW_STRING_5 + LRM + "\\" + HEBREW_STRING_6 + ".java" + PDF;
	private static String STRING_10_RESULT = LRE + "d:\\myFolder\\" + HEBREW_STRING_2 + LRM + "\\123/" + HEBREW_STRING_3 + ".java" + PDF;
	private static String STRING_11_RESULT = LRE + "d:\\myFolder\\" + HEBREW_STRING_2 + LRM + "\\123/" + HEBREW_STRING_3 + LRM + "." + HEBREW_STRING_5 + PDF;
	private static String STRING_12_RESULT = LRE + "d:\\myFolder\\" + HEBREW_STRING_2 + LRM + "\\123" + HEBREW_STRING_3 + LRM + "." + HEBREW_STRING_6 + PDF;
	private static String STRING_13_RESULT = LRE + "d:\\myFolder\\" + HEBREW_STRING_2 + LRM + "\\123/myfile." + HEBREW_STRING_6 + PDF;
	private static String STRING_14_RESULT = LRE + "d:\\myFolder\\" + HEBREW_STRING_2 + LRM + "\\123myfile." + HEBREW_STRING_6 + PDF;
	private static String STRING_15_RESULT = LRE + "d:\\myFolder\\" + HEBREW_STRING_2 + "12-=" + LRM + "\\<>?34" + HEBREW_STRING_6 + ".java" + PDF;
	private static String STRING_16_RESULT = LRE + HEBREW_STRING_2 + LRM + "/" + HEBREW_STRING_6 + LRM + "/" + HEBREW_STRING_4 + LRM + "." + HEBREW_STRING_5 + PDF;
	private static String STRING_17_RESULT = LRE + HEBREW_STRING_7 + LRM + "/" + HEBREW_STRING_8 + LRM + "/" + HEBREW_STRING_9 + LRM + "/" + HEBREW_STRING_10 + LRM + "/" + HEBREW_STRING_11 + LRM + "/" + HEBREW_STRING_12 + LRM + "/" + HEBREW_STRING_13 + PDF;
	private static String STRING_18_RESULT = LRE + "_" + HEBREW_STRING_2 + " mixed text starts and ends with neutral." + PDF;
	private static String STRING_19_RESULT = LRE + "english and " + HEBREW_STRING_2 + " text starts with LTR ends with neutral _" + PDF;

	private static String OTHER_STRING_NO_DELIM = LRE + "\u05ea\u05e9\u05e8\u05e7\u05e6 abcdef-\u05e5\u05e4\u05e3" + PDF;

	private static String OTHER_STRING_1_RESULT = LRE + "*" + "." + "java" + PDF;
	private static String OTHER_STRING_2_RESULT = LRE + "*" + "." + "\u05d0\u05d1\u05d2" + PDF;
	private static String OTHER_STRING_3_RESULT = LRE + "\u05d0\u05d1\u05d2 " + LRM + "=" + " \u05ea\u05e9\u05e8\u05e7\u05e6" + PDF;
	// result strings if null delimiter is passed
	private static String OTHER_STRING_1_ND_RESULT = LRE + "*" + "." + "java" + PDF;
	private static String OTHER_STRING_2_ND_RESULT = LRE + "*" + "." + "\u05d0\u05d1\u05d2" + PDF;
	private static String OTHER_STRING_3_ND_RESULT = LRE + "\u05d0\u05d1\u05d2 " + "=" + " \u05ea\u05e9\u05e8\u05e7\u05e6" + PDF;

	private static String[] RESULT_DEFAULT_PATHS = {PATH_1_RESULT, PATH_2_RESULT, PATH_3_RESULT, PATH_4_RESULT, PATH_5_RESULT, PATH_6_RESULT, PATH_7_RESULT, PATH_8_RESULT, PATH_9_RESULT, PATH_10_RESULT, PATH_11_RESULT, PATH_12_RESULT, PATH_13_RESULT};

	private static String[] RESULT_ADDITIONAL_STRINGS = {STRING_1_RESULT, STRING_2_RESULT, STRING_3_RESULT, STRING_4_RESULT, STRING_5_RESULT, STRING_6_RESULT, STRING_7_RESULT, STRING_8_RESULT, STRING_9_RESULT, STRING_10_RESULT, STRING_11_RESULT, STRING_12_RESULT, STRING_13_RESULT, STRING_14_RESULT, STRING_15_RESULT, STRING_16_RESULT, STRING_17_RESULT, STRING_18_RESULT, STRING_19_RESULT};

	private static String[] RESULT_STAR_PATHS = {OTHER_STRING_1_RESULT, OTHER_STRING_2_RESULT};

	private static String[] RESULT_EQUALS_PATHS = {OTHER_STRING_3_RESULT};

	private static String[] RESULT_STAR_PATHS_ND = {OTHER_STRING_1_ND_RESULT, OTHER_STRING_2_ND_RESULT};

	private static String[] RESULT_EQUALS_PATHS_ND = {OTHER_STRING_3_ND_RESULT};

	// whether or not the current platform supports directional characters
	private static boolean isSupportedPlatform = false;
	static {
		String osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
		if (osName.startsWith("windows") || osName.startsWith("linux") || osName.startsWith("mac")) { //$NON-NLS-1$	//$NON-NLS-2$ //$NON-NLS-3$

			// Only consider platforms that can support control characters
			isSupportedPlatform = true;
		}
	}

	protected static String defaultDelimiters = TextProcessor.getDefaultDelimiters();

	/**
	 * Constructor.
	 *
	 * @param name test name
	 */
	public BidiTextProcessorTestCase(String name) {
		super(name);
	}

	/*
	 * Test TextProcessor for file paths.
	 */
	public void testBidiPaths() {
		for (int i = 0; i < TEST_DEFAULT_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_DEFAULT_PATHS[i]);
			if (isSupportedPlatform)
				verifyBidiResult("Process path " + (i + 1), result, RESULT_DEFAULT_PATHS[i]);
			else
				verifyResult("Process path " + (i + 1), result, TEST_DEFAULT_PATHS[i]);
		}
	}

	public void testBidiPathsDeprocess() {
		for (int i = 0; i < TEST_DEFAULT_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_DEFAULT_PATHS[i]);
			String resultDP = TextProcessor.deprocess(result);
			verifyBidiResult("Deprocess path " + (i + 1), resultDP, TEST_DEFAULT_PATHS[i]);
		}
	}

	public void testBidiPathsWithNullDelimiter() {
		// should use default delimiters
		for (int i = 0; i < TEST_DEFAULT_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_DEFAULT_PATHS[i], null);
			if (isSupportedPlatform)
				verifyBidiResult("Process path " + (i + 1), result, RESULT_DEFAULT_PATHS[i]);
			else
				verifyResult("Process path " + (i + 1), result, TEST_DEFAULT_PATHS[i]);
		}
	}

	public void testBidiStringWithNoDelimiters() {
		String result = TextProcessor.process(OTHER_STRING_NO_DELIM);
		assertEquals("Other string containing no delimiters not equivalent.", OTHER_STRING_NO_DELIM, result);
	}

	/*
	 * Test other possible uses for TextProcessor, including file associations and
	 * variable assignment statements.
	 */
	public void testOtherStrings() {
		int testNum = 1;
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_STAR_PATHS[i], "*.");
			if (isSupportedPlatform)
				verifyBidiResult("Other (star) string" + testNum, result, RESULT_STAR_PATHS[i]);
			else
				verifyResult("Other (star) string" + testNum, result, TEST_STAR_PATHS[i]);
			testNum++;
		}

		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], "=");
			if (isSupportedPlatform)
				verifyBidiResult("Other (equals) string" + testNum, result, RESULT_EQUALS_PATHS[i]);
			else
				verifyResult("Other (equals) string" + testNum, result, TEST_EQUALS_PATHS[i]);
			testNum++;
		}
	}

	public void testOtherStringsDeprocess() {
		int testNum = 1;
		for (String testStarPath : TEST_STAR_PATHS) {
			String result = TextProcessor.process(testStarPath, "*.");
			String resultDP = TextProcessor.deprocess(result);
			verifyBidiResult("Deprocess other (star) string" + testNum, resultDP, testStarPath);
			testNum++;
		}
		for (String testEqualsPath : TEST_EQUALS_PATHS) {
			String result = TextProcessor.process(testEqualsPath, "=");
			String resultDP = TextProcessor.deprocess(result);
			verifyBidiResult("Deprocess other (equals) string" + testNum, resultDP, testEqualsPath);
			testNum++;
		}
	}

	public void testOtherStringsWithNullDelimiter() {
		int testNum = 1;
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_STAR_PATHS[i], null);
			if (isSupportedPlatform)
				verifyBidiResult("Other (star) string" + testNum, result, RESULT_STAR_PATHS_ND[i]);
			else
				verifyResult("Other (star) string" + testNum, result, TEST_STAR_PATHS[i]);
			testNum++;
		}

		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], null);
			if (isSupportedPlatform)
				verifyBidiResult("Other (equals) string" + testNum, result, RESULT_EQUALS_PATHS_ND[i]);
			else {
				verifyResult("Other (equals) string" + testNum, result, TEST_EQUALS_PATHS[i]);
			}
			testNum++;
		}
	}

	/*
	 * Test the result to ensure markers aren't added more than once if the
	 * string is processed multiple times.
	 */
	public void testDoubleProcessPaths() {
		for (int i = 0; i < TEST_DEFAULT_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_DEFAULT_PATHS[i]);
			result = TextProcessor.process(result);
			if (isSupportedPlatform)
				verifyBidiResult("Path " + (i + 1), result, RESULT_DEFAULT_PATHS[i]);
			else
				verifyResult("Path " + (i + 1), result, TEST_DEFAULT_PATHS[i]);
		}
	}

	/*
	 * Test the result to ensure markers aren't added more than once if the
	 * string is processed multiple times.
	 */
	public void testDoubleProcessOtherStrings() {
		int testNum = 1;
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_STAR_PATHS[i], "*.");
			result = TextProcessor.process(result, "*.");
			if (isSupportedPlatform)
				verifyBidiResult("Other (star) string " + testNum, result, RESULT_STAR_PATHS[i]);
			else
				verifyResult("Other (star) string " + testNum, result, TEST_STAR_PATHS[i]);
			testNum++;
		}

		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], "=");
			result = TextProcessor.process(result, "=");
			if (isSupportedPlatform)
				verifyBidiResult("Other (equals) string" + testNum, result, RESULT_EQUALS_PATHS[i]);
			else
				verifyResult("Other (equals) string" + testNum, result, TEST_EQUALS_PATHS[i]);
			testNum++;
		}
	}

	public void testAdditionalStrings() {
		for (int i = 0; i < TEST_ADDITIONAL_STRINGS.length; i++) {
			String result = TextProcessor.process(TEST_ADDITIONAL_STRINGS[i]);
			if (isSupportedPlatform)
				verifyBidiResult("Additional string " + (i + 1), result, RESULT_ADDITIONAL_STRINGS[i]);
			else
				verifyResult("Additional string " + (i + 1), result, TEST_ADDITIONAL_STRINGS[i]);
		}
	}

	public void testAdditionalStringsDeprocess() {
		for (int i = 0; i < TEST_ADDITIONAL_STRINGS.length; i++) {
			String result = TextProcessor.process(TEST_ADDITIONAL_STRINGS[i]);
			String resultDP = TextProcessor.deprocess(result);
			verifyBidiResult("Additional string " + (i + 1), resultDP, TEST_ADDITIONAL_STRINGS[i]);
		}
	}

	public void testEmptyStringParams() {
		verifyBidiResult("TextProcessor.process(String) for empty string ", TextProcessor.process(""), EMPTY_STRING);
		verifyBidiResult("TextProcessor.process(String, String) for empty strings ", TextProcessor.process("", ""), EMPTY_STRING);
	}

	public void testEmptyStringParamsDeprocess() {
		verifyBidiResult("TextProcessor.deprocess(String) for empty string ", TextProcessor.deprocess(""), EMPTY_STRING);
	}

	public void testNullParams() {
		assertNull("TextProcessor.process(String) for null param ", TextProcessor.process(null));
		assertNull("TextProcessor.process(String, String) for params ", TextProcessor.process(null, null));
	}

	public void testNullParamsDeprocess() {
		assertNull("TextProcessor.deprocess(String) for null param ", TextProcessor.deprocess(null));
	}

	private void verifyBidiResult(String testName, String result, String expected) {
		boolean testResult = result.equals(expected);
		assertTrue(testName + " result string is not the same as expected string.", testResult);
	}
}
