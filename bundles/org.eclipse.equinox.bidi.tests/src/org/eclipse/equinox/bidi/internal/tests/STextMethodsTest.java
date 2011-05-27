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
import org.eclipse.equinox.bidi.custom.*;

/**
 * Tests most public methods of BidiComplexEngine
 */

public class STextMethodsTest extends STextTestBase {

	final static int LTR = STextFeatures.DIR_LTR;
	final static int RTL = STextFeatures.DIR_RTL;
	final static STextEnvironment envLTR = new STextEnvironment(null, false, STextEnvironment.ORIENT_LTR);
	final static STextEnvironment envRTL = new STextEnvironment(null, false, STextEnvironment.ORIENT_RTL);
	final static STextEnvironment envRTLMIR = new STextEnvironment(null, true, STextEnvironment.ORIENT_RTL);
	final static STextEnvironment envIGN = new STextEnvironment(null, false, STextEnvironment.ORIENT_IGNORE);
	final static STextEnvironment envCLR = new STextEnvironment(null, false, STextEnvironment.ORIENT_CONTEXTUAL_LTR);
	final static STextEnvironment envCRL = new STextEnvironment(null, false, STextEnvironment.ORIENT_CONTEXTUAL_RTL);
	final static STextEnvironment envERR = new STextEnvironment(null, false, 9999);

	private void doTestTools() {

		// This method tests utility methods used by the JUnits
		String data = "56789ABCDEFGHIJKLMNOPQRSTUVWXYZ~#@&><^|`";
		String text = toUT16(data);
		String dat2 = toPseudo(text);
		assertEquals(data, dat2);

		text = toPseudo(data);
		assertEquals("56789abcdefghijklmnopqrstuvwxyz~#@&><^|`", text);

		text = array_display(null);
		assertEquals("null", text);
	}

	private void doTestState() {
		String data, lean, full, model;
		int[] state = new int[1];
		state[0] = -1;
		String type = ISTextTypes.JAVA;
		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(type, null, null, lean, state);
		model = "A@=B@+C@;/* D=E+F;";
		assertEquals("full1", model, toPseudo(full));
		data = "A=B+C; D=E+F;";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(type, null, null, lean, state);
		model = "A=B+C; D=E+F;";
		assertEquals("full2", model, toPseudo(full));
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(type, null, null, lean, state);
		model = "A=B+C;@*/ D@=E@+F;";
		assertEquals("full3", model, toPseudo(full));
	}

	private void doTestOrientation() {
		int orient;
		orient = STextEnvironment.DEFAULT.getOrientation();
		assertEquals("orient #1", STextEnvironment.ORIENT_LTR, orient);

		orient = envIGN.getOrientation();
		assertEquals("orient #2", STextEnvironment.ORIENT_IGNORE, orient);

		orient = envCRL.getOrientation();
		assertEquals("orient #3", STextEnvironment.ORIENT_CONTEXTUAL_RTL, orient);

		orient = envERR.getOrientation();
		assertEquals("orient #4", STextEnvironment.ORIENT_UNKNOWN, orient);
	}

	private void doTestOrient(STextFeatures f, String label, String data, String resLTR, String resRTL, String resCon) {
		String full, lean;
		String type = ISTextTypes.COMMA_DELIMITED;

		lean = toUT16(data);
		full = STextEngine.leanToFullText(type, f, envLTR, lean, null);
		assertEquals(label + "LTR full", resLTR, toPseudo(full));
		full = STextEngine.leanToFullText(type, f, envRTL, lean, null);
		assertEquals("label + RTL full", resRTL, toPseudo(full));
		full = STextEngine.leanToFullText(type, f, envCRL, lean, null);
		assertEquals(label + "CON full", resCon, toPseudo(full));
	}

