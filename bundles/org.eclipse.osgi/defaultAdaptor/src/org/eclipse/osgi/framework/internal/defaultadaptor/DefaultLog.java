/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.defaultadaptor;

import java.io.*;
import java.util.Date;

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

public class DefaultLog implements FrameworkLog {
	protected boolean newSession = true;

	protected static final String SESSION = "!SESSION"; //$NON-NLS-1$
	protected static final String ENTRY = "!ENTRY"; //$NON-NLS-1$
	protected static final String SUBENTRY = "!SUBENTRY"; //$NON-NLS-1$
	protected static final String MESSAGE = "!MESSAGE"; //$NON-NLS-1$
	protected static final String STACK = "!STACK"; //$NON-NLS-1$

	protected static final String LINE_SEPARATOR;
	protected static final String TAB_STRING = "\t"; //$NON-NLS-1$

	protected boolean consoleLog = false;

	static {
		String s = System.getProperty("line.separator"); //$NON-NLS-1$
		LINE_SEPARATOR = s == null ? "\n" : s; //$NON-NLS-1$
	}
	/**
	 * The File object to store messages.  This value may be null.
	 */
	protected File outFile;

	/**
	 * The Writer to log messages to.
	 */
	protected Writer writer;

	protected boolean append = false;

	/**
	 * The default constructor for DefaultLog.  Constructs a DefaultLog
	 * that uses System.err to log messages to.
	 */
	public DefaultLog() {
		this((Writer)null);
	}

	/**
	 * Constructs a DefaultLog that uses the specified Writer to log messages
	 * to.  If the Writer is null then System.err is used to log messages to.
	 * @param writer The Writer to log messages to or null if System.err is
	 * to be used.
	 */
	public DefaultLog(Writer writer) {
		this.append = false;
		if (writer == null)
			// log to System.err by default
			this.writer = logForStream(System.err);
		else
			this.writer = writer;
	}

	/**
	 * Constructs a DefaultLog that uses the specified File to create
	 * a FileWriter to log messages to.
	 * @param outFile The File to log messages to.
	 * @param append If set to true then the contents of outFile will be
	 * appended to.
	 * @throws IOException if any problem occurs while constructing a
	 * FileWriter from the outFile.
	 */
	public DefaultLog(File outFile, boolean append) {
		this.append = true;
		this.outFile = outFile;
		this.writer = null;
	}

	/**
	 * Closes the FrameworkLog.
	 */
	public void close() {
		try {
			if (writer != null) {
				writer.close();
				writer = null;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void openFile() {
		if (writer == null) {
			if (outFile != null) {
				try {
					writer = new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(outFile.getAbsolutePath(),
									append), "UTF-8")); //$NON-NLS-1$
				} catch (IOException e) {
					writer = new PrintWriter(System.err);
				}
			}
			else {
				writer = new PrintWriter(System.err);
			}
		}
	}

	protected void closeFile() {
		if (outFile != null) {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				writer = null;
			}
		}
	}

	public void log(FrameworkEvent frameworkEvent){
		Bundle b = frameworkEvent.getBundle();
		Throwable t = frameworkEvent.getThrowable();
		String stack = null;
		if (t != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			// this is the stacktrace code "0"
			pw.println(" 0");
			t.printStackTrace(pw);
			if (t instanceof BundleException){
				Throwable n = ((BundleException)t).getNestedException();
				if (n != null) {
					pw.println("Nested exception:");
					n.printStackTrace(pw);
				}
			}
			stack = sw.toString();
		}
		
		FrameworkLogEntry logEntry = 
			new FrameworkLogEntry(0, b.getLocation() + " 0 0", "FrameworkEvent.ERROR", stack);

		log(logEntry);
	}

