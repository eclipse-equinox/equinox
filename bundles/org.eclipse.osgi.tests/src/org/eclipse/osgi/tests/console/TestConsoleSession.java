/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.console;

import java.io.*;
import org.eclipse.osgi.framework.console.ConsoleSession;

public class TestConsoleSession extends ConsoleSession {
	private InputStream in;
	private OutputStream out;

	public TestConsoleSession(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}

	protected void doClose() {
		try {
			in.close();
			out.close();
		} catch (IOException e) {
			// do nothing
		}
	}

	public InputStream getInput() {
		return in;
	}

	public OutputStream getOutput() {
		return out;
	}

}
