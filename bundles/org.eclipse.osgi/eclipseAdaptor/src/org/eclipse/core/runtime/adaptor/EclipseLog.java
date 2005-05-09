/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.SecureAction;
import org.osgi.framework.*;

/**
 * Internal class.
 */
public class EclipseLog implements FrameworkLog {
	private static final String PASSWORD = "-password"; //$NON-NLS-1$	
	protected static final String SESSION = "!SESSION"; //$NON-NLS-1$
	protected static final String ENTRY = "!ENTRY"; //$NON-NLS-1$
	protected static final String SUBENTRY = "!SUBENTRY"; //$NON-NLS-1$
	protected static final String MESSAGE = "!MESSAGE"; //$NON-NLS-1$
	protected static final String STACK = "!STACK"; //$NON-NLS-1$

	protected static final String LINE_SEPARATOR;
	protected static final String TAB_STRING = "\t"; //$NON-NLS-1$

	//Constants for rotating log file
	public static final int DEFAULT_LOG_SIZE = 1000;
	public static final int DEFAULT_LOG_FILES = 10;
	public static final int LOG_SIZE_MIN = 10;

	public static final String PROP_LOG_SIZE_MAX = "eclipse.log.size.max"; //$NON-NLS-1$
	public static final String PROP_LOG_FILE_MAX = "eclipse.log.backup.max"; //$NON-NLS-1$
	public static final String LOG_EXT = ".log"; //$NON-NLS-1$
	public static final String BACKUP_MARK = ".bak_"; //$NON-NLS-1$

	static {
		String s = System.getProperty("line.separator"); //$NON-NLS-1$
		LINE_SEPARATOR = s == null ? "\n" : s; //$NON-NLS-1$
	}
	private static final SecureAction secureAction = new SecureAction();

	protected boolean consoleLog = false;
	protected boolean newSession = true;
	/**
	 * The File object to store messages.  This value may be null.
	 */
	protected File outFile;

	/**
	 * The Writer to log messages to.
	 */
	protected Writer writer;

	int maxLogSize = DEFAULT_LOG_SIZE; // The value is in KB.
	int maxLogFiles = DEFAULT_LOG_FILES;
	int backupIdx = 0;

	public EclipseLog(File outFile) {
		this.outFile = outFile;
		this.writer = null;
		readLogProperties();
	}

	public EclipseLog(Writer writer) {
		if (writer == null)
			// log to System.err by default
			this.writer = logForStream(System.err);
		else
			this.writer = writer;
	}

	public EclipseLog() {
		this((Writer) null);
	}

	private Throwable getRoot(Throwable t) {
		Throwable root = null;
		if (t instanceof BundleException)
			root = ((BundleException) t).getNestedException();
		if (t instanceof InvocationTargetException)
			root = ((InvocationTargetException) t).getTargetException();
		// skip inner InvocationTargetExceptions and BundleExceptions
		if (root instanceof InvocationTargetException || root instanceof BundleException) {
			Throwable deeplyNested = getRoot(root);
			if (deeplyNested != null)
				// if we have something more specific, use it, otherwise keep what we have
				root = deeplyNested;
		}
		return root;
	}

	/**
	 * Helper method for writing out argument arrays.
	 */
	protected void writeArgs(String header, String[] args) throws IOException {
		if (args == null || args.length == 0)
			return;
		write(header);
		for (int i = 0; i < args.length; i++) {
			//mask out the password argument for security
			if (i > 0 && PASSWORD.equals(args[i - 1]))
				write(" (omitted)"); //$NON-NLS-1$
			else
				write(" " + args[i]); //$NON-NLS-1$
		}
		writeln();
	}

	/*
	 * Main should have set the session start-up timestamp so return that. 
	 * Return the "now" time if not available.
	 */
	protected String getSessionTimestamp() {
		String ts = System.getProperty("eclipse.startTime"); //$NON-NLS-1$
		if (ts != null) {
			try {
				return getDate(new Date(Long.parseLong(ts)));
			} catch (NumberFormatException e) {
				// fall through and use the timestamp from right now
			}
		}
		return getDate(new Date());
	}

