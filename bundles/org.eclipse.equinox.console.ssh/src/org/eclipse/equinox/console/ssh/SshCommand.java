/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.ssh;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.eclipse.equinox.console.storage.DigestUtil;
import org.eclipse.equinox.console.storage.SecureUserStore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * This class implements a command for starting/stopping a simple ssh server.
 *
 */
public class SshCommand {
	private String defaultHost = null;
	private int defaultPort;
    private List<CommandProcessor> processors = new ArrayList<CommandProcessor>();
    private String host = null;
    private int port;
    private SshServ sshServ;
    private BundleContext context;
    private ServiceRegistration<?> configuratorRegistration;
    private boolean isEnabled = false;
    private final Object lock = new Object();
    
    private static final String DEFAULT_USER = "equinox";
    private static final String DEFAULT_PASSWORD = "equinox";
    private static final String DEFAULT_USER_STORE_PROPERTY = "osgi.console.ssh.useDefaultSecureStorage";
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String USE_CONFIG_ADMIN_PROP = "osgi.console.useConfigAdmin";
    private static final String SSH_PID = "osgi.console.ssh";
    private static final String ENABLED = "enabled";
    
    public SshCommand(CommandProcessor processor, BundleContext context) {
        processors.add(processor);
        this.context = context;
        
        if ("true".equals(context.getProperty(USE_CONFIG_ADMIN_PROP))) {
        	Dictionary<String, String> sshProperties = new Hashtable<String, String>();
        	sshProperties.put(Constants.SERVICE_PID, SSH_PID);
        	try {
        		synchronized (lock) {
        			configuratorRegistration = context.registerService(ManagedService.class.getName(), new SshConfigurator(), sshProperties);
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
    	String sshPort = null;
        String consolePropValue = context.getProperty(SSH_PID);
        if(consolePropValue != null) {
        	int index = consolePropValue.lastIndexOf(":");
        	if (index > -1) {
        		defaultHost = consolePropValue.substring(0, index);
        	}
        	sshPort = consolePropValue.substring(index + 1);
        	isEnabled = true;
        } 
        if (sshPort != null && !"".equals(sshPort)) {
        	try {
        		defaultPort = Integer.parseInt(sshPort);
			} catch (NumberFormatException e) {
				// do nothing
			}
        }
    }
    
    public synchronized void startService() {
    	Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put("osgi.command.scope", "equinox");
		properties.put("osgi.command.function", new String[] {"ssh"});
		if ((port > 0 || defaultPort > 0) && isEnabled == true) {
			try{
				ssh(new String[]{"start"});
			} catch (Exception e) {
				System.out.println("Cannot start ssh. Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
		context.registerService(SshCommand.class.getName(), this, properties);
    }

    @Descriptor("start/stop a ssh server")
    public synchronized void ssh(String[] arguments) throws Exception {
        String command = null;
        String newHost = null;
        int newPort = 0;
        
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
        		throw new Exception("Unrecognized ssh command/option " + arguments[i]);
        	}
        }
        
        if (command == null) {
        	throw new Exception("No ssh command specified");
        }
        
        if (newPort != 0) {
        	port = newPort;
        } else if (port == 0) {
        	port = defaultPort;
        }
        
        if (port == 0) {
        	throw new Exception("No ssh port specified");
        }
        
        if (newHost != null) {
        	host = newHost;
        } else {
        	host = defaultHost;
        }
        
        if ("start".equals(command)) {
            if (sshServ != null) {
                throw new IllegalStateException("ssh is already running on port " + port);
            }
            
            checkPortAvailable(port);
            
            sshServ = new SshServ(processors, context, host, port);
            sshServ.setName("equinox ssh");
            
            if ("true".equals(context.getProperty(DEFAULT_USER_STORE_PROPERTY))) {
            	try {
            		checkUserStore();
            		registerUserAdmin();
            	} catch (NoClassDefFoundError e) {
            		System.out.println("If you want to use secure storage, please install Equinox security bundle and its dependencies");
            		sshServ = null;
    				return;
            	} catch (IOException e) {
            		e.printStackTrace();
            		sshServ = null;
    				return;
            	}
            }
            
            try {
				sshServ.start();
			} catch (RuntimeException e) {
				sshServ = null;
				return;
			}    
        } else if ("stop".equals(command)) {
            if (sshServ == null) {
                System.out.println("ssh is not running.");
                return;
            }
            
            sshServ.stopSshServer();
            sshServ = null;
        } 
    }
    
    public synchronized void addCommandProcessor(CommandProcessor processor) {
    	processors.add(processor);
    	if (sshServ != null) {
    		sshServ.addCommandProcessor(processor);
    	}
    }
    
    public synchronized void removeCommandProcessor(CommandProcessor processor) {
    	processors.remove(processor);
    	if (sshServ != null) {
    		sshServ.removeCommandProcessor(processor);
    	}
    }
    
    private void checkPortAvailable(int port) throws Exception {
    	ServerSocket socket = null;
    	try {
    		socket = new ServerSocket(port);
    	} catch (BindException e) {
    		throw new Exception ("Port " + port + " already in use");
    	} finally {
    		if (socket != null) {
    			socket.close();
    		}
    	}
    }
    
    /*
     * Register user administration commands
     */
    private void registerUserAdmin() {
    	Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put("osgi.command.scope", "equinox");
		properties.put("osgi.command.function", new String[] {"addUser", "addUser", "deleteUser", "resetPassword", "setPassword", "addRoles", "removeRoles", "listUsers"});
		context.registerService(UserAdminCommand.class.getName(), new UserAdminCommand(), properties);
    }
    
    /*
     * Create user store if not available. Add the default user, if there is no other user in the store.
     */
    private void checkUserStore() throws Exception {
    	SecureUserStore.initStorage();
    	if(SecureUserStore.getUserNames().length == 0) {
    		SecureUserStore.putUser(DEFAULT_USER, DigestUtil.encrypt(DEFAULT_PASSWORD), null );
    	}
    }
    
    private void printHelp() {
    	StringBuffer help = new StringBuffer();
    	help.append("ssh - start simple ssh server");
    	help.append("\n");
    	help.append("Usage: ssh start | stop [-port port] [-host host]");
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
    
    class SshConfigurator implements ManagedService {
    	@SuppressWarnings("rawtypes")
		private Dictionary properties;
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public synchronized void updated(Dictionary props) throws ConfigurationException {
			if (props != null) {
				this.properties = props;
				properties.put(Constants.SERVICE_PID, SSH_PID);
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
			if (sshServ == null && isEnabled == true) {
				try {
					ssh(new String[]{"start"});
				} catch (Exception e) {
					System.out.println("Cannot start ssh: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
    	
    }
}
