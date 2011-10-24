/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation  
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.console.telnet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;

/**
 * A telnet server, which listens for telnet connections and starts a telnet connection manager
 * when a connection is accepted. If there are multiple CommandProcessor, a telnet connection
 * is created for each of them.
 *
 */
public class TelnetServer extends Thread {
	
	private ServerSocket server;
    private boolean isRunning = true;
    private List<CommandProcessor> processors = null;
    private BundleContext context;
    private List<Socket> sockets = new ArrayList<Socket>();
    private Map<CommandProcessor, List<TelnetConnection>> processorToConnectionsMapping = new HashMap<CommandProcessor, List<TelnetConnection>>();
    
    public TelnetServer(BundleContext context, List<CommandProcessor> processors, String host, int port) throws IOException {
    	this.context = context;
    	this.processors = processors;
    	if(host != null) {
    		server = new ServerSocket(port, 0, InetAddress.getByName(host));
    	} else {
    		server = new ServerSocket(port);
    	}
    }
    
	public void run()
    {
        try
        {
            while (isRunning)
            {
                final Socket socket = server.accept();
                sockets.add(socket);
                for (CommandProcessor processor : processors) {
                	TelnetConnection telnetConnection = new TelnetConnection(socket, processor, context);
                	List<TelnetConnection> telnetConnections = processorToConnectionsMapping.get(processor);
                	if (telnetConnections == null) {
                		telnetConnections = new ArrayList<TelnetConnection>();
                		processorToConnectionsMapping.put(processor, telnetConnections);
                	}
                	telnetConnections.add(telnetConnection);
                	telnetConnection.start();
                }
            }
        } catch (IOException e) {
            if (isRunning == true) {
                e.printStackTrace();
            }
        } finally {
        	isRunning = false;
            try {
                if (server != null) {
                    server.close();
                }
            } catch (IOException e){
            	// do nothing
            }
        }
    }
	
	public synchronized void addCommandProcessor(CommandProcessor processor) {
		processors.add(processor);
		if (!sockets.isEmpty()) {
			List<TelnetConnection> telnetConnections = new ArrayList<TelnetConnection>();
			for (Socket socket : sockets) {
				TelnetConnection telnetConnection = new TelnetConnection(socket, processor, context);
				telnetConnections.add(telnetConnection);
				telnetConnection.start();
			}
			processorToConnectionsMapping.put(processor, telnetConnections);
		}
	}
	
	public synchronized void removeCommandProcessor(CommandProcessor processor) {
		processors.remove(processor);
		List<TelnetConnection> telnetConnections = processorToConnectionsMapping.remove(processor);
		if (telnetConnections != null) {
			for (TelnetConnection telnetConnection : telnetConnections) {
				telnetConnection.close();
			}
		}
	}
	
	public synchronized void stopTelnetServer() {
		isRunning = false;
		try {
            if (server != null) {
                server.close();
            }    
        } catch (IOException e){
        	// do nothing
        }
        
        for(List<TelnetConnection> telnetConnections : processorToConnectionsMapping.values()) {
        	for (TelnetConnection telnetConnection : telnetConnections) {
        		telnetConnection.close();
        	}
        }
        
		this.interrupt();
	}
}
