/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation  
 *******************************************************************************/

package org.eclipse.equinox.console.telnet;
/**
 * A callback through which the TelnetInputScanner notifies the TelnetConnectionManger
 * that the terminal type negotiation with the client has finished. This is importednt, because
 * the TelnetConnectionManager should start the CommandSession after the negotiation is finished.
 * This is necessary, because the user input should be interpreted with the correct terminal type.
 */
public class NegotiationFinishedCallback implements Callback {
	
	private TelnetConnection telnetConnection;
	
	public NegotiationFinishedCallback(TelnetConnection telnetConnection) {
		this.telnetConnection = telnetConnection;
	}
	
	public void finished() {
		telnetConnection.telnetNegotiationFinished();
	}

}
