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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.equinox.bidi.complexp.IComplExpProcessor;
import org.eclipse.equinox.bidi.complexp.StringProcessor;
import org.eclipse.equinox.bidi.complexp.IBiDiProcessor;

/**
 * Tests all plug-in extensions
 */

public class ExtensionsTest extends TestCase {
    public static Test suite() {
        return new TestSuite(ExtensionsTest.class);
    }

    public ExtensionsTest() {
        super();
    }

    public ExtensionsTest(String name) {
        super(name);
    }

    static int state = IComplExpProcessor.STATE_NOTHING_GOING;

    static void doTest1(IComplExpProcessor complexp, String label, String data, String result)
    {
        String full;
        full = complexp.leanToFullText(Tools.toUT16(data), state);
        state = complexp.getFinalState();
        Tools.verify(label + " data = " + data, full, result);
    }

    static void doTest2(IComplExpProcessor complexp, String label, String data, String result)
    {
        String full;
        full = complexp.leanToFullText(data, state);
        state = complexp.getFinalState();
        Tools.verify(label + " data = " + data, full, result);
    }

    public static void testExtensions() {
        Tools.separ("ExtensionsTest");
        IComplExpProcessor ce;

        ce = StringProcessor.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        doTest1(ce, "Comma #1", "ab,cd, AB, CD, EFG", "ab,cd, AB@, CD@, EFG");

        ce = StringProcessor.getProcessor(IBiDiProcessor.EMAIL);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        ce.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
        doTest1(ce, "Email #1", "abc.DEF:GHI", "abc.DEF@:GHI");
        doTest1(ce, "Email #2", "DEF.GHI \"A.B\":JK ", "DEF@.GHI @\"A.B\"@:JK ");
        doTest1(ce, "Email #3", "DEF,GHI (A,B);JK ", "DEF@,GHI @(A,B)@;JK ");
        doTest1(ce, "Email #4", "DEF.GHI (A.B :JK ", "DEF@.GHI @(A.B :JK ");

        ce = StringProcessor.getProcessor(IBiDiProcessor.FILE);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        doTest1(ce, "File #1", "c:\\A\\B\\FILE.EXT", "c:\\A@\\B@\\FILE@.EXT");

        ce = StringProcessor.getProcessor(IBiDiProcessor.JAVA);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        doTest1(ce, "Java #1", "A = B + C;", "A@ = B@ + C;");
        doTest1(ce, "Java #2", "A   = B + C;", "A@   = B@ + C;");
        doTest1(ce, "Java #3", "A = \"B+C\"+D;", "A@ = \"B+C\"@+D;");
        doTest1(ce, "Java #4", "A = \"B+C+D;", "A@ = \"B+C+D;");
        doTest1(ce, "Java #5", "A = \"B\\\"C\"+D;", "A@ = \"B\\\"C\"@+D;");
        doTest1(ce, "Java #6", "A = /*B+C*/ D;", "A@ = /*B+C*/@ D;");
        doTest1(ce, "Java #7", "A = /*B+C* D;", "A@ = /*B+C* D;");
        doTest1(ce, "Java #8", "X+Y+Z */ B;  ", "X+Y+Z */@ B;  ");
        doTest1(ce, "Java #9", "A = //B+C* D;", "A@ = //B+C* D;");
        doTest1(ce, "Java #10", "A = //B+C`|D+E;", "A@ = //B+C`|D@+E;");

        ce = StringProcessor.getProcessor(IBiDiProcessor.PROPERTY);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        doTest1(ce, "Property #0", "NAME,VAL1,VAL2", "NAME,VAL1,VAL2");
        doTest1(ce, "Property #1", "NAME=VAL1,VAL2", "NAME@=VAL1,VAL2");
        doTest1(ce, "Property #2", "NAME=VAL1,VAL2=VAL3", "NAME@=VAL1,VAL2=VAL3");

        String data;
        ce = StringProcessor.getProcessor(IBiDiProcessor.REGEXP);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        data = Tools.toUT16("ABC(?")+"#"+Tools.toUT16("DEF)GHI");
        doTest2(ce, "Regex #0.0", data, "A@B@C@(?#DEF)@G@H@I");
        data = Tools.toUT16("ABC(?")+"#"+Tools.toUT16("DEF");
        doTest2(ce, "Regex #0.1", data, "A@B@C@(?#DEF");
        doTest1(ce, "Regex #0.2", "GHI)JKL", "GHI)@J@K@L");
        data = Tools.toUT16("ABC(?")+"<"+Tools.toUT16("DEF")+">"+Tools.toUT16("GHI");
        doTest2(ce, "Regex #1", data, "A@B@C@(?<DEF>@G@H@I");
        doTest1(ce, "Regex #2.0", "ABC(?'DEF'GHI", "A@B@C@(?'DEF'@G@H@I");
        doTest1(ce, "Regex #2.1", "ABC(?'DEFGHI", "A@B@C@(?'DEFGHI");
        data = Tools.toUT16("ABC(?(")+"<"+Tools.toUT16("DEF")+">"+Tools.toUT16(")GHI");
        doTest2(ce, "Regex #3", data, "A@B@C@(?(<DEF>)@G@H@I");
        doTest1(ce, "Regex #4", "ABC(?('DEF')GHI", "A@B@C@(?('DEF')@G@H@I");
        doTest1(ce, "Regex #5", "ABC(?(DEF)GHI", "A@B@C@(?(DEF)@G@H@I");
        data = Tools.toUT16("ABC(?")+"&"+Tools.toUT16("DEF)GHI");
        doTest2(ce, "Regex #6", data, "A@B@C@(?&DEF)@G@H@I");
        data = Tools.toUT16("ABC(?")+"P<"+Tools.toUT16("DEF")+">"+Tools.toUT16("GHI");
        doTest2(ce, "Regex #7", data, "A@B@C(?p<DEF>@G@H@I");
        data = Tools.toUT16("ABC\\k")+"<"+Tools.toUT16("DEF")+">"+Tools.toUT16("GHI");
        doTest2(ce, "Regex #8", data, "A@B@C\\k<DEF>@G@H@I");
        doTest1(ce, "Regex #9", "ABC\\k'DEF'GHI", "A@B@C\\k'DEF'@G@H@I");
        doTest1(ce, "Regex #10", "ABC\\k{DEF}GHI", "A@B@C\\k{DEF}@G@H@I");
        data = Tools.toUT16("ABC(?")+"P="+Tools.toUT16("DEF)GHI");
        doTest2(ce, "Regex #11", data, "A@B@C(?p=DEF)@G@H@I");
        doTest1(ce, "Regex #12", "ABC\\g{DEF}GHI", "A@B@C\\g{DEF}@G@H@I");
        data = Tools.toUT16("ABC\\g")+"<"+Tools.toUT16("DEF")+">"+Tools.toUT16("GHI");
        doTest2(ce, "Regex #13", data, "A@B@C\\g<DEF>@G@H@I");
        doTest1(ce, "Regex #14", "ABC\\g'DEF'GHI", "A@B@C\\g'DEF'@G@H@I");
        data = Tools.toUT16("ABC(?(")+"R&"+Tools.toUT16("DEF)GHI");
        doTest2(ce, "Regex #15", data, "A@B@C(?(r&DEF)@G@H@I");
        data = Tools.toUT16("ABC")+"\\Q"+Tools.toUT16("DEF")+"\\E"+Tools.toUT16("GHI");
        doTest2(ce, "Regex #16.0", data, "A@B@C\\qDEF\\eG@H@I");
        data = Tools.toUT16("ABC")+"\\Q"+Tools.toUT16("DEF");
        doTest2(ce, "Regex #16.1", data, "A@B@C\\qDEF");
        data = Tools.toUT16("GHI")+"\\E"+Tools.toUT16("JKL");
        doTest2(ce, "Regex #16.2", data, "GHI\\eJ@K@L");
        doTest1(ce, "Regex #17.0", "abc[d-h]ijk", "abc[d-h]ijk");
        doTest1(ce, "Regex #17.1", "aBc[d-H]iJk", "aBc[d-H]iJk");
        doTest1(ce, "Regex #17.2", "aB*[!-H]iJ2", "aB*[!-@H]iJ@2");
        doTest1(ce, "Regex #17.3", "aB*[1-2]J3", "aB*[@1-2]J@3");
        doTest1(ce, "Regex #17.4", "aB*[5-6]J3", "aB*[@5-@6]@J@3");
        doTest1(ce, "Regex #17.5", "a*[5-6]J3", "a*[5-@6]@J@3");
        doTest1(ce, "Regex #17.6", "aB*123", "aB*@123");
        doTest1(ce, "Regex #17.7", "aB*567", "aB*@567");

        ce = StringProcessor.getProcessor(IBiDiProcessor.SQL);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        doTest1(ce, "SQL #0", "abc GHI", "abc GHI");
        doTest1(ce, "SQL #1", "abc DEF   GHI", "abc DEF@   GHI");
        doTest1(ce, "SQL #2", "ABC, DEF,   GHI", "ABC@, DEF@,   GHI");
        doTest1(ce, "SQL #3", "ABC'DEF GHI' JKL,MN", "ABC@'DEF GHI'@ JKL@,MN");
        doTest1(ce, "SQL #4.0", "ABC'DEF GHI JKL", "ABC@'DEF GHI JKL");
        doTest1(ce, "SQL #4.1", "MNO PQ' RS,TUV", "MNO PQ'@ RS@,TUV");
        doTest1(ce, "SQL #5", "ABC\"DEF GHI\" JKL,MN", "ABC@\"DEF GHI\"@ JKL@,MN");
        doTest1(ce, "SQL #6", "ABC\"DEF GHI JKL", "ABC@\"DEF GHI JKL");
        doTest1(ce, "SQL #7", "ABC/*DEF GHI*/ JKL,MN", "ABC@/*DEF GHI@*/ JKL@,MN");
        doTest1(ce, "SQL #8.0", "ABC/*DEF GHI JKL", "ABC@/*DEF GHI JKL");
        doTest1(ce, "SQL #8.1", "MNO PQ*/RS,TUV", "MNO PQ@*/RS@,TUV");
        doTest1(ce, "SQL #9", "ABC--DEF GHI JKL", "ABC@--DEF GHI JKL");
        doTest1(ce, "SQL #10", "ABC--DEF--GHI,JKL", "ABC@--DEF--GHI,JKL");
        doTest1(ce, "SQL #11", "ABC'DEF '' G I' JKL,MN", "ABC@'DEF '' G I'@ JKL@,MN");
        doTest1(ce, "SQL #12", "ABC\"DEF \"\" G I\" JKL,MN", "ABC@\"DEF \"\" G I\"@ JKL@,MN");
        doTest1(ce, "SQL #13", "ABC--DEF GHI`|JKL MN", "ABC@--DEF GHI`|JKL@ MN");

        ce = StringProcessor.getProcessor(IBiDiProcessor.SYSTEM_USER);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        doTest1(ce, "System #1", "HOST(JACK)", "HOST@(JACK)");

        ce = StringProcessor.getProcessor(IBiDiProcessor.UNDERSCORE);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        doTest1(ce, "Underscore #1", "A_B_C_d_e_F_G", "A@_B@_C_d_e_F@_G");

        ce = StringProcessor.getProcessor(IBiDiProcessor.URL);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        doTest1(ce, "URL #1", "WWW.DOMAIN.COM/DIR1/DIR2/dir3/DIR4", "WWW@.DOMAIN@.COM@/DIR1@/DIR2/dir3/DIR4");

        ce = StringProcessor.getProcessor(IBiDiProcessor.XPATH);
        assertNotNull(ce);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        doTest1(ce, "Xpath #1", "abc(DEF)GHI", "abc(DEF@)GHI");
        doTest1(ce, "Xpath #2", "DEF.GHI \"A.B\":JK ", "DEF@.GHI@ \"A.B\"@:JK ");
        doTest1(ce, "Xpath #3", "DEF!GHI 'A!B'=JK ", "DEF@!GHI@ 'A!B'@=JK ");
        doTest1(ce, "Xpath #4", "DEF.GHI 'A.B :JK ", "DEF@.GHI@ 'A.B :JK ");

        ce = StringProcessor.getProcessor(IBiDiProcessor.EMAIL);
        state = IComplExpProcessor.STATE_NOTHING_GOING;
        String operators = ce.getOperators();
        assertEquals("<>.:,;@", operators);
        ce.setOperators("+-*/");
        doTest1(ce, "DelimsEsc #1", "abc+DEF-GHI", "abc+DEF@-GHI");
        doTest1(ce, "DelimsEsc #2", "DEF-GHI (A*B)/JK ", "DEF@-GHI @(A*B)@/JK ");
        doTest1(ce, "DelimsEsc #3", "DEF-GHI (A*B)/JK ", "DEF@-GHI @(A*B)@/JK ");
        doTest1(ce, "DelimsEsc #4", "DEF-GHI (A*B\\)*C) /JK ", "DEF@-GHI @(A*B\\)*C) @/JK ");
        doTest1(ce, "DelimsEsc #5", "DEF-GHI (A\\\\\\)*C) /JK ", "DEF@-GHI @(A\\\\\\)*C) @/JK ");
        doTest1(ce, "DelimsEsc #6", "DEF-GHI (A\\\\)*C /JK ", "DEF@-GHI @(A\\\\)@*C @/JK ");
        doTest1(ce, "DelimsEsc #7", "DEF-GHI (A\\)*C /JK ", "DEF@-GHI @(A\\)*C /JK ");


        Tools.printStepErrorCount();
    }
}
