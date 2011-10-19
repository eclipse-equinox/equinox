/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.ssh;

import java.io.InputStream;

import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.eclipse.equinox.console.common.InputHandler;

/**
 * This class customizes the generic handler with a concrete content processor,
 * which provides ssh protocol handling.
 *
 */
public class SshInputHandler extends InputHandler {
	public SshInputHandler(InputStream input, ConsoleInputStream in, ConsoleOutputStream out) {
        super(input, in, out);
        inputScanner = new SshInputScanner(in, out);
    }
}