	protected void writeSession() throws IOException {
		write(SESSION);
		writeSpace();
		String date = getSessionTimestamp();
		write(date);
		writeSpace();
		for (int i = SESSION.length() + date.length(); i < 78; i++) {
			write("-"); //$NON-NLS-1$
		}
		writeln();
		// Write out certain values found in System.getProperties()
		try {
			String key = "eclipse.buildId"; //$NON-NLS-1$
			String value = System.getProperty(key, "unknown"); //$NON-NLS-1$
			writeln(key + "=" + value); //$NON-NLS-1$

			key = "java.fullversion"; //$NON-NLS-1$
			value = System.getProperty(key);
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
		// The Bootloader has some information that we might be interested in.
		write("BootLoader constants: OS=" + EnvironmentInfo.getDefault().getOS()); //$NON-NLS-1$
		write(", ARCH=" + EnvironmentInfo.getDefault().getOSArch()); //$NON-NLS-1$
		write(", WS=" + EnvironmentInfo.getDefault().getWS()); //$NON-NLS-1$
		writeln(", NL=" + EnvironmentInfo.getDefault().getNL()); //$NON-NLS-1$
		// Add the command-line arguments used to invoke the platform 
		// XXX: this includes runtime-private arguments - should we do that?
		writeArgs("Framework arguments: ", EnvironmentInfo.getDefault().getNonFrameworkArgs()); //$NON-NLS-1$
		writeArgs("Command-line arguments: ", EnvironmentInfo.getDefault().getCommandLineArgs()); //$NON-NLS-1$
	}

	/**
	 * Closes the FrameworkLog.
	 */
	public void close() {
		try {
			if (writer != null) {
				Writer tmpWriter = writer;
				writer = null;
				tmpWriter.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void openFile() {
		if (writer == null) {
			if (outFile != null) {
				try {
					writer = logForStream(secureAction.getFileOutputStream(outFile, true));
				} catch (IOException e) {
					writer = logForStream(System.err);
				}
			} else {
				writer = logForStream(System.err);
			}
		}
	}

	protected void closeFile() {
		if (outFile != null) {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// we cannot log here; just print the stacktrace.
					e.printStackTrace();
				}
				writer = null;
			}
		}
	}

	public void log(FrameworkEvent frameworkEvent) {
		Bundle b = frameworkEvent.getBundle();
		Throwable t = frameworkEvent.getThrowable();

		FrameworkLogEntry logEntry = new FrameworkLogEntry(b.getLocation() + " 0 0", "FrameworkEvent.ERROR", 0, t, null); //$NON-NLS-1$ //$NON-NLS-2$

		log(logEntry);
	}

	public synchronized void log(FrameworkLogEntry logEntry) {
		if (logEntry == null)
			return;
		try {
			checkLogFileSize();
			openFile();
			if (newSession) {
				writeSession();
				newSession = false;
			}
			writeLog(0, logEntry);
			writer.flush();
		} catch (Exception e) {
			// any exceptions during logging should be caught 
			System.err.println("An exception occurred while writing to the platform log:");//$NON-NLS-1$
			e.printStackTrace(System.err);
			System.err.println("Logging to the console instead.");//$NON-NLS-1$
			//we failed to write, so dump log entry to console instead
			try {
				writer = logForStream(System.err);
				writeLog(0, logEntry);
				writer.flush();
			} catch (Exception e2) {
				System.err.println("An exception occurred while logging to the console:");//$NON-NLS-1$
				e2.printStackTrace(System.err);
			}
		} finally {
			closeFile();
		}
	}

	public synchronized void setWriter(Writer newWriter, boolean append) {
		setOutput(null, newWriter, append);
	}

	public synchronized void setFile(File newFile, boolean append) throws IOException {
		if (newFile != null && !newFile.equals(this.outFile)) {
			// If it's a new file, then reset.
			readLogProperties();
			backupIdx = 0;
		}
		setOutput(newFile, null, append);
		System.getProperties().put(EclipseStarter.PROP_LOGFILE, newFile.getAbsolutePath());
	}

	public synchronized File getFile() {
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
			boolean copyFailed = false;
			if (append && oldOutFile != null && oldOutFile.isFile()) {
				Reader fileIn = null;
				try {
					openFile();
					fileIn = new InputStreamReader(secureAction.getFileInputStream(oldOutFile), "UTF-8"); //$NON-NLS-1$
					copyReader(fileIn, this.writer);
				} catch (IOException e) {
					copyFailed = true;
					e.printStackTrace();
				} finally {
					if (fileIn != null) {
						try {
							fileIn.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						// delete the old file if copying didn't fail
						if (!copyFailed)
							oldOutFile.delete();
					}
					closeFile();
				}
			}
		}
	}

	private void copyReader(Reader reader, Writer aWriter) throws IOException {
		char buffer[] = new char[1024];
		int count;
		while ((count = reader.read(buffer, 0, buffer.length)) > 0) {
			aWriter.write(buffer, 0, count);
		}
	}

	protected String getDate(Date date) {
		try {
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS"); //$NON-NLS-1$
			return formatter.format(date);
		} catch (Exception e) {
			// If there were problems writing out the date, ignore and
			// continue since that shouldn't stop us from logging the rest
			// of the information
		}
		return Long.toString(System.currentTimeMillis());
	}

	protected String getStackTrace(Throwable t) {
		if (t == null)
			return null;

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		t.printStackTrace(pw);
		// ensure the root exception is fully logged
		Throwable root = getRoot(t);
		if (root != null) {
			pw.println("Root exception:"); //$NON-NLS-1$
			root.printStackTrace(pw);
		}
		return sw.toString();
	}

	protected Writer logForStream(OutputStream output) {
		try {
			return new BufferedWriter(new OutputStreamWriter(output, "UTF-8")); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			return new BufferedWriter(new OutputStreamWriter(output));
		}
	}

	protected void writeLog(int depth, FrameworkLogEntry entry) throws IOException {
		writeEntry(depth, entry);
		writeMessage(entry);
		writeStack(entry);

		FrameworkLogEntry[] children = entry.getChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				writeLog(depth + 1, children[i]);
			}
		}
	}

