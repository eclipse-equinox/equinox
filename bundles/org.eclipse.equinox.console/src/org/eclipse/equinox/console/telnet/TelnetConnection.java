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
import java.io.PrintStream;
import java.net.Socket;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import java.io.Closeable;

import org.eclipse.equinox.console.common.ConsoleInputHandler;
import org.eclipse.equinox.console.common.ConsoleInputScanner;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.osgi.framework.BundleContext;

/**
 * This class manages a telnet connection. It is responsible for wrapping the original io streams
 * from the socket, and starting a CommandSession to execute commands from the telnet.
 *
 */
public class TelnetConnection extends Thread implements Closeable {
	
	private Socket socket;
	private CommandProcessor processor;
	private BundleContext context;
	protected boolean isTelnetNegotiationFinished = false;
    private Callback callback;
    private static final long TIMEOUT = 1000;
    private static final long NEGOTIATION_TIMEOUT = 60000;
    private static final String PROMPT = "prompt";
    private static final String OSGI_PROMPT = "osgi> ";
    private static final String SCOPE = "SCOPE";
    private static final String EQUINOX_SCOPE = "equinox:*";
    private static final String CLOSEABLE = "CLOSEABLE";
	
	public TelnetConnection (Socket socket, CommandProcessor processor, BundleContext context) {
		this.socket = socket;
		this.processor = processor;
		this.context = context;
		callback = new NegotiationFinishedCallback(this);
	}
	
	public void run() {
		try {
			ConsoleInputStream in = new ConsoleInputStream();
			TelnetOutputStream out = new TelnetOutputStream(socket.getOutputStream());
			out.autoSend();
			TelnetInputHandler telnetInputHandler = new TelnetInputHandler(socket.getInputStream(), in, out, callback);
			telnetInputHandler.start();
			
			long start = System.currentTimeMillis();
			
			synchronized (this) {
				while (isTelnetNegotiationFinished == false && System.currentTimeMillis() - start < NEGOTIATION_TIMEOUT) {
					try {
						wait(TIMEOUT);
					} catch (InterruptedException e) {
						// do nothing
					}
				}
			}
			final CommandSession session;
			PrintStream output = new PrintStream(out);
			
			ConsoleInputStream inp = new ConsoleInputStream();
			
	        ConsoleInputHandler consoleInputHandler = new ConsoleInputHandler(in, inp, out);
	        consoleInputHandler.getScanner().setBackspace(telnetInputHandler.getScanner().getBackspace());
	        consoleInputHandler.getScanner().setDel(telnetInputHandler.getScanner().getDel());
	        consoleInputHandler.getScanner().setCurrentEscapesToKey(telnetInputHandler.getScanner().getCurrentEscapesToKey());
	        consoleInputHandler.getScanner().setEscapes(telnetInputHandler.getScanner().getEscapes());
	        ((ConsoleInputScanner)consoleInputHandler.getScanner()).setContext(context);
	        
	        consoleInputHandler.start();
	        
	        session = processor.createSession(inp, output, output);
	        session.put(SCOPE, EQUINOX_SCOPE);
	        session.put(PROMPT, OSGI_PROMPT);
	        // Store this closeable object in the session, so that the disconnect command can close it
	        session.put(CLOSEABLE, this);
	        ((ConsoleInputScanner)consoleInputHandler.getScanner()).setSession(session);
	        
			try {
	            session.execute("gosh --login --noshutdown");
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            session.close();
	            try {
	                socket.close();
	            }
	            catch (IOException e) {
	            	// do nothing
	            }
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		try {
			this.interrupt();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void telnetNegotiationFinished() {
		isTelnetNegotiationFinished = true;
		notify();
	}
}
