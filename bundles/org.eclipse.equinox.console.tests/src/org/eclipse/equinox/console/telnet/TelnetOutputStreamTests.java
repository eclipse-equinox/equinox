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

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

public class TelnetOutputStreamTests {

	@Test
    public void testAutoSend() throws Exception {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        TelnetOutputStream out = new TelnetOutputStream(byteOut);
        out.autoSend();
        out.flush();
        byte[] message = byteOut.toByteArray();

        Assert.assertNotNull("Auto message not sent", message);
        Assert.assertFalse("Auto message not sent", message.length == 0);
        Assert.assertTrue("Error sending auto message. Expected length: " + TelnetOutputStream.autoMessage.length + ", actual length: "
            + message.length, message.length == TelnetOutputStream.autoMessage.length);

        for (int i = 0; i < message.length; i++) {
            Assert.assertEquals("Wrong char in auto message. Position: " + i + ", expected: " + TelnetOutputStream.autoMessage[i] + ", read: "
                + message[i], TelnetOutputStream.autoMessage[i], message[i]);
        }
    }
}
