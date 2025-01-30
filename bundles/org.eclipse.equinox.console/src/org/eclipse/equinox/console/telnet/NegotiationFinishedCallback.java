/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation  
 *******************************************************************************/

package org.eclipse.equinox.console.telnet;

/**
 * A callback through which the TelnetInputScanner notifies the
 * TelnetConnectionManger that the terminal type negotiation with the client has
 * finished. This is importednt, because the TelnetConnectionManager should
 * start the CommandSession after the negotiation is finished. This is
 * necessary, because the user input should be interpreted with the correct
 * terminal type.
 */
public class NegotiationFinishedCallback implements Callback {

	private final TelnetConnection telnetConnection;

	public NegotiationFinishedCallback(TelnetConnection telnetConnection) {
		this.telnetConnection = telnetConnection;
	}

	@Override
	public void finished() {
		telnetConnection.telnetNegotiationFinished();
	}

}
