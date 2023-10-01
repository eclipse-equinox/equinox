/*******************************************************************************
 * Copyright (c) 2011 SAP AG
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