	public synchronized void log(FrameworkLogEntry logEntry){
		if (logEntry == null)
			return;
		try {
			openFile();
			if (newSession) {
				writeSession();
				newSession = false;
			}
			writeLog(logEntry);
			writer.flush();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		finally {
			closeFile();
		}
	}

	public synchronized void setWriter(Writer newWriter, boolean append) {
		setOutput(null, newWriter, append);
	}

	public synchronized void setFile(File newFile, boolean append) throws IOException {
		setOutput(newFile, null, append);
	}

	public synchronized File getFile(){
		return outFile;
	}

	public void setConsoleLog(boolean consoleLog) {
		this.consoleLog = consoleLog;
	}

	private void setOutput(File newOutFile, Writer newWriter, boolean append) {
		if (newOutFile == null || !newOutFile.equals(this.outFile)) {
			if (this.writer != null) {
				try {
					this.writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.writer = null;
			}
			// Append old outFile to newWriter. We only attempt to do this
			// if the current Writer is backed by a File and this is not
			// a new session.
			File oldOutFile = this.outFile;
			this.outFile = newOutFile;
			this.writer = newWriter;
			if (append && !newSession && oldOutFile != null) {
				Reader fileIn = null;
				try {
					openFile();
					fileIn = new InputStreamReader(new FileInputStream(
							oldOutFile.getAbsolutePath()), "UTF-8");
					copyReader(fileIn, this.writer);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (fileIn != null) {
						try {
							fileIn.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					closeFile();
				}
			}
		}
	}

	private void copyReader(Reader reader, Writer writer) throws IOException {
		char buffer[] = new char[1024];
		int count;
		while ((count = reader.read(buffer, 0, buffer.length)) > 0) {
			writer.write(buffer, 0, count);
		}
	}

	protected String getDate() {
		return new Date().toString();
	}
	protected Writer logForStream(OutputStream output) {
		try {
			return new BufferedWriter(new OutputStreamWriter(output, "UTF-8")); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			return new BufferedWriter(new OutputStreamWriter(output));
		}
	}

	protected void writeLog(FrameworkLogEntry entry) throws IOException {
		writeEntry(entry);
		writeMessage(entry);
		writeStack(entry);
	}

	protected void writeSession() throws IOException {
		write(SESSION);
		writeSpace();
		String date = getDate();
		write(date);
		writeSpace();
		for (int i = SESSION.length() + date.length(); i < 78; i++) {
			write("-"); //$NON-NLS-1$
		}
		writeln();

		// Write out certain values found in System.getProperties()
		try {
			String key = "java.fullversion"; //$NON-NLS-1$
			String value = System.getProperty(key);
			if (value == null) {
				key = "java.version"; //$NON-NLS-1$
				value = System.getProperty(key);
				writeln(key + "=" + value); //$NON-NLS-1$
				key = "java.vendor"; //$NON-NLS-1$
				value = System.getProperty(key);
				writeln(key + "=" + value); //$NON-NLS-1$
			} else {
				writeln(key + "=" + value); //$NON-NLS-1$
			}
		} catch (Exception e) {
			// If we're not allowed to get the values of these properties
			// then just skip over them.
		}


	}

	protected void writeEntry(FrameworkLogEntry entry) throws IOException {
		if (entry.getDepth() == 0) {
			write(ENTRY);
		} else {
			write(SUBENTRY);
			writeSpace();
			write(Integer.toString(entry.getDepth()));
		}
		writeSpace();
		write(entry.getEntry());
		writeSpace();
		write(getDate());
		writeln();
	}

	protected void writeMessage(FrameworkLogEntry entry) throws IOException {
		write(MESSAGE);
		writeSpace();
		writeln(entry.getMessage());
	}

	protected void writeStack(FrameworkLogEntry entry) throws IOException {
		String stack = entry.getStack();
		if (stack != null) {
			write(STACK);
			writeSpace();
			write(stack);
			if (!stack.endsWith(LINE_SEPARATOR))
				writeln();
		}
	}

	protected void write(String message) throws IOException {
		if (message != null){
			writer.write(message);
			if (consoleLog) 
				System.out.print(message);
		}
	}
	protected void writeln(String s) throws IOException {
		write(s);
		writeln();
	}
	protected void writeln() throws IOException {
		write(LINE_SEPARATOR);
	}
	protected void writeSpace() throws IOException {
		write(" "); //$NON-NLS-1$
	}
}
