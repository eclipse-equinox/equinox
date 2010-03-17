/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.bidi.internal.tests;

import org.eclipse.equinox.bidi.complexp.IComplExpProcessor;
import org.eclipse.equinox.bidi.complexp.IBiDiProcessor;
import org.eclipse.equinox.bidi.complexp.StringProcessor;
import org.eclipse.equinox.bidi.complexp.ComplExpUtil;

/**
 * Tests most public methods of ComplExpBasic
 */

public class MethodsTest extends ComplExpTestBase {

	final static int LTR = IComplExpProcessor.DIRECTION_LTR;
	final static int RTL = IComplExpProcessor.DIRECTION_RTL;

	IComplExpProcessor processor;

	private void doTestTools() {

		// This method tests utility methods used by the JUnits
		String data = "56789ABCDEFGHIJKLMNOPQRSTUVWXYZ~#@&><^|`";
		String text = toUT16(data);
		String dat2 = toPseudo(text);
		assertEquals(data, dat2);

		text = toPseudo(data);
		assertEquals("56789abcdefghijklmnopqrstuvwxyz~#@&><^|`", text);

		int[] arA = new int[] { 1, 2 };
		int[] arB = new int[] { 3, 4, 5 };
		assertFalse(arrays_equal(arA, arB));

		assertTrue(arrays_equal(arA, arA));

		arB = new int[] { 3, 4 };
		assertFalse(arrays_equal(arA, arB));

		int[][] ar2A = new int[][] { { 1 }, { 1, 2 }, { 1, 2, 3 } };
		int[][] ar2B = new int[][] { { 1 }, { 1, 2 } };
		assertTrue(arrays2_equal(ar2A, ar2A));

		assertFalse(arrays2_equal(ar2A, ar2B));

		ar2B = new int[][] { { 1 }, { 1, 2 }, { 1, 2, 3, 4 } };
		assertFalse(arrays2_equal(ar2A, ar2B));

		ar2B = new int[][] { { 1 }, { 1, 2 }, { 1, 2, 4 } };
		assertFalse(arrays2_equal(ar2A, ar2B));

		text = array_display(null);
		assertEquals("null", text);

		text = array2_display(null);
		assertEquals("null", text);
	}

	private void doTestState() {
		String data, lean, full, model;
		int state;

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "A@=B@+C@;/* D=E+F;";
		assertEquals("full1", model, toPseudo(full));
		state = processor.getFinalState();
		data = "A=B+C; D=E+F;";
		lean = toUT16(data);
		full = processor.leanToFullText(lean, state);
		model = "A=B+C; D=E+F;";
		assertEquals("full2", model, toPseudo(full));
		state = processor.getFinalState();
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		full = processor.leanToFullText(lean, state);
		model = "A=B+C;*/@ D@=E@+F;";
		assertEquals("full3", model, toPseudo(full));
	}

	private void doTestOrientation() {
		int orient;

		processor = StringProcessor
				.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
		orient = processor.recallOrientation();
		// TBD: the following test cannot succeed with the current
		// implementation.
		// it will need allocating separate data for each processor use.
		// assertEquals("orient #1", IComplExpProcessor.ORIENT_LTR, orient);

		processor.assumeOrientation(IComplExpProcessor.ORIENT_IGNORE);
		orient = processor.recallOrientation();
		assertEquals("orient #2", IComplExpProcessor.ORIENT_IGNORE, orient);

		processor.assumeOrientation(IComplExpProcessor.ORIENT_CONTEXTUAL_RTL);
		orient = processor.recallOrientation();
		processor.leanToFullText("--!**");
		assertEquals("orient #3", IComplExpProcessor.ORIENT_CONTEXTUAL_RTL,
				orient);

		processor.assumeOrientation(9999);
		orient = processor.recallOrientation();
		processor.leanToFullText("--!**");
		assertEquals("orient #4", IComplExpProcessor.ORIENT_UNKNOWN, orient);
	}

	private void doTestOrient(String label, String data, String resLTR,
			String resRTL, String resCon) {
		String full, lean;

		lean = toUT16(data);
		processor.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
		full = processor.leanToFullText(lean);
		assertEquals(label + "LTR full", resLTR, toPseudo(full));
		processor.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
		full = processor.leanToFullText(lean);
		assertEquals("label + RTL full", resRTL, toPseudo(full));
		processor.assumeOrientation(IComplExpProcessor.ORIENT_CONTEXTUAL_RTL);
		full = processor.leanToFullText(lean);
		assertEquals(label + "CON full", resCon, toPseudo(full));
	}

