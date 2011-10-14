package org.eclipse.equinox.console.telnet;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.easymock.EasyMock;
import org.eclipse.equinox.console.commands.DisconnectCommand;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class TelnetDisconnectionTest {
	private static final String HOST = "localhost";
    private InputStream in;

    @Test
    public void testTelneConnection() throws Exception {
        ServerSocket servSocket = null;
        Socket socketClient = null;
        Socket socketServer = null;
        TelnetConnection connection = null;
        OutputStream outClient = null;
        OutputStream outServer = null;

        try {
            servSocket = new ServerSocket(0);
            socketClient = new Socket(HOST, servSocket.getLocalPort());
            socketServer = servSocket.accept();

            CommandProcessor processor = EasyMock.createMock(CommandProcessor.class);
            connection = new TelnetConnection(socketServer, processor, null);
            
            final CommandSession session = EasyMock.createMock(CommandSession.class);
            EasyMock.makeThreadSafe(session, true);
            session.put((String)EasyMock.anyObject(), EasyMock.anyObject());
            EasyMock.expectLastCall().times(3);
            EasyMock.expect(session.get("CLOSEABLE")).andReturn(connection);
            EasyMock.expect(session.execute((String)EasyMock.anyObject())).andReturn(null);
            session.close();
    		EasyMock.expectLastCall();
            EasyMock.replay(session);
            
            EasyMock.expect(processor.createSession((ConsoleInputStream) EasyMock.anyObject(), (PrintStream) EasyMock.anyObject(), (PrintStream) EasyMock.anyObject())).andReturn(session);
            EasyMock.replay(processor);
            
            connection.start();
            
            BundleContext context = EasyMock.createMock(BundleContext.class);
            final DisconnectCommand command = new DisconnectCommand(context);
            
            PipedOutputStream outputStream = new PipedOutputStream();
            PipedInputStream inputStream = new PipedInputStream(outputStream);
            
            in = System.in;
            System.setIn(inputStream);
            
            new Thread() {
            	public void run() {
            		command.disconnect(session);
            	}
            }.start();
            
            outputStream.write(new byte[]{'y'});
            outputStream.write('\n');
            outputStream.flush();

            Thread.sleep(3000);
            Assert.assertTrue("Socket is not closed!", socketServer.isClosed());
           
            connection.telnetNegotiationFinished();
            Thread.sleep(5000);
            EasyMock.verify(session, processor);
        } finally {
        	if (socketClient != null) {
        		socketClient.close();
        	}
        	if (outClient != null) {
        		outClient.close();
        	}
        	if (outServer != null) {
        		outServer.close();
        	}

        	if (socketServer != null) {
        		socketServer.close();
        	}

        	if (servSocket != null) {
        		servSocket.close();
        	}
        	
        	System.setIn(in);
        }
    }
}