	protected void writeEntry(int depth, FrameworkLogEntry entry) throws IOException {
		if (depth == 0) {
			writeln(); // write a blank line before all !ENTRY tags bug #64406
			write(ENTRY);
		} else {
			write(SUBENTRY);
			writeSpace();
			write(Integer.toString(depth));
		}
		writeSpace();
		write(entry.getEntry());
		writeSpace();
		write(getDate(new Date()));
		writeln();
	}

	protected void writeMessage(FrameworkLogEntry entry) throws IOException {
		write(MESSAGE);
		writeSpace();
		writeln(entry.getMessage());
	}

	protected void writeStack(FrameworkLogEntry entry) throws IOException {
		Throwable t = entry.getThrowable();
		if (t != null) {
			String stack = getStackTrace(t);
			write(STACK);
			writeSpace();
			write(Integer.toString(entry.getStackCode()));
			writeln();
			write(stack);
		}
	}

	protected void write(String message) throws IOException {
		if (message != null) {
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

	protected boolean checkLogFileSize() {
		if (maxLogSize == 0)
			return true; // no size limitation.

		boolean isBackupOK = true;
		if (outFile != null) {
			if ((outFile.length() >> 10) > maxLogSize) { // Use KB as file size unit.
				String logFilename = outFile.getAbsolutePath();

				// Delete old backup file that will be replaced.
				String backupFilename = ""; //$NON-NLS-1$
				if (logFilename.toLowerCase().endsWith(LOG_EXT)) {
					backupFilename = logFilename.substring(0, logFilename.length() - LOG_EXT.length()) + BACKUP_MARK + backupIdx + LOG_EXT;
				} else {
					backupFilename = logFilename + BACKUP_MARK + backupIdx; //$NON-NLS-1$
				}
				File backupFile = new File(backupFilename);
				if (backupFile.exists()) {
					if (!backupFile.delete()) {
						System.err.println("Error when trying to delete old log file: " + backupFile.getName());//$NON-NLS-1$ 
						if (backupFile.renameTo(new File(backupFile.getAbsolutePath() + System.currentTimeMillis()))) {
							System.err.println("So we rename it to filename: " + backupFile.getName()); //$NON-NLS-1$
						} else {
							System.err.println("And we also cannot rename it!"); //$NON-NLS-1$
							isBackupOK = false;
						}
					}
				}

				// Rename current log file to backup one.
				boolean isRenameOK = outFile.renameTo(backupFile);
				if (!isRenameOK) {
					System.err.println("Error when trying to rename log file to backup one."); //$NON-NLS-1$
					isBackupOK = false;
				}
				File newFile = new File(logFilename);
				setOutput(newFile, null, false);

				// Write a new SESSION header to new log file.
				openFile();
				try {
					writeSession();
					writeln();
					writeln("This is a continuation of log file " + backupFile.getAbsolutePath());//$NON-NLS-1$
					writeln("Created Time: " + getDate(new Date(System.currentTimeMillis()))); //$NON-NLS-1$
					writer.flush();
				} catch (IOException ioe) {
					ioe.printStackTrace(System.err);
				}
				closeFile();
				backupIdx = (++backupIdx) % maxLogFiles;
			}
		}
		return isBackupOK;
	}

	protected void readLogProperties() {
		String newMaxLogSize = (String) System.getProperties().get(PROP_LOG_SIZE_MAX);
		if (newMaxLogSize != null) {
			maxLogSize = Integer.parseInt(newMaxLogSize);
			if (maxLogSize != 0 && maxLogSize < LOG_SIZE_MIN) {
				// If the value is '0', then it means no size limitation.
				// Also, make sure no inappropriate(too small) assigned value.
				maxLogSize = LOG_SIZE_MIN;
			}
		}

		String newMaxLogFiles = (String) System.getProperties().get(PROP_LOG_FILE_MAX);
		if (newMaxLogFiles != null) {
			maxLogFiles = Integer.parseInt(newMaxLogFiles);
			if (maxLogFiles < 1) {
				// Make sure no invalid assigned value. (at least >= 1)
				maxLogFiles = DEFAULT_LOG_FILES;
			}
		}
	}
}
