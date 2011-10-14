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
import java.io.StringBufferInputStream;

import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.eclipse.equinox.console.ssh.SshInputHandler;
import org.junit.Assert;
import org.junit.Test;


public class SshInputHandlerTests {
	
	private static final long WAIT_TIME = 10000;

    @Test
    public void testHandler() throws Exception {
        StringBufferInputStream input = new StringBufferInputStream("abcde");
        ConsoleInputStream in = new ConsoleInputStream();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ConsoleOutputStream out = new ConsoleOutputStream(byteOut);
        SshInputHandler handler = new SshInputHandler(input, in, out);
        handler.start();

        // wait for the accept thread to start execution
        try {
            Thread.sleep(WAIT_TIME);
        } catch (InterruptedException ie) {
            // do nothing
        }

        String res = byteOut.toString();
        Assert.assertTrue("Wrong input. Expected abcde, read " + res, res.equals("abcde"));
    }
}
