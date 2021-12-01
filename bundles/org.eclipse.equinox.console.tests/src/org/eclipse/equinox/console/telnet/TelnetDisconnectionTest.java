package org.eclipse.equinox.console.telnet;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.commands.DisconnectCommand;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class TelnetDisconnectionTest {
	private static final String HOST = "localhost";
	private InputStream in;

	@Test
	public void testTelnetConnection() throws Exception {
		TelnetConnection connection = null;

		try (ServerSocket servSocket = new ServerSocket(0);
				Socket socketClient = new Socket(HOST, servSocket.getLocalPort());
				Socket socketServer = servSocket.accept();
				CommandSession session = mock(CommandSession.class)) {

			CommandProcessor processor = mock(CommandProcessor.class);
			connection = new TelnetConnection(socketServer, processor, null);

			when(session.get("CLOSEABLE")).thenReturn(connection);
			when(session.execute(any(String.class))).thenReturn(null);

			when(processor.createSession(any(ConsoleInputStream.class), any(PrintStream.class), any(PrintStream.class)))
					.thenReturn(session);

			connection.start();
			Thread.sleep(60000);

			BundleContext context = mock(BundleContext.class);
			final DisconnectCommand command = new DisconnectCommand(context);

			PipedOutputStream outputStream = new PipedOutputStream();
			PipedInputStream inputStream = new PipedInputStream(outputStream);

			in = System.in;
			System.setIn(inputStream);

			new Thread() {
				@Override
				public void run() {
					command.disconnect(session);
				}
			}.start();

			outputStream.write(new byte[] { 'y' });
			outputStream.write('\n');
			outputStream.flush();

			Thread.sleep(3000);
			assertTrue("Socket is not closed!", socketServer.isClosed());

			connection.telnetNegotiationFinished();
			Thread.sleep(5000);
		} finally {
			System.setIn(in);
		}
	}
}
