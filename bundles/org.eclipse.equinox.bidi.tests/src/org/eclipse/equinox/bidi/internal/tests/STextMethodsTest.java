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
import org.eclipse.equinox.bidi.custom.STextCharTypes;
import org.eclipse.equinox.bidi.custom.STextProcessor;

/**
 * Tests most public methods of BidiComplexEngine
 */

public class STextMethodsTest extends STextTestBase {

	final static int LTR = STextEngine.DIR_LTR;
	final static int RTL = STextEngine.DIR_RTL;
	final static STextEnvironment envLTR = new STextEnvironment(null, false, STextEnvironment.ORIENT_LTR);
	final static STextEnvironment envRTL = new STextEnvironment(null, false, STextEnvironment.ORIENT_RTL);
	final static STextEnvironment envRTLMIR = new STextEnvironment(null, true, STextEnvironment.ORIENT_RTL);
	final static STextEnvironment envIGN = new STextEnvironment(null, false, STextEnvironment.ORIENT_IGNORE);
	final static STextEnvironment envCLR = new STextEnvironment(null, false, STextEnvironment.ORIENT_CONTEXTUAL_LTR);
	final static STextEnvironment envCRL = new STextEnvironment(null, false, STextEnvironment.ORIENT_CONTEXTUAL_RTL);
	final static STextEnvironment envERR = new STextEnvironment(null, false, 9999);
	final static byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
	int dirArabic, dirHebrew;

	class MyComma extends STextProcessor {

		public String getSeparators(STextEnvironment environment) {
			return ","; //$NON-NLS-1$
		}

		public boolean skipProcessing(STextEnvironment environment, String text, STextCharTypes charTypes) {
			byte charType = charTypes.getBidiTypeAt(0);
			if (charType == AL)
				return true;
			return false;
		}

		public int getDirection(STextEnvironment environment, String text) {
			return getDirection(environment, text, new STextCharTypes(this, environment, text));
		}

		public int getDirection(STextEnvironment environment, String text, STextCharTypes charTypes) {
			for (int i = 0; i < text.length(); i++) {
				byte charType = charTypes.getBidiTypeAt(i);
				if (charType == AL)
					return dirArabic;
			}
			return dirHebrew;
		}
	}