	private void doTestScripts() {
		boolean flag;
		flag = processor.handlesArabicScript();
		assertTrue("Handles Arabic 1", flag);
		flag = processor.handlesHebrewScript();
		assertTrue("Handles Hebrew 1", flag);

		processor.selectBidiScript(false, false);
		flag = processor.handlesArabicScript();
		assertFalse("Handles Arabic 2", flag);
		flag = processor.handlesHebrewScript();
		assertFalse("Handles Hebrew 2", flag);
		doTestOrient("Scripts #1 ", "BCD,EF", "BCD,EF", ">@BCD,EF@^", "@BCD,EF");
		processor.selectBidiScript(true, false);
		flag = processor.handlesArabicScript();
		assertTrue("Handles Arabic 3", flag);
		flag = processor.handlesHebrewScript();
		assertFalse("Handles Hebrew 3", flag);
		doTestOrient("Scripts #2 ", "d,EF", "d,EF", ">@d,EF@^", "d,EF");
		doTestOrient("Scripts #3 ", "#,eF", "#,eF", ">@#,eF@^", "@#,eF");
		doTestOrient("Scripts #4 ", "#,12", "#@,12", ">@#@,12@^", "@#@,12");
		doTestOrient("Scripts #5 ", "#,##", "#@,##", ">@#@,##@^", "@#@,##");
		doTestOrient("Scripts #6 ", "#,89", "#@,89", ">@#@,89@^", "@#@,89");
		doTestOrient("Scripts #7 ", "#,ef", "#,ef", ">@#,ef@^", "@#,ef");
		doTestOrient("Scripts #8 ", "#,", "#,", ">@#,@^", "@#,");
		doTestOrient("Scripts #9 ", "9,ef", "9,ef", ">@9,ef@^", "9,ef");
		doTestOrient("Scripts #10 ", "9,##", "9@,##", ">@9@,##@^", "9@,##");
		doTestOrient("Scripts #11 ", "7,89", "7@,89", ">@7@,89@^", "7@,89");
		doTestOrient("Scripts #12 ", "7,EF", "7,EF", ">@7,EF@^", "@7,EF");
		doTestOrient("Scripts #13 ", "BCD,EF", "BCD,EF", ">@BCD,EF@^",
				"@BCD,EF");

		processor.selectBidiScript(false, true);
		flag = processor.handlesArabicScript();
		assertFalse("Handles Arabic 4", flag);
		flag = processor.handlesHebrewScript();
		assertTrue("Handles Hebrew 4", flag);
		doTestOrient("Scripts #14 ", "BCd,EF", "BCd,EF", ">@BCd,EF@^",
				"@BCd,EF");
		doTestOrient("Scripts #15 ", "BCD,eF", "BCD,eF", ">@BCD,eF@^",
				"@BCD,eF");
		doTestOrient("Scripts #16 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^",
				"@BCD@,EF");
		doTestOrient("Scripts #17 ", "BCD,12", "BCD@,12", ">@BCD@,12@^",
				"@BCD@,12");
		doTestOrient("Scripts #18 ", "BCD,", "BCD,", ">@BCD,@^", "@BCD,");

		processor.selectBidiScript(true, true);
		doTestOrient("Scripts #19 ", "123,45|67", "123,45|67", ">@123,45|67@^",
				"@123,45|67");
		doTestOrient("Scripts #20 ", "5,e", "5,e", ">@5,e@^", "5,e");
		doTestOrient("Scripts #21 ", "5,#", "5@,#", ">@5@,#@^", "5@,#");
		doTestOrient("Scripts #22 ", "5,6", "5@,6", ">@5@,6@^", "5@,6");
		doTestOrient("Scripts #23 ", "5,D", "5@,D", ">@5@,D@^", "5@,D");
		doTestOrient("Scripts #24 ", "5,--", "5,--", ">@5,--@^", "@5,--");
	}

	private void doTestLeanOffsets() {
		String lean, data, label;
		int state;
		int[] offsets;
		int[] model;

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		offsets = processor.leanBidiCharOffsets(lean);
		model = new int[] { 1, 3, 5 };
		label = "leanBidiCharOffsets() #1 expected=" + array_display(model)
				+ " result=" + array_display(offsets);
		assertTrue(label, arrays_equal(model, offsets));
		state = processor.getFinalState();
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		offsets = processor.leanBidiCharOffsets(lean, state);
		model = new int[] { 8, 10, 12 };
		label = "leanBidiCharOffsets() #2 expected=" + array_display(model)
				+ " result=" + array_display(offsets);
		assertTrue(label, arrays_equal(model, offsets));
		offsets = processor.leanBidiCharOffsets();
		model = new int[] { 8, 10, 12 };
		label = "leanBidiCharOffsets() #3 expected=" + array_display(model)
				+ " result=" + array_display(offsets);
		assertTrue(label, arrays_equal(model, offsets));
	}

