/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.bidi.internal.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.equinox.bidi.complexp.IComplExpProcessor;
import org.eclipse.equinox.bidi.complexp.StringProcessor;

/**
 * Tests contribution of BiDi processors.
 */
public class ExtensibilityTest extends TestCase {
	public static Test suite() {
		return new TestSuite(ExtensibilityTest.class);
	}

	public ExtensibilityTest() {
		super();
	}

	public ExtensibilityTest(String name) {
		super(name);
	}

	public void testBaseContributions() {
		String[] types = StringProcessor.getKnownTypes();
		assertNotNull(types);
		assertTrue(types.length > 0);
		
		// check one of the types that we know should be there
		assertTrue(isTypePresent(types, "regex"));
		
		IComplExpProcessor processor = StringProcessor.getProcessor("regex");
		assertNotNull(processor);
	}
	
	public void testOtherContributions() {
		String[] types = StringProcessor.getKnownTypes();
		assertNotNull(types);
		assertTrue(types.length > 0);
		
		// check the type added by the test bundle
		assertTrue(isTypePresent(types, "test"));
		
		IComplExpProcessor processor = StringProcessor.getProcessor("test");
		assertNotNull(processor);
	}
	
	private boolean isTypePresent(String[] types, String type) {
		for(int i = 0; i < types.length; i++) {
			if ("regex".equalsIgnoreCase(types[i]))
				return true;
		}
		return false;
	}
}
