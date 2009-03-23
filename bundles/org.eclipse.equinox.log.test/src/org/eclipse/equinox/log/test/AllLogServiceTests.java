package org.eclipse.equinox.log.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllLogServiceTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("Test log service"); //$NON-NLS-1$
		suite.addTestSuite(LogServiceTest.class);
		suite.addTestSuite(LogReaderServiceTest.class);
		return suite;
	}
}
