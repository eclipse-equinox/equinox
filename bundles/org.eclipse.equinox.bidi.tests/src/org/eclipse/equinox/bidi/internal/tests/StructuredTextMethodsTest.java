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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.equinox.bidi.advanced.*;
import org.eclipse.equinox.bidi.custom.StructuredTextCharTypes;
import org.eclipse.equinox.bidi.custom.StructuredTextTypeHandler;
import org.junit.Test;

/**
 * Tests most public methods of BidiComplexEngine
 */
public class StructuredTextMethodsTest extends StructuredTextTestBase {

	private final static int LTR = IStructuredTextExpert.DIR_LTR;
	private final static int RTL = IStructuredTextExpert.DIR_RTL;
	private final static StructuredTextEnvironment envLTR = new StructuredTextEnvironment(null, false,
			StructuredTextEnvironment.ORIENT_LTR);
	private final static StructuredTextEnvironment envRTL = new StructuredTextEnvironment(null, false,
			StructuredTextEnvironment.ORIENT_RTL);
	private final static StructuredTextEnvironment envRTLMIR = new StructuredTextEnvironment(null, true,
			StructuredTextEnvironment.ORIENT_RTL);
	private final static StructuredTextEnvironment envIGN = new StructuredTextEnvironment(null, false,
			StructuredTextEnvironment.ORIENT_IGNORE);
	private final static StructuredTextEnvironment envCLR = new StructuredTextEnvironment(null, false,
			StructuredTextEnvironment.ORIENT_CONTEXTUAL_LTR);
	private final static StructuredTextEnvironment envCRL = new StructuredTextEnvironment(null, false,
			StructuredTextEnvironment.ORIENT_CONTEXTUAL_RTL);
	private final static StructuredTextEnvironment envERR = new StructuredTextEnvironment(null, false, 9999);
	private final static TestHandlerMyComma testMyCommaLL = new TestHandlerMyComma(LTR, LTR);
	private final static TestHandlerMyComma testMyCommaRR = new TestHandlerMyComma(RTL, RTL);
	private final static TestHandlerMyComma testMyCommaRL = new TestHandlerMyComma(RTL, LTR);

	private static class TestHandlerMyComma extends StructuredTextTypeHandler {

		private final static byte AL = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;

		final int dirArabic;
		final int dirHebrew;

		public TestHandlerMyComma(int dirArabic, int dirHebrew) {
			this.dirArabic = dirArabic;
			this.dirHebrew = dirHebrew;
		}

		@Override
		public String getSeparators(IStructuredTextExpert expert) {
			return ","; //$NON-NLS-1$
		}

		@Override
		public boolean skipProcessing(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes) {
			byte charType = charTypes.getBidiTypeAt(0);
			if (charType == AL)
				return true;
			return false;
		}

		@Override
		public int getDirection(IStructuredTextExpert expert, String text) {
			return getDirection(expert, text, new StructuredTextCharTypes(expert, text));
		}

		@Override
		public int getDirection(IStructuredTextExpert expert, String text, StructuredTextCharTypes charTypes) {
			for (int i = 0; i < text.length(); i++) {
				byte charType = charTypes.getBidiTypeAt(i);
				if (charType == AL)
					return dirArabic;
			}
			return dirHebrew;
		}
	}

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
		IStructuredTextExpert expert = StructuredTextExpertFactory
				.getStatefulExpert(StructuredTextTypeHandlerFactory.JAVA);

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		full = expert.leanToFullText(lean);
		model = "A@=B@+C@;/* D=E+F;";
		assertEquals("full1", model, toPseudo(full));

		data = "A=B+C; D=E+F;";
		lean = toUT16(data);
		full = expert.leanToFullText(lean);
		model = data;
		assertEquals("full2", model, toPseudo(full));

