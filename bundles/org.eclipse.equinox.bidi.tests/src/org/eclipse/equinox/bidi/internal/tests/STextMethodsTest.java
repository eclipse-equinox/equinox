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
import org.eclipse.equinox.bidi.STextProcessorFactory;
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
		int[] state = new int[1];
		state[0] = -1;
		//processor = STextProcessorFactory.PROC_JAVA;

		STextProcessorNew processorNew = STextProcessorFactoryNew.getMultipassProcessor(STextProcessorFactory.JAVA);

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		//full = STextEngine.leanToFullText(processor, null, lean, state);
		full = processorNew.leanToFullText(lean);

		model = "A@=B@+C@;/* D=E+F;";
		assertEquals("full1", model, toPseudo(full));
		data = "A=B+C; D=E+F;";
		lean = toUT16(data);
		//full = STextEngine.leanToFullText(processor, null, lean, state);
		full = processorNew.leanToFullText(lean);

		model = "A=B+C; D=E+F;";
		assertEquals("full2", model, toPseudo(full));
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		//full = STextEngine.leanToFullText(processor, null, lean, state);
		full = processorNew.leanToFullText(lean);

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

	private void doTestOrient(String processorDefID, String label, String data, String resLTR, String resRTL, String resCon) {
		String full, lean;

		STextProcessorNew processorLTR = STextProcessorFactoryNew.getProcessor(processorDefID, envLTR);
		STextProcessorNew processorRTL = STextProcessorFactoryNew.getProcessor(processorDefID, envRTL);
		STextProcessorNew processorCRL = STextProcessorFactoryNew.getProcessor(processorDefID, envCRL);

		lean = toUT16(data);
		//full = STextEngine.leanToFullText(processor, envLTR, lean, null);
		full = processorLTR.leanToFullText(lean);
		assertEquals(label + "LTR full", resLTR, toPseudo(full));
		//full = STextEngine.leanToFullText(processor, envRTL, lean, null);
		full = processorRTL.leanToFullText(lean);
		assertEquals("label + RTL full", resRTL, toPseudo(full));
		//full = STextEngine.leanToFullText(processor, envCRL, lean, null);
		full = processorCRL.leanToFullText(lean);
		assertEquals(label + "CON full", resCon, toPseudo(full));
	}

	private void doTestSkipProcessing() {
		doTestOrient("test.MyCommaLL", "Skip #1 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient("test.MyCommaLL", "Skip #2 ", "#CD,EF", "#CD,EF", ">@#CD,EF@^", "@#CD,EF");
	}

	private void doTestLeanOffsets() {
		String lean, data, label;
		int[] state = new int[1];
		//processor = STextProcessorFactory.PROC_JAVA;
		STextProcessorNew processorNew = STextProcessorFactoryNew.getMultipassProcessor(STextProcessorFactory.JAVA);

		int[] offsets;
		int[] model;

		data = "A=B+C;/* D=E+F;";
		lean = toUT16(data);
		state[0] = -1;
		//offsets = STextEngine.leanBidiCharOffsets(processor, null, lean, state);
		offsets = processorNew.leanBidiCharOffsets(lean);
		model = new int[] {1, 3, 5};
		label = "leanBidiCharOffsets() #1 ";
		assertEquals(label, array_display(model), array_display(offsets));
		data = "A=B+C;*/ D=E+F;";
		lean = toUT16(data);
		//offsets = STextEngine.leanBidiCharOffsets(processor, null, lean, state);
		offsets = processorNew.leanBidiCharOffsets(lean);
		model = new int[] {6, 10, 12};
		label = "leanBidiCharOffsets() #2 ";
		assertEquals(label, array_display(model), array_display(offsets));
	}

	private void doTestFullOffsets(String label, String data, int[] resLTR, int[] resRTL, int[] resCon) {
		String full, lean, msg;
		int[] offsets;
		//		processor = STextProcessorFactory.PROC_COMMA_DELIMITED;
		STextProcessorNew processorNew = STextProcessorFactoryNew.getMultipassProcessor(STextProcessorFactory.COMMA_DELIMITED, envLTR);
		STextProcessorNew processorRTL = STextProcessorFactoryNew.getMultipassProcessor(STextProcessorFactory.COMMA_DELIMITED, envRTL);
		STextProcessorNew processorCLR = STextProcessorFactoryNew.getMultipassProcessor(STextProcessorFactory.COMMA_DELIMITED, envCLR);

		lean = toUT16(data);
		//		full = STextEngine.leanToFullText(processor, envLTR, lean, null);
		full = processorNew.leanToFullText(lean);
		//		offsets = STextEngine.fullBidiCharOffsets(processor, envLTR, full, null);
		offsets = processorNew.fullBidiCharOffsets(full);
		msg = label + "LTR ";
		assertEquals(msg, array_display(resLTR), array_display(offsets));
		//full = STextEngine.leanToFullText(processor, envRTL, lean, null);
		full = processorRTL.leanToFullText(lean);
		//offsets = STextEngine.fullBidiCharOffsets(processor, envRTL, full, null);
		offsets = processorRTL.fullBidiCharOffsets(full);
		msg = label + "RTL ";
		assertEquals(msg, array_display(resRTL), array_display(offsets));
		//full = STextEngine.leanToFullText(processor, envCLR, lean, null);
		full = processorCLR.leanToFullText(lean);
		//		offsets = STextEngine.fullBidiCharOffsets(processor, envCLR, full, null);
		offsets = processorCLR.fullBidiCharOffsets(full);
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
		STextProcessorNew processorRL = STextProcessorFactoryNew.getProcessor("test.MyCommaRL");
		dirA = processorRL.getCurDirection(toUT16("###"));
		dirH = processorRL.getCurDirection(toUT16("ABC"));
		assertTrue("TestDirection #1", dirA == RTL && dirH == LTR);

		STextProcessorNew processorRR = STextProcessorFactoryNew.getProcessor("test.MyCommaRR");
		dirA = processorRR.getCurDirection(toUT16("###"));
		dirH = processorRR.getCurDirection(toUT16("ABC"));
		assertTrue("TestDirection #2", dirA == RTL && dirH == RTL);

		STextProcessorNew processorLL = STextProcessorFactoryNew.getProcessor("test.MyCommaLL");
		lean = toUT16("ABC,#DEF,HOST,com");
		full = processorLL.leanToFullText(lean);
		assertEquals("TestDirection #9 full", "ABC@,#DEF@,HOST,com", toPseudo(full));

		lean = toUT16("ABC,DEF,HOST,com");
		full = processorLL.leanToFullText(lean);

		assertEquals("TestDirection #10 full", "ABC@,DEF@,HOST,com", toPseudo(full));

		STextEnvironment environment = new STextEnvironment(null, true, STextEnvironment.ORIENT_LTR);
		STextProcessorNew processorNew = STextProcessorFactoryNew.getProcessor("test.MyCommaRL", environment);
		dirA = processorNew.getCurDirection(toUT16("###"));
		dirH = processorNew.getCurDirection(toUT16("ABC"));
		assertTrue("TestDirection #10.5", dirA == RTL && dirH == LTR);

		lean = toUT16("ABC,#DEF,HOST,com");
		full = processorNew.leanToFullText(lean);
		assertEquals("TestDirection #11 full", "<&ABC,#DEF,HOST,com&^", toPseudo(full));

		data = "ABc,#DEF,HOSt,COM";
		lean = toUT16(data);
		//full = STextEngine.leanToFullText(processor, environment, lean, null);
		full = processorNew.leanToFullText(lean);
		model = "<&ABc,#DEF,HOSt,COM&^";
		assertEquals("TestDirection #12 full", model, toPseudo(full));

		data = "ABc,#DEF,HOSt,";
		lean = toUT16(data);
		//full = STextEngine.leanToFullText(processor, environment, lean, null);
		full = processorNew.leanToFullText(lean);
		model = "<&ABc,#DEF,HOSt,&^";
		assertEquals("TestDirection #13 full", model, toPseudo(full));

		data = "ABC,DEF,HOST,com";
		lean = toUT16(data);
		//full = STextEngine.leanToFullText(processor, environment, lean, null);
		full = processorNew.leanToFullText(lean);
		model = "ABC@,DEF@,HOST,com";
		assertEquals("TestDirection #14 full", model, toPseudo(full));

		data = "--,---,----";
		lean = toUT16(data);
		//full = STextEngine.leanToFullText(processor, environment, lean, null);
		full = processorNew.leanToFullText(lean);
		model = "--,---,----";
		assertEquals("TestDirection #15 full", model, toPseudo(full));

		data = "ABC,|DEF,HOST,com";
		lean = toUT16(data);
		//		full = STextEngine.leanToFullText(processor, environment, lean, null);
		full = processorNew.leanToFullText(lean);

		model = "ABC,|DEF@,HOST,com";
		assertEquals("TestDirection #16 full", model, toPseudo(full));

		data = "ABc,|#DEF,HOST,com";
		lean = toUT16(data);
		processorNew = STextProcessorFactoryNew.getProcessor("test.MyCommaRL", envRTLMIR);
		full = processorNew.leanToFullText(lean);
		model = "ABc,|#DEF,HOST,com";
		assertEquals("TestDirection #17 full", model, toPseudo(full));
		int dir = processorNew.getCurDirection(lean);
		assertEquals("Test curDirection", RTL, dir);
	}

	public void testMethods() {

		doTestTools();

		doTestState();

		doTestOrientation();

		doTestOrient(STextProcessorFactory.COMMA_DELIMITED, "Methods #1 ", "", "", "", "");
		doTestOrient(STextProcessorFactory.COMMA_DELIMITED, "Methods #2 ", "abc", "abc", ">@abc@^", "abc");
		doTestOrient(STextProcessorFactory.COMMA_DELIMITED, "Methods #3 ", "ABC", "ABC", ">@ABC@^", "@ABC");
		doTestOrient(STextProcessorFactory.COMMA_DELIMITED, "Methods #4 ", "bcd,ef", "bcd,ef", ">@bcd,ef@^", "bcd,ef");
		doTestOrient(STextProcessorFactory.COMMA_DELIMITED, "Methods #5 ", "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
		doTestOrient(STextProcessorFactory.COMMA_DELIMITED, "Methods #6 ", "cde,FG", "cde,FG", ">@cde,FG@^", "cde,FG");
		doTestOrient(STextProcessorFactory.COMMA_DELIMITED, "Methods #7 ", "CDE,fg", "CDE,fg", ">@CDE,fg@^", "@CDE,fg");
		doTestOrient(STextProcessorFactory.COMMA_DELIMITED, "Methods #8 ", "12..def,GH", "12..def,GH", ">@12..def,GH@^", "12..def,GH");
		doTestOrient(STextProcessorFactory.COMMA_DELIMITED, "Methods #9 ", "34..DEF,gh", "34..DEF,gh", ">@34..DEF,gh@^", "@34..DEF,gh");

		doTestSkipProcessing();

		doTestLeanOffsets();

		doTestFullOffsets("TestFullOffsets ", "BCD,EF,G", new int[] {3, 7}, new int[] {0, 1, 5, 9, 12, 13}, new int[] {0, 4, 8});

		doTestMirrored();

		doTestDirection();

		STextProcessorNew processorNew = STextProcessorFactoryNew.getProcessor(STextProcessorFactory.COMMA_DELIMITED);
		//		processor = STextProcessorFactory.PROC_COMMA_DELIMITED;
		String data = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
		String lean = toUT16(data);
		//String full = STextEngine.leanToFullText(processor, null, lean, null);
		String full = processorNew.leanToFullText(lean);
		String model = "A@,B@,C@,D@,E@,F@,G@,H@,I@,J@,K@,L@,M@,N@,O@,P@,Q@,R@,S@,T@,U@,V@,W@,X@,Y@,Z";
		assertEquals("many inserts", model, toPseudo(full));
	}
}
