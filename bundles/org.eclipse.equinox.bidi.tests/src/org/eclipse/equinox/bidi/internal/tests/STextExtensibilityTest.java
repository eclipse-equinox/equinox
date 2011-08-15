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

import org.eclipse.equinox.bidi.STextTypeHandlerFactory;
import org.eclipse.equinox.bidi.advanced.ISTextExpert;
import org.eclipse.equinox.bidi.advanced.STextExpertFactory;
import org.eclipse.equinox.bidi.custom.STextTypeHandler;

/**
 * Tests contribution of BiDi handlers.
 */
public class STextExtensibilityTest extends STextTestBase {

	public void testBaseContributions() {
		String[] types = STextTypeHandlerFactory.getAllHandlerIDs();
		assertNotNull(types);
		assertTrue(types.length > 0);

		// check one of the types that we know should be there
		assertTrue(isTypePresent(types, "regex"));

		STextTypeHandler handler = STextTypeHandlerFactory.getHandler("regex");
		assertNotNull(handler);
	}

	public void testOtherContributions() {
		String[] types = STextTypeHandlerFactory.getAllHandlerIDs();
		assertNotNull(types);
		assertTrue(types.length > 0);

		// check the type added by the test bundle
		assertTrue(isTypePresent(types, "test"));

		STextTypeHandler handler = STextTypeHandlerFactory.getHandler("test");
		assertNotNull(handler);

		handler = STextTypeHandlerFactory.getHandler("badtest");
		assertNull(handler);

		String data, lean, full, model;
		data = "ABC.DEF:HOST-COM=HELLO";
		lean = toUT16(data);
		handler = STextTypeHandlerFactory.getHandler("test");

		ISTextExpert expert = STextExpertFactory.getExpert("test");
		full = expert.leanToFullText(lean);

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