	private void doTestFullOffsets(String label, String data, int[] resLTR,
			int[] resRTL, int[] resCon) {
		String full, lean, msg;
		int[] offsets;

		lean = toUT16(data);
		processor.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
		full = processor.leanToFullText(lean);
		// the following line avoids a compiler warning about full never being
		// read
		full += "";
		offsets = processor.fullBidiCharOffsets();
		msg = label + "LTR expected=" + array_display(resLTR) + " result="
				+ array_display(offsets);
		assertTrue(msg, arrays_equal(resLTR, offsets));
		processor.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
		full = processor.leanToFullText(lean);
		offsets = processor.fullBidiCharOffsets();
		msg = label + "RTL expected=" + array_display(resRTL) + " result="
				+ array_display(offsets);
		assertTrue(msg, arrays_equal(resRTL, offsets));
		processor.assumeOrientation(IComplExpProcessor.ORIENT_CONTEXTUAL_LTR);
		full = processor.leanToFullText(lean);
		offsets = processor.fullBidiCharOffsets();
		msg = label + "CON expected=" + array_display(resCon) + " result="
				+ array_display(offsets);
		assertTrue(msg, arrays_equal(resCon, offsets));
	}

	private void doTestMirrored() {
		boolean mirrored;

		mirrored = ComplExpUtil.isMirroredDefault();
		assertFalse("mirrored #1", mirrored);
		processor = StringProcessor
				.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
		mirrored = processor.isMirrored();
		assertFalse("mirrored #2", mirrored);
		ComplExpUtil.assumeMirroredDefault(true);
		mirrored = ComplExpUtil.isMirroredDefault();
		assertTrue("mirrored #3", mirrored);
		processor = StringProcessor
				.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
		mirrored = processor.isMirrored();
		// TBD: the following test cannot succeed with the current
		// implementation.
		// it will need allocating separate data for each processor use.
		// assertTrue("mirrored #4", mirrored);
		processor.assumeMirrored(false);
		mirrored = processor.isMirrored();
		assertFalse("mirrored #5", mirrored);
	}

