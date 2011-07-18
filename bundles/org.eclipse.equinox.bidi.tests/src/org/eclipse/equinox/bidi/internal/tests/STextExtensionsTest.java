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

import org.eclipse.equinox.bidi.STextEngine;
import org.eclipse.equinox.bidi.STextEnvironment;
import org.eclipse.equinox.bidi.custom.STextProcessor;

/**
 * Tests all plug-in extensions
 */

public class STextExtensionsTest extends STextTestBase {

	STextEnvironment env = STextEnvironment.DEFAULT;
	STextEnvironment envArabic = new STextEnvironment("ar", false, STextEnvironment.ORIENT_LTR);
	STextEnvironment envHebrew = new STextEnvironment("he", false, STextEnvironment.ORIENT_LTR);

	STextProcessor processor;
	int[] state = new int[1];

	private void doTest1(String label, String data, String result) {
		String full;
		full = STextEngine.leanToFullText(processor, env, toUT16(data), state);
		assertEquals(label + " data = " + data, result, toPseudo(full));
	}

	private void doTest2(String label, String data, String result) {
		String full;
		full = STextEngine.leanToFullText(processor, env, data, state);
		assertEquals(label + " data = " + data, result, toPseudo(full));
	}

	private void doTest3(String label, String data, String result) {
		String full;
		full = STextEngine.leanToFullText(processor, env, toUT16(data), state);
		assertEquals(label + " data = " + data, result, toPseudo(full));
	}

