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

import org.eclipse.equinox.bidi.*;

/**
 * Tests most public methods of BidiComplexHelper
 */

public class BidiComplexMethodsTest extends BidiComplexTestBase {

	final static int LTR = BidiComplexFeatures.DIR_LTR;

	final static int RTL = BidiComplexFeatures.DIR_RTL;

	final static BidiComplexEnvironment envLTR = new BidiComplexEnvironment(null, false, BidiComplexEnvironment.ORIENT_LTR);

	final static BidiComplexEnvironment envRTL = new BidiComplexEnvironment(null, false, BidiComplexEnvironment.ORIENT_RTL);

	final static BidiComplexEnvironment envRTLMIR = new BidiComplexEnvironment(null, true, BidiComplexEnvironment.ORIENT_RTL);

	final static BidiComplexEnvironment envIGN = new BidiComplexEnvironment(null, false, BidiComplexEnvironment.ORIENT_IGNORE);

	final static BidiComplexEnvironment envCLR = new BidiComplexEnvironment(null, false, BidiComplexEnvironment.ORIENT_CONTEXTUAL_LTR);

	final static BidiComplexEnvironment envCRL = new BidiComplexEnvironment(null, false, BidiComplexEnvironment.ORIENT_CONTEXTUAL_RTL);

	final static BidiComplexEnvironment envERR = new BidiComplexEnvironment(null, false, 9999);

	BidiComplexHelper helper;

	private void doTestTools() {

		// This method tests utility methods used by the JUnits
		String data = "56789ABCDEFGHIJKLMNOPQRSTUVWXYZ~#@&><^|`";
		String text = toUT16(data);
		String dat2 = toPseudo(text);
		assertEquals(data, dat2);

		text = toPseudo(data);
		assertEquals("56789abcdefghijklmnopqrstuvwxyz~#@&><^|`", text);

		int[] arrayA = new int[] {1, 2};
		int[] arrayB = new int[] {3, 4, 5};
		assertFalse(arrays_equal(arrayA, arrayB));

		assertTrue(arrays_equal(arrayA, arrayA));

		arrayB = new int[] {3, 4};
		assertFalse(arrays_equal(arrayA, arrayB));

		text = array_display(null);
		assertEquals("null", text);
	}

	private void doTestState() {
		String data, lean, full, model;
		int state;

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "A@=B@+C@;/* D=E+F;";
		assertEquals("full1", model, toPseudo(full));
		state = helper.getFinalState();
		data = "A=B+C; D=E+F;";
		lean = toUT16(data);
		full = helper.leanToFullText(lean, state);
		model = "A=B+C; D=E+F;";
		assertEquals("full2", model, toPseudo(full));
		state = helper.getFinalState();
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		full = helper.leanToFullText(lean, state);
		model = "A=B+C;*/@ D@=E@+F;";
		assertEquals("full3", model, toPseudo(full));
	}

	private void doTestOrientation() {
		int orient;

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.COMMA_DELIMITED);
		orient = helper.getEnvironment().orientation;
		assertEquals("orient #1", BidiComplexEnvironment.ORIENT_LTR, orient);

		helper.setEnvironment(envIGN);
		orient = helper.getEnvironment().orientation;
		assertEquals("orient #2", BidiComplexEnvironment.ORIENT_IGNORE, orient);

		helper.setEnvironment(envCRL);
		orient = helper.getEnvironment().orientation;
		helper.leanToFullText("--!**");
		assertEquals("orient #3", BidiComplexEnvironment.ORIENT_CONTEXTUAL_RTL, orient);

