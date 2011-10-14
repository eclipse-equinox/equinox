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

package org.eclipse.equinox.console.telnet;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Dictionary;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.easymock.EasyMock;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.Test;
import org.osgi.framework.BundleContext;


public class TelnetCommandTests {
	
	private static final int TEST_CONTENT = 100;
	private static final String TELNET_PORT_PROP_NAME = "osgi.console";
	private static final String USE_CONFIG_ADMIN_PROP = "osgi.console.useConfigAdmin";
	private static final String STOP_COMMAND = "stop";
	private static final String HOST = "localhost";
	private static final String FALSE = "false";
	private static final int TELNET_PORT = 2223;
	private static final long WAIT_TIME = 5000;
	
	@Test
	public void testTelnetCommand() throws Exception {
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
        
        BundleContext context = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(context.getProperty(USE_CONFIG_ADMIN_PROP)).andReturn(FALSE);
        EasyMock.expect(context.getProperty(TELNET_PORT_PROP_NAME)).andReturn(Integer.toString(TELNET_PORT));
        EasyMock.expect(context.registerService((String)EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary<String, ?>)EasyMock.anyObject())).andReturn(null);
        EasyMock.replay(context);
        
        TelnetCommand command = new TelnetCommand(processor, context);
        command.start();
        
        Socket socketClient = null;
        try {
            socketClient = new Socket(HOST, TELNET_PORT);
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
        } finally {
            if (socketClient != null) {
                socketClient.close();
            }
            command.telnet(new String[] {STOP_COMMAND});
        }
        EasyMock.verify(context);
	}
}
