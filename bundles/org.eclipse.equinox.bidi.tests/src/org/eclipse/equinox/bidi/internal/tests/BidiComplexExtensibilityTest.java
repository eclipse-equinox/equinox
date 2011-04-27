/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.bidi.internal.tests;

import org.eclipse.equinox.bidi.BidiComplexEngine;
import org.eclipse.equinox.bidi.custom.BidiComplexStringProcessor;
import org.eclipse.equinox.bidi.custom.IBidiComplexProcessor;

/**
 * Tests contribution of BiDi processors.
 */
public class BidiComplexExtensibilityTest extends BidiComplexTestBase {

	public void testBaseContributions() {
		String[] types = BidiComplexStringProcessor.getKnownTypes();
		assertNotNull(types);
		assertTrue(types.length > 0);

		// check one of the types that we know should be there
		assertTrue(isTypePresent(types, "regex"));

		IBidiComplexProcessor processor = BidiComplexStringProcessor.getProcessor("regex");
		assertNotNull(processor);
	}

	public void testOtherContributions() {
		String[] types = BidiComplexStringProcessor.getKnownTypes();
		assertNotNull(types);
		assertTrue(types.length > 0);

		// check the type added by the test bundle
		assertTrue(isTypePresent(types, "test"));

		IBidiComplexProcessor processor = BidiComplexStringProcessor.getProcessor("test");
		assertNotNull(processor);

		processor = BidiComplexStringProcessor.getProcessor("badtest");
		assertNull(processor);

		String data, lean, full, model;
		data = "ABC.DEF:HOST-COM=HELLO";
		lean = toUT16(data);
		full = BidiComplexEngine.leanToFullText("test", null, null, lean, null);
		model = "ABC@.DEF@:HOST@-COM@=HELLO";
		assertEquals("Test 'test' plugin", model, toPseudo(full));
	}

	private boolean isTypePresent(String[] types, String type) {
		for (int i = 0; i < types.length; i++) {
			if (type.equalsIgnoreCase(types[i]))
				return true;
		}
		return false;
	}
}
