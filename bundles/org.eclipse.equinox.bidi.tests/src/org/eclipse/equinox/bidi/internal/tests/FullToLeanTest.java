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
import org.eclipse.equinox.bidi.complexp.StringProcessor;
import org.eclipse.equinox.bidi.complexp.IBiDiProcessor;

/**
 * Tests fullToLean method
 */

public class FullToLeanTest extends ComplExpTestBase {

	private IComplExpProcessor processor;

	protected void setUp() throws Exception {
		super.setUp();
		processor = StringProcessor
				.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
	}

	static int[] getpos(IComplExpProcessor complexp, boolean leanToFull) {
		int[] pos = new int[5];

		if (leanToFull) {
			pos[0] = complexp.leanToFullPos(0);
			pos[1] = complexp.leanToFullPos(4);
			pos[2] = complexp.leanToFullPos(7);
			pos[3] = complexp.leanToFullPos(10);
			pos[4] = complexp.leanToFullPos(30);
		} else {
			pos[0] = complexp.fullToLeanPos(0);
			pos[1] = complexp.fullToLeanPos(4);
			pos[2] = complexp.fullToLeanPos(7);
			pos[3] = complexp.fullToLeanPos(10);
			pos[4] = complexp.fullToLeanPos(30);
		}
		return pos;
	}

	private void doTest1(String msg, String data, String leanLTR,
			String fullLTR, int[] l2fPosLTR, int[] f2lPosLTR, String leanRTL,
			String fullRTL, int[] l2fPosRTL, int[] f2lPosRTL) {
		String text, full, lean, label;
		int[] pos;

		text = toUT16(data);
		processor.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
		lean = processor.fullToLeanText(text);
		assertEquals(msg + "LTR lean", leanLTR, toPseudo(lean));
		full = processor.leanToFullText(lean);
		assertEquals(msg + "LTR full", fullLTR, toPseudo(full));
		pos = getpos(processor, true);
		label = msg + "leanToFullPos() LTR, expected="
				+ array_display(l2fPosLTR) + " result=" + array_display(pos);
		assertTrue(label, arrays_equal(l2fPosLTR, pos));
		pos = getpos(processor, false);
		label = msg + "fullToLeanPos() LTR, expected="
				+ array_display(f2lPosLTR) + " result=" + array_display(pos);
		assertTrue(label, arrays_equal(f2lPosLTR, pos));
		processor.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
		lean = processor.fullToLeanText(text);
		assertEquals(msg + "RTL lean", leanRTL, toPseudo(lean));
		full = processor.leanToFullText(lean);
		assertEquals(msg + "RTL full", fullRTL, toPseudo(full));
		pos = getpos(processor, true);
		label = msg + "leanToFullPos() RTL, expected="
				+ array_display(l2fPosRTL) + " result=" + array_display(pos);
		assertTrue(label, arrays_equal(l2fPosRTL, pos));
		pos = getpos(processor, false);
		label = msg + "fullToLeanPos() RTL, expected="
				+ array_display(f2lPosRTL) + " result=" + array_display(pos);
		assertTrue(label, arrays_equal(f2lPosRTL, pos));
	}

	private void doTest2(String msg) {
		String text, data, full, lean, model;
		int state, state2, state3;

		processor = StringProcessor.getProcessor(IBiDiProcessor.SQL);
		processor.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
		data = "update \"AB_CDE\" set \"COL1\"@='01', \"COL2\"@='02' /* GH IJK";
		text = toUT16(data);
		lean = processor.fullToLeanText(text);
		state = processor.getFinalState();
		model = "update \"AB_CDE\" set \"COL1\"='01', \"COL2\"='02' /* GH IJK";
		assertEquals(msg + "LTR lean", model, toPseudo(lean));
		full = processor.leanToFullText(lean);
		assertEquals(msg + "LTR full", data, toPseudo(full));
		assertEquals(msg + "state from leanToFullText", processor
				.getFinalState(), state);
		data = "THIS IS A COMMENT LINE";
		text = toUT16(data);
		lean = processor.fullToLeanText(text, state);
		state2 = processor.getFinalState();
		model = "THIS IS A COMMENT LINE";
		assertEquals(msg + "LTR lean2", model, toPseudo(lean));
		full = processor.leanToFullText(lean, state);
		assertEquals(msg + "LTR full2", data, toPseudo(full));
		assertEquals(msg + "state from leanToFullText2", processor
				.getFinalState(), state2);
		data = "SOME MORE */ where \"COL3\"@=123";
		text = toUT16(data);
		lean = processor.fullToLeanText(text, state2);
		state3 = processor.getFinalState();
		model = "SOME MORE */ where \"COL3\"=123";
		assertEquals(msg + "LTR lean3", model, toPseudo(lean));
		full = processor.leanToFullText(lean, state2);
		assertEquals(msg + "LTR full3", data, toPseudo(full));
		assertEquals(msg + "state from leanToFullText3", processor
				.getFinalState(), state3);
	}

