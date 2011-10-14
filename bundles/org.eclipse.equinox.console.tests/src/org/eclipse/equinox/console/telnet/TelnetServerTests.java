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

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.easymock.EasyMock;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;

public class TelnetServerTests {

    private static final String HOST = "localhost";
    private static final int PORT = 38888;
    private static final long WAIT_TIME = 5000;
    private static final int TEST_CONTENT = 100;

    @Test
    public void testTelnetServer() throws Exception {

    	CommandSession session = EasyMock.createMock(CommandSession.class);
    	session.put((String)EasyMock.anyObject(), EasyMock.anyObject());
        EasyMock.expectLastCall().times(3);
        EasyMock.expect(session.execute((String)EasyMock.anyObject())).andReturn(new Object());
        session.close();
		EasyMock.expectLastCall();
        EasyMock.replay(session);
        
        CommandProcessor processor = EasyMock.createMock(CommandProcessor.class);
        EasyMock.expect(processor.createSession((ConsoleInputStream)EasyMock.anyObject(), (PrintStream)EasyMock.anyObject(), (PrintStream)EasyMock.anyObject())).andReturn(session);
        EasyMock.replay(processor);
        
        List<CommandProcessor> processors = new ArrayList<CommandProcessor>();
        processors.add(processor);
        TelnetServer telnetServer = new TelnetServer(null, processors, HOST, PORT);
        telnetServer.start();
        Socket socketClient = null;

        try {
            socketClient = new Socket("localhost", PORT);
            OutputStream outClient = socketClient.getOutputStream();
            outClient.write(TEST_CONTENT);
            outClient.write('\n');
            outClient.flush();
            // wait for the accept thread to finish execution
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException ie) {
                // do nothing
            }
            verify();
        } catch(ConnectException e) {
        	Assert.fail("Telnet port not open");
        } finally {
            if (socketClient != null) {
                socketClient.close();
            }
            telnetServer.stopTelnetServer();
        }
    }

    @Test
    public void testTelnetServerWithoutHost() throws Exception {
    	CommandSession session = EasyMock.createMock(CommandSession.class);
    	session.put((String)EasyMock.anyObject(), EasyMock.anyObject());
        EasyMock.expectLastCall().times(4);
        EasyMock.expect(session.execute((String)EasyMock.anyObject())).andReturn(new Object());
        session.close();
		EasyMock.expectLastCall();
        EasyMock.replay(session);
        
        CommandProcessor processor = EasyMock.createMock(CommandProcessor.class);
        EasyMock.expect(processor.createSession((ConsoleInputStream)EasyMock.anyObject(), (PrintStream)EasyMock.anyObject(), (PrintStream)EasyMock.anyObject())).andReturn(session);
        EasyMock.replay(processor);
        
        List<CommandProcessor> processors = new ArrayList<CommandProcessor>();
        processors.add(processor);
        TelnetServer telnetServer = new TelnetServer(null, processors, null, PORT);
        telnetServer.start();
        Socket socketClient = null;

        try {
            socketClient = new Socket("localhost", PORT);
            OutputStream outClient = socketClient.getOutputStream();
            outClient.write(TEST_CONTENT);
            outClient.write('\n');
            outClient.flush();

            // wait for the accept thread to finish execution
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException ie) {
                // do nothing
            }
        } catch(ConnectException e) {
        	Assert.fail("Telnet port not open");
        } finally {
            if (socketClient != null) {
                socketClient.close();
            }
            telnetServer.stopTelnetServer();
        }

    }

}
