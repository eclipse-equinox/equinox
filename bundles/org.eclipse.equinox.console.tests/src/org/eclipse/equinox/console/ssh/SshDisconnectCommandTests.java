package org.eclipse.equinox.console.ssh;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringBufferInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.DefaultConnectFuture;
import org.apache.sshd.server.Environment;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.equinox.console.commands.DisconnectCommand;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.storage.DigestUtil;
import org.eclipse.equinox.console.storage.SecureUserStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class SshDisconnectCommandTests {
	private static final int TEST_CONTENT = 100;
	private static final String USER_STORE_FILE_NAME = "org.eclipse.equinox.console.jaas.file";
	private static final String JAAS_CONFIG_FILE_NAME = "jaas.config";
	private static final String JAAS_CONFIG_PROPERTY_NAME = "java.security.auth.login.config";
	private static final String DEFAULT_USER_STORAGE = "osgi.console.ssh.useDefaultSecureStorage";
	private static final String USE_CONFIG_ADMIN_PROP = "osgi.console.useConfigAdmin";
	private static final String SSH_PORT_PROP_NAME = "osgi.console.ssh";
	private static final String STORE_FILE_NAME = SshCommandTests.class.getName() + "_store";
	private static final String GOGO_SHELL_COMMAND = "gosh --login --noshutdown";
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String START_COMMAND = "start";
	private static final String STOP_COMMAND = "stop";
	private static final String TERM_PROPERTY = "TERM";
	private static final String XTERM = "XTERM";
	private static final String HOST = "localhost";
	private static final int SSH_PORT = 2222;
	private static final long WAIT_TIME = 5000;
	private SshSession sshSession;
	private InputStream in;

	@Before
	public void init() throws Exception {
		clean();
		initStore();
        initJaasConfigFile();
	}
	
	@Test
	public void testSshCommand() throws Exception {
		final CommandSession session = EasyMock.createMock(CommandSession.class);
		EasyMock.makeThreadSafe(session, true);
		session.put((String)EasyMock.anyObject(), EasyMock.anyObject());
		EasyMock.expectLastCall();
		session.put((String)EasyMock.anyObject(), EasyMock.anyObject());
		EasyMock.expectLastCall();
		session.put((String)EasyMock.anyObject(), EasyMock.anyObject());
		EasyMock.expectLastCall();
		session.put((String)EasyMock.anyObject(), EasyMock.anyObject());
		EasyMock.expectLastCall();
		session.put((String)EasyMock.anyObject(), EasyMock.anyObject());
		EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

			@Override
			public Object answer() throws Throwable {
				sshSession = (SshSession)EasyMock.getCurrentArguments()[1];
				return null;
			}
			
		});
		EasyMock.expect(session.execute(GOGO_SHELL_COMMAND)).andReturn(null);
		EasyMock.expect(session.get("CLOSEABLE")).andReturn(sshSession);
		session.close();
		EasyMock.expectLastCall().atLeastOnce();
		EasyMock.replay(session);

		CommandProcessor processor = EasyMock.createMock(CommandProcessor.class);
		EasyMock.expect(processor.createSession((ConsoleInputStream)EasyMock.anyObject(), (PrintStream)EasyMock.anyObject(), (PrintStream)EasyMock.anyObject())).andReturn(session);
		EasyMock.replay(processor);

		BundleContext context = EasyMock.createMock(BundleContext.class);
		EasyMock.makeThreadSafe(context, true);
		EasyMock.expect(context.getProperty(USE_CONFIG_ADMIN_PROP)).andReturn(FALSE);
		EasyMock.expect(context.getProperty(DEFAULT_USER_STORAGE)).andReturn(TRUE).anyTimes();
		EasyMock.expect(context.getProperty(SSH_PORT_PROP_NAME)).andReturn(Integer.toString(SSH_PORT));
		EasyMock.expect(context.registerService((String)EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary<String, ?>)EasyMock.anyObject())).andReturn(null);
		EasyMock.replay(context);

		Map<String, String> environment = new HashMap<String, String>();
		environment.put(TERM_PROPERTY, XTERM);
		Environment env = EasyMock.createMock(Environment.class);
		EasyMock.expect(env.getEnv()).andReturn(environment);
		EasyMock.replay(env);

		SshCommand command = new SshCommand(processor, context);
		command.ssh(new String[] {START_COMMAND});

		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		try {
			ConnectFuture connectFuture = client.connect(HOST, SSH_PORT);
			DefaultConnectFuture defaultConnectFuture = (DefaultConnectFuture) connectFuture;

			try {
				Thread.sleep(WAIT_TIME);
			} catch (InterruptedException ie) {
				// do nothing
			}
			ClientSession sshSession = defaultConnectFuture.getSession();

			int ret = ClientSession.WAIT_AUTH;                
			sshSession.authPassword(USERNAME, PASSWORD);
			ret = sshSession.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);

			if ((ret & ClientSession.CLOSED) != 0) {
				System.err.println("error");
				System.exit(-1);
			}
			ClientChannel channel = sshSession.createChannel("shell");
			
			PipedOutputStream outputStream = new PipedOutputStream();
			PipedInputStream inputStream = new PipedInputStream(outputStream);
			
			final DisconnectCommand disconnectCommand = new DisconnectCommand(context);
			in = System.in;
            System.setIn(inputStream);
            
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            channel.setIn(new StringBufferInputStream(TEST_CONTENT + "\n"));
            channel.setOut(byteOut);
            channel.setErr(byteOut);
            channel.open();
            
            
            new Thread() {
            	public void run() {
            		disconnectCommand.disconnect(session);
            	}
            }.start();
            
            outputStream.write(new byte[]{'y'});
            outputStream.write('\n');
            outputStream.flush();
            
            Thread.sleep(WAIT_TIME);
            
            try {
				Thread.sleep(WAIT_TIME);
			} catch (InterruptedException ie) {
				// do nothing
			}
			
            try {
				outputStream.write(TEST_CONTENT);
				outputStream.write('\n');
				outputStream.flush();
				Assert.fail("Connection not closed");
			} catch (Exception e) {
				// we should be here
			}
			sshSession.close(true);
		} finally {
			client.stop();
		}

		command.ssh(new String[] {STOP_COMMAND});
		return;
	}
	
	@After
	public void cleanUp() {
		clean();
	}
	
	private void clean() {
		System.setProperty(USER_STORE_FILE_NAME, "");
    	File file = new File(STORE_FILE_NAME);
    	if (file.exists()) {
    		file.delete();
    	}
    	
    	System.setProperty(JAAS_CONFIG_PROPERTY_NAME, "");
    	File jaasConfFile = new File(JAAS_CONFIG_FILE_NAME);
    	if (jaasConfFile.exists()) {
    		jaasConfFile.delete();
    	}
    	
    	System.setIn(in);
	}
	
	private void initStore() throws Exception {
		System.setProperty(USER_STORE_FILE_NAME, STORE_FILE_NAME);
        SecureUserStore.initStorage();
        SecureUserStore.putUser(USERNAME, DigestUtil.encrypt(PASSWORD), null);
	}
	
	private void initJaasConfigFile() throws Exception {
		System.setProperty(JAAS_CONFIG_PROPERTY_NAME, JAAS_CONFIG_FILE_NAME);
		File jaasConfFile = new File(JAAS_CONFIG_FILE_NAME);
    	if (!jaasConfFile.exists()) {
    		PrintWriter out = null;
    		try {
				out = new PrintWriter(jaasConfFile);
				out.println("equinox_console {");
				out.println("	org.eclipse.equinox.console.jaas.SecureStorageLoginModule REQUIRED;");
				out.println("};");
			} finally {
				if (out != null) {
					out.close();
				}
			}
    	}
	}
}
