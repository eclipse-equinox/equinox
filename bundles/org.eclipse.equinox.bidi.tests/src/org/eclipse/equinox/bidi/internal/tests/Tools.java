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

/**
 * Tools: common methods and their test.
 */

public class Tools extends TestCase {
    public static Test suite() {
        return new TestSuite(Tools.class);
    }

    public Tools() {
        super();
    }

    public Tools(String name) {
        super(name);
    }

    static final char LRM = 0x200E;
    static final char RLM = 0x200F;
    static final char LRE = 0x202A;
    static final char RLE = 0x202B;
    static final char PDF = 0x202C;

    static int errCnt;
    static int errCntTotal;
    static boolean isTestingTheTools;

    static String toPseudo(String text)
    {
        char[] chars = text.toCharArray();
        int len = chars.length;
        char c;
        int i;
        for (i = 0; i < len; i++) {
            c = chars[i];
            if (c >= 'A' && c <= 'Z') {
                chars[i] = (char)(c + 'a' - 'A');
            }
            else if (c >= 0x05D0 && c < 0x05EA) {
                chars[i] = (char)(c + 'A' - 0x05D0);
            }
            else if (c == 0x05EA) {
                chars[i] = '~';
            }
            else if (c == 0x0644) {
                chars[i] = '#';
            }
            else if (c >= 0x0665 && c <= 0x0669) {
                chars[i] = (char)(c + '5' - 0x0665);
            }
            else if (c == LRM) {
                chars[i] = '@';
            }
            else if (c == RLM) {
                chars[i] = '&';
            }
            else if (c == LRE) {
                chars[i] = '>';
            }
            else if (c == RLE) {
                chars[i] = '<';
            }
            else if (c == PDF) {
                chars[i] = '^';
            }
            else if (c == '\n') {
                chars[i] = '|';
            }
            else if (c == '\r') {
                chars[i] = '`';
            }
        }
        return new String(chars);
    }

    static String toUT16(String text)
    {
        char[] chars = text.toCharArray();
        int len = chars.length;
        char c;
        int i;
        for (i = 0; i < len; i++) {
            c = chars[i];
            if (c >= '5' && c <= '9') {
                chars[i] = (char)(0x0665 + c - '5');
            }
            if (c >= 'A' && c <= 'Z') {
                chars[i] = (char)(0x05D0 + c - 'A');
            }
            else if (c == '~') {
                chars[i] = (char)(0x05EA);
            }
            else if (c == '#') {
                chars[i] = (char)(0x0644);
            }
            else if (c == '@') {
                chars[i] = LRM;
            }
            else if (c == '&') {
                chars[i] = RLM;
            }
            else if (c == '>') {
                chars[i] = LRE;
            }
            else if (c == '<') {
                chars[i] = RLE;
            }
            else if (c == '^') {
                chars[i] = PDF;
            }
            else if (c == '|') {
                chars[i] = '\n';
            }
            else if (c == '`') {
                chars[i] = '\r';
            }
        }
        return new String(chars);
    }

