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
 * Tests all plug-in extensions
 */

public class BidiComplexExtensionsTest extends BidiComplexTestBase {

	static int state = BidiComplexHelper.STATE_NOTHING_GOING;

	private void doTest1(BidiComplexHelper complexp, String label, String data, String result) {
		String full;
		full = complexp.leanToFullText(toUT16(data), state);
		state = complexp.getFinalState();
		assertEquals(label + " data = " + data, result, toPseudo(full));
	}

	private void doTest2(BidiComplexHelper complexp, String label, String data, String result) {
		String full;
		full = complexp.leanToFullText(data, state);
		state = complexp.getFinalState();
		assertEquals(label + " data = " + data, result, toPseudo(full));
	}

	public void testExtensions() {

		BidiComplexHelper helper;

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.COMMA_DELIMITED);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "Comma #1", "ab,cd, AB, CD, EFG", "ab,cd, AB@, CD@, EFG");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.EMAIL);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "Email #1", "abc.DEF:GHI", "abc.DEF@:GHI");
		doTest1(helper, "Email #2", "DEF.GHI \"A.B\":JK ", "DEF@.GHI @\"A.B\"@:JK ");
		doTest1(helper, "Email #3", "DEF,GHI (A,B);JK ", "DEF@,GHI @(A,B)@;JK ");
		doTest1(helper, "Email #4", "DEF.GHI (A.B :JK ", "DEF@.GHI @(A.B :JK ");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.FILE);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "File #1", "c:\\A\\B\\FILE.EXT", "c:\\A@\\B@\\FILE@.EXT");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.JAVA);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "Java #1", "A = B + C;", "A@ = B@ + C;");
		doTest1(helper, "Java #2", "A   = B + C;", "A@   = B@ + C;");
		doTest1(helper, "Java #3", "A = \"B+C\"+D;", "A@ = \"B+C\"@+D;");
		doTest1(helper, "Java #4", "A = \"B+C+D;", "A@ = \"B+C+D;");
		doTest1(helper, "Java #5", "A = \"B\\\"C\"+D;", "A@ = \"B\\\"C\"@+D;");
		doTest1(helper, "Java #6", "A = /*B+C*/ D;", "A@ = /*B+C*/@ D;");
		doTest1(helper, "Java #7", "A = /*B+C* D;", "A@ = /*B+C* D;");
		doTest1(helper, "Java #8", "X+Y+Z */ B;  ", "X+Y+Z */@ B;  ");
		doTest1(helper, "Java #9", "A = //B+C* D;", "A@ = //B+C* D;");
		doTest1(helper, "Java #10", "A = //B+C`|D+E;", "A@ = //B+C`|D@+E;");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.PROPERTY);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "Property #0", "NAME,VAL1,VAL2", "NAME,VAL1,VAL2");
		doTest1(helper, "Property #1", "NAME=VAL1,VAL2", "NAME@=VAL1,VAL2");
		doTest1(helper, "Property #2", "NAME=VAL1,VAL2=VAL3", "NAME@=VAL1,VAL2=VAL3");

		String data;
		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.REGEXP);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		data = toUT16("ABC(?") + "#" + toUT16("DEF)GHI");
		doTest2(helper, "Regex #0.0", data, "A@B@C@(?#DEF)@G@H@I");
		data = toUT16("ABC(?") + "#" + toUT16("DEF");
		doTest2(helper, "Regex #0.1", data, "A@B@C@(?#DEF");
		doTest1(helper, "Regex #0.2", "GHI)JKL", "GHI)@J@K@L");
		data = toUT16("ABC(?") + "<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2(helper, "Regex #1", data, "A@B@C@(?<DEF>@G@H@I");
		doTest1(helper, "Regex #2.0", "ABC(?'DEF'GHI", "A@B@C@(?'DEF'@G@H@I");
		doTest1(helper, "Regex #2.1", "ABC(?'DEFGHI", "A@B@C@(?'DEFGHI");
		data = toUT16("ABC(?(") + "<" + toUT16("DEF") + ">" + toUT16(")GHI");
		doTest2(helper, "Regex #3", data, "A@B@C@(?(<DEF>)@G@H@I");
		doTest1(helper, "Regex #4", "ABC(?('DEF')GHI", "A@B@C@(?('DEF')@G@H@I");
		doTest1(helper, "Regex #5", "ABC(?(DEF)GHI", "A@B@C@(?(DEF)@G@H@I");
		data = toUT16("ABC(?") + "&" + toUT16("DEF)GHI");
		doTest2(helper, "Regex #6", data, "A@B@C@(?&DEF)@G@H@I");
		data = toUT16("ABC(?") + "P<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2(helper, "Regex #7", data, "A@B@C(?p<DEF>@G@H@I");
		data = toUT16("ABC\\k") + "<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2(helper, "Regex #8", data, "A@B@C\\k<DEF>@G@H@I");
		doTest1(helper, "Regex #9", "ABC\\k'DEF'GHI", "A@B@C\\k'DEF'@G@H@I");
		doTest1(helper, "Regex #10", "ABC\\k{DEF}GHI", "A@B@C\\k{DEF}@G@H@I");
		data = toUT16("ABC(?") + "P=" + toUT16("DEF)GHI");
		doTest2(helper, "Regex #11", data, "A@B@C(?p=DEF)@G@H@I");
		doTest1(helper, "Regex #12", "ABC\\g{DEF}GHI", "A@B@C\\g{DEF}@G@H@I");
		data = toUT16("ABC\\g") + "<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2(helper, "Regex #13", data, "A@B@C\\g<DEF>@G@H@I");
		doTest1(helper, "Regex #14", "ABC\\g'DEF'GHI", "A@B@C\\g'DEF'@G@H@I");
		data = toUT16("ABC(?(") + "R&" + toUT16("DEF)GHI");
		doTest2(helper, "Regex #15", data, "A@B@C(?(r&DEF)@G@H@I");
		data = toUT16("ABC") + "\\Q" + toUT16("DEF") + "\\E" + toUT16("GHI");
		doTest2(helper, "Regex #16.0", data, "A@B@C\\qDEF\\eG@H@I");
		data = toUT16("ABC") + "\\Q" + toUT16("DEF");
		doTest2(helper, "Regex #16.1", data, "A@B@C\\qDEF");
		data = toUT16("GHI") + "\\E" + toUT16("JKL");
		doTest2(helper, "Regex #16.2", data, "GHI\\eJ@K@L");
		doTest1(helper, "Regex #17.0", "abc[d-h]ijk", "abc[d-h]ijk");
		doTest1(helper, "Regex #17.1", "aBc[d-H]iJk", "aBc[d-H]iJk");
		doTest1(helper, "Regex #17.2", "aB*[!-H]iJ2", "aB*[!-@H]iJ@2");
		doTest1(helper, "Regex #17.3", "aB*[1-2]J3", "aB*[@1-2]J@3");
		doTest1(helper, "Regex #17.4", "aB*[5-6]J3", "aB*[@5-@6]@J@3");
		doTest1(helper, "Regex #17.5", "a*[5-6]J3", "a*[5-@6]@J@3");
		doTest1(helper, "Regex #17.6", "aB*123", "aB*@123");
		doTest1(helper, "Regex #17.7", "aB*567", "aB*@567");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.SQL);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "SQL #0", "abc GHI", "abc GHI");
		doTest1(helper, "SQL #1", "abc DEF   GHI", "abc DEF@   GHI");
		doTest1(helper, "SQL #2", "ABC, DEF,   GHI", "ABC@, DEF@,   GHI");
		doTest1(helper, "SQL #3", "ABC'DEF GHI' JKL,MN", "ABC@'DEF GHI'@ JKL@,MN");
		doTest1(helper, "SQL #4.0", "ABC'DEF GHI JKL", "ABC@'DEF GHI JKL");
		doTest1(helper, "SQL #4.1", "MNO PQ' RS,TUV", "MNO PQ'@ RS@,TUV");
		doTest1(helper, "SQL #5", "ABC\"DEF GHI\" JKL,MN", "ABC@\"DEF GHI\"@ JKL@,MN");
		doTest1(helper, "SQL #6", "ABC\"DEF GHI JKL", "ABC@\"DEF GHI JKL");
		doTest1(helper, "SQL #7", "ABC/*DEF GHI*/ JKL,MN", "ABC@/*DEF GHI@*/ JKL@,MN");
		doTest1(helper, "SQL #8.0", "ABC/*DEF GHI JKL", "ABC@/*DEF GHI JKL");
		doTest1(helper, "SQL #8.1", "MNO PQ*/RS,TUV", "MNO PQ@*/RS@,TUV");
		doTest1(helper, "SQL #9", "ABC--DEF GHI JKL", "ABC@--DEF GHI JKL");
		doTest1(helper, "SQL #10", "ABC--DEF--GHI,JKL", "ABC@--DEF--GHI,JKL");
		doTest1(helper, "SQL #11", "ABC'DEF '' G I' JKL,MN", "ABC@'DEF '' G I'@ JKL@,MN");
		doTest1(helper, "SQL #12", "ABC\"DEF \"\" G I\" JKL,MN", "ABC@\"DEF \"\" G I\"@ JKL@,MN");
		doTest1(helper, "SQL #13", "ABC--DEF GHI`|JKL MN", "ABC@--DEF GHI`|JKL@ MN");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.SYSTEM_USER);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "System #1", "HOST(JACK)", "HOST@(JACK)");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.UNDERSCORE);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "Underscore #1", "A_B_C_d_e_F_G", "A@_B@_C_d_e_F@_G");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.URL);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "URL #1", "WWW.DOMAIN.COM/DIR1/DIR2/dir3/DIR4", "WWW@.DOMAIN@.COM@/DIR1@/DIR2/dir3/DIR4");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.XPATH);
		assertNotNull(helper);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		doTest1(helper, "Xpath #1", "abc(DEF)GHI", "abc(DEF@)GHI");
		doTest1(helper, "Xpath #2", "DEF.GHI \"A.B\":JK ", "DEF@.GHI@ \"A.B\"@:JK ");
		doTest1(helper, "Xpath #3", "DEF!GHI 'A!B'=JK ", "DEF@!GHI@ 'A!B'@=JK ");
		doTest1(helper, "Xpath #4", "DEF.GHI 'A.B :JK ", "DEF@.GHI@ 'A.B :JK ");

		helper = new BidiComplexHelper(IBidiComplexExpressionTypes.EMAIL);
		state = BidiComplexHelper.STATE_NOTHING_GOING;
		BidiComplexFeatures f1 = helper.getFeatures();
		assertEquals("<>.:,;@", f1.operators);
		BidiComplexFeatures f2 = new BidiComplexFeatures("+-*/", f1.specialsCount, f1.dirArabic, f1.dirHebrew, f1.ignoreArabic, f1.ignoreHebrew);
		helper.setFeatures(f2);
		doTest1(helper, "DelimsEsc #1", "abc+DEF-GHI", "abc+DEF@-GHI");
		doTest1(helper, "DelimsEsc #2", "DEF-GHI (A*B)/JK ", "DEF@-GHI @(A*B)@/JK ");
		doTest1(helper, "DelimsEsc #3", "DEF-GHI (A*B)/JK ", "DEF@-GHI @(A*B)@/JK ");
		doTest1(helper, "DelimsEsc #4", "DEF-GHI (A*B\\)*C) /JK ", "DEF@-GHI @(A*B\\)*C) @/JK ");
		doTest1(helper, "DelimsEsc #5", "DEF-GHI (A\\\\\\)*C) /JK ", "DEF@-GHI @(A\\\\\\)*C) @/JK ");
		doTest1(helper, "DelimsEsc #6", "DEF-GHI (A\\\\)*C /JK ", "DEF@-GHI @(A\\\\)@*C @/JK ");
		doTest1(helper, "DelimsEsc #7", "DEF-GHI (A\\)*C /JK ", "DEF@-GHI @(A\\)*C /JK ");
	}
}
