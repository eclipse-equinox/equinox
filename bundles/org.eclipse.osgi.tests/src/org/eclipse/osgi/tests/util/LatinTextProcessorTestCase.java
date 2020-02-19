/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.osgi.tests.util;

import org.eclipse.osgi.util.TextProcessor;

/**
 * Test for strings that use the TextProcessor but are not run in a bidi locale.
 * Latin locales should return the same String that was passed in.
 *
 */
public class LatinTextProcessorTestCase extends TextProcessorTestCase {
	protected static String[] ALL_PATHS;
	static {
		// merge all test strings into one array for Latin locales
		int size = TEST_DEFAULT_PATHS.length + TEST_STAR_PATHS.length + TEST_EQUALS_PATHS.length + TEST_ADDITIONAL_STRINGS.length;
		ALL_PATHS = new String[size];
		int idx = 0;
		for (String testDefaultPath : TEST_DEFAULT_PATHS) {
			ALL_PATHS[idx] = testDefaultPath;
			idx++;
		}
		for (String testStartPath : TEST_STAR_PATHS) {
			ALL_PATHS[idx] = testStartPath;
			idx++;
		}
		for (String testEqualsPath : TEST_EQUALS_PATHS) {
			ALL_PATHS[idx] = testEqualsPath;
			idx++;
		}
		for (String testAdditionalString : TEST_ADDITIONAL_STRINGS) {
			ALL_PATHS[idx] = testAdditionalString;
			idx++;
		}
	}

	/**
	 * Constructor for class.
	 *
	 * @param name test name
	 */
	public LatinTextProcessorTestCase(String name) {
		super(name);
	}

	public void testLatinPaths() {
		// test all strings using process(String) method
		for (int i = 0; i < ALL_PATHS.length; i++) {
			String result = TextProcessor.process(ALL_PATHS[i]);
			verifyResult("Process string " + (i + 1), result, ALL_PATHS[i]);
		}
	}

	public void testLatinPathsDeprocess(){
		// test all strings using process(String) method
		for (int i = 0; i < ALL_PATHS.length; i++) {
			String result = TextProcessor.process(ALL_PATHS[i]);
			String resultDP = TextProcessor.deprocess(result);
			verifyResult("Deprocess string " + (i + 1), resultDP, ALL_PATHS[i]);
		}
	}

	public void testLatinPathsWithNullDelimiter() {
		// should use default delimiters
		for (int i = 0; i < ALL_PATHS.length; i++) {
			String result = TextProcessor.process(ALL_PATHS[i], null);
			verifyResult("Process string " + (i + 1), result, ALL_PATHS[i]);
		}
	}

	public void testLatinOtherStrings() {
		// test the process(String, String) method
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_STAR_PATHS[i], "*.");
			verifyResult("File association " + (i + 1), result, TEST_STAR_PATHS[i]);
		}

		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], "=");
			verifyResult("Equals expression " + (i + 1), result, TEST_EQUALS_PATHS[i]);
		}
	}

	public void testLatinOtherStringsDeprocess() {
		// test the process(String, String) method
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_STAR_PATHS[i], "*.");
			String resultDP = TextProcessor.deprocess(result);
			verifyResult("File association " + (i + 1), resultDP, TEST_STAR_PATHS[i]);
		}

		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], "=");
			String resultDP = TextProcessor.deprocess(result);
			verifyResult("Equals expression " + (i + 1), resultDP, TEST_EQUALS_PATHS[i]);
		}
	}

	public void testLatinOtherStringsWithNoDelimiter() {
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_STAR_PATHS[i], null);
			verifyResult("File association " + (i + 1), result, TEST_STAR_PATHS[i]);
		}

		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], null);
			verifyResult("Equals expression " + (i + 1), result, TEST_EQUALS_PATHS[i]);
		}
	}

	public void testEmptyStringParams() {
		verifyResult("TextProcessor.process(String) for empty string ", TextProcessor.process(""), EMPTY_STRING);
		verifyResult("TextProcessor.process(String, String) for empty strings ", TextProcessor.process("", ""), EMPTY_STRING);
	}

	public void testEmptyStringParamsDeprocess() {
		verifyResult("TextProcessor.deprocess(String) for empty string ", TextProcessor.deprocess(""), EMPTY_STRING);
	}

	public void testNullParams() {
		assertNull("TextProcessor.process(String) for null param ", TextProcessor.process(null));
		assertNull("TextProcessor.process(String, String) for params ", TextProcessor.process(null, null));
	}

	public void testNullParamsDeprocess() {
		assertNull("TextProcessor.deprocess(String) for null param ", TextProcessor.deprocess(null));
	}
}
