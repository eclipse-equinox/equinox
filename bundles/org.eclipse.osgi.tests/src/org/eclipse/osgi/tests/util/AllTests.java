package org.eclipse.osgi.tests.util;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {

	// Test suite to run all text processor session tests
	public static Test suite(){
		return new AllTests();
	}
	
	public AllTests() {
		addBidiTests();
		addLatinTests();
	}

	private void addBidiTests(){
		addTest(new TextProcessorSessionTest("org.eclipse.osgi.tests", BidiTextProcessorTestCase.class, "iw"));
	}
	
	private void addLatinTests(){
		addTest(new TextProcessorSessionTest("org.eclipse.osgi.tests", LatinTextProcessorTestCase.class, "en"));
	}
}