	private void doTestScripts(STextFeatures f1) {
		STextFeatures f2;
		boolean flag;
		flag = f1.getIgnoreArabic();
		assertFalse("Ignores Arabic 1", flag);
		flag = f1.getIgnoreHebrew();
		assertFalse("Ignores Hebrew 1", flag);

		f2 = new STextFeatures(f1.getSeparators(), 0, -1, -1, true, true);
		flag = f2.getIgnoreArabic();
		assertTrue("Ignores Arabic 2", flag);
		flag = f2.getIgnoreHebrew();
		assertTrue("Ignores Hebrew 2", flag);
		doTestOrient(f2, "Scripts #1 ", "BCD,EF", "BCD,EF", ">@BCD,EF@^", "@BCD,EF");
		f2 = new STextFeatures(f1.getSeparators(), 0, -1, -1, false, true);
		flag = f2.getIgnoreArabic();
		assertFalse("Ignores Arabic 3", flag);
		flag = f2.getIgnoreHebrew();
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

		f2 = new STextFeatures(f1.getSeparators(), 0, -1, -1, true, false);
		flag = f2.getIgnoreArabic();
		assertTrue("Ignores Arabic 4", flag);
		flag = f2.getIgnoreHebrew();
		assertFalse("Ignores Hebrew 4", flag);
		doTestOrient(f2, "Scripts #14 ", "BCd,EF", "BCd,EF", ">@BCd,EF@^", "@BCd,EF");
		doTestOrient(f2, "Scripts #15 ", "BCD,eF", "BCD,eF", ">@BCD,eF@^", "@BCD,eF");
		doTestOrient(f2, "Scripts #16 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient(f2, "Scripts #17 ", "BCD,12", "BCD@,12", ">@BCD@,12@^", "@BCD@,12");
		doTestOrient(f2, "Scripts #18 ", "BCD,", "BCD,", ">@BCD,@^", "@BCD,");

		f2 = new STextFeatures(f1.getSeparators(), 0, -1, -1, false, false);
		doTestOrient(f2, "Scripts #19 ", "123,45|67", "123,45|67", ">@123,45|67@^", "@123,45|67");
		doTestOrient(f2, "Scripts #20 ", "5,e", "5,e", ">@5,e@^", "5,e");
		doTestOrient(f2, "Scripts #21 ", "5,#", "5@,#", ">@5@,#@^", "5@,#");
		doTestOrient(f2, "Scripts #22 ", "5,6", "5@,6", ">@5@,6@^", "5@,6");
		doTestOrient(f2, "Scripts #23 ", "5,D", "5@,D", ">@5@,D@^", "5@,D");
		doTestOrient(f2, "Scripts #24 ", "5,--", "5,--", ">@5,--@^", "@5,--");
	}

	private void doTestLeanOffsets() {
		String lean, data, label;
		int[] state = new int[1];
		String type = ISTextTypes.JAVA;
		int[] offsets;
		int[] model;

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		state[0] = -1;
		offsets = STextEngine.leanBidiCharOffsets(type, null, null, lean, state);
		model = new int[] {1, 3, 5};
		label = "leanBidiCharOffsets() #1 ";
		assertEquals(label, array_display(model), array_display(offsets));
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		offsets = STextEngine.leanBidiCharOffsets(type, null, null, lean, state);
		model = new int[] {6, 10, 12};
		label = "leanBidiCharOffsets() #2 ";
		assertEquals(label, array_display(model), array_display(offsets));
	}

	private void doTestFullOffsets(String label, String data, int[] resLTR, int[] resRTL, int[] resCon) {
		String full, lean, msg;
		int[] offsets;
		String type = ISTextTypes.COMMA_DELIMITED;

		lean = toUT16(data);
		full = STextEngine.leanToFullText(type, null, envLTR, lean, null);
		offsets = STextEngine.fullBidiCharOffsets(type, null, envLTR, full, null);
		msg = label + "LTR ";
		assertEquals(msg, array_display(resLTR), array_display(offsets));
		full = STextEngine.leanToFullText(type, null, envRTL, lean, null);
		offsets = STextEngine.fullBidiCharOffsets(type, null, envRTL, full, null);
		msg = label + "RTL ";
		assertEquals(msg, array_display(resRTL), array_display(offsets));
		full = STextEngine.leanToFullText(type, null, envCLR, lean, null);
		offsets = STextEngine.fullBidiCharOffsets(type, null, envCLR, full, null);
		msg = label + "CON ";
		assertEquals(msg, array_display(resCon), array_display(offsets));
	}

	private void doTestMirrored() {
		boolean mirrored;
		mirrored = STextEnvironment.DEFAULT.getMirrored();
		assertFalse("mirrored #1", mirrored);
		STextEnvironment env = new STextEnvironment(null, true, STextEnvironment.ORIENT_LTR);
		mirrored = env.getMirrored();
		assertTrue("mirrored #2", mirrored);
	}

	private void doTestDirection() {
		String data, lean, full, model, msg;
		ISTextProcessor processor = STextStringProcessor.getProcessor(ISTextTypes.COMMA_DELIMITED);
		STextFeatures f1 = processor.getFeatures(null);
		msg = "TestDirection #1";
		assertTrue(msg, f1.getDirArabic() == LTR && f1.getDirHebrew() == LTR);

		STextFeatures f2 = new STextFeatures(f1.getSeparators(), 0, RTL, RTL, false, false);
		f1 = f2;
		msg = "TestDirection #2";
		assertTrue(msg, f1.getDirArabic() == RTL && f1.getDirHebrew() == RTL);

		STextEnvironment environment = new STextEnvironment(null, false, STextEnvironment.ORIENT_LTR);
		processor = STextStringProcessor.getProcessor(ISTextTypes.EMAIL);
		f1 = processor.getFeatures(environment);
		msg = "TestDirection #3";
		assertTrue(msg, f1.getDirArabic() == LTR && f1.getDirHebrew() == LTR);
		data = "#ABC.#DEF:HOST.com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, environment, lean, null);
		model = "#ABC@.#DEF@:HOST.com";
		assertEquals("TestDirection #9 full", model, toPseudo(full));

		data = "ABC.DEF:HOST.com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, environment, lean, null);
		model = "ABC@.DEF@:HOST.com";
		assertEquals("TestDirection #10 full", model, toPseudo(full));

		environment = new STextEnvironment(null, true, STextEnvironment.ORIENT_LTR);
		f1 = processor.getFeatures(environment);
		msg = "TestDirection #10.5";
		assertTrue(msg, f1.getDirArabic() == RTL && f1.getDirHebrew() == LTR);
		data = "#ABC.#DEF:HOST.com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, environment, lean, null);
		model = "<&#ABC.#DEF:HOST.com&^";
		assertEquals("TestDirection #11 full", model, toPseudo(full));

		data = "#ABc.#DEF:HOSt.COM";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, environment, lean, null);
		model = "<&#ABc.#DEF:HOSt.COM&^";
		assertEquals("TestDirection #12 full", model, toPseudo(full));

		data = "#ABc.#DEF:HOSt.";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, environment, lean, null);
		model = "<&#ABc.#DEF:HOSt.&^";
		assertEquals("TestDirection #13 full", model, toPseudo(full));