	public void testExtensions() {

		String data;
		processor = STextEngine.PROC_COMMA_DELIMITED;
		state[0] = STextEngine.STATE_INITIAL;

		doTest1("Comma #1", "ab,cd, AB, CD, EFG", "ab,cd, AB@, CD@, EFG");

		processor = STextEngine.PROC_EMAIL;
		state[0] = STextEngine.STATE_INITIAL;
		doTest1("Email #1", "abc.DEF:GHI", "abc.DEF@:GHI");
		doTest1("Email #2", "DEF.GHI \"A.B\":JK ", "DEF@.GHI @\"A.B\"@:JK ");
		doTest1("Email #3", "DEF,GHI (A,B);JK ", "DEF@,GHI @(A,B)@;JK ");
		doTest1("Email #4", "DEF.GHI (A.B :JK ", "DEF@.GHI @(A.B :JK ");
		env = envArabic;
		doTest1("Email #5", "#EF.GHI \"A.B\":JK ", "<&#EF.GHI \"A.B\":JK &^");
		doTest1("Email #6", "#EF,GHI (A,B);JK ", "<&#EF,GHI (A,B);JK &^");
		doTest1("Email #7", "#EF.GHI (A.B :JK ", "<&#EF.GHI (A.B :JK &^");
		data = toUT16("peter.pan") + "@" + toUT16("#EF.GHI");
		doTest2("Email #8", data, "<&peter&.pan@#EF.GHI&^");
		env = envHebrew;
		data = toUT16("peter.pan") + "@" + toUT16("DEF.GHI");
		doTest2("Email #9", data, "peter.pan@DEF@.GHI");

		processor = STextEngine.PROC_FILE;
		state[0] = STextEngine.STATE_INITIAL;
		doTest1("File #1", "c:\\A\\B\\FILE.EXT", "c:\\A@\\B@\\FILE@.EXT");

		processor = STextEngine.PROC_JAVA;
		state[0] = STextEngine.STATE_INITIAL;
		doTest1("Java #1", "A = B + C;", "A@ = B@ + C;");
		doTest1("Java #2", "A   = B + C;", "A@   = B@ + C;");
		doTest1("Java #3", "A = \"B+C\"+D;", "A@ = \"B+C\"@+D;");
		doTest1("Java #4", "A = \"B+C+D;", "A@ = \"B+C+D;");
		doTest1("Java #5", "A = \"B\\\"C\"+D;", "A@ = \"B\\\"C\"@+D;");
		doTest1("Java #6", "A = /*B+C*/ D;", "A@ = /*B+C@*/ D;");
		doTest1("Java #7", "A = /*B+C* D;", "A@ = /*B+C* D;");
		doTest1("Java #8", "X+Y+Z */ B;  ", "X+Y+Z @*/ B;  ");
		doTest1("Java #9", "A = //B+C* D;", "A@ = //B+C* D;");
		doTest1("Java #10", "A = //B+C`|D+E;", "A@ = //B+C`|D@+E;");

		processor = STextEngine.PROC_PROPERTY;
		state[0] = STextEngine.STATE_INITIAL;
		doTest1("Property #0", "NAME,VAL1,VAL2", "NAME,VAL1,VAL2");
		doTest1("Property #1", "NAME=VAL1,VAL2", "NAME@=VAL1,VAL2");
		doTest1("Property #2", "NAME=VAL1,VAL2=VAL3", "NAME@=VAL1,VAL2=VAL3");

		processor = STextEngine.PROC_REGEXP;
		state[0] = STextEngine.STATE_INITIAL;
		data = toUT16("ABC(?") + "#" + toUT16("DEF)GHI");
		doTest2("Regex #0.0", data, "A@B@C@(?#DEF)@G@H@I");
		data = toUT16("ABC(?") + "#" + toUT16("DEF");
		doTest2("Regex #0.1", data, "A@B@C@(?#DEF");
		doTest1("Regex #0.2", "GHI)JKL", "GHI)@J@K@L");
		data = toUT16("ABC(?") + "<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2("Regex #1", data, "A@B@C@(?<DEF>@G@H@I");
		doTest1("Regex #2.0", "ABC(?'DEF'GHI", "A@B@C@(?'DEF'@G@H@I");
		doTest1("Regex #2.1", "ABC(?'DEFGHI", "A@B@C@(?'DEFGHI");
		data = toUT16("ABC(?(") + "<" + toUT16("DEF") + ">" + toUT16(")GHI");
		doTest2("Regex #3", data, "A@B@C@(?(<DEF>)@G@H@I");
		doTest1("Regex #4", "ABC(?('DEF')GHI", "A@B@C@(?('DEF')@G@H@I");
		doTest1("Regex #5", "ABC(?(DEF)GHI", "A@B@C@(?(DEF)@G@H@I");
		data = toUT16("ABC(?") + "&" + toUT16("DEF)GHI");
		doTest2("Regex #6", data, "A@B@C@(?&DEF)@G@H@I");
		data = toUT16("ABC(?") + "P<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2("Regex #7", data, "A@B@C(?p<DEF>@G@H@I");
		data = toUT16("ABC\\k") + "<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2("Regex #8", data, "A@B@C\\k<DEF>@G@H@I");
		doTest1("Regex #9", "ABC\\k'DEF'GHI", "A@B@C\\k'DEF'@G@H@I");
		doTest1("Regex #10", "ABC\\k{DEF}GHI", "A@B@C\\k{DEF}@G@H@I");
		data = toUT16("ABC(?") + "P=" + toUT16("DEF)GHI");
		doTest2("Regex #11", data, "A@B@C(?p=DEF)@G@H@I");
		doTest1("Regex #12", "ABC\\g{DEF}GHI", "A@B@C\\g{DEF}@G@H@I");
		data = toUT16("ABC\\g") + "<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2("Regex #13", data, "A@B@C\\g<DEF>@G@H@I");
		doTest1("Regex #14", "ABC\\g'DEF'GHI", "A@B@C\\g'DEF'@G@H@I");
		data = toUT16("ABC(?(") + "R&" + toUT16("DEF)GHI");
		doTest2("Regex #15", data, "A@B@C(?(r&DEF)@G@H@I");
		data = toUT16("ABC") + "\\Q" + toUT16("DEF") + "\\E" + toUT16("GHI");
		doTest2("Regex #16.0", data, "A@B@C\\qDEF\\eG@H@I");
		data = toUT16("ABC") + "\\Q" + toUT16("DEF");
		doTest2("Regex #16.1", data, "A@B@C\\qDEF");
		data = toUT16("GHI") + "\\E" + toUT16("JKL");
		doTest2("Regex #16.2", data, "GHI\\eJ@K@L");
		doTest1("Regex #17.0", "abc[d-h]ijk", "abc[d-h]ijk");
		doTest1("Regex #17.1", "aBc[d-H]iJk", "aBc[d-H]iJk");
		doTest1("Regex #17.2", "aB*[!-H]iJ2", "aB*[!-@H]iJ@2");
		doTest1("Regex #17.3", "aB*[1-2]J3", "aB*[@1-2]J@3");
		doTest1("Regex #17.4", "aB*[5-6]J3", "aB*[@5-@6]@J@3");
		doTest1("Regex #17.5", "a*[5-6]J3", "a*[5-@6]@J@3");
		doTest1("Regex #17.6", "aB*123", "aB*@123");
		doTest1("Regex #17.7", "aB*567", "aB*@567");

		env = envArabic;
		data = toUT16("#BC(?") + "#" + toUT16("DEF)GHI");
		doTest2("Regex #0.0", data, "<&#BC(?#DEF)GHI&^");
		data = toUT16("#BC(?") + "#" + toUT16("DEF");
		doTest2("Regex #0.1", data, "<&#BC(?#DEF&^");
		doTest1("Regex #0.2", "#HI)JKL", "<&#HI)JKL&^");
		data = toUT16("#BC(?") + "<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2("Regex #1", data, "<&#BC(?<DEF>GHI&^");
		doTest1("Regex #2.0", "#BC(?'DEF'GHI", "<&#BC(?'DEF'GHI&^");
		doTest1("Regex #2.1", "#BC(?'DEFGHI", "<&#BC(?'DEFGHI&^");
		data = toUT16("#BC(?(") + "<" + toUT16("DEF") + ">" + toUT16(")GHI");
		doTest2("Regex #3", data, "<&#BC(?(<DEF>)GHI&^");
		doTest1("Regex #4", "#BC(?('DEF')GHI", "<&#BC(?('DEF')GHI&^");
		doTest1("Regex #5", "#BC(?(DEF)GHI", "<&#BC(?(DEF)GHI&^");
		data = toUT16("#BC(?") + "&" + toUT16("DEF)GHI");
		doTest2("Regex #6", data, "<&#BC(?&DEF)GHI&^");
		data = toUT16("#BC(?") + "P<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2("Regex #7", data, "<&#BC(?p<DEF>GHI&^");
		data = toUT16("#BC\\k") + "<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2("Regex #8", data, "<&#BC\\k<DEF>GHI&^");
		doTest1("Regex #9", "#BC\\k'DEF'GHI", "<&#BC\\k'DEF'GHI&^");
		doTest1("Regex #10", "#BC\\k{DEF}GHI", "<&#BC\\k{DEF}GHI&^");
		data = toUT16("#BC(?") + "P=" + toUT16("DEF)GHI");
		doTest2("Regex #11", data, "<&#BC(?p=DEF)GHI&^");
		doTest1("Regex #12", "#BC\\g{DEF}GHI", "<&#BC\\g{DEF}GHI&^");
		data = toUT16("#BC\\g") + "<" + toUT16("DEF") + ">" + toUT16("GHI");
		doTest2("Regex #13", data, "<&#BC\\g<DEF>GHI&^");
		doTest1("Regex #14", "#BC\\g'DEF'GHI", "<&#BC\\g'DEF'GHI&^");
		data = toUT16("#BC(?(") + "R&" + toUT16("DEF)GHI");
		doTest2("Regex #15", data, "<&#BC(?(r&DEF)GHI&^");
		data = toUT16("#BC") + "\\Q" + toUT16("DEF") + "\\E" + toUT16("GHI");
		doTest2("Regex #16.0", data, "<&#BC\\qDEF\\eGHI&^");
		data = toUT16("#BC") + "\\Q" + toUT16("DEF");
		doTest2("Regex #16.1", data, "<&#BC\\qDEF&^");
		data = toUT16("#HI") + "\\E" + toUT16("JKL");
		doTest2("Regex #16.2", data, "<&#HI\\eJKL&^");
		env = envHebrew;

		processor = STextEngine.PROC_SQL;
		state[0] = STextEngine.STATE_INITIAL;
		doTest1("SQL #0", "abc GHI", "abc GHI");
		doTest1("SQL #1", "abc DEF   GHI", "abc DEF@   GHI");
		doTest1("SQL #2", "ABC, DEF,   GHI", "ABC@, DEF@,   GHI");
		doTest1("SQL #3", "ABC'DEF GHI' JKL,MN", "ABC@'DEF GHI'@ JKL@,MN");
		doTest1("SQL #4.0", "ABC'DEF GHI JKL", "ABC@'DEF GHI JKL");
		doTest1("SQL #4.1", "MNO PQ' RS,TUV", "MNO PQ'@ RS@,TUV");
		doTest1("SQL #5", "ABC\"DEF GHI\" JKL,MN", "ABC@\"DEF GHI\"@ JKL@,MN");
		doTest1("SQL #6", "ABC\"DEF GHI JKL", "ABC@\"DEF GHI JKL");
		doTest1("SQL #7", "ABC/*DEF GHI*/ JKL,MN", "ABC@/*DEF GHI@*/ JKL@,MN");
		doTest1("SQL #8.0", "ABC/*DEF GHI JKL", "ABC@/*DEF GHI JKL");
		doTest1("SQL #8.1", "MNO PQ*/RS,TUV", "MNO PQ@*/RS@,TUV");
		doTest1("SQL #9", "ABC--DEF GHI JKL", "ABC@--DEF GHI JKL");
		doTest1("SQL #10", "ABC--DEF--GHI,JKL", "ABC@--DEF--GHI,JKL");
		doTest1("SQL #11", "ABC'DEF '' G I' JKL,MN", "ABC@'DEF '' G I'@ JKL@,MN");
		doTest1("SQL #12", "ABC\"DEF \"\" G I\" JKL,MN", "ABC@\"DEF \"\" G I\"@ JKL@,MN");
		doTest1("SQL #13", "ABC--DEF GHI`|JKL MN", "ABC@--DEF GHI`|JKL@ MN");

		processor = STextEngine.PROC_SYSTEM_USER;
		state[0] = STextEngine.STATE_INITIAL;
		doTest1("System #1", "HOST(JACK)", "HOST@(JACK)");

		processor = STextEngine.PROC_UNDERSCORE;
		state[0] = STextEngine.STATE_INITIAL;
		doTest1("Underscore #1", "A_B_C_d_e_F_G", "A@_B@_C_d_e_F@_G");

		processor = STextEngine.PROC_URL;
		state[0] = STextEngine.STATE_INITIAL;
		doTest1("URL #1", "WWW.DOMAIN.COM/DIR1/DIR2/dir3/DIR4", "WWW@.DOMAIN@.COM@/DIR1@/DIR2/dir3/DIR4");

		processor = STextEngine.PROC_XPATH;
		state[0] = STextEngine.STATE_INITIAL;
		doTest1("Xpath #1", "abc(DEF)GHI", "abc(DEF@)GHI");
		doTest1("Xpath #2", "DEF.GHI \"A.B\":JK ", "DEF@.GHI@ \"A.B\"@:JK ");
		doTest1("Xpath #3", "DEF!GHI 'A!B'=JK ", "DEF@!GHI@ 'A!B'@=JK ");
		doTest1("Xpath #4", "DEF.GHI 'A.B :JK ", "DEF@.GHI@ 'A.B :JK ");

		processor = STextEngine.PROC_EMAIL;
		state[0] = STextEngine.STATE_INITIAL;
		assertEquals("<>.:,;@", processor.getSeparators(null, "", null));
		doTest3("DelimsEsc #1", "abc.DEF.GHI", "abc.DEF@.GHI");
		doTest3("DelimsEsc #2", "DEF.GHI (A:B);JK ", "DEF@.GHI @(A:B)@;JK ");
		doTest3("DelimsEsc #3", "DEF.GHI (A:B);JK ", "DEF@.GHI @(A:B)@;JK ");
		doTest3("DelimsEsc #4", "DEF.GHI (A:B\\):C) ;JK ", "DEF@.GHI @(A:B\\):C) @;JK ");
		doTest3("DelimsEsc #5", "DEF.GHI (A\\\\\\):C) ;JK ", "DEF@.GHI @(A\\\\\\):C) @;JK ");
		doTest3("DelimsEsc #6", "DEF.GHI (A\\\\):C ;JK ", "DEF@.GHI @(A\\\\)@:C @;JK ");
		doTest3("DelimsEsc #7", "DEF.GHI (A\\):C ;JK ", "DEF@.GHI @(A\\):C ;JK ");
	}
}