	private void doTestDirection() {
		int[][] dir;
		int[][] modir;
		String data, lean, full, model, msg;

		processor = StringProcessor
				.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
		dir = processor.getDirection();
		modir = new int[][] { { LTR, LTR }, { LTR, LTR } };
		msg = "TestDirection #1 expected=" + array2_display(modir) + " result="
				+ array2_display(dir);
		assertTrue(msg, arrays2_equal(modir, dir));

		processor.setDirection(RTL);
		dir = processor.getDirection();
		modir = new int[][] { { RTL, RTL }, { RTL, RTL } };
		msg = "TestDirection #2 expected=" + array2_display(modir) + " result="
				+ array2_display(dir);
		assertTrue(msg, arrays2_equal(modir, dir));

		processor.setDirection(LTR, RTL);
		dir = processor.getDirection();
		modir = new int[][] { { LTR, RTL }, { LTR, RTL } };
		msg = "TestDirection #3 expected=" + array2_display(modir) + " result="
				+ array2_display(dir);
		assertTrue(msg, arrays2_equal(modir, dir));

		processor.setArabicDirection(RTL);
		dir = processor.getDirection();
		modir = new int[][] { { RTL, RTL }, { LTR, RTL } };
		msg = "TestDirection #4 expected=" + array2_display(modir) + " result="
				+ array2_display(dir);
		assertTrue(msg, arrays2_equal(modir, dir));

		processor.setArabicDirection(RTL, LTR);
		dir = processor.getDirection();
		modir = new int[][] { { RTL, LTR }, { LTR, RTL } };
		msg = "TestDirection #5 expected=" + array2_display(modir) + " result="
				+ array2_display(dir);
		assertTrue(msg, arrays2_equal(modir, dir));

		processor.setHebrewDirection(RTL);
		dir = processor.getDirection();
		modir = new int[][] { { RTL, LTR }, { RTL, RTL } };
		msg = "TestDirection #6 expected=" + array2_display(modir) + " result="
				+ array2_display(dir);
		assertTrue(msg, arrays2_equal(modir, dir));

		processor.setHebrewDirection(RTL, LTR);
		dir = processor.getDirection();
		modir = new int[][] { { RTL, LTR }, { RTL, LTR } };
		msg = "TestDirection #7 expected=" + array2_display(modir) + " result="
				+ array2_display(dir);
		assertTrue(msg, arrays2_equal(modir, dir));

		processor = StringProcessor.getProcessor(IBiDiProcessor.EMAIL);
		processor.assumeMirrored(false);
		processor.setArabicDirection(LTR, RTL);
		processor.setHebrewDirection(LTR, LTR);
		data = "#ABC.#DEF:HOST.com";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "#ABC@.#DEF@:HOST.com";
		assertEquals("TestDirection #9 full", model, toPseudo(full));

		data = "ABC.DEF:HOST.com";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "ABC@.DEF@:HOST.com";
		assertEquals("TestDirection #10 full", model, toPseudo(full));

		processor.assumeMirrored(true);
		data = "#ABC.#DEF:HOST.com";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "<&#ABC.#DEF:HOST.com&^";
		assertEquals("TestDirection #11 full", model, toPseudo(full));

		data = "#ABc.#DEF:HOSt.COM";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "<&#ABc.#DEF:HOSt.COM&^";
		assertEquals("TestDirection #12 full", model, toPseudo(full));

		data = "#ABc.#DEF:HOSt.";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "<&#ABc.#DEF:HOSt.&^";
		assertEquals("TestDirection #13 full", model, toPseudo(full));

		data = "ABC.DEF:HOST.com";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "ABC@.DEF@:HOST.com";
		assertEquals("TestDirection #14 full", model, toPseudo(full));

		data = "--.---:----";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "--.---:----";
		assertEquals("TestDirection #15 full", model, toPseudo(full));

		data = "ABC.|DEF:HOST.com";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "ABC.|DEF@:HOST.com";
		assertEquals("TestDirection #16 full", model, toPseudo(full));

		processor.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
		data = "#ABc.|#DEF:HOST.com";
		lean = toUT16(data);
		full = processor.leanToFullText(lean);
		model = "#ABc.|#DEF:HOST.com";
		assertEquals("TestDirection #17 full", model, toPseudo(full));
	}

	public void testMethods() {

		doTestTools();

		processor = StringProcessor.getProcessor(IBiDiProcessor.JAVA);
		doTestState();

		doTestOrientation();

		processor = StringProcessor
				.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
		doTestOrient("Methods #1 ", "", "", "", "");
		doTestOrient("Methods #2 ", "abc", "abc", ">@abc@^", "abc");
		doTestOrient("Methods #3 ", "ABC", "ABC", ">@ABC@^", "@ABC");
		doTestOrient("Methods #4 ", "bcd,ef", "bcd,ef", ">@bcd,ef@^", "bcd,ef");
		doTestOrient("Methods #5 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^",
				"@BCD@,EF");
		doTestOrient("Methods #6 ", "cde,FG", "cde,FG", ">@cde,FG@^", "cde,FG");
		doTestOrient("Methods #7 ", "CDE,fg", "CDE,fg", ">@CDE,fg@^", "@CDE,fg");
		doTestOrient("Methods #8 ", "12..def,GH", "12..def,GH",
				">@12..def,GH@^", "12..def,GH");
		doTestOrient("Methods #9 ", "34..DEF,gh", "34..DEF,gh",
				">@34..DEF,gh@^", "@34..DEF,gh");

		doTestScripts();

		processor = StringProcessor.getProcessor(IBiDiProcessor.JAVA);
		doTestLeanOffsets();

		processor = StringProcessor
				.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
		doTestFullOffsets("TestFullOffsets ", "BCD,EF,G", new int[] { 3, 7 },
				new int[] { 0, 1, 5, 9, 12, 13 }, new int[] { 0, 4, 8 });

		doTestMirrored();

		doTestDirection();

		processor = StringProcessor
				.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
		processor.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
		processor.setDirection(LTR);
		String data = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
		String lean = toUT16(data);
		String full = processor.leanToFullText(lean);
		String model = "A@,B@,C@,D@,E@,F@,G@,H@,I@,J@,K@,L@,M@,N@,O@,P@,Q@,R@,S@,T@,U@,V@,W@,X@,Y@,Z";
		assertEquals("many inserts", model, toPseudo(full));

	}
}