		helper.setEnvironment(envERR);
		orient = helper.getEnvironment().orientation;
		helper.leanToFullText("--!**");
		assertEquals("orient #4", BidiComplexEnvironment.ORIENT_UNKNOWN, orient);
	}

	private void doTestOrient(BidiComplexFeatures f, String label, String data, String resLTR, String resRTL, String resCon) {
		String full, lean;

		lean = toUT16(data);
		helper.setEnvironment(envLTR);
		helper.setFeatures(f);
		full = helper.leanToFullText(lean);
		assertEquals(label + "LTR full", resLTR, toPseudo(full));
		helper.setEnvironment(envRTL);
		helper.setFeatures(f);
		full = helper.leanToFullText(lean);
		assertEquals("label + RTL full", resRTL, toPseudo(full));
		helper.setEnvironment(envCRL);
		helper.setFeatures(f);
		full = helper.leanToFullText(lean);
		assertEquals(label + "CON full", resCon, toPseudo(full));
	}

	private void doTestScripts() {
		BidiComplexFeatures f2, f1 = helper.getFeatures();
		boolean flag;
		flag = f1.ignoreArabic;
		assertFalse("Ignores Arabic 1", flag);
		flag = f1.ignoreHebrew;
		assertFalse("Ignores Hebrew 1", flag);

		f2 = new BidiComplexFeatures(f1.operators, 0, -1, -1, true, true);
		flag = f2.ignoreArabic;
		assertTrue("Ignores Arabic 2", flag);
		flag = f2.ignoreHebrew;
		assertTrue("Ignores Hebrew 2", flag);
		doTestOrient(f2, "Scripts #1 ", "BCD,EF", "BCD,EF", ">@BCD,EF@^", "@BCD,EF");
		f2 = new BidiComplexFeatures(f1.operators, 0, -1, -1, false, true);
		flag = f2.ignoreArabic;
		assertFalse("Ignores Arabic 3", flag);
		flag = f2.ignoreHebrew;
		assertTrue("Ignores Hebrew 3", flag);
		doTestOrient(f2, "Scripts #2 ", "d,EF", "d,EF", ">@d,EF@^", "d,EF");
		doTestOrient(f2, "Scripts #3 ", "#,eF", "#,eF", ">@#,eF@^", "@#,eF");
		doTestOrient(f2, "Scripts #4 ", "#,12", "#@,12", ">@#@,12@^", "@#@,12");
		doTestOrient(f2, "Scripts #5 ", "#,##", "#@,##", ">@#@,##@^", "@#@,##");
		doTestOrient(f2, "Scripts #6 ", "#,89", "#@,89", ">@#@,89@^", "@#@,89");
		doTestOrient(f2, "Scripts #7 ", "#,ef", "#,ef", ">@#,ef@^", "@#,ef");
		doTestOrient(f2, "Scripts #8 ", "#,", "#,", ">@#,@^", "@#,");
		doTestOrient(f2, "Scripts #9 ", "9,ef", "9,ef", ">@9,ef@^", "9,ef");
		doTestOrient(f2, "Scripts #10 ", "9,##", "9@,##", ">@9@,##@^", "9@,##");
		doTestOrient(f2, "Scripts #11 ", "7,89", "7@,89", ">@7@,89@^", "7@,89");
		doTestOrient(f2, "Scripts #12 ", "7,EF", "7,EF", ">@7,EF@^", "@7,EF");
		doTestOrient(f2, "Scripts #13 ", "BCD,EF", "BCD,EF", ">@BCD,EF@^", "@BCD,EF");

		f2 = new BidiComplexFeatures(f1.operators, 0, -1, -1, true, false);
		flag = f2.ignoreArabic;
		assertTrue("Ignores Arabic 4", flag);
		flag = f2.ignoreHebrew;
		assertFalse("Ignores Hebrew 4", flag);
		doTestOrient(f2, "Scripts #14 ", "BCd,EF", "BCd,EF", ">@BCd,EF@^", "@BCd,EF");
		doTestOrient(f2, "Scripts #15 ", "BCD,eF", "BCD,eF", ">@BCD,eF@^", "@BCD,eF");
		doTestOrient(f2, "Scripts #16 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient(f2, "Scripts #17 ", "BCD,12", "BCD@,12", ">@BCD@,12@^", "@BCD@,12");
		doTestOrient(f2, "Scripts #18 ", "BCD,", "BCD,", ">@BCD,@^", "@BCD,");

		f2 = new BidiComplexFeatures(f1.operators, 0, -1, -1, false, false);
		doTestOrient(f2, "Scripts #19 ", "123,45|67", "123,45|67", ">@123,45|67@^", "@123,45|67");
		doTestOrient(f2, "Scripts #20 ", "5,e", "5,e", ">@5,e@^", "5,e");
		doTestOrient(f2, "Scripts #21 ", "5,#", "5@,#", ">@5@,#@^", "5@,#");
		doTestOrient(f2, "Scripts #22 ", "5,6", "5@,6", ">@5@,6@^", "5@,6");
		doTestOrient(f2, "Scripts #23 ", "5,D", "5@,D", ">@5@,D@^", "5@,D");
		doTestOrient(f2, "Scripts #24 ", "5,--", "5,--", ">@5,--@^", "@5,--");
	}

	private void doTestLeanOffsets() {
		String lean, data, label;
		int state;
		int[] offsets;
		int[] model;

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		helper.leanToFullText(lean);
		offsets = helper.leanBidiCharOffsets();
		model = new int[] {1, 3, 5};
		label = "leanBidiCharOffsets() #1 expected=" + array_display(model) + " result=" + array_display(offsets);
		assertTrue(label, arrays_equal(model, offsets));
		state = helper.getFinalState();
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		helper.leanToFullText(lean, state);
		offsets = helper.leanBidiCharOffsets();
		model = new int[] {8, 10, 12};
		label = "leanBidiCharOffsets() #2 expected=" + array_display(model) + " result=" + array_display(offsets);
		assertTrue(label, arrays_equal(model, offsets));
		offsets = helper.leanBidiCharOffsets();
		model = new int[] {8, 10, 12};
		label = "leanBidiCharOffsets() #3 expected=" + array_display(model) + " result=" + array_display(offsets);
		assertTrue(label, arrays_equal(model, offsets));
	}

	private void doTestFullOffsets(String label, String data, int[] resLTR, int[] resRTL, int[] resCon) {
		String full, lean, msg;
		int[] offsets;

		lean = toUT16(data);
		helper.setEnvironment(envLTR);
		full = helper.leanToFullText(lean);
		// the following line avoids a compiler warning about full never being
		// read
		full += "";
		offsets = helper.fullBidiCharOffsets();
		msg = label + "LTR expected=" + array_display(resLTR) + " result=" + array_display(offsets);
		assertTrue(msg, arrays_equal(resLTR, offsets));
		helper.setEnvironment(envRTL);
		full = helper.leanToFullText(lean);
		offsets = helper.fullBidiCharOffsets();
		msg = label + "RTL expected=" + array_display(resRTL) + " result=" + array_display(offsets);
		assertTrue(msg, arrays_equal(resRTL, offsets));
		helper.setEnvironment(envCLR);
		full = helper.leanToFullText(lean);
		offsets = helper.fullBidiCharOffsets();
		msg = label + "CON expected=" + array_display(resCon) + " result=" + array_display(offsets);
		assertTrue(msg, arrays_equal(resCon, offsets));
	}

	private void doTestMirrored() {
		boolean mirrored;

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.COMMA_DELIMITED);
		mirrored = helper.getEnvironment().mirrored;
		assertFalse("mirrored #1", mirrored);
		BidiComplexEnvironment env = new BidiComplexEnvironment(null, true, BidiComplexEnvironment.ORIENT_LTR);
		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.COMMA_DELIMITED, env);
		mirrored = helper.getEnvironment().mirrored;
		assertTrue("mirrored #2", mirrored);
	}

	private void doTestDirection() {
		String data, lean, full, model, msg;

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.COMMA_DELIMITED);
		BidiComplexFeatures f1 = helper.getFeatures();
		msg = "TestDirection #1";
		assertTrue(msg, f1.dirArabic == LTR && f1.dirHebrew == LTR);

		BidiComplexFeatures f2 = new BidiComplexFeatures(f1.operators, 0, RTL, RTL, false, false);
		helper.setFeatures(f2);
		f1 = helper.getFeatures();
		msg = "TestDirection #2";
		assertTrue(msg, f1.dirArabic == RTL && f1.dirHebrew == RTL);

		BidiComplexEnvironment environment = new BidiComplexEnvironment(null, false, BidiComplexEnvironment.ORIENT_LTR);
		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.EMAIL, environment);
		f1 = helper.getFeatures();
		msg = "TestDirection #3";
		assertTrue(msg, f1.dirArabic == LTR && f1.dirHebrew == LTR);
		data = "#ABC.#DEF:HOST.com";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "#ABC@.#DEF@:HOST.com";
		assertEquals("TestDirection #9 full", model, toPseudo(full));

		data = "ABC.DEF:HOST.com";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "ABC@.DEF@:HOST.com";
		assertEquals("TestDirection #10 full", model, toPseudo(full));

		environment = new BidiComplexEnvironment(null, true, BidiComplexEnvironment.ORIENT_LTR);
		helper.setEnvironment(environment);
		f1 = helper.getFeatures();
		msg = "TestDirection #10.5";
		assertTrue(msg, f1.dirArabic == RTL && f1.dirHebrew == LTR);
		data = "#ABC.#DEF:HOST.com";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "<&#ABC.#DEF:HOST.com&^";
		assertEquals("TestDirection #11 full", model, toPseudo(full));

		data = "#ABc.#DEF:HOSt.COM";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "<&#ABc.#DEF:HOSt.COM&^";
		assertEquals("TestDirection #12 full", model, toPseudo(full));

		data = "#ABc.#DEF:HOSt.";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "<&#ABc.#DEF:HOSt.&^";
		assertEquals("TestDirection #13 full", model, toPseudo(full));

		data = "ABC.DEF:HOST.com";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "ABC@.DEF@:HOST.com";
		assertEquals("TestDirection #14 full", model, toPseudo(full));

		data = "--.---:----";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "--.---:----";
		assertEquals("TestDirection #15 full", model, toPseudo(full));

		data = "ABC.|DEF:HOST.com";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "ABC.|DEF@:HOST.com";
		assertEquals("TestDirection #16 full", model, toPseudo(full));

		helper.setEnvironment(envRTLMIR);
		data = "#ABc.|#DEF:HOST.com";
		lean = toUT16(data);
		full = helper.leanToFullText(lean);
		model = "#ABc.|#DEF:HOST.com";
		assertEquals("TestDirection #17 full", model, toPseudo(full));
		assertEquals("Test curDirection", RTL, helper.getCurDirection());
	}

	public void testMethods() {

		doTestTools();

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.JAVA);
		doTestState();

		doTestOrientation();

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.COMMA_DELIMITED);
		BidiComplexFeatures f2 = helper.getFeatures();
		doTestOrient(f2, "Methods #1 ", "", "", "", "");
		doTestOrient(f2, "Methods #2 ", "abc", "abc", ">@abc@^", "abc");
		doTestOrient(f2, "Methods #3 ", "ABC", "ABC", ">@ABC@^", "@ABC");
		doTestOrient(f2, "Methods #4 ", "bcd,ef", "bcd,ef", ">@bcd,ef@^", "bcd,ef");
		doTestOrient(f2, "Methods #5 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient(f2, "Methods #6 ", "cde,FG", "cde,FG", ">@cde,FG@^", "cde,FG");
		doTestOrient(f2, "Methods #7 ", "CDE,fg", "CDE,fg", ">@CDE,fg@^", "@CDE,fg");
		doTestOrient(f2, "Methods #8 ", "12..def,GH", "12..def,GH", ">@12..def,GH@^", "12..def,GH");
		doTestOrient(f2, "Methods #9 ", "34..DEF,gh", "34..DEF,gh", ">@34..DEF,gh@^", "@34..DEF,gh");

		doTestScripts();

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.JAVA);
		doTestLeanOffsets();

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.COMMA_DELIMITED);
		doTestFullOffsets("TestFullOffsets ", "BCD,EF,G", new int[] {3, 7}, new int[] {0, 1, 5, 9, 12, 13}, new int[] {0, 4, 8});

		doTestMirrored();

		doTestDirection();

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.COMMA_DELIMITED);
		String data = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
		String lean = toUT16(data);
		String full = helper.leanToFullText(lean);
		String model = "A@,B@,C@,D@,E@,F@,G@,H@,I@,J@,K@,L@,M@,N@,O@,P@,Q@,R@,S@,T@,U@,V@,W@,X@,Y@,Z";
		assertEquals("many inserts", model, toPseudo(full));
	}
}
