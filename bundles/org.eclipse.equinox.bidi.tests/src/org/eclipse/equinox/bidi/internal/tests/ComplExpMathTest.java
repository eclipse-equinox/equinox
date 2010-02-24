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
 * Tests RTL arithmetic
 */
public class ComplExpMathTest extends TestCase {
    public static Test suite() {
        return new TestSuite(ComplExpMathTest.class);
    }

    public ComplExpMathTest() {
        super();
    }

    public ComplExpMathTest(String name) {
        super(name);
    }

    static void test1(IComplExpProcessor complexp, String data,
                      String resLTR, String resRTL)
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
    }

    public static void testRTLarithmetic() {
        Tools.separ("ComplExpMathTest");
        IComplExpProcessor complexp = StringProcessor.getProcessor(IBiDiProcessor.RTL_ARITHMETIC);
        test1(complexp, "", "", "");
        test1(complexp, "1+abc", "<&1+abc&^", "1+abc");
        test1(complexp, "2+abc-def", "<&2+abc&-def&^", "2+abc&-def");
        test1(complexp, "a+3*bc/def", "<&a&+3*bc&/def&^", "a&+3*bc&/def");
        test1(complexp, "4+abc/def", "<&4+abc&/def&^", "4+abc&/def");
        test1(complexp, "13ABC", "<&13ABC&^", "13ABC");
        test1(complexp, "14ABC-DE", "<&14ABC-DE&^", "14ABC-DE");
        test1(complexp, "15ABC+DE", "<&15ABC+DE&^", "15ABC+DE");
        test1(complexp, "16ABC*DE", "<&16ABC*DE&^", "16ABC*DE");
        test1(complexp, "17ABC/DE", "<&17ABC/DE&^", "17ABC/DE");

        Tools.printStepErrorCount();
    }
}
