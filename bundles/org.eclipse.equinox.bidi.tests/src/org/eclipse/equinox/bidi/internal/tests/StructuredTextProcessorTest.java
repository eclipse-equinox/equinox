/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.bidi.internal.tests;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.eclipse.equinox.bidi.StructuredTextProcessor;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.equinox.bidi.advanced.IStructuredTextExpert;
import org.eclipse.equinox.bidi.advanced.StructuredTextExpertFactory;
import org.junit.*;

/**
 * Tests methods in BidiComplexUtil
 */

public class StructuredTextProcessorTest extends StructuredTextTestBase {

	private static final String HEBREW = "iw";

	private static final String HEBREW2 = "he";

	private static final String ARABIC = "ar";

	private static final String FARSI = "fa";

	private static final String URDU = "ur";

	private Locale locale;

	@Before
	public void setUp() throws Exception {
		locale = Locale.getDefault();
	}

	@After
	public void tearDown() {
		Locale.setDefault(locale);
	}

	private void doTest1(String data, String result) {
		Locale.setDefault(Locale.ENGLISH);
		String full = StructuredTextProcessor.process(toUT16(data));
		// Since https://bugs.eclipse.org/467836 , processing also works in non-bidi
		// locales:
		assertEquals("Util #1 full EN - ", result, toPseudo(full));
		Locale.setDefault(new Locale(HEBREW2));
		full = StructuredTextProcessor.process(toUT16(data));
		assertEquals("Util #1 full HE - ", result, toPseudo(full));
		Locale.setDefault(new Locale(ARABIC));
		full = StructuredTextProcessor.process(toUT16(data));
		assertEquals("Util #1 full AR - ", result, toPseudo(full));
		Locale.setDefault(new Locale(FARSI));
		full = StructuredTextProcessor.process(toUT16(data));
		assertEquals("Util #1 full FA - ", result, toPseudo(full));
		Locale.setDefault(new Locale(URDU));
		full = StructuredTextProcessor.process(toUT16(data));
		assertEquals("Util #1 full UR - ", result, toPseudo(full));
		Locale.setDefault(new Locale(HEBREW));
		full = StructuredTextProcessor.process(toUT16(data));
		String ful2 = StructuredTextProcessor.process(toUT16(data), (String) null);
		assertEquals("Util #1 full - ", result, toPseudo(full));
		assertEquals("Util #1 ful2 - ", result, toPseudo(ful2));
		String lean = StructuredTextProcessor.deprocess(full);
		assertEquals("Util #1 lean - ", data, toPseudo(lean));
	}

	private void doTest2(String msg, String data, String result) {
		doTest2(msg, data, result, data);
	}

	private void doTest2(String msg, String data, String result, String resLean) {
		String full = StructuredTextProcessor.process(toUT16(data), "*");
		assertEquals(msg + "full", result, toPseudo(full));
		String lean = StructuredTextProcessor.deprocess(full);
		assertEquals(msg + "lean", resLean, toPseudo(lean));
	}

	private void doTest3(String msg, String data, String result) {
		doTest3(msg, data, result, data);
	}

	private void doTest3(String msg, String data, String result, String resLean) {
		String full = StructuredTextProcessor.processTyped(toUT16(data),
				StructuredTextTypeHandlerFactory.COMMA_DELIMITED);
		assertEquals(msg + "full", result, toPseudo(full));
		String lean = StructuredTextProcessor.deprocessTyped(full, StructuredTextTypeHandlerFactory.COMMA_DELIMITED);
		assertEquals(msg + "lean", resLean, toPseudo(lean));
	}

	private void doTest4(String msg, String data, int[] offsets, int direction, int affixLength, String result) {
		String txt = msg + "text=" + data + "\n    offsets=" + array_display(offsets) + "\n    direction=" + direction
				+ "\n    affixLength=" + affixLength;
		String lean = toUT16(data);
		IStructuredTextExpert expert = StructuredTextExpertFactory.getExpert();
		String full = expert.insertMarks(lean, offsets, direction, affixLength);
		assertEquals(txt, result, toPseudo(full));
	}

	@Test
	public void testStructuredTextProcessor() {
		// Test process() and deprocess() with default delimiters
		doTest1("ABC/DEF/G", ">@ABC@/DEF@/G@^");
		// Test process() and deprocess() with specified delimiters
		doTest2("Util #2.1 - ", "", "");
		doTest2("Util #2.2 - ", ">@ABC@^", ">@ABC@^", "ABC");
		doTest2("Util #2.3 - ", "abc", "abc");
		doTest2("Util #2.4 - ", "!abc", ">@!abc@^");
		doTest2("Util #2.5 - ", "abc!", ">@abc!@^");
		doTest2("Util #2.6 - ", "ABC*DEF*G", ">@ABC@*DEF@*G@^");
		// Test process() and deprocess() with specified expression type
		doTest3("Util #3.1 - ", "ABC,DEF,G", ">@ABC@,DEF@,G@^");
		doTest3("Util #3.2 - ", "", "");
		doTest3("Util #3.3 - ", ">@DEF@^", ">@DEF@^", "DEF");
		// Test insertMarks()
		doTest4("Util #4.1 - ", "ABCDEFG", new int[] { 3, 6 }, 0, 0, "ABC@DEF@G");
		doTest4("Util #4.2 - ", "ABCDEFG", new int[] { 3, 6 }, 0, 2, ">@ABC@DEF@G@^");
		doTest4("Util #4.3 - ", "ABCDEFG", new int[] { 3, 6 }, 1, 0, "ABC&DEF&G");
		doTest4("Util #4.4 - ", "ABCDEFG", new int[] { 3, 6 }, 1, 2, "<&ABC&DEF&G&^");
		doTest4("Util #4.5 - ", "", new int[] { 3, 6 }, 0, 0, "");
		doTest4("Util #4.6 - ", "", new int[] { 3, 6 }, 0, 2, "");
		doTest4("Util #4.7 - ", "ABCDEFG", null, 1, 0, "ABCDEFG");
		doTest4("Util #4.8 - ", "ABCDEFG", null, 1, 2, "<&ABCDEFG&^");
	}
}
