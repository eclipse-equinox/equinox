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

package org.eclipse.equinox.console.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class represents a generic handler of content, read from some input
 * stream. It reads from the stream, and passes what is read to a processor,
 * which performs some actions on the content, eventually writing to an output
 * stream. This handler should be customized with a concrete content processor.
 */
public abstract class InputHandler extends Thread {

	protected Scanner inputScanner;
	protected OutputStream out;
	protected ConsoleInputStream in;
	protected InputStream input;
	protected byte[] buffer;
	protected static final int MAX_SIZE = 2048;

	public InputHandler(InputStream input, ConsoleInputStream in, OutputStream out) {
		this.input = input;
		this.in = in;
		this.out = out;
		buffer = new byte[MAX_SIZE];
	}

	@Override
	public void run() {
		int count;
		try {
			while ((count = input.read(buffer)) > -1) {
				for (int i = 0; i < count; i++) {
					inputScanner.scan(buffer[i]);
				}
			}
		} catch (IOException e) {
			// Printing stack trace is not needed since the streams are closed immediately
			// do nothing
		} finally {
			try {
				in.close();
			} catch (IOException e1) {
				// do nothing
			}
			try {
				out.close();
			} catch (IOException e1) {
				// do nothing
			}
		}
	}

	public Scanner getScanner() {
		return inputScanner;
	}

}
