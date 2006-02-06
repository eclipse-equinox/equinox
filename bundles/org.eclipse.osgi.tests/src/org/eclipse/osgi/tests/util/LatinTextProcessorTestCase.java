package org.eclipse.osgi.tests.util;

import org.eclipse.osgi.util.TextProcessor;

/**
 * Test for strings that use the TextProcessor but are not run in a bidi locale. 
 * Latin locales should return the same String that was passed in.
 *
 */
public class LatinTextProcessorTestCase extends TextProcessorTestCase {
	private static String[] ALL_PATHS;
	static{
		// merge all test strings into one array for Latin locales
		int size = TEST_DEFAULT_PATHS.length + TEST_STAR_PATHS.length + TEST_EQUALS_PATHS.length;
		ALL_PATHS = new String[size];
		int idx = 0;
		for (int i = 0; i < TEST_DEFAULT_PATHS.length; i++){
			ALL_PATHS[idx] = TEST_DEFAULT_PATHS[i];
			idx++;
		}
		for (int i = 0; i < TEST_STAR_PATHS.length; i++){
			ALL_PATHS[idx] = TEST_STAR_PATHS[i];
			idx++;
		}
		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++){
			ALL_PATHS[idx] = TEST_EQUALS_PATHS[i];
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
	
	public void testLatinPaths(){
		// test all strings using process(String) method
		for (int i = 0; i < ALL_PATHS.length; i++){
			String result = TextProcessor.process(ALL_PATHS[i]);
			verifyLatinResult("String " + (i + 1), result, ALL_PATHS[i]);
		}
	}
	
	public void testLatinOtherStrings(){
		// test the process(String, String) method
		for (int i = 0; i < TEST_STAR_PATHS.length; i++){
			String result = TextProcessor.process(TEST_STAR_PATHS[i], "*.");
			verifyLatinResult("File association " + (i + 1), result, TEST_STAR_PATHS[i]);
		}
		
		for (int i = 0; i < TEST_EQUALS_PATHS.length; i++){
			String result = TextProcessor.process(TEST_EQUALS_PATHS[i], "*.");
			verifyLatinResult("Equals expression " + (i + 1), result, TEST_EQUALS_PATHS[i]);
		}
	}
	
	private void verifyLatinResult(String testName, String expected, String result){
		assertTrue(testName + " result string is not the same as string passed in.",
				result.equals(expected));
	}
}
