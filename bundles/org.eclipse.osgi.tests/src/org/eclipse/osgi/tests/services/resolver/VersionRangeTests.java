/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
		range = new VersionRange("[1.0.0, 1.0.0.-)"); //$NON-NLS-1$
		assertEquals("0.1", Version.parseVersion("1.0"), range.getMinimum()); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("0.9", !range.isIncluded(Version.parseVersion("0.9"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.0", range.isIncluded(Version.parseVersion("1"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.1", range.isIncluded(Version.parseVersion("1.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.2", range.isIncluded(Version.parseVersion("1.0.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("2.1", !range.isIncluded(Version.parseVersion("1.0.0.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("2.2", !range.isIncluded(Version.parseVersion("1.0.1"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("2.3", !range.isIncluded(Version.parseVersion("1.1"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("2.4", !range.isIncluded(Version.parseVersion("2"))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testInvertedRange() {
		VersionRange range;
		range = new VersionRange("[2.0.0, 1.0.0]"); //$NON-NLS-1$
		assertTrue("1.0", !range.isIncluded(Version.parseVersion("1"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.1", !range.isIncluded(Version.parseVersion("1.5"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.2", !range.isIncluded(Version.parseVersion("2.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.3", !range.isIncluded(Version.parseVersion("2.5"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.4", !range.isIncluded(Version.parseVersion("0.5"))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testGreaterThan() {
		// any version equal or greater than 1.0 is ok 
		VersionRange lowerBound = new VersionRange("1.0.0"); //$NON-NLS-1$
		assertTrue("1.0", !lowerBound.isIncluded(Version.parseVersion("0.9"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.1", lowerBound.isIncluded(Version.parseVersion("1.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.2", lowerBound.isIncluded(Version.parseVersion("1.9.9.x"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.3", lowerBound.isIncluded(Version.parseVersion("999.999.999.foo"))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testLowerThan() {
		// any version lower than 2.0 is ok 		
		VersionRange upperBound = new VersionRange("[0,2.0)"); //$NON-NLS-1$
		assertTrue("1.0", upperBound.isIncluded(Version.parseVersion("0.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.1", upperBound.isIncluded(Version.parseVersion("0.9"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.2", upperBound.isIncluded(Version.parseVersion("1.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.3", upperBound.isIncluded(Version.parseVersion("1.9.9.x"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.4", !upperBound.isIncluded(Version.parseVersion("2.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.5", !upperBound.isIncluded(Version.parseVersion("2.1"))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testNullMin() {
		VersionRange nullMin = new VersionRange(null, true, new Version("1.0"), false); //$NON-NLS-1$
		assertNotNull("0.1", nullMin.getMinimum()); //$NON-NLS-1$
		assertEquals("0.2", Version.emptyVersion, nullMin.getMinimum()); //$NON-NLS-1$
		assertTrue("1.0", nullMin.isIncluded(null)); //$NON-NLS-1$
		assertTrue("1.1", nullMin.isIncluded(new Version("0.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.2", nullMin.isIncluded(new Version("0.9.9"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.3", nullMin.isIncluded(Version.parseVersion("0.9.9.x"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("1.4", nullMin.isIncluded(Version.parseVersion("2.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("1.5", nullMin.isIncluded(Version.parseVersion("2.1"))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testNullMax() {
		VersionRange nullMaxAny = new VersionRange(new Version("0"), true, null, true); //$NON-NLS-1$
		assertTrue("1.0", nullMaxAny.isIncluded(Version.parseVersion("0.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.1", nullMaxAny.isIncluded(Version.parseVersion("0.9"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.2", nullMaxAny.isIncluded(Version.parseVersion("1.0"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.3", nullMaxAny.isIncluded(Version.parseVersion("1.9.9.x"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.4", nullMaxAny.isIncluded(Version.parseVersion("999.999.999.foo"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("1.5", nullMaxAny.isIncluded(new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE))); //$NON-NLS-1$
	}

	public static Test suite() {
		return new TestSuite(VersionRangeTests.class);
	}
}
