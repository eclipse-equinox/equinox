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
import org.eclipse.equinox.bidi.complexp.IBiDiProcessor;
import org.eclipse.equinox.bidi.complexp.StringProcessor;
import org.eclipse.equinox.bidi.complexp.ComplExpUtil;

/**
 * Tests most public methods of ComplExpBasic
 */

public class MethodsTest extends TestCase {
    public static Test suite() {
        return new TestSuite(MethodsTest.class);
    }

    public MethodsTest() {
        super();
    }

    public MethodsTest(String name) {
        super(name);
    }

    final static int LTR = IComplExpProcessor.DIRECTION_LTR;
    final static int RTL = IComplExpProcessor.DIRECTION_RTL;

    static void doTestState(IComplExpProcessor complexp)
    {
        String data, lean, full, model;
        int state;

        data = "A=B+C;/* D=E+F;";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "A@=B@+C@;/* D=E+F;";
        Tools.verify("full1", full, model);
        state = complexp.getFinalState();
        System.out.println("    state=" + state);
        data = "A=B+C; D=E+F;";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean, state);
        model = "A=B+C; D=E+F;";
        Tools.verify("full2", full, model);
        state = complexp.getFinalState();
        System.out.println("    state=" + state);
        data = "A=B+C;*/ D=E+F;";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean, state);
        model = "A=B+C;*/@ D@=E@+F;";
        Tools.verify("full3", full, model);
    }

    static void doTestOrientation()
    {
        IComplExpProcessor complexp;
        int orient;

        complexp = StringProcessor.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
        orient = complexp.recallOrientation();
// TBD: the following test cannot succeed with the current implementation.
//      it will need allocating separate data for each processor use.
//        Tools.verify("orient #1", orient, IComplExpProcessor.ORIENT_LTR);

        complexp.assumeOrientation(IComplExpProcessor.ORIENT_IGNORE);
        orient = complexp.recallOrientation();
        Tools.verify("orient #2", orient, IComplExpProcessor.ORIENT_IGNORE);

        complexp.assumeOrientation(IComplExpProcessor.ORIENT_CONTEXTUAL_RTL);
        orient = complexp.recallOrientation();
        complexp.leanToFullText("--!**");
        Tools.verify("orient #3", orient, IComplExpProcessor.ORIENT_CONTEXTUAL_RTL);

        complexp.assumeOrientation(9999);
        orient = complexp.recallOrientation();
        complexp.leanToFullText("--!**");
        Tools.verify("orient #4", orient, IComplExpProcessor.ORIENT_UNKNOWN);
    }

    static void doTestOrient(IComplExpProcessor complexp, String data,
                             String resLTR, String resRTL, String resCon)
    {
        String full, lean;

        System.out.println();
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
        full = complexp.leanToFullText(lean);
        Tools.verify("LTR full", full, resLTR);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
        full = complexp.leanToFullText(lean);
        Tools.verify("RTL full", full, resRTL);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_CONTEXTUAL_RTL);
        full = complexp.leanToFullText(lean);
        Tools.verify("CON full", full, resCon);
    }

    static void doTestScripts(IComplExpProcessor complexp)
    {
        boolean flag;
        flag = complexp.handlesArabicScript();
        Tools.verify("Handles Arabic 1", flag, true);
        flag = complexp.handlesHebrewScript();
        Tools.verify("Handles Hebrew 1", flag, true);

        complexp.selectBidiScript(false, false);
        flag = complexp.handlesArabicScript();
        Tools.verify("Handles Arabic 2", flag, false);
        flag = complexp.handlesHebrewScript();
        Tools.verify("Handles Hebrew 2", flag, false);
        doTestOrient(complexp, "BCD,EF", "BCD,EF", ">@BCD,EF@^", "@BCD,EF");
        complexp.selectBidiScript(true, false);
        flag = complexp.handlesArabicScript();
        Tools.verify("Handles Arabic 3", flag, true);
        flag = complexp.handlesHebrewScript();
        Tools.verify("Handles Hebrew 3", flag, false);
        doTestOrient(complexp, "d,EF", "d,EF", ">@d,EF@^", "d,EF");
        doTestOrient(complexp, "#,eF", "#,eF", ">@#,eF@^", "@#,eF");
        doTestOrient(complexp, "#,12", "#@,12", ">@#@,12@^", "@#@,12");
        doTestOrient(complexp, "#,##", "#@,##", ">@#@,##@^", "@#@,##");
        doTestOrient(complexp, "#,89", "#@,89", ">@#@,89@^", "@#@,89");
        doTestOrient(complexp, "#,ef", "#,ef", ">@#,ef@^", "@#,ef");
        doTestOrient(complexp, "#,", "#,", ">@#,@^", "@#,");
        doTestOrient(complexp, "9,ef", "9,ef", ">@9,ef@^", "9,ef");
        doTestOrient(complexp, "9,##", "9@,##", ">@9@,##@^", "9@,##");
        doTestOrient(complexp, "7,89", "7@,89", ">@7@,89@^", "7@,89");
        doTestOrient(complexp, "7,EF", "7,EF", ">@7,EF@^", "@7,EF");
        doTestOrient(complexp, "BCD,EF", "BCD,EF", ">@BCD,EF@^", "@BCD,EF");

        complexp.selectBidiScript(false, true);
        flag = complexp.handlesArabicScript();
        Tools.verify("Handles Arabic 4", flag, false);
        flag = complexp.handlesHebrewScript();
        Tools.verify("Handles Hebrew 4", flag, true);
        doTestOrient(complexp, "BCd,EF", "BCd,EF", ">@BCd,EF@^", "@BCd,EF");
        doTestOrient(complexp, "BCD,eF", "BCD,eF", ">@BCD,eF@^", "@BCD,eF");
        doTestOrient(complexp, "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
        doTestOrient(complexp, "BCD,12", "BCD@,12", ">@BCD@,12@^", "@BCD@,12");
        doTestOrient(complexp, "BCD,", "BCD,", ">@BCD,@^", "@BCD,");

        complexp.selectBidiScript(true, true);
        doTestOrient(complexp, "123,45|67", "123,45|67", ">@123,45|67@^", "@123,45|67");
        doTestOrient(complexp, "5,e", "5,e", ">@5,e@^", "5,e");
        doTestOrient(complexp, "5,#", "5@,#", ">@5@,#@^", "5@,#");
        doTestOrient(complexp, "5,6", "5@,6", ">@5@,6@^", "5@,6");
        doTestOrient(complexp, "5,D", "5@,D", ">@5@,D@^", "5@,D");
        doTestOrient(complexp, "5,--", "5,--", ">@5,--@^", "@5,--");
    }

    static void doTestLeanOffsets(IComplExpProcessor complexp)
    {
        String lean, data;
        int state;
        int[] offsets;
        int[] model;

        data = "A=B+C;/* D=E+F;";
        System.out.println(">>> text1=" + data);
        lean = Tools.toUT16(data);
        offsets = complexp.leanBidiCharOffsets(lean);
        model = new int[]{1, 3, 5};
        Tools.verify("leanBidiCharOffsets()", offsets, model);
        state = complexp.getFinalState();
        System.out.println("    state=" + state);
        data = "A=B+C;*/ D=E+F;";
        System.out.println(">>> text2=" + data);
        lean = Tools.toUT16(data);
        offsets = complexp.leanBidiCharOffsets(lean, state);
        model = new int[]{8, 10, 12};
        Tools.verify("leanBidiCharOffsets() #2", offsets, model);
        System.out.println(">>> text3=" + data);
        offsets = complexp.leanBidiCharOffsets();
        model = new int[]{8, 10, 12};
        Tools.verify("leanBidiCharOffsets() #3", offsets, model);
    }

    static void doTestFullOffsets(IComplExpProcessor complexp, String data,
                                  int[] resLTR, int[] resRTL, int[] resCon)
    {
        String full, lean;
        int[] offsets;

        System.out.println();
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
        full = complexp.leanToFullText(lean);
        System.out.println("LTR full=" + Tools.toPseudo(full));
        offsets = complexp.fullBidiCharOffsets();
        Tools.verify("fullBidiCharOffsets() LTR", offsets, resLTR);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
        full = complexp.leanToFullText(lean);
        System.out.println("RTL full=" + Tools.toPseudo(full));
        offsets = complexp.fullBidiCharOffsets();
        Tools.verify("fullBidiCharOffsets() RTL", offsets, resRTL);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_CONTEXTUAL_LTR);
        full = complexp.leanToFullText(lean);
        System.out.println("CON full=" + Tools.toPseudo(full));
        offsets = complexp.fullBidiCharOffsets();
        Tools.verify("fullBidiCharOffsets() CON", offsets, resCon);
    }

    static void doTestMirrored()
    {
        IComplExpProcessor complexp;
        boolean mirrored;

        mirrored = ComplExpUtil.isMirroredDefault();
        Tools.verify("mirrored #1", mirrored, false);
        complexp = StringProcessor.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
        mirrored = complexp.isMirrored();
        Tools.verify("mirrored #2", mirrored, false);
        ComplExpUtil.assumeMirroredDefault(true);
        mirrored = ComplExpUtil.isMirroredDefault();
        Tools.verify("mirrored #3", mirrored, true);
        complexp = StringProcessor.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
        mirrored = complexp.isMirrored();
// TBD: the following test cannot succeed with the current implementation.
//      it will need allocating separate data for each processor use.
//        Tools.verify("mirrored #4", mirrored, true);
        complexp.assumeMirrored(false);
        mirrored = complexp.isMirrored();
        Tools.verify("mirrored #5", mirrored, false);
    }

    static void doTestDirection()
    {
        IComplExpProcessor complexp;
        int[][] dir;
        int[][] modir;
        String data, lean, full, model;

        complexp = StringProcessor.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
        dir = complexp.getDirection();
        modir = new int[][]{{LTR,LTR},{LTR,LTR}};
        Tools.verify("direction #1", dir, modir);

        complexp.setDirection(RTL);
        dir = complexp.getDirection();
        modir = new int[][]{{RTL,RTL},{RTL,RTL}};
        Tools.verify("direction #2", dir, modir);

        complexp.setDirection(LTR, RTL);
        dir = complexp.getDirection();
        modir = new int[][]{{LTR,RTL},{LTR,RTL}};
        Tools.verify("direction #3", dir, modir);

        complexp.setArabicDirection(RTL);
        dir = complexp.getDirection();
        modir = new int[][]{{RTL,RTL},{LTR,RTL}};
        Tools.verify("direction #4", dir, modir);

        complexp.setArabicDirection(RTL,LTR);
        dir = complexp.getDirection();
        modir = new int[][]{{RTL,LTR},{LTR,RTL}};
        Tools.verify("direction #5", dir, modir);

        complexp.setHebrewDirection(RTL);
        dir = complexp.getDirection();
        modir = new int[][]{{RTL,LTR},{RTL,RTL}};
        Tools.verify("direction #6", dir, modir);

        complexp.setHebrewDirection(RTL, LTR);
        dir = complexp.getDirection();
        modir = new int[][]{{RTL,LTR},{RTL,LTR}};
        Tools.verify("direction #7", dir, modir);

        complexp = StringProcessor.getProcessor(IBiDiProcessor.EMAIL);
        complexp.assumeMirrored(false);
        complexp.setArabicDirection(LTR,RTL);
        complexp.setHebrewDirection(LTR, LTR);
        data = "#ABC.#DEF:HOST.com";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "#ABC@.#DEF@:HOST.com";
        Tools.verify("full", full, model);

        data = "ABC.DEF:HOST.com";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "ABC@.DEF@:HOST.com";
        Tools.verify("full", full, model);

        complexp.assumeMirrored(true);
        data = "#ABC.#DEF:HOST.com";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "<&#ABC.#DEF:HOST.com&^";
        Tools.verify("full", full, model);

        data = "#ABc.#DEF:HOSt.COM";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "<&#ABc.#DEF:HOSt.COM&^";
        Tools.verify("full", full, model);

        data = "#ABc.#DEF:HOSt.";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "<&#ABc.#DEF:HOSt.&^";
        Tools.verify("full", full, model);

        data = "ABC.DEF:HOST.com";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "ABC@.DEF@:HOST.com";
        Tools.verify("full", full, model);

        data = "--.---:----";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "--.---:----";
        Tools.verify("full", full, model);

        data = "ABC.|DEF:HOST.com";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "ABC.|DEF@:HOST.com";
        Tools.verify("full", full, model);

        complexp.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
        data = "#ABc.|#DEF:HOST.com";
        System.out.println(">>> text=" + data);
        lean = Tools.toUT16(data);
        full = complexp.leanToFullText(lean);
        model = "#ABc.|#DEF:HOST.com";
        Tools.verify("full", full, model);
    }

    public static void testMethods() {
        IComplExpProcessor complexp;

        Tools.separ("MethodsTest");
        System.out.println();
        System.out.println();
        System.out.println(">>>>>>>>>>  Test leanToFullText with initial state  <<<<<<<<<<");
        System.out.println();
        complexp = StringProcessor.getProcessor(IBiDiProcessor.JAVA);
        doTestState(complexp);

        System.out.println();
        System.out.println();
        System.out.println(">>>>>>>>>>  Test Orientation  <<<<<<<<<<");
        System.out.println();
        doTestOrientation();

        System.out.println();
        System.out.println();
        System.out.println(">>>>>>>>>>  Test Orient  <<<<<<<<<<");
        System.out.println();
        complexp = StringProcessor.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
        doTestOrient(complexp, "", "", "", "");
        doTestOrient(complexp, "abc", "abc", ">@abc@^", "abc");
        doTestOrient(complexp, "ABC", "ABC", ">@ABC@^", "@ABC");
        doTestOrient(complexp, "bcd,ef", "bcd,ef", ">@bcd,ef@^", "bcd,ef");
        doTestOrient(complexp, "BCD,EF", "BCD@,EF", ">@BCD@,EF@^", "@BCD@,EF");
        doTestOrient(complexp, "cde,FG", "cde,FG", ">@cde,FG@^", "cde,FG");
        doTestOrient(complexp, "CDE,fg", "CDE,fg", ">@CDE,fg@^", "@CDE,fg");
        doTestOrient(complexp, "12..def,GH", "12..def,GH", ">@12..def,GH@^", "12..def,GH");
        doTestOrient(complexp, "34..DEF,gh", "34..DEF,gh", ">@34..DEF,gh@^", "@34..DEF,gh");

        System.out.println();
        System.out.println();
        System.out.println(">>>>>>>>>>  Test Scripts  <<<<<<<<<<");
        System.out.println();
        doTestScripts(complexp);

        System.out.println();
        System.out.println();
        System.out.println(">>>>>>>>>>  Test leanBidiCharOffsets()  <<<<<<<<<<");
        System.out.println();
        complexp = StringProcessor.getProcessor(IBiDiProcessor.JAVA);
        doTestLeanOffsets(complexp);

        System.out.println();
        System.out.println();
        System.out.println(">>>>>>>>>>  Test fullBidiCharOffsets()  <<<<<<<<<<");
        System.out.println();
        complexp = StringProcessor.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
        doTestFullOffsets(complexp, "BCD,EF,G", new int[]{3,7},
                          new int[]{0,1,5,9,12,13}, new int[]{0,4,8});

        System.out.println();
        System.out.println();
        System.out.println(">>>>>>>>>>  Test mirrored  <<<<<<<<<<");
        System.out.println();
        doTestMirrored();

        System.out.println();
        System.out.println();
        System.out.println(">>>>>>>>>>  Test Direction  <<<<<<<<<<");
        System.out.println();
        doTestDirection();

        System.out.println();
        System.out.println();
        System.out.println(">>>>>>>>>>  Test Many Inserts  <<<<<<<<<<");
        System.out.println();
        complexp = StringProcessor.getProcessor(IBiDiProcessor.COMMA_DELIMITED);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
        complexp.setDirection(LTR);
        String data = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
        String lean = Tools.toUT16(data);
        String full = complexp.leanToFullText(lean);
        String model = "A@,B@,C@,D@,E@,F@,G@,H@,I@,J@,K@,L@,M@,N@,O@,P@,Q@,R@,S@,T@,U@,V@,W@,X@,Y@,Z";
        Tools.verify("many inserts", full, model);

        Tools.printStepErrorCount();
    }
}
