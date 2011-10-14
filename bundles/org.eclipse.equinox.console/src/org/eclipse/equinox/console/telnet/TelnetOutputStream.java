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

import org.eclipse.equinox.console.common.ConsoleOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class adds to the output stream wrapper initial negotiation of telnet communication.
 */
public class TelnetOutputStream extends ConsoleOutputStream {
	
    static final byte[] autoMessage = new byte[]{(byte) 255, (byte) 251, (byte) 1, // IAC WILL ECHO
                                                 (byte) 255, (byte) 251, (byte) 3, // IAC WILL SUPPRESS GO_AHEAD
                                                 (byte) 255, (byte) 253, (byte) 31, // IAC DO NAWS
                                                 (byte) 255, (byte) 253, (byte) 24}; // IAC DO TTYPE
  
    public TelnetOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Sends the options which a server wants to negotiate with a telnet client.
     */
    public synchronized void autoSend() throws IOException {
        write(autoMessage);
        flush();
    }

}
