/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.resolver;

import junit.framework.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class VersionRangeTests extends TestCase {
	public void testSingleVersionRange() {
		VersionRange range;
		range = new VersionRange("[1.0.0, 1.0.0.-)");
		assertEquals("0.1", Version.parseVersion("1.0"), range.getMinimum());
		assertTrue("0.9", !range.isIncluded(Version.parseVersion("0.9")));
		assertTrue("1.0", range.isIncluded(Version.parseVersion("1")));
		assertTrue("1.1", range.isIncluded(Version.parseVersion("1.0")));
		assertTrue("1.2", range.isIncluded(Version.parseVersion("1.0.0")));
		assertTrue("2.1", !range.isIncluded(Version.parseVersion("1.0.0.0")));
		assertTrue("2.2", !range.isIncluded(Version.parseVersion("1.0.1")));
		assertTrue("2.3", !range.isIncluded(Version.parseVersion("1.1")));
		assertTrue("2.4", !range.isIncluded(Version.parseVersion("2")));
	}

	public void testInvertedRange() {
		VersionRange range;
		range = new VersionRange("[2.0.0, 1.0.0]");
		assertTrue("1.0", !range.isIncluded(Version.parseVersion("1")));
		assertTrue("1.1", !range.isIncluded(Version.parseVersion("1.5")));
		assertTrue("1.2", !range.isIncluded(Version.parseVersion("2.0")));
		assertTrue("1.3", !range.isIncluded(Version.parseVersion("2.5")));
		assertTrue("1.4", !range.isIncluded(Version.parseVersion("0.5")));
	}

	public void testGreaterThan() {
		// any version equal or greater than 1.0 is ok 
		VersionRange lowerBound = new VersionRange("1.0.0");
		assertTrue("1.0", !lowerBound.isIncluded(Version.parseVersion("0.9")));
		assertTrue("1.1", lowerBound.isIncluded(Version.parseVersion("1.0")));
		assertTrue("1.2", lowerBound.isIncluded(Version.parseVersion("1.9.9.x")));
		assertTrue("1.3", lowerBound.isIncluded(Version.parseVersion("999.999.999.foo")));
	}

	public void testLowerThan() {
		// any version lower than 2.0 is ok 		
		VersionRange upperBound = new VersionRange("[0,2.0)");
		assertTrue("1.0", upperBound.isIncluded(Version.parseVersion("0.0")));
		assertTrue("1.1", upperBound.isIncluded(Version.parseVersion("0.9")));
		assertTrue("1.2", upperBound.isIncluded(Version.parseVersion("1.0")));
		assertTrue("1.3", upperBound.isIncluded(Version.parseVersion("1.9.9.x")));
		assertTrue("1.4", !upperBound.isIncluded(Version.parseVersion("2.0")));
		assertTrue("1.5", !upperBound.isIncluded(Version.parseVersion("2.1")));
	}

	public static Test suite() {
		return new TestSuite(VersionRangeTests.class);
	}
}