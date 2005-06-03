/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.eclipseadaptor;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.internal.adaptor.FilePath;
import org.eclipse.osgi.tests.OSGiTest;

public class FilePathTest extends OSGiTest {

	public static Test suite() {
		return new TestSuite(FilePathTest.class);
	}

	public FilePathTest(String name) {
		super(name);
	}

	public void testColonOnPath() {
		FilePath path = new FilePath("/c:b/a");
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			// Windows-specific testing
			assertTrue("1.0", !path.isAbsolute());
			assertEquals("2.0", "c:", path.getDevice());
			String[] segments = path.getSegments();
			assertEquals("3.0", 2, segments.length);
			assertEquals("3.1", "b", segments[0]);
			assertEquals("3.2", "a", segments[1]);
			return;
		}
		// this runs on non-Windows platforms		
		assertTrue("1.0", path.isAbsolute());
		assertNull("2.0", path.getDevice());
		String[] segments = path.getSegments();
		assertEquals("3.0", 2, segments.length);
		assertEquals("3.1", "c:b", segments[0]);
		assertEquals("3.2", "a", segments[1]);
	}

}
