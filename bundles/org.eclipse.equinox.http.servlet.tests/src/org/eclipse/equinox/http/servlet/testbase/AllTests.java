
package org.eclipse.equinox.http.servlet.testbase;

import org.eclipse.equinox.http.servlet.tests.DispatchingTest;
import org.eclipse.equinox.http.servlet.tests.ServletTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(DispatchingTest.class));
        suite.addTest(new TestSuite(ServletTest.class));
        return suite;
    }

}
