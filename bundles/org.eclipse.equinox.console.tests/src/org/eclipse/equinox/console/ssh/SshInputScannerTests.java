/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.ssh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.junit.Assert;
import org.junit.Test;


public class SshInputScannerTests {
	
	private static final byte ESC = 27;
	
	@Test
    public void testScan() throws Exception {
        ConsoleInputStream in = new ConsoleInputStream();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ConsoleOutputStream out = new ConsoleOutputStream(byteOut);
        SshInputScanner scanner = new SshInputScanner(in, out);
        try {
            scanner.scan((byte) 'a');
            scanner.scan((byte) 'b');
            scanner.scan((byte) 'c');
        } catch (IOException e) {
            System.out.println("Error while scanning: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        String output = byteOut.toString();
        Assert.assertTrue("Output incorrect. Expected abc, but read " + output, output.equals("abc"));
    }

    @Test
    public void testScanESC() throws Exception {
        ConsoleInputStream in = new ConsoleInputStream();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ConsoleOutputStream out = new ConsoleOutputStream(byteOut);
        SshInputScanner scanner = new SshInputScanner(in, out);

        try {
            scanner.scan((byte) 'a');
            scanner.scan((byte) ESC);
            scanner.scan((byte) 'b');
        } catch (IOException e) {
            System.out.println("Error while scanning: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        String output = byteOut.toString();
        Assert.assertTrue("Output incorrect. Expected ab, but read " + output, output.equals("ab"));
    }
}
