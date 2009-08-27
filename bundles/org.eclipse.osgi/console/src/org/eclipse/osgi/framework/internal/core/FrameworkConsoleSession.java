/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.io.*;
import java.net.Socket;
import org.eclipse.osgi.framework.console.ConsoleSession;

public class FrameworkConsoleSession extends ConsoleSession {
	private final Socket s;
	private final InputStream in;
	private final OutputStream out;

	public FrameworkConsoleSession(InputStream in, OutputStream out, Socket s) {
		this.in = in;
		this.out = out;
		this.s = s;
	}

	public synchronized InputStream getInput() {
		return in;
	}

	public synchronized OutputStream getOutput() {
		return out;
	}

	public void doClose() {
		if (s != null)
			try {
				s.close();
			} catch (IOException ioe) {
				// do nothing
			}
		if (out != null)
			try {
				out.close();
			} catch (IOException e) {
				// do nothing
			}
		if (in != null)
			try {
				in.close();
			} catch (IOException ioe) {
				// do nothing
			}
	}

}
