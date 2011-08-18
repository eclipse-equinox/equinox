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

import org.eclipse.equinox.bidi.STextDirection;
import org.eclipse.equinox.bidi.STextTypeHandlerFactory;
import org.eclipse.equinox.bidi.advanced.*;

/**
 * Tests most public methods of BidiComplexEngine
 */
public class STextMethodsTest extends STextTestBase {

	final static int LTR = STextDirection.DIR_LTR;
	final static int RTL = STextDirection.DIR_RTL;
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
		ISTextExpert expert = STextExpertFactory.getPrivateExpert(STextTypeHandlerFactory.JAVA);

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
		int orient = STextEnvironment.DEFAULT.getOrientation();
		assertEquals("orient #1", STextEnvironment.ORIENT_LTR, orient);

		orient = envIGN.getOrientation();
		assertEquals("orient #2", STextEnvironment.ORIENT_IGNORE, orient);

		orient = envCRL.getOrientation();
		assertEquals("orient #3", STextEnvironment.ORIENT_CONTEXTUAL_RTL, orient);

		orient = envERR.getOrientation();
		assertEquals("orient #4", STextEnvironment.ORIENT_UNKNOWN, orient);
	}

	private void doTestOrient(String handlerDefID, String label, String data, String resLTR, String resRTL, String resCon) {
		String full, lean;

		ISTextExpert expertLTR = STextExpertFactory.getExpert(handlerDefID, envLTR);
		ISTextExpert expertRTL = STextExpertFactory.getExpert(handlerDefID, envRTL);
		ISTextExpert expertCRL = STextExpertFactory.getExpert(handlerDefID, envCRL);

		lean = toUT16(data);
		full = expertLTR.leanToFullText(lean);
		assertEquals(label + "LTR full", resLTR, toPseudo(full));
		full = expertRTL.leanToFullText(lean);
		assertEquals("label + RTL full", resRTL, toPseudo(full));
		full = expertCRL.leanToFullText(lean);
		assertEquals(label + "CON full", resCon, toPseudo(full));
	}

	private void doTestSkipProcessing() {
		doTestOrient("test.MyCommaLL", "Skip #1 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient("test.MyCommaLL", "Skip #2 ", "#CD,EF", "#CD,EF", ">@#CD,EF@^", "@#CD,EF");
	}

	private void doTestLeanOffsets() {
		String lean, data, label;
		ISTextExpert expert = STextExpertFactory.getPrivateExpert(STextTypeHandlerFactory.JAVA);

		int[] offsets;
		int[] model;

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		offsets = expert.leanBidiCharOffsets(lean);
		model = new int[] {1, 3, 5};
		label = "leanBidiCharOffsets() #1 ";
		assertEquals(label, array_display(model), array_display(offsets));
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		offsets = expert.leanBidiCharOffsets(lean);
		model = new int[] {6, 10, 12};
		label = "leanBidiCharOffsets() #2 ";
		assertEquals(label, array_display(model), array_display(offsets));
	}

	private void doTestFullOffsets(String label, String data, int[] resLTR, int[] resRTL, int[] resCon) {
		String full, lean, msg;
		int[] offsets;
		ISTextExpert expertLTR = STextExpertFactory.getExpert(STextTypeHandlerFactory.COMMA_DELIMITED, envLTR);
		ISTextExpert expertRTL = STextExpertFactory.getExpert(STextTypeHandlerFactory.COMMA_DELIMITED, envRTL);
		ISTextExpert expertCLR = STextExpertFactory.getExpert(STextTypeHandlerFactory.COMMA_DELIMITED, envCLR);

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
		mirrored = STextEnvironment.DEFAULT.getMirrored();
		assertFalse("mirrored #1", mirrored);
		STextEnvironment env = new STextEnvironment(null, true, STextEnvironment.ORIENT_LTR);
		mirrored = env.getMirrored();
		assertTrue("mirrored #2", mirrored);
	}

	private void doTestDirection() {
		String data, lean, full, model;
		int dirA, dirH;
		ISTextExpert expertRL = STextExpertFactory.getExpert("test.MyCommaRL");
		dirA = expertRL.getTextDirection(toUT16("###"));
		dirH = expertRL.getTextDirection(toUT16("ABC"));
		assertTrue("TestDirection #1", dirA == RTL && dirH == LTR);

		ISTextExpert expertRR = STextExpertFactory.getExpert("test.MyCommaRR");
		dirA = expertRR.getTextDirection(toUT16("###"));
		dirH = expertRR.getTextDirection(toUT16("ABC"));
		assertTrue("TestDirection #2", dirA == RTL && dirH == RTL);

		ISTextExpert expertLL = STextExpertFactory.getExpert("test.MyCommaLL");
		lean = toUT16("ABC,#DEF,HOST,com");
		full = expertLL.leanToFullText(lean);
		assertEquals("TestDirection #9 full", "ABC@,#DEF@,HOST,com", toPseudo(full));

		lean = toUT16("ABC,DEF,HOST,com");
		full = expertLL.leanToFullText(lean);

		assertEquals("TestDirection #10 full", "ABC@,DEF@,HOST,com", toPseudo(full));

		STextEnvironment environment = new STextEnvironment(null, true, STextEnvironment.ORIENT_LTR);
		ISTextExpert expert = STextExpertFactory.getExpert("test.MyCommaRL", environment);
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
		expert = STextExpertFactory.getExpert("test.MyCommaRL", envRTLMIR);
		full = expert.leanToFullText(lean);
		model = "ABc,|#DEF,HOST,com";
		assertEquals("TestDirection #17 full", model, toPseudo(full));
		int dir = expert.getTextDirection(lean);
		assertEquals("Test curDirection", RTL, dir);
	}

	public void testMethods() {

		doTestTools();

		doTestState();

		doTestOrientation();

		doTestOrient(STextTypeHandlerFactory.COMMA_DELIMITED, "Methods #1 ", "", "", "", "");
		doTestOrient(STextTypeHandlerFactory.COMMA_DELIMITED, "Methods #2 ", "abc", "abc", ">@abc@^", "abc");
		doTestOrient(STextTypeHandlerFactory.COMMA_DELIMITED, "Methods #3 ", "ABC", "ABC", ">@ABC@^", "@ABC");
		doTestOrient(STextTypeHandlerFactory.COMMA_DELIMITED, "Methods #4 ", "bcd,ef", "bcd,ef", ">@bcd,ef@^", "bcd,ef");
		doTestOrient(STextTypeHandlerFactory.COMMA_DELIMITED, "Methods #5 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient(STextTypeHandlerFactory.COMMA_DELIMITED, "Methods #6 ", "cde,FG", "cde,FG", ">@cde,FG@^", "cde,FG");
		doTestOrient(STextTypeHandlerFactory.COMMA_DELIMITED, "Methods #7 ", "CDE,fg", "CDE,fg", ">@CDE,fg@^", "@CDE,fg");
		doTestOrient(STextTypeHandlerFactory.COMMA_DELIMITED, "Methods #8 ", "12..def,GH", "12..def,GH", ">@12..def,GH@^", "12..def,GH");
		doTestOrient(STextTypeHandlerFactory.COMMA_DELIMITED, "Methods #9 ", "34..DEF,gh", "34..DEF,gh", ">@34..DEF,gh@^", "@34..DEF,gh");

		doTestSkipProcessing();

		doTestLeanOffsets();

		doTestFullOffsets("TestFullOffsets ", "BCD,EF,G", new int[] {3, 7}, new int[] {0, 1, 5, 9, 12, 13}, new int[] {0, 4, 8});

		doTestMirrored();

		doTestDirection();

		ISTextExpert expert = STextExpertFactory.getExpert(STextTypeHandlerFactory.COMMA_DELIMITED);
		String data = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
		String lean = toUT16(data);
		String full = expert.leanToFullText(lean);
		String model = "A@,B@,C@,D@,E@,F@,G@,H@,I@,J@,K@,L@,M@,N@,O@,P@,Q@,R@,S@,T@,U@,V@,W@,X@,Y@,Z";
		assertEquals("many inserts", model, toPseudo(full));
	}
}
