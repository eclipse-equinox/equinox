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

import org.eclipse.equinox.bidi.complexp.*;

/**
 * Tests methods in ComplExpUtil
 */

public class ComplExpUtilTest extends TestCase {
    public static Test suite() {
        return new TestSuite(ComplExpUtilTest.class);
    }

    public ComplExpUtilTest() {
        super();
    }

    public ComplExpUtilTest(String name) {
        super(name);
    }

    static void doTest1(String data, String result)
    {
        String full = ComplExpUtil.process(Tools.toUT16(data));
        String ful2 = ComplExpUtil.process(Tools.toUT16(data), null);
        System.out.println();
        System.out.println(">>> process() text=" + data);
        Tools.verify("full", full, result);
        Tools.verify("ful2", ful2, result);
        String lean = ComplExpUtil.deprocess(full);
        Tools.verify("lean", lean, data);
    }

    static void doTest2(String data, String result)
    {
        doTest2(data, result, data);
    }

    static void doTest2(String data, String result, String resLean)
    {
        String full = ComplExpUtil.process(Tools.toUT16(data), "*");
        System.out.println();
        System.out.println(">>> process() ASTERISK text=" + data);
        Tools.verify("full", full, result);
        String lean = ComplExpUtil.deprocess(full);
        Tools.verify("lean", lean, resLean);
    }

    static void doTest3(String data, String result)
    {
        doTest3(data, result, data);
    }

    static void doTest3(String data, String result, String resLean)
    {
        String full = ComplExpUtil.processTyped(Tools.toUT16(data), IBiDiProcessor.COMMA_DELIMITED);
        System.out.println();
        System.out.println(">>> process() COMMA text=" + data);
        Tools.verify("full", full, result);
        String lean = ComplExpUtil.deprocess(full, IBiDiProcessor.COMMA_DELIMITED);
        Tools.verify("lean", lean, resLean);
    }

    static void doTest4(String data, int[] offsets, int direction, boolean affix,
                      String result)
    {
        System.out.println();
        System.out.println(">>> insertMarks() text=" + data);
        System.out.println("    offsets=" + Tools.array_display(offsets));
        System.out.print  ("    direction=" + direction);
        System.out.println("  affix=" + affix);
        String lean = Tools.toUT16(data);
        String full = ComplExpUtil.insertMarks(lean, offsets, direction, affix);
        Tools.verify("full", full, result);
    }

    public static void testComplExpUtil() {
        Tools.separ("ComplExpUtilTest");

        // Test process() and deprocess() with default delimiters
        doTest1("ABC/DEF/G", ">@ABC@/DEF@/G@^");
        // Test process() and deprocess() with specified delimiters
        doTest2("", "");
        doTest2(">@ABC@^", ">@ABC@^", "ABC");
        doTest2("abc", "abc");
        doTest2("!abc", ">@!abc@^");
        doTest2("abc!", ">@abc!@^");
        doTest2("ABC*DEF*G", ">@ABC@*DEF@*G@^");
        // Test process() and deprocess() with specified expression type
        doTest3("ABC,DEF,G", ">@ABC@,DEF@,G@^");
        doTest3("", "");
        doTest3(">@DEF@^", ">@DEF@^", "DEF");
        String str = ComplExpUtil.deprocess(Tools.toUT16("ABC,DE"), "wrong_type");
        Tools.verify("deprocess(9999)", str, "ABC,DE");
        Tools.verify("invalid type", ComplExpUtil.process("abc", "wrong_type"), "abc");
        // Test insertMarks()
        doTest4("ABCDEFG", new int[]{3, 6}, 0, false, "ABC@DEF@G");
        doTest4("ABCDEFG", new int[]{3, 6}, 0, true, ">@ABC@DEF@G@^");
        doTest4("ABCDEFG", new int[]{3, 6}, 1, false, "ABC&DEF&G");
        doTest4("ABCDEFG", new int[]{3, 6}, 1, true, "<&ABC&DEF&G&^");
        doTest4("", new int[]{3, 6}, 0, false, "");
        doTest4("", new int[]{3, 6}, 0, true, "");
        doTest4("ABCDEFG", null, 1, false, "ABCDEFG");
        doTest4("ABCDEFG", null, 1, true, "<&ABCDEFG&^");

        Tools.printStepErrorCount();
    }
}
