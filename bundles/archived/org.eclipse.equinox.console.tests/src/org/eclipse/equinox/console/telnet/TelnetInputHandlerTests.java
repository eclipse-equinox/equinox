/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Lazar Kirchev, SAP AG - initial contribution
 ******************************************************************************/

package org.eclipse.equinox.console.telnet;

import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.junit.Assert;
import org.junit.Test;

import static org.easymock.EasyMock.*;

import java.io.ByteArrayOutputStream;
import java.io.StringBufferInputStream;

public class TelnetInputHandlerTests {

    private static final long WAIT_TIME = 10000;

    @Test
    public void testHandler() throws Exception {
        StringBufferInputStream input = new StringBufferInputStream("abcde");
        ConsoleInputStream in = new ConsoleInputStream();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ConsoleOutputStream out = new ConsoleOutputStream(byteOut);
        Callback callback = createMock(Callback.class);
        TelnetInputHandler handler = new TelnetInputHandler(input, in, out, callback);
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
