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

import org.eclipse.equinox.bidi.STextEngine;
import org.eclipse.equinox.bidi.custom.STextStringProcessor;
import org.eclipse.equinox.bidi.custom.ISTextProcessor;

/**
 * Tests contribution of BiDi processors.
 */
public class STextExtensibilityTest extends STextTestBase {

	public void testBaseContributions() {
		String[] types = STextStringProcessor.getKnownTypes();
		assertNotNull(types);
		assertTrue(types.length > 0);

		// check one of the types that we know should be there
		assertTrue(isTypePresent(types, "regex"));

		ISTextProcessor processor = STextStringProcessor.getProcessor("regex");
		assertNotNull(processor);
	}

	public void testOtherContributions() {
		String[] types = STextStringProcessor.getKnownTypes();
		assertNotNull(types);
		assertTrue(types.length > 0);

		// check the type added by the test bundle
		assertTrue(isTypePresent(types, "test"));

		ISTextProcessor processor = STextStringProcessor.getProcessor("test");
		assertNotNull(processor);

		processor = STextStringProcessor.getProcessor("badtest");
		assertNull(processor);

		String data, lean, full, model;
		data = "ABC.DEF:HOST-COM=HELLO";
		lean = toUT16(data);
		full = STextEngine.leanToFullText("test", null, null, lean, null);
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
