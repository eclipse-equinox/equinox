/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.transforms;

import java.io.*;

/**
 * This class allows the easy piping of data from an input stream to an external process.  
 * The resulting output is then returned via the {@link InputStream} methods of this class.
 */
public class ProcessPipeInputStream extends InputStream {
	protected Process process = null;
	protected InputStream standardInput;
	protected IOException failure;
	private String commandString;
	private String[] environment;
	private File workingDirectory;

	/**
	 * Create a new process pipe with the provided stream supplying the contents of the standard input to the process.
	 * 
	 * @param standardInput the contents of standard input
	 */
	public ProcessPipeInputStream(InputStream standardInput, String commandString, String[] environment, File workingDirectory) {
		this.standardInput = standardInput;
		if (commandString == null)
			throw new IllegalArgumentException();
		this.commandString = commandString;
		this.environment = environment;
		this.workingDirectory = workingDirectory;
	}

	/**
	 * Get the command string for this process.
	 * The format of this String must conform to the specifications of the command parameter of {@link Runtime#exec(String[], String[], File)}.
	 * @return the command string.  Never <code>null</code>.
	 */
	protected String getCommandString() {
		return commandString;
	}

	/**
	 * Get the environment variables for this process.
	 * The format of the Strings in this array must conform to the specifications of the environment parameter of {@link Runtime#exec(String[], String[], File)}.
	 * @return the environment variables or <code>null</code>
	 */
	protected String[] getEnvironment() {
		return environment;
	}

	/**
	 * Get the working directory for the process. 
	 * @return the working directory or <code>null</code>
	 */
	protected File getWorkingDirectory() {
		return workingDirectory;
	}

	public int read() throws IOException {
		synchronized (this) {
			if (failure != null) {
				IOException e = new IOException("Problem piping the stream."); //$NON-NLS-1$
				e.fillInStackTrace();
				e.initCause(failure);
				throw e;
			}
			if (process == null) {

				process = Runtime.getRuntime().exec(getCommandString(), getEnvironment(), getWorkingDirectory());

				Thread thread = new Thread(new Runnable() {

					public void run() {
						byte[] buffer = new byte[2048];
						int len = 0;
						try {
							while ((len = standardInput.read(buffer)) > 0) {
								process.getOutputStream().write(buffer, 0, len);
							}
							process.getOutputStream().close();
						} catch (IOException e) {
							synchronized (ProcessPipeInputStream.this) {
								failure = e;
								process.destroy();
							}
						}

					}
				});
				thread.start();

			}
			return process.getInputStream().read();
		}
	}

	/**
	 * Resets the stream.  This has the effect of destroying the process, if one exists.
	 */
	public void reset() throws IOException {
		synchronized (this) {
			if (process != null) {
				process.destroy();
				process = null;
				failure = null;
			}
		}
		super.reset();
	}
}
