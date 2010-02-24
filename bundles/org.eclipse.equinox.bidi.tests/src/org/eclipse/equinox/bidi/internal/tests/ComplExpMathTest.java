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

import org.eclipse.equinox.bidi.complexp.IBiDiProcessor;
import org.eclipse.equinox.bidi.complexp.IComplExpProcessor;
import org.eclipse.equinox.bidi.complexp.StringProcessor;

/**
 * Tests RTL arithmetic
 */
public class ComplExpMathTest extends ComplExpTestBase {
	
	private IComplExpProcessor processor;
	
	protected void setUp() throws Exception {
		super.setUp();
		processor = StringProcessor.getProcessor(IBiDiProcessor.RTL_ARITHMETIC);
	}

    private void verifyOneLine(String data, String resLTR, String resRTL)
    {
        String lean = toUT16(data);
        processor.assumeOrientation(IComplExpProcessor.ORIENT_LTR);
        String fullLTR = processor.leanToFullText(lean);
        assertEquals(resLTR, toPseudo(fullLTR));
        
        processor.assumeOrientation(IComplExpProcessor.ORIENT_RTL);
        String fullRTL = processor.leanToFullText(lean);
        assertEquals(resRTL, toPseudo(fullRTL));
    }
    
    public void testRTLarithmetic() {
        verifyOneLine("", "", "");
        verifyOneLine("1+abc", "<&1+abc&^", "1+abc");
        verifyOneLine("2+abc-def", "<&2+abc&-def&^", "2+abc&-def");
        verifyOneLine("a+3*bc/def", "<&a&+3*bc&/def&^", "a&+3*bc&/def");
        verifyOneLine("4+abc/def", "<&4+abc&/def&^", "4+abc&/def");
        verifyOneLine("13ABC", "<&13ABC&^", "13ABC");
        verifyOneLine("14ABC-DE", "<&14ABC-DE&^", "14ABC-DE");
        verifyOneLine("15ABC+DE", "<&15ABC+DE&^", "15ABC+DE");
        verifyOneLine("16ABC*DE", "<&16ABC*DE&^", "16ABC*DE");
        verifyOneLine("17ABC/DE", "<&17ABC/DE&^", "17ABC/DE");
    }
}
