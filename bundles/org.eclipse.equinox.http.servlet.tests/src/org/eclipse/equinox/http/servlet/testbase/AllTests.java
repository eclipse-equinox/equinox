
package org.eclipse.equinox.http.servlet.testbase;

import org.eclipse.equinox.http.servlet.tests.DispatchingTest;
import org.eclipse.equinox.http.servlet.tests.ServletTest;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new JUnit4TestAdapter(DispatchingTest.class));
        suite.addTest(new JUnit4TestAdapter(ServletTest.class));
        return suite;
    }

}
