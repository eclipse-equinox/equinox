package org.eclipse.osgi.tests.util;

import junit.framework.TestCase;

/**
 * Common class for TextProcessor test cases.
 *
 */
public abstract class TextProcessorTestCase extends TestCase {
	// strings to test
	
	// paths
	protected static String PATH_1 = "d:\\test\\\u05d0\u05d1\u05d2\u05d3 \u05d4\u05d5\\segment";
	protected static String PATH_2 = "\\test\\\u05d0\u05d1\u05d2\u05d3 \u05d4\u05d5\\segment";
	protected static String PATH_3 = "d:\\\u05ea\u05e9\u05e8\u05e7\u05e6 abcdef-\u05e5\u05e4\u05e3\\xyz\\abcdef\\\u05e2\u05e1\u05e0";
	protected static String PATH_4 = "\\\u05ea\u05e9\u05e8\u05e7\u05e6 abcdef-\u05e5\u05e4\u05e3\\xyz\\abcdef\\\u05e2\u05e1\05e0";
	protected static String PATH_5 = "d:\\\u05ea\u05e9\u05e8\u05e7\u05e6 abcdef-\u05e5\u05e4\u05e3\\xyz\\abcdef\\\u05e2\u05e1\05e0\\\u05df\u05fd\u05dd.java";
	protected static String PATH_6 = "d:\\\u05ea\u05e9\u05e8\u05e7\u05e6 abcdef-\u05e5\u05e4\u05e3\\xyz\\abcdef\\\u05e2\u05e1\05e0\\\u05df\u05fd\u05dd.\u05dc\u05db\u05da";
	protected static String PATH_7 = "d:\\\u05ea\u05e9\u05e8\u05e7\u05e6 abcdef-\u05e5\u05e4\u05e3\\xyz\\abcdef\\\u05e2\u05e1\05e0\\Test.java";
	protected static String PATH_8 = "\\test\\jkl\u05d0\u05d1\u05d2\u05d3 \u05d4\u05d5\\segment";
	protected static String PATH_9 = "\\test\\\u05d0\u05d1\u05d2\u05d3 \u05d4\u05d5jkl\\segment";

	// other strings - file associations and assignment statements
	protected static String OTHER_STRING_1 = "*.java";
	protected static String OTHER_STRING_2 = "*.\u05d0\u05d1\u05d2";
	protected static String OTHER_STRING_3 = "\u05d0\u05d1\u05d2 = \u05ea\u05e9\u05e8\u05e7\u05e6";
	
	protected static String[] TEST_DEFAULT_PATHS = {
		PATH_1, PATH_2, PATH_3, PATH_4, PATH_5, PATH_6, PATH_7, PATH_8, PATH_9
	};
	
	protected static String[] TEST_STAR_PATHS = { OTHER_STRING_1, OTHER_STRING_2};
	
	protected static String[] TEST_EQUALS_PATHS = {OTHER_STRING_3};
	
	/**
	 * Constructor for class
	 * 
	 * @param name
	 */
	public TextProcessorTestCase(String name) {
		super(name);
	}

}