	public void testFullToLean() {

		doTest1("testFullToLean #1 - ", "", "", "",
				new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 0, 0, 0, 0 }, "",
				"", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 0, 0, 0, 0 });
		doTest1("testFullToLean #01 - ", "1.abc", "1.abc", "1.abc", new int[] {
				0, 4, 7, 10, 30 }, new int[] { 0, 4, 5, 5, 5 }, "1.abc",
				">@1.abc@^", new int[] { 2, 6, 9, 12, 32 }, new int[] { 0, 2,
						5, 5, 5 });
		doTest1("testFullToLean #02 - ", "2.abc,def", "2.abc,def", "2.abc,def",
				new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4, 7, 9, 9 },
				"2.abc,def", ">@2.abc,def@^", new int[] { 2, 6, 9, 12, 32 },
				new int[] { 0, 2, 5, 8, 9 });
		doTest1("testFullToLean #03 - ", "@a.3.bc,def", "a.3.bc,def",
				"a.3.bc,def", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4,
						7, 10, 10 }, "a.3.bc,def", ">@a.3.bc,def@^", new int[] {
						2, 6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 10 });
		doTest1("testFullToLean #04 - ", "@@a.4.bc,def", "a.4.bc,def",
				"a.4.bc,def", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4,
						7, 10, 10 }, "a.4.bc,def", ">@a.4.bc,def@^", new int[] {
						2, 6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 10 });
		doTest1("testFullToLean #05 - ", "@5.abc,def", "5.abc,def",
				"5.abc,def", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4,
						7, 9, 9 }, "5.abc,def", ">@5.abc,def@^", new int[] { 2,
						6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 9 });
		doTest1("testFullToLean #06 - ", "@@6.abc,def", "6.abc,def",
				"6.abc,def", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4,
						7, 9, 9 }, "6.abc,def", ">@6.abc,def@^", new int[] { 2,
						6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 9 });
		doTest1("testFullToLean #07 - ", "7abc,@def", "7abc,@def", "7abc,@def",
				new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4, 7, 9, 9 },
				"7abc,@def", ">@7abc,@def@^", new int[] { 2, 6, 9, 12, 32 },
				new int[] { 0, 2, 5, 8, 9 });
		doTest1("testFullToLean #08 - ", "8abc,@@def", "8abc,@def",
				"8abc,@def", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4,
						7, 9, 9 }, "8abc,@def", ">@8abc,@def@^", new int[] { 2,
						6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 9 });
		doTest1("testFullToLean #09 - ", "9abc,def@", "9abc,def", "9abc,def",
				new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4, 7, 8, 8 },
				"9abc,def", ">@9abc,def@^", new int[] { 2, 6, 9, 12, 32 },
				new int[] { 0, 2, 5, 8, 8 });
		doTest1("testFullToLean #10 - ", "10abc,def@@", "10abc,def",
				"10abc,def", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4,
						7, 9, 9 }, "10abc,def", ">@10abc,def@^", new int[] { 2,
						6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 9 });
		doTest1("testFullToLean #11 - ", "@a.11.bc,@def@", "a.11.bc,@def",
				"a.11.bc,@def", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0,
						4, 7, 10, 12 }, "a.11.bc,@def", ">@a.11.bc,@def@^",
				new int[] { 2, 6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 12 });
		doTest1("testFullToLean #12 - ", "@@a.12.bc,@@def@@", "a.12.bc,@def",
				"a.12.bc,@def", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0,
						4, 7, 10, 12 }, "a.12.bc,@def", ">@a.12.bc,@def@^",
				new int[] { 2, 6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 12 });
		doTest1("testFullToLean #13 - ", "13ABC", "13ABC", "13ABC", new int[] {
				0, 4, 7, 10, 30 }, new int[] { 0, 4, 5, 5, 5 }, "13ABC",
				">@13ABC@^", new int[] { 2, 6, 9, 12, 32 }, new int[] { 0, 2,
						5, 5, 5 });
		doTest1("testFullToLean #14 - ", "14ABC,DE", "14ABC,DE", "14ABC@,DE",
				new int[] { 0, 4, 8, 11, 31 }, new int[] { 0, 4, 6, 8, 8 },
				"14ABC,DE", ">@14ABC@,DE@^", new int[] { 2, 6, 10, 13, 33 },
				new int[] { 0, 2, 5, 7, 8 });
		doTest1("testFullToLean #15 - ", "15ABC@,DE", "15ABC,DE", "15ABC@,DE",
				new int[] { 0, 4, 8, 11, 31 }, new int[] { 0, 4, 6, 8, 8 },
				"15ABC,DE", ">@15ABC@,DE@^", new int[] { 2, 6, 10, 13, 33 },
				new int[] { 0, 2, 5, 7, 8 });
		doTest1("testFullToLean #16 - ", "16ABC@@,DE", "16ABC,DE", "16ABC@,DE",
				new int[] { 0, 4, 8, 11, 31 }, new int[] { 0, 4, 6, 8, 8 },
				"16ABC,DE", ">@16ABC@,DE@^", new int[] { 2, 6, 10, 13, 33 },
				new int[] { 0, 2, 5, 7, 8 });
		doTest1("testFullToLean #17 - ", "17ABC,@@DE", "17ABC,@DE",
				"17ABC,@DE", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4,
						7, 9, 9 }, "17ABC,@DE", ">@17ABC,@DE@^", new int[] { 2,
						6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 9 });
		doTest1("testFullToLean #18 - ", "18ABC,DE,FGH", "18ABC,DE,FGH",
				"18ABC@,DE@,FGH", new int[] { 0, 4, 8, 12, 32 }, new int[] { 0,
						4, 6, 8, 12 }, "18ABC,DE,FGH", ">@18ABC@,DE@,FGH@^",
				new int[] { 2, 6, 10, 14, 34 }, new int[] { 0, 2, 5, 7, 12 });
		doTest1("testFullToLean #19 - ", "19ABC@,DE@,FGH", "19ABC,DE,FGH",
				"19ABC@,DE@,FGH", new int[] { 0, 4, 8, 12, 32 }, new int[] { 0,
						4, 6, 8, 12 }, "19ABC,DE,FGH", ">@19ABC@,DE@,FGH@^",
				new int[] { 2, 6, 10, 14, 34 }, new int[] { 0, 2, 5, 7, 12 });
		doTest1("testFullToLean #20 - ", "20ABC,@DE,@FGH", "20ABC,@DE,@FGH",
				"20ABC,@DE,@FGH", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0,
						4, 7, 10, 14 }, "20ABC,@DE,@FGH", ">@20ABC,@DE,@FGH@^",
				new int[] { 2, 6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 14 });
		doTest1("testFullToLean #21 - ", "21ABC@@,DE@@,FGH", "21ABC,DE,FGH",
				"21ABC@,DE@,FGH", new int[] { 0, 4, 8, 12, 32 }, new int[] { 0,
						4, 6, 8, 12 }, "21ABC,DE,FGH", ">@21ABC@,DE@,FGH@^",
				new int[] { 2, 6, 10, 14, 34 }, new int[] { 0, 2, 5, 7, 12 });
		doTest1("testFullToLean #22 - ", "22ABC,@@DE,@@FGH", "22ABC,@DE,@FGH",
				"22ABC,@DE,@FGH", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0,
						4, 7, 10, 14 }, "22ABC,@DE,@FGH", ">@22ABC,@DE,@FGH@^",
				new int[] { 2, 6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 14 });
		doTest1("testFullToLean #23 - ", ">@23abc@^", "23abc", "23abc",
				new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4, 5, 5, 5 },
				"23abc", ">@23abc@^", new int[] { 2, 6, 9, 12, 32 }, new int[] {
						0, 2, 5, 5, 5 });
		doTest1("testFullToLean #24 - ", "24abc@^", "24abc", "24abc",
				new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4, 5, 5, 5 },
				"24abc", ">@24abc@^", new int[] { 2, 6, 9, 12, 32 }, new int[] {
						0, 2, 5, 5, 5 });
		doTest1("testFullToLean #25 - ", ">@25abc", "25abc", "25abc",
				new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4, 5, 5, 5 },
				"25abc", ">@25abc@^", new int[] { 2, 6, 9, 12, 32 }, new int[] {
						0, 2, 5, 5, 5 });
		doTest1("testFullToLean #26 - ", "26AB,CD@EF,GHI", "26AB,CD@EF,GHI",
				"26AB@,CD@EF@,GHI", new int[] { 0, 5, 8, 12, 32 }, new int[] {
						0, 4, 6, 9, 14 }, "26AB,CD@EF,GHI",
				">@26AB@,CD@EF@,GHI@^", new int[] { 2, 7, 10, 14, 34 },
				new int[] { 0, 2, 4, 7, 14 });
		doTest1("testFullToLean #27 - ", "27AB,CD@123ef,GHI",
				"27AB,CD@123ef,GHI", "27AB@,CD@123ef,GHI", new int[] { 0, 5, 8,
						11, 31 }, new int[] { 0, 4, 6, 9, 17 },
				"27AB,CD@123ef,GHI", ">@27AB@,CD@123ef,GHI@^", new int[] { 2,
						7, 10, 13, 33 }, new int[] { 0, 2, 4, 7, 17 });
		doTest1("testFullToLean #28 - ", ">28ABC@,DE@,FGH^", "28ABC,DE,FGH",
				"28ABC@,DE@,FGH", new int[] { 0, 4, 8, 12, 32 }, new int[] { 0,
						4, 6, 8, 12 }, "28ABC,DE,FGH", ">@28ABC@,DE@,FGH@^",
				new int[] { 2, 6, 10, 14, 34 }, new int[] { 0, 2, 5, 7, 12 });
		doTest1("testFullToLean #29 - ", ">>29ABC@,DE@,FGH^^", "29ABC,DE,FGH",
				"29ABC@,DE@,FGH", new int[] { 0, 4, 8, 12, 32 }, new int[] { 0,
						4, 6, 8, 12 }, "29ABC,DE,FGH", ">@29ABC@,DE@,FGH@^",
				new int[] { 2, 6, 10, 14, 34 }, new int[] { 0, 2, 5, 7, 12 });
		doTest1("testFullToLean #30 - ", ">30AB>C^@,DE@,FGH^",
				"30AB>C^,DE,FGH", "30AB>C^@,DE@,FGH", new int[] { 0, 4, 8, 12,
						32 }, new int[] { 0, 4, 7, 9, 14 }, "30AB>C^,DE,FGH",
				">@30AB>C^@,DE@,FGH@^", new int[] { 2, 6, 10, 14, 34 },
				new int[] { 0, 2, 5, 7, 14 });
		doTest1("testFullToLean #31 - ", ">31AB>C@,DE@,FGH^^", "31AB>C,DE,FGH",
				"31AB>C@,DE@,FGH", new int[] { 0, 4, 8, 12, 32 }, new int[] {
						0, 4, 6, 9, 13 }, "31AB>C,DE,FGH",
				">@31AB>C@,DE@,FGH@^", new int[] { 2, 6, 10, 14, 34 },
				new int[] { 0, 2, 5, 7, 13 });
		doTest1("testFullToLean #32 - ", ">@32ABC@,DE@,FGH@^", "32ABC,DE,FGH",
				"32ABC@,DE@,FGH", new int[] { 0, 4, 8, 12, 32 }, new int[] { 0,
						4, 6, 8, 12 }, "32ABC,DE,FGH", ">@32ABC@,DE@,FGH@^",
				new int[] { 2, 6, 10, 14, 34 }, new int[] { 0, 2, 5, 7, 12 });
		doTest1("testFullToLean #33 - ", "@33ABC@,DE@,FGH@^", "33ABC,DE,FGH",
				"33ABC@,DE@,FGH", new int[] { 0, 4, 8, 12, 32 }, new int[] { 0,
						4, 6, 8, 12 }, "33ABC,DE,FGH", ">@33ABC@,DE@,FGH@^",
				new int[] { 2, 6, 10, 14, 34 }, new int[] { 0, 2, 5, 7, 12 });
		doTest1("testFullToLean #34 - ", ">@34ABC@,DE@,FGH@", "34ABC,DE,FGH",
				"34ABC@,DE@,FGH", new int[] { 0, 4, 8, 12, 32 }, new int[] { 0,
						4, 6, 8, 12 }, "34ABC,DE,FGH", ">@34ABC@,DE@,FGH@^",
				new int[] { 2, 6, 10, 14, 34 }, new int[] { 0, 2, 5, 7, 12 });
		doTest1("testFullToLean #35 - ", "35ABC@@DE@@@GH@", "35ABC@DE@GH",
				"35ABC@DE@GH", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4,
						7, 10, 11 }, "35ABC@DE@GH", ">@35ABC@DE@GH@^",
				new int[] { 2, 6, 9, 12, 32 }, new int[] { 0, 2, 5, 8, 11 });
		doTest1("testFullToLean #36 - ", "36ABC@@DE@@@@@@", "36ABC@DE",
				"36ABC@DE", new int[] { 0, 4, 7, 10, 30 }, new int[] { 0, 4, 7,
						8, 8 }, "36ABC@DE", ">@36ABC@DE@^", new int[] { 2, 6,
						9, 12, 32 }, new int[] { 0, 2, 5, 8, 8 });
		doTest1("testFullToLean #37 - ", ">>>@@@@@^^^", "", "", new int[] { 0,
				4, 7, 10, 30 }, new int[] { 0, 0, 0, 0, 0 }, "", "", new int[] {
				0, 4, 7, 10, 30 }, new int[] { 0, 0, 0, 0, 0 });

		// test fullToLeanText with initial state
		doTest2("testFullToLean #38 - ");
	}
}
