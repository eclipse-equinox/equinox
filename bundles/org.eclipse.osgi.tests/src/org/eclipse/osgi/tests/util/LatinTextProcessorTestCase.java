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
		for (int i = 0; i < TEST_DEFAULT_PATHS.length; i++) {
			ALL_PATHS[idx] = TEST_DEFAULT_PATHS[i];
			idx++;
		}
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			ALL_PATHS[idx] = TEST_STAR_PATHS[i];
			idx++;
		}
		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			ALL_PATHS[idx] = TEST_EQUALS_PATHS[i];
			idx++;
		}
		for (int i = 0; i < TEST_ADDITIONAL_STRINGS.length; i++) {
			ALL_PATHS[idx] = TEST_ADDITIONAL_STRINGS[i];
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
			verifyLatinResult("Process string " + (i + 1), result, ALL_PATHS[i]);
		}
	}
	
	public void testLatinPathsDeprocess(){
		// test all strings using process(String) method
		for (int i = 0; i < ALL_PATHS.length; i++) {
			String result = TextProcessor.process(ALL_PATHS[i]);
			String resultDP = TextProcessor.deprocess(result);
			verifyLatinResult("Deprocess string " + (i + 1), resultDP, ALL_PATHS[i]);
		}		
	}

	public void testLatinPathsWithNullDelimiter() {
		// should use default delimiters
		for (int i = 0; i < ALL_PATHS.length; i++) {
			String result = TextProcessor.process(ALL_PATHS[i], null);
			verifyLatinResult("Process string " + (i + 1), result, ALL_PATHS[i]);
		}
	}
	
	public void testLatinOtherStrings() {
		// test the process(String, String) method
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_STAR_PATHS[i], "*.");
			verifyLatinResult("File association " + (i + 1), result, TEST_STAR_PATHS[i]);
		}

		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], "=");
			verifyLatinResult("Equals expression " + (i + 1), result, TEST_EQUALS_PATHS[i]);
		}
	}
	
	public void testLatinOtherStringsDeprocess() {
		// test the process(String, String) method
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_STAR_PATHS[i], "*.");
			String resultDP = TextProcessor.deprocess(result);
			verifyLatinResult("File association " + (i + 1), resultDP, TEST_STAR_PATHS[i]);
		}

		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], "=");
			String resultDP = TextProcessor.deprocess(result);
			verifyLatinResult("Equals expression " + (i + 1), resultDP, TEST_EQUALS_PATHS[i]);
		}
	}	

	public void testLatinOtherStringsWithNoDelimiter() {
		for (int i = 0; i < TEST_STAR_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_STAR_PATHS[i], null);
			verifyLatinResult("File association " + (i + 1), result, TEST_STAR_PATHS[i]);
		}

		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++) {
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], null);
			verifyLatinResult("Equals expression " + (i + 1), result, TEST_EQUALS_PATHS[i]);
		}
	}

	private void verifyLatinResult(String testName, String expected, String result) {
		assertTrue(testName + " result string is not the same as string passed in.", result.equals(expected));
	}

	public void testEmptyStringParams() {
		verifyLatinResult("TextProcessor.process(String) for empty string ", TextProcessor.process(""), EMPTY_STRING);
		verifyLatinResult("TextProcessor.process(String, String) for empty strings ", TextProcessor.process("", ""), EMPTY_STRING);
	}
	
	public void testEmptyStringParamsDeprocess() {
		verifyLatinResult("TextProcessor.deprocess(String) for empty string ", TextProcessor.deprocess(""), EMPTY_STRING);
	}
	
	public void testNullParams() {
		assertNull("TextProcessor.process(String) for null param ", TextProcessor.process(null));
		assertNull("TextProcessor.process(String, String) for params ", TextProcessor.process(null, null));
	}
	
	public void testNullParamsDeprocess() {
		assertNull("TextProcessor.deprocess(String) for null param ", TextProcessor.deprocess(null));
	}	
}
