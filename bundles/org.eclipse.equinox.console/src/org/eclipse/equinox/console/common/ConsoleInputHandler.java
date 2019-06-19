/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others
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
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.console.common;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * This class customizes the generic handler with a concrete content processor,
 * which provides command line editing.
 */
public class ConsoleInputHandler extends InputHandler {
	
	public ConsoleInputHandler(InputStream input, ConsoleInputStream in, OutputStream out) {
		super(input, in, out);
		inputScanner = new ConsoleInputScanner(in, out);
	} 
}
