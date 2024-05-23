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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Common class for TextProcessor test cases.
 */
public abstract class TextProcessorTestCase {
	// some Hebrew strings to use for test cases
	protected static String HEBREW_STRING_1 = "\u05d0\u05d1\u05d2\u05d3 \u05d4\u05d5";
	protected static String HEBREW_STRING_2 = "\u05ea\u05e9\u05e8\u05e7\u05e6";
	protected static String HEBREW_STRING_3 = "\u05e5\u05e4\u05e3";
	protected static String HEBREW_STRING_4 = "\u05e2\u05e1\u05e0";
	protected static String HEBREW_STRING_5 = "\u05df\u05fd\u05dd";
	protected static String HEBREW_STRING_6 = "\u05dc\u05db\u05da";
	protected static String HEBREW_STRING_7 = "\u05d0";
	protected static String HEBREW_STRING_8 = "\u05e9";
	protected static String HEBREW_STRING_9 = "\u05dc";
	protected static String HEBREW_STRING_10 = "\u05e3";
	protected static String HEBREW_STRING_11 = "\u05d4";
	protected static String HEBREW_STRING_12 = "\u05da";
	protected static String HEBREW_STRING_13 = "\u05df";

	// strings to test

	// paths
	protected static String PATH_1 = "d:\\test\\" + HEBREW_STRING_1 + "\\segment";
	protected static String PATH_2 = "\\test\\" + HEBREW_STRING_1 + "\\segment";
	protected static String PATH_3 = "d:\\" + HEBREW_STRING_2 + " abcdef-" + HEBREW_STRING_3 + "\\xyz\\abcdef\\"
			+ HEBREW_STRING_4;
	protected static String PATH_4 = "\\" + HEBREW_STRING_2 + " abcdef-" + HEBREW_STRING_3 + "\\xyz\\abcdef\\"
			+ HEBREW_STRING_4;
	protected static String PATH_5 = "d:\\" + HEBREW_STRING_2 + " abcdef-" + HEBREW_STRING_3 + "\\xyz\\abcdef\\"
			+ HEBREW_STRING_4 + "\\" + HEBREW_STRING_5 + ".java";
	protected static String PATH_6 = "d:\\" + HEBREW_STRING_2 + " abcdef-" + HEBREW_STRING_3 + "\\xyz\\abcdef\\"
			+ HEBREW_STRING_4 + "\\" + HEBREW_STRING_5 + "." + HEBREW_STRING_6;
	protected static String PATH_7 = "d:\\" + HEBREW_STRING_2 + " abcdef-" + HEBREW_STRING_3 + "\\xyz\\abcdef\\"
			+ HEBREW_STRING_4 + "\\Test.java";
	protected static String PATH_8 = "\\test\\jkl" + HEBREW_STRING_1 + "\\segment";
	protected static String PATH_9 = "\\test\\" + HEBREW_STRING_1 + "jkl\\segment";
	protected static String PATH_10 = "d:\\t\\" + HEBREW_STRING_7 + "\\segment";
	protected static String PATH_11 = "\\t\\" + HEBREW_STRING_7 + "\\segment";
	protected static String PATH_12 = "d:\\";
	protected static String PATH_13 = "\\t";

	protected static String STRING_1 = "d:\\all\\english";
	protected static String STRING_2 = "all english with neutrals (spaces) in the middle";
	protected static String STRING_3 = "_d:all/english with leading neutral";
	protected static String STRING_4 = "d:all/english with trailing neutral_";
	protected static String STRING_5 = "3d:all/english with leading digit";
	protected static String STRING_6 = "english with some " + HEBREW_STRING_5 + " in the middle";
	protected static String STRING_7 = "d:all/english with trailing neutral then digits_123";
	protected static String STRING_8 = "==>";
	protected static String STRING_9 = "d:\\myFolder\\" + HEBREW_STRING_5 + "\\" + HEBREW_STRING_6 + ".java";
	protected static String STRING_10 = "d:\\myFolder\\" + HEBREW_STRING_2 + "\\123/" + HEBREW_STRING_3 + ".java";
	protected static String STRING_11 = "d:\\myFolder\\" + HEBREW_STRING_2 + "\\123/" + HEBREW_STRING_3 + "."
			+ HEBREW_STRING_5;
	protected static String STRING_12 = "d:\\myFolder\\" + HEBREW_STRING_2 + "\\123" + HEBREW_STRING_3 + "."
			+ HEBREW_STRING_6;
	protected static String STRING_13 = "d:\\myFolder\\" + HEBREW_STRING_2 + "\\123/myfile." + HEBREW_STRING_6;
	protected static String STRING_14 = "d:\\myFolder\\" + HEBREW_STRING_2 + "\\123myfile." + HEBREW_STRING_6;
	protected static String STRING_15 = "d:\\myFolder\\" + HEBREW_STRING_2 + "12-=\\<>?34" + HEBREW_STRING_6 + ".java";
	protected static String STRING_16 = HEBREW_STRING_2 + "/" + HEBREW_STRING_6 + "/" + HEBREW_STRING_4 + "."
			+ HEBREW_STRING_5;
	protected static String STRING_17 = HEBREW_STRING_7 + "/" + HEBREW_STRING_8 + "/" + HEBREW_STRING_9 + "/"
			+ HEBREW_STRING_10 + "/" + HEBREW_STRING_11 + "/" + HEBREW_STRING_12 + "/" + HEBREW_STRING_13;
	protected static String STRING_18 = "_" + HEBREW_STRING_2 + " mixed text starts and ends with neutral.";
	protected static String STRING_19 = "english and " + HEBREW_STRING_2 + " text starts with LTR ends with neutral _";

	// other strings - file associations and assignment statements
	protected static String OTHER_STRING_1 = "*.java";
	protected static String OTHER_STRING_2 = "*.\u05d0\u05d1\u05d2";
	protected static String OTHER_STRING_3 = "\u05d0\u05d1\u05d2 = \u05ea\u05e9\u05e8\u05e7\u05e6";

	protected static String EMPTY_STRING = "";

	protected static String[] TEST_DEFAULT_PATHS = { PATH_1, PATH_2, PATH_3, PATH_4, PATH_5, PATH_6, PATH_7, PATH_8,
			PATH_9 };

	protected static String[] TEST_ADDITIONAL_STRINGS = { STRING_1, STRING_2, STRING_3, STRING_4, STRING_5, STRING_6,
			STRING_7, STRING_8, STRING_9, STRING_10, STRING_11, STRING_12, STRING_13, STRING_14, STRING_15, STRING_16,
			STRING_17, STRING_18, STRING_19 };

	protected static String[] TEST_STAR_PATHS = { OTHER_STRING_1, OTHER_STRING_2 };

	protected static String[] TEST_EQUALS_PATHS = { OTHER_STRING_3 };

	protected void verifyResult(String testName, String expected, String result) {
		assertTrue(result.equals(expected), testName + " result string is not the same as string passed in.");
	}

}