		data = "SOME MORE COMMENTS";
		lean = toUT16(data);
		full = expert.leanToFullText(lean);
		model = data;
		assertEquals("full3", model, toPseudo(full));

		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		full = expert.leanToFullText(lean);
		model = "A=B+C;@*/ D@=E@+F;";
		assertEquals("full4", model, toPseudo(full));
	}

	private void doTestOrientation() {
		int orient = StructuredTextEnvironment.DEFAULT.getOrientation();
		assertEquals("orient #1", StructuredTextEnvironment.ORIENT_LTR, orient);

		orient = envIGN.getOrientation();
		assertEquals("orient #2", StructuredTextEnvironment.ORIENT_IGNORE, orient);

		orient = envCRL.getOrientation();
		assertEquals("orient #3", StructuredTextEnvironment.ORIENT_CONTEXTUAL_RTL, orient);

		orient = envERR.getOrientation();
		assertEquals("orient #4", StructuredTextEnvironment.ORIENT_UNKNOWN, orient);
	}

	private void doTestOrient(StructuredTextTypeHandler handler, String label, String data, String resLTR,
			String resRTL, String resCon) {
		String full, lean;

		IStructuredTextExpert expertLTR = StructuredTextExpertFactory.getStatefulExpert(handler, envLTR);
		IStructuredTextExpert expertRTL = StructuredTextExpertFactory.getStatefulExpert(handler, envRTL);
		IStructuredTextExpert expertCRL = StructuredTextExpertFactory.getStatefulExpert(handler, envCRL);

		lean = toUT16(data);
		full = expertLTR.leanToFullText(lean);
		assertEquals(label + "LTR full", resLTR, toPseudo(full));
		full = expertRTL.leanToFullText(lean);
		assertEquals("label + RTL full", resRTL, toPseudo(full));
		full = expertCRL.leanToFullText(lean);
		assertEquals(label + "CON full", resCon, toPseudo(full));
	}

	private void doTestSkipProcessing() {
		doTestOrient(testMyCommaLL, "Skip #1 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient(testMyCommaLL, "Skip #2 ", "#CD,EF", "#CD,EF", ">@#CD,EF@^", "@#CD,EF");
	}

	private void doTestLeanOffsets() {
		String lean, data, label;
		IStructuredTextExpert expert = StructuredTextExpertFactory
				.getStatefulExpert(StructuredTextTypeHandlerFactory.JAVA);

		int[] offsets;
		int[] model;

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		offsets = expert.leanBidiCharOffsets(lean);
		model = new int[] { 1, 3, 5 };
		label = "leanBidiCharOffsets() #1 ";
		assertEquals(label, array_display(model), array_display(offsets));
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		offsets = expert.leanBidiCharOffsets(lean);
		model = new int[] { 6, 10, 12 };
		label = "leanBidiCharOffsets() #2 ";
		assertEquals(label, array_display(model), array_display(offsets));
	}

	private void doTestFullOffsets(String label, String data, int[] resLTR, int[] resRTL, int[] resCon) {
		String full, lean, msg;
		int[] offsets;
		IStructuredTextExpert expertLTR = StructuredTextExpertFactory
				.getExpert(StructuredTextTypeHandlerFactory.COMMA_DELIMITED, envLTR);
		IStructuredTextExpert expertRTL = StructuredTextExpertFactory
				.getExpert(StructuredTextTypeHandlerFactory.COMMA_DELIMITED, envRTL);
		IStructuredTextExpert expertCLR = StructuredTextExpertFactory
				.getExpert(StructuredTextTypeHandlerFactory.COMMA_DELIMITED, envCLR);

		lean = toUT16(data);
		full = expertLTR.leanToFullText(lean);
		offsets = expertLTR.fullBidiCharOffsets(full);
		msg = label + "LTR ";
		assertEquals(msg, array_display(resLTR), array_display(offsets));
		full = expertRTL.leanToFullText(lean);
		offsets = expertRTL.fullBidiCharOffsets(full);
		msg = label + "RTL ";
		assertEquals(msg, array_display(resRTL), array_display(offsets));
		full = expertCLR.leanToFullText(lean);
		offsets = expertCLR.fullBidiCharOffsets(full);
		msg = label + "CON ";
		assertEquals(msg, array_display(resCon), array_display(offsets));
	}

	private void doTestMirrored() {
		boolean mirrored;
		mirrored = StructuredTextEnvironment.DEFAULT.getMirrored();
		assertFalse("mirrored #1", mirrored);
		StructuredTextEnvironment env = new StructuredTextEnvironment(null, true, StructuredTextEnvironment.ORIENT_LTR);
		mirrored = env.getMirrored();
		assertTrue("mirrored #2", mirrored);
	}

	private void doTestDirection() {
		String data, lean, full, model;
		int dirA, dirH;
		IStructuredTextExpert expertRL = StructuredTextExpertFactory.getStatefulExpert(testMyCommaRL, envLTR);
		dirA = expertRL.getTextDirection(toUT16("###"));
		dirH = expertRL.getTextDirection(toUT16("ABC"));
		assertTrue("TestDirection #1", dirA == RTL && dirH == LTR);

		IStructuredTextExpert expertRR = StructuredTextExpertFactory.getStatefulExpert(testMyCommaRR, envLTR);
		dirA = expertRR.getTextDirection(toUT16("###"));
		dirH = expertRR.getTextDirection(toUT16("ABC"));
		assertTrue("TestDirection #2", dirA == RTL && dirH == RTL);

		IStructuredTextExpert expertLL = StructuredTextExpertFactory.getStatefulExpert(testMyCommaLL, envLTR);
		lean = toUT16("ABC,#DEF,HOST,com");
		full = expertLL.leanToFullText(lean);
		assertEquals("TestDirection #9 full", "ABC@,#DEF@,HOST,com", toPseudo(full));

		lean = toUT16("ABC,DEF,HOST,com");
		full = expertLL.leanToFullText(lean);

		assertEquals("TestDirection #10 full", "ABC@,DEF@,HOST,com", toPseudo(full));

		StructuredTextEnvironment environment = new StructuredTextEnvironment(null, true,
				StructuredTextEnvironment.ORIENT_LTR);
		IStructuredTextExpert expert = StructuredTextExpertFactory.getStatefulExpert(testMyCommaRL, environment);
		dirA = expert.getTextDirection(toUT16("###"));
		dirH = expert.getTextDirection(toUT16("ABC"));
		assertTrue("TestDirection #10.5", dirA == RTL && dirH == LTR);

		lean = toUT16("ABC,#DEF,HOST,com");
		full = expert.leanToFullText(lean);
		assertEquals("TestDirection #11 full", "<&ABC,#DEF,HOST,com&^", toPseudo(full));

		data = "ABc,#DEF,HOSt,COM";
		lean = toUT16(data);
		full = expert.leanToFullText(lean);
		model = "<&ABc,#DEF,HOSt,COM&^";
		assertEquals("TestDirection #12 full", model, toPseudo(full));

		data = "ABc,#DEF,HOSt,";
		lean = toUT16(data);
		full = expert.leanToFullText(lean);
		model = "<&ABc,#DEF,HOSt,&^";
		assertEquals("TestDirection #13 full", model, toPseudo(full));

		data = "ABC,DEF,HOST,com";
		lean = toUT16(data);
		full = expert.leanToFullText(lean);
		model = "ABC@,DEF@,HOST,com";
		assertEquals("TestDirection #14 full", model, toPseudo(full));

		data = "--,---,----";
		lean = toUT16(data);
		full = expert.leanToFullText(lean);
		model = "--,---,----";
		assertEquals("TestDirection #15 full", model, toPseudo(full));

		data = "ABC,|DEF,HOST,com";
		lean = toUT16(data);
		full = expert.leanToFullText(lean);

		model = "ABC,|DEF@,HOST,com";
		assertEquals("TestDirection #16 full", model, toPseudo(full));

		data = "ABc,|#DEF,HOST,com";
		lean = toUT16(data);
		expert = StructuredTextExpertFactory.getStatefulExpert(testMyCommaRL, envRTLMIR);
		full = expert.leanToFullText(lean);
		model = "ABc,|#DEF,HOST,com";
		assertEquals("TestDirection #17 full", model, toPseudo(full));
		int dir = expert.getTextDirection(lean);
		assertEquals("Test curDirection", RTL, dir);
	}

	@Test
	public void testMethods() {

		doTestTools();

		doTestState();

		doTestOrientation();

		StructuredTextTypeHandler commaHandler = StructuredTextTypeHandlerFactory
				.getHandler(StructuredTextTypeHandlerFactory.COMMA_DELIMITED);
		doTestOrient(commaHandler, "Methods #1 ", "", "", "", "");
		doTestOrient(commaHandler, "Methods #2 ", "abc", "abc", ">@abc@^", "abc");
		doTestOrient(commaHandler, "Methods #3 ", "ABC", "ABC", ">@ABC@^", "@ABC");
		doTestOrient(commaHandler, "Methods #4 ", "bcd,ef", "bcd,ef", ">@bcd,ef@^", "bcd,ef");
		doTestOrient(commaHandler, "Methods #5 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient(commaHandler, "Methods #6 ", "cde,FG", "cde,FG", ">@cde,FG@^", "cde,FG");
		doTestOrient(commaHandler, "Methods #7 ", "CDE,fg", "CDE,fg", ">@CDE,fg@^", "@CDE,fg");
		doTestOrient(commaHandler, "Methods #8 ", "12..def,GH", "12..def,GH", ">@12..def,GH@^", "12..def,GH");
		doTestOrient(commaHandler, "Methods #9 ", "34..DEF,gh", "34..DEF,gh", ">@34..DEF,gh@^", "@34..DEF,gh");

		doTestSkipProcessing();

		doTestLeanOffsets();

		doTestFullOffsets("TestFullOffsets ", "BCD,EF,G", new int[] { 3, 7 }, new int[] { 0, 1, 5, 9, 12, 13 },
				new int[] { 0, 4, 8 });

		doTestMirrored();

		doTestDirection();

		IStructuredTextExpert expert = StructuredTextExpertFactory
				.getExpert(StructuredTextTypeHandlerFactory.COMMA_DELIMITED);
		String data = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
		String lean = toUT16(data);
		String full = expert.leanToFullText(lean);
		String model = "A@,B@,C@,D@,E@,F@,G@,H@,I@,J@,K@,L@,M@,N@,O@,P@,Q@,R@,S@,T@,U@,V@,W@,X@,Y@,Z";
		assertEquals("many inserts", model, toPseudo(full));
	}
}