		data = "ABC.DEF:HOST.com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, environment, lean, null);
		model = "ABC@.DEF@:HOST.com";
		assertEquals("TestDirection #14 full", model, toPseudo(full));

		data = "--.---:----";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, environment, lean, null);
		model = "--.---:----";
		assertEquals("TestDirection #15 full", model, toPseudo(full));

		data = "ABC.|DEF:HOST.com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, environment, lean, null);
		model = "ABC.|DEF@:HOST.com";
		assertEquals("TestDirection #16 full", model, toPseudo(full));

		data = "#ABc.|#DEF:HOST.com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, envRTLMIR, lean, null);
		model = "#ABc.|#DEF:HOST.com";
		assertEquals("TestDirection #17 full", model, toPseudo(full));
		assertEquals("Test curDirection", RTL, STextEngine.getCurDirection(processor, null, envRTLMIR, lean));
	}

	public void testMethods() {

		doTestTools();

		doTestState();

		doTestOrientation();

		ISTextProcessor processor = STextStringProcessor.getProcessor(ISTextTypes.COMMA_DELIMITED);
		STextFeatures f = processor.getFeatures(null);
		doTestOrient(f, "Methods #1 ", "", "", "", "");
		doTestOrient(f, "Methods #2 ", "abc", "abc", ">@abc@^", "abc");
		doTestOrient(f, "Methods #3 ", "ABC", "ABC", ">@ABC@^", "@ABC");
		doTestOrient(f, "Methods #4 ", "bcd,ef", "bcd,ef", ">@bcd,ef@^", "bcd,ef");
		doTestOrient(f, "Methods #5 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient(f, "Methods #6 ", "cde,FG", "cde,FG", ">@cde,FG@^", "cde,FG");
		doTestOrient(f, "Methods #7 ", "CDE,fg", "CDE,fg", ">@CDE,fg@^", "@CDE,fg");
		doTestOrient(f, "Methods #8 ", "12..def,GH", "12..def,GH", ">@12..def,GH@^", "12..def,GH");
		doTestOrient(f, "Methods #9 ", "34..DEF,gh", "34..DEF,gh", ">@34..DEF,gh@^", "@34..DEF,gh");

		doTestScripts(f);

		doTestLeanOffsets();

		doTestFullOffsets("TestFullOffsets ", "BCD,EF,G", new int[] {3, 7}, new int[] {0, 1, 5, 9, 12, 13}, new int[] {0, 4, 8});

		doTestMirrored();

		doTestDirection();

		String type = ISTextTypes.COMMA_DELIMITED;
		String data = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
		String lean = toUT16(data);
		String full = STextEngine.leanToFullText(type, null, null, lean, null);
		String model = "A@,B@,C@,D@,E@,F@,G@,H@,I@,J@,K@,L@,M@,N@,O@,P@,Q@,R@,S@,T@,U@,V@,W@,X@,Y@,Z";
		assertEquals("many inserts", model, toPseudo(full));
	}
}
