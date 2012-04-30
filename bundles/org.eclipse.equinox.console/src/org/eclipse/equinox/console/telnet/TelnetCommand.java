/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation  
 *******************************************************************************/

package org.eclipse.equinox.console.telnet;

import java.net.BindException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * This class implements a command for starting/stopping a simple telnet server.
 *
 */
public class TelnetCommand {
	
	private String defaultHost = null;
    private int defaultPort;
    private List<CommandProcessor> processors = new ArrayList<CommandProcessor>();
    private final BundleContext context;
    private String host = null;
    private int port;
    private TelnetServer telnetServer = null;
    private ServiceRegistration<?> configuratorRegistration;
    private boolean isEnabled = false;
    
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String USE_CONFIG_ADMIN_PROP = "osgi.console.useConfigAdmin";
    private static final String TELNET_PID = "osgi.console.telnet";
    private static final String CONSOLE_PROP = "osgi.console";
    private static final String ENABLED = "enabled";
    private final Object lock = new Object();

    public TelnetCommand(CommandProcessor processor, BundleContext context)
    {
        processors.add(processor);
        this.context = context;
        if ("true".equals(context.getProperty(USE_CONFIG_ADMIN_PROP))) {
        	Dictionary<String, String> telnetProperties = new Hashtable<String, String>();
        	telnetProperties.put(Constants.SERVICE_PID, TELNET_PID);
        	try {
        		synchronized (lock) {
        			configuratorRegistration = context.registerService(ManagedService.class.getName(), new TelnetConfigurator(), telnetProperties);
        		}
        	} catch (NoClassDefFoundError e) {
        		System.out.println("Configuration Admin not available!");
        		return;
        	}
        } else {
        	parseHostAndPort();
        }
    }
    
    private void parseHostAndPort() {
    	String telnetPort = null;
        String consolePropValue = context.getProperty(CONSOLE_PROP);
        if(consolePropValue != null) {
        	int index = consolePropValue.lastIndexOf(":");
        	if (index > -1) {
        		defaultHost = consolePropValue.substring(0, index);
        	}
        	telnetPort = consolePropValue.substring(index + 1);
        	isEnabled = true;
        } 
        if (telnetPort != null && !"".equals(telnetPort)) {
        	try {
        		defaultPort = Integer.parseInt(telnetPort);
			} catch (NumberFormatException e) {
				// do nothing
			}
        } 
    }
    
    public synchronized void startService() {
    	Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put("osgi.command.scope", "equinox");
		properties.put("osgi.command.function", new String[] {"telnet"});
		if ((port > 0 || defaultPort > 0) && isEnabled == true) {
			try{
				telnet(new String[]{"start"});
			} catch (Exception e) {
				System.out.println("Cannot start telnet. Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
		context.registerService(TelnetCommand.class.getName(), this, properties);
    }

    @Descriptor("start/stop a telnet server")
    public synchronized void telnet(String[] arguments) throws Exception
    {
        String command = null;
        int newPort = 0;
        String newHost = null;
        
        for(int i = 0; i < arguments.length; i++) {
        	if("-?".equals(arguments[i]) || "-help".equals(arguments[i])) {
        		printHelp();
        		return;
        	} else if("start".equals(arguments[i])) {
        		command = "start";
        	} else if ("stop".equals(arguments[i])) {
        		command = "stop";
        	} else if ("-port".equals(arguments[i]) && (arguments.length > i + 1)) {
        		i++;
        		newPort = Integer.parseInt(arguments[i]);
        	} else if ("-host".equals(arguments[i]) && (arguments.length > i + 1)) {
        		i++;
        		newHost = arguments[i];
        	} else {
        		throw new Exception("Unrecognized telnet command/option " + arguments[i]);
        	}
        }
        
        if (command == null) {
        	throw new Exception("No telnet command specified");
        }
        
        if (newPort != 0) {
        	port = newPort;
        } else if (port == 0) {
        	port = defaultPort;
        }
        
        if (port == 0) {
        	throw new Exception("No telnet port specified");
        }
        
        if (newHost != null) {
        	host = newHost;
        } else {
        	host = defaultHost;
        }

        if ("start".equals(command)) {
            if (telnetServer != null) {
                throw new IllegalStateException("telnet is already running on port " + port);
            }
            
            try {
				telnetServer = new TelnetServer(context, processors, host, port);
			} catch (BindException e) {
				throw new Exception("Port " + port + " already in use");
			}
			
            telnetServer.setName("equinox telnet");
            telnetServer.start();    
        } else if ("stop".equals(command)) {
            if (telnetServer == null) {
                System.out.println("telnet is not running.");
                return;
            }
            
            telnetServer.stopTelnetServer();
            telnetServer = null;
        } 
    }
    
    public synchronized void addCommandProcessor(CommandProcessor processor) {
    	processors.add(processor);
    	if (telnetServer != null) {
    		telnetServer.addCommandProcessor(processor);
    	}
    }
    
    public synchronized void removeCommandProcessor(CommandProcessor processor) {
    	processors.remove(processor);
    	if (telnetServer != null) {
    		telnetServer.removeCommandProcessor(processor);
    	}
    }
    
    private void printHelp() {
    	StringBuffer help = new StringBuffer();
    	help.append("telnet - start simple telnet server");
    	help.append("\n");
    	help.append("Usage: telnet start | stop [-port port] [-host host]");
    	help.append("\n");
    	help.append("\t");
    	help.append("-port");
    	help.append("\t");
    	help.append("listen port (default=");
    	help.append(defaultPort);
    	help.append(")");
    	help.append("\n");
    	help.append("\t");
    	help.append("-host");
    	help.append("\t");
    	help.append("local host address to listen on (default is none - listen on all network interfaces)");
    	help.append("\n");
    	help.append("\t");
    	help.append("-?, -help");
    	help.append("\t");
    	help.append("show help");
    	System.out.println(help.toString());          
    }
    
    class TelnetConfigurator implements ManagedService {
    	@SuppressWarnings("rawtypes")
		private Dictionary properties;
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public synchronized void updated(Dictionary props) throws ConfigurationException {
			if (props != null) {
				this.properties = props;
				properties.put(Constants.SERVICE_PID, TELNET_PID);
			} else {
				return;
			}
			
			defaultPort = Integer.parseInt(((String)properties.get(PORT)));
			defaultHost = (String)properties.get(HOST);
			if (properties.get(ENABLED) == null) {
				isEnabled = false;
			} else {
				isEnabled = Boolean.parseBoolean((String)properties.get(ENABLED));
			}
			synchronized (lock) {
				configuratorRegistration.setProperties(properties);
			}
			if (telnetServer == null && isEnabled == true) {
				try {
					telnet(new String[]{"start"});
				} catch (Exception e) {
					System.out.println("Cannot start telnet: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
    	
    }
}