    static boolean arrays_equal(int[] one, int[] two)
    {
        int len = one.length;
        if (len != two.length) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (one[i] != two[i]) {
                return false;
            }
        }
        return true;
    }

    static boolean arrays2_equal(int[][] one, int[][] two)
    {
        int dim1, dim2;
        dim1 = one.length;
        if (dim1 != two.length) {
            return false;
        }
        for (int i = 0; i < dim1; i++) {
            dim2 = one[i].length;
            if (dim2 != two[i].length) {
                return false;
            }
            for (int j = 0; j < dim2; j++) {
                if (one[i][j] != two[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    static String array_display(int[] array)
    {
        if (array == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer(50);
        int len = array.length;
        for (int i = 0; i < len; i++) {
            sb.append(array[i]);
            sb.append(' ');
        }
        return sb.toString();
    }

    static String array2_display(int[][] array)
    {
        int dim1, dim2;
        if (array == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer(50);
        dim1 = array.length;
        for (int i = 0; i < dim1; i++) {
            dim2 = array[i].length;
            for (int j = 0; j < dim2; j++) {
                sb.append(array[i][j]);
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    static boolean verify(String msg, int result, int expected)
    {
        System.out.println("Testing: " + msg);
        System.out.println("    result   = " + result);
        if (result != expected) {
            System.out.println("!!! ******* ERROR ******* !!!");
            System.out.println("    result   = " + result);
            System.out.println("    expected = " + expected);
            errCnt++;
            if (!isTestingTheTools)
                assertEquals(expected, result);
            return false;
        }
        return true;
    }

    static boolean verify(String msg, boolean result, boolean expected)
    {
        System.out.println("Testing: " + msg);
        System.out.println("    result   = " + result);
        if (result != expected) {
            System.out.println("!!! ******* ERROR ******* !!!");
            System.out.println("    result   = " + result);
            System.out.println("    expected = " + expected);
            errCnt++;
            if (!isTestingTheTools)
                assertEquals(expected, result);
            return false;
        }
        return true;
    }

    static boolean verify(String msg, String result, String expected)
    {
        System.out.println("Testing: " + msg);
        String presult = Tools.toPseudo(result);
        System.out.println("    result   = " + presult);
        if (!presult.equals(expected)) {
            System.out.println("!!! ******* ERROR ******* !!!");
            System.out.println("    result   = " + presult);
            System.out.println("    expected = " + expected);
            errCnt++;
            if (!isTestingTheTools)
                assertEquals(expected, presult);
            return false;
        }
        return true;
    }

    static boolean verify(String msg, int[] result, int[] expected)
    {
        System.out.println("Testing: " + msg);
        System.out.println("    result   = " + array_display(result));
        if (!arrays_equal(result, expected)) {
            System.out.println("!!! ******* ERROR ******* !!!");
            System.out.println("    result   = " + array_display(result));
            System.out.println("    expected = " + array_display(expected));
            errCnt++;
            if (!isTestingTheTools)
                assertEquals(expected, result);
            return false;
        }
        return true;
    }

    static boolean verify(String msg, int[][] result, int[][] expected)
    {
        System.out.println("Testing: " + msg);
        System.out.println("    result   = " + array2_display(result));
        if (!arrays2_equal(result, expected)) {
            System.out.println("!!! ******* ERROR ******* !!!");
            System.out.println("    result   = " + array2_display(result));
            System.out.println("    expected = " + array2_display(expected));
            errCnt++;
            if (!isTestingTheTools)
                assertEquals(expected, result);
            return false;
        }
        return true;
    }

    static void printStepErrorCount()
    {
        System.out.println();
        System.out.println();
        if (errCnt == 0) {
            System.out.println("No errors were found.");
        } else {
            System.out.println("******* " + errCnt + " errors were found !!!!!!!");
            errCntTotal += errCnt;
            errCnt = 0;
        }
        System.out.println();
    }

    static void printTotalErrorCount()
    {
        errCntTotal += errCnt;
        errCnt = 0;
        System.out.println();
        System.out.println();
        if (errCntTotal == 0) {
            System.out.println("No total errors were found.");
        } else {
            System.out.println("******* " + errCntTotal + " total errors were found !!!!!!!");
            errCntTotal = 0;
        }
        System.out.println();
    }

    static void separ(String text)
    {
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("*======================================*");
        System.out.println("*                                      *");
        System.out.println("               " + text);
        System.out.println("*                                      *");
        System.out.println("*======================================*");
        System.out.println();
        System.out.println();
    }
    
    public static void testTools() {
        separ("Test Tools");
        // The following statements are meant to test the methods within
        // this file.
        isTestingTheTools = true;
        String data = "56789ABCDEFGHIJKLMNOPQRSTUVWXYZ~#@&><^|`";
        String text = toUT16(data);
        String dat2 = toPseudo(text);
        assertEquals(data, dat2);

        text = toPseudo(data);
        assertEquals("56789abcdefghijklmnopqrstuvwxyz~#@&><^|`", text);

        int[] arA = new int[]{1,2};
        int[] arB = new int[]{3,4,5};
        assertFalse(arrays_equal(arA, arB));

        assertTrue(arrays_equal(arA, arA));

        arB = new int[]{3,4};
        assertFalse(arrays_equal(arA, arB));

        int[][] ar2A = new int[][]{{1},{1,2},{1,2,3}};
        int[][] ar2B = new int[][]{{1},{1,2}};
        assertTrue(arrays2_equal(ar2A, ar2A));

        assertFalse(arrays2_equal(ar2A, ar2B));

        ar2B = new int[][]{{1},{1,2},{1,2,3,4}};
        assertFalse(arrays2_equal(ar2A, ar2B));

        ar2B = new int[][]{{1},{1,2},{1,2,4}};
        assertFalse(arrays2_equal(ar2A, ar2B));

        text = array_display(null);
        assertEquals("null", text);

        text = array2_display(null);
        assertEquals("null", text);

        assertTrue(verify("An error is NOT expected below", 0, 0));

        assertFalse(verify("An error is expected below", 0, 1));

        assertTrue(verify("An error is NOT expected below", true, true));

        assertFalse(verify("An error is expected below", true, false));

        assertTrue(verify("An error is NOT expected below", "abc", "abc"));

        assertFalse(verify("An error is expected below", "abc", "def"));

        assertTrue(verify("An error is NOT expected below", new int[]{0,1}, new int[]{0,1}));

        assertFalse(verify("An error is expected below", new int[]{0,1}, new int[]{2,3}));

        assertTrue(verify("An error is NOT expected below", new int[][]{{0,1},{2,3}}, new int[][]{{0,1},{2,3}}));

        assertFalse(verify("An error is expected below", new int[][]{{0,1},{2,3}}, new int[][]{{4,5},{6,7}}));
        System.out.println();
        System.out.println();
        System.out.println("....... The next line should show 5 errors");
        assertEquals(5, errCnt);
        printStepErrorCount();
        errCnt = 9;
        System.out.println();
        System.out.println();
        System.out.println("....... The next line should show 9 errors");
        assertEquals(9, errCnt);
        printStepErrorCount();
        System.out.println();
        System.out.println();
        System.out.println("....... The next line should show 14 total errors");
        assertEquals(14, errCntTotal);
        printTotalErrorCount();
        errCnt = 0;
        errCntTotal = 0;
        System.out.println();
        System.out.println();
        System.out.println("....... Reset error counters to zero");
        printStepErrorCount();
        printTotalErrorCount();
        isTestingTheTools = false;
    }
}
