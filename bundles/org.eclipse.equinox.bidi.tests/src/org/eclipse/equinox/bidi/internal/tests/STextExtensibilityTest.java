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

	public void testOtherContributions() {
		STextTypeHandler handler = STextTypeHandlerFactory.getHandler("test.ID");
		assertNotNull(handler);

		handler = STextTypeHandlerFactory.getHandler("badtest");
		assertNull(handler);

		String data, lean, full, model;
		data = "ABC.DEF:HOST-COM=HELLO";
		lean = toUT16(data);
		handler = STextTypeHandlerFactory.getHandler("test");

		ISTextExpert expert = STextExpertFactory.getExpert("test.ID");
		full = expert.leanToFullText(lean);

		model = "ABC@.DEF@:HOST@-COM@=HELLO";
		assertEquals("Test 'test' plugin", model, toPseudo(full));
	}

}