	STextProcessor processor;

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
		processor = STextProcessorFactory.PROC_JAVA;
		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, lean, state);
		model = "A@=B@+C@;/* D=E+F;";
		assertEquals("full1", model, toPseudo(full));
		data = "A=B+C; D=E+F;";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, lean, state);
		model = "A=B+C; D=E+F;";
		assertEquals("full2", model, toPseudo(full));
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, lean, state);
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

	private void doTestOrient(String label, String data, String resLTR, String resRTL, String resCon) {
		String full, lean;

		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, envLTR, lean, null);
		assertEquals(label + "LTR full", resLTR, toPseudo(full));
		full = STextEngine.leanToFullText(processor, envRTL, lean, null);
		assertEquals("label + RTL full", resRTL, toPseudo(full));
		full = STextEngine.leanToFullText(processor, envCRL, lean, null);
		assertEquals(label + "CON full", resCon, toPseudo(full));
	}

	private void doTestSkipProcessing() {
		processor = new MyComma();
		doTestOrient("Skip #1 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient("Skip #2 ", "#CD,EF", "#CD,EF", ">@#CD,EF@^", "@#CD,EF");
	}

	private void doTestLeanOffsets() {
		String lean, data, label;
		int[] state = new int[1];
		processor = STextProcessorFactory.PROC_JAVA;
		int[] offsets;
		int[] model;

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		state[0] = -1;
		offsets = STextEngine.leanBidiCharOffsets(processor, null, lean, state);
		model = new int[] {1, 3, 5};
		label = "leanBidiCharOffsets() #1 ";
		assertEquals(label, array_display(model), array_display(offsets));
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		offsets = STextEngine.leanBidiCharOffsets(processor, null, lean, state);
		model = new int[] {6, 10, 12};
		label = "leanBidiCharOffsets() #2 ";
		assertEquals(label, array_display(model), array_display(offsets));
	}

	private void doTestFullOffsets(String label, String data, int[] resLTR, int[] resRTL, int[] resCon) {
		String full, lean, msg;
		int[] offsets;
		processor = STextProcessorFactory.PROC_COMMA_DELIMITED;

		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, envLTR, lean, null);
		offsets = STextEngine.fullBidiCharOffsets(processor, envLTR, full, null);
		msg = label + "LTR ";
		assertEquals(msg, array_display(resLTR), array_display(offsets));
		full = STextEngine.leanToFullText(processor, envRTL, lean, null);
		offsets = STextEngine.fullBidiCharOffsets(processor, envRTL, full, null);
		msg = label + "RTL ";
		assertEquals(msg, array_display(resRTL), array_display(offsets));
		full = STextEngine.leanToFullText(processor, envCLR, lean, null);
		offsets = STextEngine.fullBidiCharOffsets(processor, envCLR, full, null);
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
		int dirA, dirH;
		processor = new MyComma();
		dirArabic = RTL;
		dirHebrew = LTR;
		msg = "TestDirection #1";
		String text = toUT16("###");
		dirA = processor.getDirection(null, text);
		text = toUT16("ABC");
		dirH = processor.getDirection(null, toUT16("ABC"));
		assertTrue(msg, dirA == RTL && dirH == LTR);

		dirArabic = RTL;
		dirHebrew = RTL;
		msg = "TestDirection #2";
		text = toUT16("###");
		dirA = processor.getDirection(null, text);
		text = toUT16("ABC");
		dirH = processor.getDirection(null, text);
		assertTrue(msg, dirA == RTL && dirH == RTL);

		dirArabic = dirHebrew = LTR;
		msg = "TestDirection #3";
		data = "ABC,#DEF,HOST,com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, lean, null);
		model = "ABC@,#DEF@,HOST,com";
		assertEquals("TestDirection #9 full", model, toPseudo(full));

		data = "ABC,DEF,HOST,com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, null, lean, null);
		model = "ABC@,DEF@,HOST,com";
		assertEquals("TestDirection #10 full", model, toPseudo(full));

		dirArabic = RTL;
		msg = "TestDirection #10.5";
		text = toUT16("###");
		dirA = processor.getDirection(null, text);
		text = toUT16("ABC");
		dirH = processor.getDirection(null, text);
		assertTrue(msg, dirA == RTL && dirH == LTR);
		STextEnvironment environment = new STextEnvironment(null, true, STextEnvironment.ORIENT_LTR);
		data = "ABC,#DEF,HOST,com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, environment, lean, null);
		model = "<&ABC,#DEF,HOST,com&^";
		assertEquals("TestDirection #11 full", model, toPseudo(full));

		data = "ABc,#DEF,HOSt,COM";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, environment, lean, null);
		model = "<&ABc,#DEF,HOSt,COM&^";
		assertEquals("TestDirection #12 full", model, toPseudo(full));

		data = "ABc,#DEF,HOSt,";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, environment, lean, null);
		model = "<&ABc,#DEF,HOSt,&^";
		assertEquals("TestDirection #13 full", model, toPseudo(full));

		data = "ABC,DEF,HOST,com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, environment, lean, null);
		model = "ABC@,DEF@,HOST,com";
		assertEquals("TestDirection #14 full", model, toPseudo(full));

		data = "--,---,----";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, environment, lean, null);
		model = "--,---,----";
		assertEquals("TestDirection #15 full", model, toPseudo(full));

		data = "ABC,|DEF,HOST,com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, environment, lean, null);
		model = "ABC,|DEF@,HOST,com";
		assertEquals("TestDirection #16 full", model, toPseudo(full));

		data = "ABc,|#DEF,HOST,com";
		lean = toUT16(data);
		full = STextEngine.leanToFullText(processor, envRTLMIR, lean, null);
		model = "ABc,|#DEF,HOST,com";
		assertEquals("TestDirection #17 full", model, toPseudo(full));
		assertEquals("Test curDirection", RTL, STextEngine.getCurDirection(processor, envRTLMIR, lean));
	}

	public void testMethods() {

		doTestTools();

		doTestState();

		doTestOrientation();

		processor = STextProcessorFactory.PROC_COMMA_DELIMITED;
		doTestOrient("Methods #1 ", "", "", "", "");
		doTestOrient("Methods #2 ", "abc", "abc", ">@abc@^", "abc");
		doTestOrient("Methods #3 ", "ABC", "ABC", ">@ABC@^", "@ABC");
		doTestOrient("Methods #4 ", "bcd,ef", "bcd,ef", ">@bcd,ef@^", "bcd,ef");
		doTestOrient("Methods #5 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient("Methods #6 ", "cde,FG", "cde,FG", ">@cde,FG@^", "cde,FG");
		doTestOrient("Methods #7 ", "CDE,fg", "CDE,fg", ">@CDE,fg@^", "@CDE,fg");
		doTestOrient("Methods #8 ", "12..def,GH", "12..def,GH", ">@12..def,GH@^", "12..def,GH");
		doTestOrient("Methods #9 ", "34..DEF,gh", "34..DEF,gh", ">@34..DEF,gh@^", "@34..DEF,gh");

		doTestSkipProcessing();

		doTestLeanOffsets();

		doTestFullOffsets("TestFullOffsets ", "BCD,EF,G", new int[] {3, 7}, new int[] {0, 1, 5, 9, 12, 13}, new int[] {0, 4, 8});

		doTestMirrored();

		doTestDirection();

		processor = STextProcessorFactory.PROC_COMMA_DELIMITED;
		String data = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
		String lean = toUT16(data);
		String full = STextEngine.leanToFullText(processor, null, lean, null);
		String model = "A@,B@,C@,D@,E@,F@,G@,H@,I@,J@,K@,L@,M@,N@,O@,P@,Q@,R@,S@,T@,U@,V@,W@,X@,Y@,Z";
		assertEquals("many inserts", model, toPseudo(full));
	}
}
