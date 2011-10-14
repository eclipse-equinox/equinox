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

import java.io.InputStream;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.eclipse.equinox.console.common.InputHandler;

/**
 * This class customizes the generic handler with a concrete content processor,
 * which provides telnet protocol handling.
 */
public class TelnetInputHandler extends InputHandler {
	
    public TelnetInputHandler(InputStream input, ConsoleInputStream in, ConsoleOutputStream out, Callback callback) {
        super(input, in, out);
        inputScanner = new TelnetInputScanner(in, out, callback);
    }
}
