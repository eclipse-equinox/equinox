package org.eclipse.equinox.common.tests.text;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	StringMatcherFindTest.class, 
	StringMatcherPlainTest.class, 
	StringMatcherWildcardTest.class
})
public class StringMatcherTests {
	// empty
}
