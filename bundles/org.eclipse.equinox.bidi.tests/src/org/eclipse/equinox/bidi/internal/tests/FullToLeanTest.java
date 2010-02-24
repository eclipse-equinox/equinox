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
 * Tests fullToLean method
 */

public class FullToLeanTest extends TestCase {
    public static Test suite() {
        return new TestSuite(FullToLeanTest.class);
    }

    public FullToLeanTest() {
        super();
    }

    public FullToLeanTest(String name) {
        super(name);
    }

    static int[] getpos(IComplExpProcessor complexp, boolean leanToFull)
    {
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

    static void doTest1(IComplExpProcessor complexp, String data,
                      String leanLTR, String fullLTR,
                      int[] l2fPosLTR, int[] f2lPosLTR,
                      String leanRTL, String fullRTL,
                      int[] l2fPosRTL, int[] f2lPosRTL)
    {
        String text, full, lean;
        int[] pos;

        System.out.println();
        System.out.println(">>> text=" + data);
        text = Tools.toUT16(data);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
        lean = complexp.fullToLeanText(text);
        Tools.verify("LTR lean", lean, leanLTR);
        full = complexp.leanToFullText(lean);
        Tools.verify("LTR full", full, fullLTR);
        pos = getpos(complexp, true);
        Tools.verify("leanToFullPos()", pos, l2fPosLTR);
        pos = getpos(complexp, false);
        Tools.verify("fullToLeanPos()", pos, f2lPosLTR);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
        lean = complexp.fullToLeanText(text);
        Tools.verify("RTL lean", lean, leanRTL);
        full = complexp.leanToFullText(lean);
        Tools.verify("RTL full", full, fullRTL);
        pos = getpos(complexp, true);
        Tools.verify("leanToFullPos()", pos, l2fPosRTL);
        pos = getpos(complexp, false);
        Tools.verify("fullToLeanPos()", pos, f2lPosRTL);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
        lean = complexp.fullToLeanText(text);
    }

    static void doTest2()
    {
        IComplExpProcessor complexp;
        String text, data, full, lean, model;
        int state, state2, state3;

        complexp = StringProcessor.getProcessor(IBiDiProcessor.SQL);
        complexp.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
        System.out.println();
        data = "update \"AB_CDE\" set \"COL1\"@='01', \"COL2\"@='02' /* GH IJK";
        System.out.println(">>> text=" + data);
        text = Tools.toUT16(data);
        lean = complexp.fullToLeanText(text);
        state = complexp.getFinalState();
        model = "update \"AB_CDE\" set \"COL1\"='01', \"COL2\"='02' /* GH IJK";
        Tools.verify("LTR lean", lean, model);
        full = complexp.leanToFullText(lean);
        Tools.verify("LTR full", full, data);
        Tools.verify("state from leanToFullText", state, complexp.getFinalState());
        data = "THIS IS A COMMENT LINE";
        System.out.println(">>> text=" + data);
        text = Tools.toUT16(data);
        lean = complexp.fullToLeanText(text, state);
        state2 = complexp.getFinalState();
        model = "THIS IS A COMMENT LINE";
        Tools.verify("LTR lean", lean, model);
        full = complexp.leanToFullText(lean, state);
        Tools.verify("LTR full", full, data);
        Tools.verify("state from leanToFullText", state2, complexp.getFinalState());
        data = "SOME MORE */ where \"COL3\"@=123";
        System.out.println(">>> text=" + data);
        text = Tools.toUT16(data);
        lean = complexp.fullToLeanText(text, state2);
        state3 = complexp.getFinalState();
        model = "SOME MORE */ where \"COL3\"=123";
        Tools.verify("LTR lean", lean, model);
        full = complexp.leanToFullText(lean, state2);
        Tools.verify("LTR full", full, data);
        Tools.verify("state from leanToFullText", state3, complexp.getFinalState());
    }

    public static void testFullToLean() {
        Tools.separ("FullToLeanTest");
        IComplExpProcessor complexp = StringProcessor.getProcessor(IBiDiProcessor.COMMA_DELIMITED);

        doTest1(complexp, "",
              "", "",
              new int[]{0,4,7,10,30}, new int[]{0,0,0,0,0},
              "", "",
              new int[]{0,4,7,10,30}, new int[]{0,0,0,0,0});
        doTest1(complexp, "1.abc",
              "1.abc", "1.abc",
              new int[]{0,4,7,10,30}, new int[]{0,4,5,5,5},
              "1.abc", ">@1.abc@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,5,5});
        doTest1(complexp, "2.abc,def",
              "2.abc,def", "2.abc,def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,9,9},
              "2.abc,def", ">@2.abc,def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,9});
        doTest1(complexp, "@a.3.bc,def",
              "a.3.bc,def", "a.3.bc,def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,10,10},
              "a.3.bc,def", ">@a.3.bc,def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,10});
        doTest1(complexp, "@@a.4.bc,def",
              "a.4.bc,def", "a.4.bc,def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,10,10},
              "a.4.bc,def", ">@a.4.bc,def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,10});
        doTest1(complexp, "@5.abc,def",
              "5.abc,def", "5.abc,def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,9,9},
              "5.abc,def", ">@5.abc,def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,9});
        doTest1(complexp, "@@6.abc,def",
              "6.abc,def", "6.abc,def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,9,9},
              "6.abc,def", ">@6.abc,def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,9});
        doTest1(complexp, "7abc,@def",
              "7abc,@def", "7abc,@def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,9,9},
              "7abc,@def", ">@7abc,@def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,9});
        doTest1(complexp, "8abc,@@def",
              "8abc,@def", "8abc,@def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,9,9},
              "8abc,@def", ">@8abc,@def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,9});
        doTest1(complexp, "9abc,def@",
              "9abc,def", "9abc,def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,8,8},
              "9abc,def", ">@9abc,def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,8});
        doTest1(complexp, "10abc,def@@",
              "10abc,def", "10abc,def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,9,9},
              "10abc,def", ">@10abc,def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,9});
        doTest1(complexp, "@a.11.bc,@def@",
              "a.11.bc,@def", "a.11.bc,@def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,10,12},
              "a.11.bc,@def", ">@a.11.bc,@def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,12});
        doTest1(complexp, "@@a.12.bc,@@def@@",
              "a.12.bc,@def", "a.12.bc,@def",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,10,12},
              "a.12.bc,@def", ">@a.12.bc,@def@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,12});
        doTest1(complexp, "13ABC",
              "13ABC", "13ABC",
              new int[]{0,4,7,10,30}, new int[]{0,4,5,5,5},
              "13ABC", ">@13ABC@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,5,5});
        doTest1(complexp, "14ABC,DE",
              "14ABC,DE", "14ABC@,DE",
              new int[]{0,4,8,11,31}, new int[]{0,4,6,8,8},
              "14ABC,DE", ">@14ABC@,DE@^",
              new int[]{2,6,10,13,33}, new int[]{0,2,5,7,8});
        doTest1(complexp, "15ABC@,DE",
              "15ABC,DE", "15ABC@,DE",
              new int[]{0,4,8,11,31}, new int[]{0,4,6,8,8},
              "15ABC,DE", ">@15ABC@,DE@^",
              new int[]{2,6,10,13,33}, new int[]{0,2,5,7,8});
        doTest1(complexp, "16ABC@@,DE",
              "16ABC,DE", "16ABC@,DE",
              new int[]{0,4,8,11,31}, new int[]{0,4,6,8,8},
              "16ABC,DE", ">@16ABC@,DE@^",
              new int[]{2,6,10,13,33}, new int[]{0,2,5,7,8});
        doTest1(complexp, "17ABC,@@DE",
              "17ABC,@DE", "17ABC,@DE",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,9,9},
              "17ABC,@DE", ">@17ABC,@DE@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,9});
        doTest1(complexp, "18ABC,DE,FGH",
              "18ABC,DE,FGH", "18ABC@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,6,8,12},
              "18ABC,DE,FGH", ">@18ABC@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,12});
        doTest1(complexp, "19ABC@,DE@,FGH",
              "19ABC,DE,FGH", "19ABC@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,6,8,12},
              "19ABC,DE,FGH", ">@19ABC@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,12});
        doTest1(complexp, "20ABC,@DE,@FGH",
              "20ABC,@DE,@FGH", "20ABC,@DE,@FGH",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,10,14},
              "20ABC,@DE,@FGH", ">@20ABC,@DE,@FGH@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,14});
        doTest1(complexp, "21ABC@@,DE@@,FGH",
              "21ABC,DE,FGH", "21ABC@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,6,8,12},
              "21ABC,DE,FGH", ">@21ABC@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,12});
        doTest1(complexp, "22ABC,@@DE,@@FGH",
              "22ABC,@DE,@FGH", "22ABC,@DE,@FGH",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,10,14},
              "22ABC,@DE,@FGH", ">@22ABC,@DE,@FGH@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,14});
        doTest1(complexp, ">@23abc@^",
              "23abc", "23abc",
              new int[]{0,4,7,10,30}, new int[]{0,4,5,5,5},
              "23abc", ">@23abc@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,5,5});
        doTest1(complexp, "24abc@^",
              "24abc", "24abc",
              new int[]{0,4,7,10,30}, new int[]{0,4,5,5,5},
              "24abc", ">@24abc@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,5,5});
        doTest1(complexp, ">@25abc",
              "25abc", "25abc",
              new int[]{0,4,7,10,30}, new int[]{0,4,5,5,5},
              "25abc", ">@25abc@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,5,5});
        doTest1(complexp, "26AB,CD@EF,GHI",
              "26AB,CD@EF,GHI", "26AB@,CD@EF@,GHI",
              new int[]{0,5,8,12,32}, new int[]{0,4,6,9,14},
              "26AB,CD@EF,GHI", ">@26AB@,CD@EF@,GHI@^",
              new int[]{2,7,10,14,34}, new int[]{0,2,4,7,14});
        doTest1(complexp, "27AB,CD@123ef,GHI",
              "27AB,CD@123ef,GHI", "27AB@,CD@123ef,GHI",
              new int[]{0,5,8,11,31}, new int[]{0,4,6,9,17},
              "27AB,CD@123ef,GHI", ">@27AB@,CD@123ef,GHI@^",
              new int[]{2,7,10,13,33}, new int[]{0,2,4,7,17});
        doTest1(complexp, ">28ABC@,DE@,FGH^",
              "28ABC,DE,FGH", "28ABC@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,6,8,12},
              "28ABC,DE,FGH", ">@28ABC@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,12});
        doTest1(complexp, ">>29ABC@,DE@,FGH^^",
              "29ABC,DE,FGH", "29ABC@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,6,8,12},
              "29ABC,DE,FGH", ">@29ABC@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,12});
        doTest1(complexp, ">30AB>C^@,DE@,FGH^",
              "30AB>C^,DE,FGH", "30AB>C^@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,7,9,14},
              "30AB>C^,DE,FGH", ">@30AB>C^@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,14});
        doTest1(complexp, ">31AB>C@,DE@,FGH^^",
              "31AB>C,DE,FGH", "31AB>C@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,6,9,13},
              "31AB>C,DE,FGH", ">@31AB>C@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,13});
        doTest1(complexp, ">@32ABC@,DE@,FGH@^",
              "32ABC,DE,FGH", "32ABC@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,6,8,12},
              "32ABC,DE,FGH", ">@32ABC@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,12});
        doTest1(complexp, "@33ABC@,DE@,FGH@^",
              "33ABC,DE,FGH", "33ABC@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,6,8,12},
              "33ABC,DE,FGH", ">@33ABC@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,12});
        doTest1(complexp, ">@34ABC@,DE@,FGH@",
              "34ABC,DE,FGH", "34ABC@,DE@,FGH",
              new int[]{0,4,8,12,32}, new int[]{0,4,6,8,12},
              "34ABC,DE,FGH", ">@34ABC@,DE@,FGH@^",
              new int[]{2,6,10,14,34}, new int[]{0,2,5,7,12});
        doTest1(complexp, "35ABC@@DE@@@GH@",
              "35ABC@DE@GH", "35ABC@DE@GH",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,10,11},
              "35ABC@DE@GH", ">@35ABC@DE@GH@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,11});
        doTest1(complexp, "36ABC@@DE@@@@@@",
              "36ABC@DE", "36ABC@DE",
              new int[]{0,4,7,10,30}, new int[]{0,4,7,8,8},
              "36ABC@DE", ">@36ABC@DE@^",
              new int[]{2,6,9,12,32}, new int[]{0,2,5,8,8});
        doTest1(complexp, ">>>@@@@@^^^",
              "", "",
              new int[]{0,4,7,10,30}, new int[]{0,0,0,0,0},
              "", "",
              new int[]{0,4,7,10,30}, new int[]{0,0,0,0,0});

        // test fullToLeanText with initial state
        doTest2();

        Tools.printStepErrorCount();
    }
}
