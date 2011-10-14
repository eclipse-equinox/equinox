package org.eclipse.equinox.console.telnet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.Assert;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;


public class TelnetCommandWithConfigAdminTests {
	private static final int TEST_CONTENT = 100;
	private static final String STOP_COMMAND = "stop";
	private static final String HOST = "localhost";
	private static final String TELNET_PORT = "2223";
	private static final long WAIT_TIME = 5000;
	private static final String USE_CONFIG_ADMIN_PROP = "osgi.console.useConfigAdmin";
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private ManagedService configurator;
	
	@Test
	public void testTelnetCommandWithConfigAdminEnabledTelnet() throws Exception {
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

        final ServiceRegistration<?> registration = EasyMock.createMock(ServiceRegistration.class);
        registration.setProperties((Dictionary)EasyMock.anyObject());

        EasyMock.expectLastCall();
        EasyMock.replay(registration);

        BundleContext context = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(context.getProperty(USE_CONFIG_ADMIN_PROP)).andReturn(TRUE);
        EasyMock.expect(
        		(ServiceRegistration) context.registerService(
        				(String)EasyMock.anyObject(), 
        				(ManagedService)EasyMock.anyObject(), 
        				(Dictionary<String, ?>)EasyMock.anyObject())
        	).andAnswer((IAnswer<ServiceRegistration<?>>) new IAnswer<ServiceRegistration<?>>() {
        		public ServiceRegistration<?> answer() {
        			configurator = (ManagedService) EasyMock.getCurrentArguments()[1];
        			return registration;
        		}
			});
        EasyMock.expect(
        		context.registerService(
        				(String)EasyMock.anyObject(), 
        				(TelnetCommand)EasyMock.anyObject(), 
        				(Dictionary<String, ?>)EasyMock.anyObject())).andReturn(null);
        EasyMock.replay(context);
        
        TelnetCommand command = new TelnetCommand(processor, context);
        command.start();
        Dictionary props = new Hashtable();
		props.put("port", TELNET_PORT);
		props.put("host", HOST);
		props.put("enabled", TRUE);
		configurator.updated(props);
		
        Socket socketClient = null;
        try {
            socketClient = new Socket(HOST, Integer.parseInt(TELNET_PORT));
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
	
	@Test
	public void testTelnetCommandWithConfigAdminDisabledTelnet() throws Exception {
		disabledTelnet(false);
	}
	
	@Test
	public void testTelnetCommandWithConfigAdminDisabledTelnetByDefault() throws Exception {
		disabledTelnet(true);
	}
	
	private void disabledTelnet(boolean isDefault) throws Exception {
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

        final ServiceRegistration<?> registration = EasyMock.createMock(ServiceRegistration.class);
        registration.setProperties((Dictionary)EasyMock.anyObject());

        EasyMock.expectLastCall();
        EasyMock.replay(registration);

        BundleContext context = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(context.getProperty(USE_CONFIG_ADMIN_PROP)).andReturn(TRUE);
        EasyMock.expect(
        		(ServiceRegistration) context.registerService(
        				(String)EasyMock.anyObject(), 
        				(ManagedService)EasyMock.anyObject(), 
        				(Dictionary<String, ?>)EasyMock.anyObject())
        	).andAnswer((IAnswer<ServiceRegistration<?>>) new IAnswer<ServiceRegistration<?>>() {
        		public ServiceRegistration<?> answer() {
        			configurator = (ManagedService) EasyMock.getCurrentArguments()[1];
        			return registration;
        		}
			});
        EasyMock.expect(
        		context.registerService(
        				(String)EasyMock.anyObject(), 
        				(TelnetCommand)EasyMock.anyObject(), 
        				(Dictionary<String, ?>)EasyMock.anyObject())).andReturn(null);
        EasyMock.replay(context);
        
        TelnetCommand command = new TelnetCommand(processor, context);
        command.start();
        Dictionary props = new Hashtable();
		props.put("port", TELNET_PORT);
		props.put("host", HOST);
		if (isDefault == false) {
			props.put("enabled", FALSE);
		}
		configurator.updated(props);
		
        Socket socketClient = null;
        try {
            socketClient = new Socket(HOST, Integer.parseInt(TELNET_PORT));
            Assert.fail("It should not be possible to open a socket to " + HOST + ":" + TELNET_PORT);
        } catch (IOException e) {
        	// this is ok, there should be an exception
        } finally {
            if (socketClient != null) {
                socketClient.close();
            }
            try {
            	command.telnet(new String[] {STOP_COMMAND});
            } catch (IllegalStateException e) {
            	//this is expected
            }
        }
        EasyMock.verify(context);
	}
	
}
