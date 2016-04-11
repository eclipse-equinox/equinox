
package org.eclipse.equinox.http.servlet.testbase;

import org.eclipse.equinox.http.servlet.tests.DispatchingTest;
import org.eclipse.equinox.http.servlet.tests.ServletTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({DispatchingTest.class,ServletTest.class})
public class AllTests {
	// see @SuiteClasses
}
