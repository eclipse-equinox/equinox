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
import org.eclipse.equinox.console.common.KEYS;
import org.eclipse.equinox.console.common.terminal.ANSITerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.SCOTerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.TerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.VT100TerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.VT220TerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.VT320TerminalTypeMappings;
import org.junit.Assert;
import org.junit.Test;

import static org.easymock.EasyMock.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;

public class TelnetInputScannerTests {

    private static final int IAC = 255;

    private static final int DO = 253;
    
    private static final int DO_NOT = 254;

    private static final int TTYPE = 24;

    private static final int WILL = 251;
    
    private static final int WILL_NOT = 252;

    private static final int SB = 250;

    private static final int SE = 240;
    
    private static final int EL = 248;

    private static final int SEND = 1;

    private static final int IS = 0;

    protected static final byte ESC = 27;

    @Test
    public void testScan() throws Exception {
        ConsoleInputStream in = new ConsoleInputStream();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ConsoleOutputStream out = new ConsoleOutputStream(byteOut);
        Callback callback = createMock(Callback.class);
        TelnetInputScanner scanner = new TelnetInputScanner(in, out, callback);
        try {
            scanner.scan((byte) SE);
            scanner.scan((byte) EL);
            scanner.scan((byte) SB);
            scanner.scan((byte) WILL);
            scanner.scan((byte) WILL_NOT);
            scanner.scan((byte) DO);
            scanner.scan((byte) DO_NOT);
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
        Callback callback = createMock(Callback.class);
        TelnetInputScanner scanner = new TelnetInputScanner(in, out, callback);

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

    @Test
    public void testTTNegotiations() throws Exception {
        Map<byte[], TerminalTypeMappings> ttMappings = new HashMap<byte[], TerminalTypeMappings>();
        ttMappings.put(new byte[] { 'A', 'N', 'S', 'I' }, new ANSITerminalTypeMappings());
        ttMappings.put(new byte[] { 'V', 'T', '1', '0', '0' }, new VT100TerminalTypeMappings());
        ttMappings.put(new byte[] { 'V', 'T', '2', '2', '0' }, new VT220TerminalTypeMappings());
        ttMappings.put(new byte[] { 'X', 'T', 'E', 'R', 'M' }, new VT220TerminalTypeMappings());
        ttMappings.put(new byte[] { 'V', 'T', '3', '2', '0' }, new VT320TerminalTypeMappings());
        ttMappings.put(new byte[] { 'S', 'C', 'O' }, new SCOTerminalTypeMappings());

        for (byte[] ttype : ttMappings.keySet()) {
            testTerminalTypeNegotiation(ttype, ttMappings.get(ttype));
        }
    }

    private void testTerminalTypeNegotiation(byte[] terminalType, TerminalTypeMappings mappings) throws Exception {
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);

        byte[] requestNegotiation = { (byte) IAC, (byte) DO, (byte) TTYPE };

        TestCallback testCallback = new TestCallback();
        TelnetOutputStream out = new TelnetOutputStream(serverOut);
        TelnetInputScanner scanner = new TelnetInputScanner(new ConsoleInputStream(), out, testCallback);
        out.write(requestNegotiation);
        out.flush();

        int read = clientIn.read();
        Assert.assertEquals("Unexpected input ", IAC, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", DO, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", TTYPE, read);

        scanner.scan(IAC);
        scanner.scan(WILL);
        scanner.scan(TTYPE);

        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", IAC, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", SB, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", TTYPE, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", SEND, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", IAC, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", SE, read);

        scanner.scan(IAC);
        scanner.scan(SB);
        scanner.scan(TTYPE);
        scanner.scan(IS);
        scanner.scan('A');
        scanner.scan('B');
        scanner.scan('C');
        scanner.scan('D');
        scanner.scan('E');
        scanner.scan(IAC);
        scanner.scan(SE);

        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", IAC, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", SB, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", TTYPE, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", SEND, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", IAC, read);
        read = clientIn.read();
        Assert.assertEquals("Unexpected input ", SE, read);

        scanner.scan(IAC);
        scanner.scan(SB);
        scanner.scan(TTYPE);
        scanner.scan(IS);
        for (byte symbol : terminalType) {
            scanner.scan(symbol);
        }
        scanner.scan(IAC);
        scanner.scan(SE);

        Assert.assertEquals("Incorrect BACKSPACE: ", mappings.getBackspace(), scanner.getBackspace());
        Assert.assertEquals("Incorrect DELL: ", mappings.getDel(), scanner.getDel());

        Map<String, KEYS> currentEscapesToKey = scanner.getCurrentEscapesToKey();
        Map<String, KEYS> expectedEscapesToKey = mappings.getEscapesToKey();
        for (String escape : expectedEscapesToKey.keySet()) {
            KEYS key = expectedEscapesToKey.get(escape);
            Assert.assertEquals("Incorrect " + key.name(), key, currentEscapesToKey.get(escape));
        }

        Assert.assertTrue("Callback not called ", testCallback.getState());
    }

    class TestCallback implements Callback {

        private boolean isCalled = false;

        @Override
        public void finished() {
            isCalled = true;
        }

        public boolean getState() {
            return isCalled;
        }
    }

}
