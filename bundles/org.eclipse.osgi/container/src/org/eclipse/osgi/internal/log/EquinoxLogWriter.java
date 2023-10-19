/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Copyright (c) 2023 Robert Bosch GmbH - https://github.com/eclipse-equinox/equinox/issues/221
 *******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.LogFilter;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

class EquinoxLogWriter implements SynchronousLogListener, LogFilter {
	private static final String PASSWORD = "-password"; //$NON-NLS-1$
	/** The session tag */
	private static final String SESSION = "!SESSION"; //$NON-NLS-1$
	/** The entry tag */
	private static final String ENTRY = "!ENTRY"; //$NON-NLS-1$
	/** The sub-entry tag */
	private static final String SUBENTRY = "!SUBENTRY"; //$NON-NLS-1$
	/** The message tag */
	private static final String MESSAGE = "!MESSAGE"; //$NON-NLS-1$
	/** The stacktrace tag */
	private static final String STACK = "!STACK"; //$NON-NLS-1$

	/** The line separator used in the log output */
	private static final String LINE_SEPARATOR;

	static {
		String s = System.lineSeparator();
		LINE_SEPARATOR = s == null ? "\n" : s; //$NON-NLS-1$
	}

	//Constants for rotating log file
	/** The default size a log file can grow before it is rotated */
	private static final int DEFAULT_LOG_SIZE = 1000;
	/** The default number of backup log files */
	private static final int DEFAULT_LOG_FILES = 10;
	/** The minimum size limit for log rotation */
	private static final int LOG_SIZE_MIN = 10;

	/** The system property used to specify the log level */
	private static final String PROP_LOG_LEVEL = "eclipse.log.level"; //$NON-NLS-1$
	/** The system property used to specify size a log file can grow before it is rotated */
	private static final String PROP_LOG_SIZE_MAX = "eclipse.log.size.max"; //$NON-NLS-1$
	/** The system property used to specify the maximim number of backup log files to use */
	private static final String PROP_LOG_FILE_MAX = "eclipse.log.backup.max"; //$NON-NLS-1$
	/** The extension used for log files */
	private static final String LOG_EXT = ".log"; //$NON-NLS-1$
	/** The extension markup to use for backup log files*/
	private static final String BACKUP_MARK = ".bak_"; //$NON-NLS-1$

	/** The system property used to specify command line args should be omitted from the log */
	private static final String PROP_LOG_INCLUDE_COMMAND_LINE = "eclipse.log.include.commandline"; //$NON-NLS-1$
	/** The system property used to specify the log file writer should be closed eagerly */
	private static final String PROP_CLOSE_LOG_FILE_EAGERLY = "eclipse.log.closeFile.eagerClose"; //$NON-NLS-1$

	/** Indicates if the console messages should be printed to the console (System.out) */
	private boolean consoleLog = false;
	/** Indicates if the next log message is part of a new session */
	private boolean newSession = true;
	/**
	 * The File object to store messages.  This value may be null.
	 */
	private File outFile;

	/**
	 * The Writer to log messages to.
	 */
	private Writer writer;

	private final String loggerName;
	private final boolean enabled;
	private final EquinoxConfiguration environmentInfo;

	int maxLogSize = DEFAULT_LOG_SIZE; // The value is in KB.
	int maxLogFiles = DEFAULT_LOG_FILES;
	int backupIdx = 0;

	private int logLevel = FrameworkLogEntry.OK;
	private boolean includeCommandLine = true;

	private LoggerAdmin loggerAdmin = null;

	/**
	 * Controls whether the log file should be closed eagerly upon every log
	 * invocation.
	 * 
	 * Can be controlled by property
	 * {@link EquinoxLogWriter#PROP_CLOSE_LOG_FILE_EAGERLY}.
	 */
	private boolean closeLogFileEagerly = false;

	/**
	 * Constructs an EclipseLog which uses the specified File to log messages to
	 * @param outFile a file to log messages to
	 */
	public EquinoxLogWriter(File outFile, String loggerName, boolean enabled, EquinoxConfiguration environmentInfo) {
		this.outFile = outFile;
		this.writer = null;
		this.loggerName = loggerName;
		this.enabled = enabled;
		this.environmentInfo = environmentInfo;
		readLogProperties();
	}

	/**
	 * Constructs an EclipseLog which uses the specified Writer to log messages to
	 * @param writer a writer to log messages to
	 */
	public EquinoxLogWriter(Writer writer, String loggerName, boolean enabled, EquinoxConfiguration environmentInfo) {
		if (writer == null)
			// log to System.err by default
			this.writer = logForErrorStream();
		else
			this.writer = writer;
		this.loggerName = loggerName;
		this.enabled = enabled;
		this.environmentInfo = environmentInfo;
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
	 * @param header the header
	 * @param args the list of arguments
	 */
	private void writeArgs(String header, String[] args) throws IOException {
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

	/**
	 * Returns the session timestamp.  This is the time the platform was started
	 * @return the session timestamp
	 */
	private String getSessionTimestamp() {
		// Main should have set the session start-up timestamp so return that.
		// Return the "now" time if not available.
		String ts = environmentInfo.getConfiguration("eclipse.startTime"); //$NON-NLS-1$
		if (ts != null) {
			try {
				return getDate(new Date(Long.parseLong(ts)));
			} catch (NumberFormatException e) {
				// fall through and use the timestamp from right now
			}
		}
		return getDate(new Date());
	}

	/**
	 * Writes the session
	 * @throws IOException if an error occurs writing to the log
	 */
	private void writeSession() throws IOException {
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
			String value = environmentInfo.getConfiguration(key, "unknown"); //$NON-NLS-1$
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
		write("BootLoader constants: OS=" + environmentInfo.getOS()); //$NON-NLS-1$
		write(", ARCH=" + environmentInfo.getOSArch()); //$NON-NLS-1$
		write(", WS=" + environmentInfo.getWS()); //$NON-NLS-1$
		writeln(", NL=" + environmentInfo.getNL()); //$NON-NLS-1$
		// Add the command-line arguments used to invoke the platform
		// XXX: this includes runtime-private arguments - should we do that?
		if (includeCommandLine) {
			writeArgs("Framework arguments: ", environmentInfo.getNonFrameworkArgs()); //$NON-NLS-1$
			writeArgs("Command-line arguments: ", environmentInfo.getCommandLineArgs()); //$NON-NLS-1$
		}
	}

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

	/**
	 * If a File is used to log messages to then the File opened and a Writer is created
	 * to log messages to.
	 */
	private void openFile() {
		if (writer == null) {
			if (outFile != null) {
				try {
					writer = logForStream(ExtendedLogServiceFactory.secureAction.getFileOutputStream(outFile, true));
				} catch (IOException e) {
					writer = logForErrorStream();
				}
			} else {
				writer = logForErrorStream();
			}
		}
	}

	/**
	 * If a File is used to log messages to then the writer is closed.
	 */
	private void closeFile() {
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

	private synchronized void log(FrameworkLogEntry logEntry) {
		if (logEntry == null)
			return;
		if (!isLoggable(logEntry.getSeverity()))
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
			// close log file writer upon exceptions
			closeFile();
			// any exceptions during logging should be caught
			System.err.println("An exception occurred while writing to the platform log:");//$NON-NLS-1$
			e.printStackTrace(System.err);
			System.err.println("Logging to the console instead.");//$NON-NLS-1$
			//we failed to write, so dump log entry to console instead
			try {
				writer = logForErrorStream();
				writeLog(0, logEntry);
				writer.flush();
			} catch (Exception e2) {
				System.err.println("An exception occurred while logging to the console:");//$NON-NLS-1$
				e2.printStackTrace(System.err);
			} finally {
				// ensure that the error stream writer is closed
				closeFile();
			}
		} finally {
			if (closeLogFileEagerly) {
				closeFile();
			}
		}
	}

	public synchronized void setWriter(Writer newWriter, boolean append) {
		setOutput(null, newWriter, append);
	}

	/**
	 * @throws IOException
	 */
	public synchronized void setFile(File newFile, boolean append) throws IOException {
		if (newFile != null && !newFile.equals(this.outFile)) {
			// If it's a new file, then reset.
			readLogProperties();
			backupIdx = 0;
		}
		setOutput(newFile, null, append);
		environmentInfo.setConfiguration(EclipseStarter.PROP_LOGFILE, newFile == null ? "" : newFile.getAbsolutePath()); //$NON-NLS-1$
	}

	synchronized void setLoggerAdmin(LoggerAdmin loggerAdmin) {
		this.loggerAdmin = loggerAdmin;
		applyLogLevel();
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
			if (append && oldOutFile != null && oldOutFile.isFile()) {
				openFile();
				try (Reader fileIn = new InputStreamReader(
						ExtendedLogServiceFactory.secureAction.getFileInputStream(oldOutFile),
						StandardCharsets.UTF_8)) {
					copyReader(fileIn, this.writer);
					// delete the old file if copying didn't fail
					oldOutFile.delete();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					closeFile();
				}
			}
		}
	}

	/* XXX from JDK >=9 better use: reader.transferTo(this.aWriter); */
	private void copyReader(Reader reader, Writer aWriter) throws IOException {
		char buffer[] = new char[1024];
		int count;
		while ((count = reader.read(buffer, 0, buffer.length)) > 0) {
			aWriter.write(buffer, 0, count);
		}
	}

	/**
	 * Returns a date string using the correct format for the log.
	 * @param date the Date to format
	 * @return a date string.
	 */
	private String getDate(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		StringBuilder sb = new StringBuilder();
		appendPaddedInt(c.get(Calendar.YEAR), 4, sb).append('-');
		appendPaddedInt(c.get(Calendar.MONTH) + 1, 2, sb).append('-');
		appendPaddedInt(c.get(Calendar.DAY_OF_MONTH), 2, sb).append(' ');
		appendPaddedInt(c.get(Calendar.HOUR_OF_DAY), 2, sb).append(':');
		appendPaddedInt(c.get(Calendar.MINUTE), 2, sb).append(':');
		appendPaddedInt(c.get(Calendar.SECOND), 2, sb).append('.');
		appendPaddedInt(c.get(Calendar.MILLISECOND), 3, sb);
		return sb.toString();
	}

	private StringBuilder appendPaddedInt(int value, int pad, StringBuilder buffer) {
		pad = pad - 1;
		if (pad == 0)
			return buffer.append(Integer.toString(value));
		int padding = (int) Math.pow(10, pad);
		if (value >= padding)
			return buffer.append(Integer.toString(value));
		while (padding > value && padding > 1) {
			buffer.append('0');
			padding = padding / 10;
		}
		buffer.append(value);
		return buffer;
	}

	/**
	 * Returns a stacktrace string using the correct format for the log
	 * @param t the Throwable to get the stacktrace for
	 * @return a stacktrace string
	 */
	private String getStackTrace(Throwable t) {
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

	/**
	 * Returns a Writer for the given OutputStream
	 * @param output an OutputStream to use for the Writer
	 * @return a Writer for the given OutputStream
	 */
	private Writer logForStream(OutputStream output) {
		return new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
	}

	/**
	 * Always use the default encoding for System.err
	 */
	private Writer logForErrorStream() {
		return new BufferedWriter(new OutputStreamWriter(System.err));
	}

	/**
	 * Writes the log entry to the log using the specified depth.  A depth value of 0
	 * indicates that the log entry is the root entry.  Any value greater than 0 indicates
	 * a sub-entry.
	 * @param depth the depth of th entry
	 * @param entry the entry to log
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeLog(int depth, FrameworkLogEntry entry) throws IOException {
		writeEntry(depth, entry);
		writeMessage(entry);
		writeStack(entry);

		FrameworkLogEntry[] children = entry.getChildren();
		if (children != null) {
			for (FrameworkLogEntry child : children) {
				writeLog(depth + 1, child);
			}
		}
	}

	/**
	 * Writes the ENTRY or SUBENTRY header for an entry.  A depth value of 0
	 * indicates that the log entry is the root entry.  Any value greater than 0 indicates
	 * a sub-entry.
	 * @param depth the depth of th entry
	 * @param entry the entry to write the header for
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeEntry(int depth, FrameworkLogEntry entry) throws IOException {
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
		write(Integer.toString(entry.getSeverity()));
		writeSpace();
		write(Integer.toString(entry.getBundleCode()));
		writeSpace();
		write(getDate(new Date()));
		writeln();
	}

	/**
	 * Writes the MESSAGE header to the log for the given entry.
	 * @param entry the entry to write the message for
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeMessage(FrameworkLogEntry entry) throws IOException {
		write(MESSAGE);
		writeSpace();
		writeln(entry.getMessage());
	}

	/**
	 * Writes the STACK header to the log for the given entry.
	 * @param entry the entry to write the stacktrace for
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeStack(FrameworkLogEntry entry) throws IOException {
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

	/**
	 * Writes the given message to the log.
	 * @param message the message
	 * @throws IOException if any error occurs writing to the log
	 */
	private void write(String message) throws IOException {
		if (message != null) {
			writer.write(message);
			if (consoleLog)
				System.out.print(message);
		}
	}

	/**
	 * Writes the given message to the log and a newline.
	 * @param s the message
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeln(String s) throws IOException {
		write(s);
		writeln();
	}

	/**
	 * Writes a newline log.
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeln() throws IOException {
		write(LINE_SEPARATOR);
	}

	/**
	 * Writes a space to the log.
	 * @throws IOException if any error occurs writing to the log
	 */
	private void writeSpace() throws IOException {
		write(" "); //$NON-NLS-1$
	}

	/**
	 * Checks the log file size.  If the log file size reaches the limit then the log
	 * is rotated
	 * @return false if an error occured trying to rotate the log
	 */
	private boolean checkLogFileSize() {
		if (maxLogSize == 0)
			return true; // no size limitation.

		boolean isBackupOK = true;
		if (outFile != null) {
			if ((ExtendedLogServiceFactory.secureAction.length(outFile) >> 10) > maxLogSize) { // Use KB as file size unit.
				String logFilename = outFile.getAbsolutePath();

				// Delete old backup file that will be replaced.
				String backupFilename = ""; //$NON-NLS-1$
				if (logFilename.toLowerCase().endsWith(LOG_EXT)) {
					backupFilename = logFilename.substring(0, logFilename.length() - LOG_EXT.length()) + BACKUP_MARK + backupIdx + LOG_EXT;
				} else {
					backupFilename = logFilename + BACKUP_MARK + backupIdx;
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

	/**
	 * Reads the PROP_LOG_SIZE_MAX and PROP_LOG_FILE_MAX properties.
	 */
	private void readLogProperties() {
		String newMaxLogSize = environmentInfo.getConfiguration(PROP_LOG_SIZE_MAX);
		if (newMaxLogSize != null) {
			maxLogSize = Integer.parseInt(newMaxLogSize);
			if (maxLogSize != 0 && maxLogSize < LOG_SIZE_MIN) {
				// If the value is '0', then it means no size limitation.
				// Also, make sure no inappropriate(too small) assigned value.
				maxLogSize = LOG_SIZE_MIN;
			}
		}

		String newMaxLogFiles = environmentInfo.getConfiguration(PROP_LOG_FILE_MAX);
		if (newMaxLogFiles != null) {
			maxLogFiles = Integer.parseInt(newMaxLogFiles);
			if (maxLogFiles < 1) {
				// Make sure no invalid assigned value. (at least >= 1)
				maxLogFiles = DEFAULT_LOG_FILES;
			}
		}

		String newLogLevel = environmentInfo.getConfiguration(PROP_LOG_LEVEL);
		if (newLogLevel != null) {
			if (newLogLevel.equals("ERROR")) //$NON-NLS-1$
				logLevel = FrameworkLogEntry.ERROR;
			else if (newLogLevel.equals("WARNING")) //$NON-NLS-1$
				logLevel = FrameworkLogEntry.ERROR | FrameworkLogEntry.WARNING;
			else if (newLogLevel.equals("INFO")) //$NON-NLS-1$
				logLevel = FrameworkLogEntry.INFO | FrameworkLogEntry.ERROR | FrameworkLogEntry.WARNING | FrameworkLogEntry.CANCEL;
			else
				logLevel = FrameworkLogEntry.OK; // OK (0) means log everything
		}

		includeCommandLine = "true".equals(environmentInfo.getConfiguration(PROP_LOG_INCLUDE_COMMAND_LINE, "true")); //$NON-NLS-1$//$NON-NLS-2$
		applyLogLevel();

		String newCloseLogFileEagerlyValue = environmentInfo.getConfiguration(PROP_CLOSE_LOG_FILE_EAGERLY);
		if (newCloseLogFileEagerlyValue != null) {
			if (Boolean.valueOf(newCloseLogFileEagerlyValue) == Boolean.TRUE) {
				closeLogFileEagerly = true;
			}
		}
	}

	void applyLogLevel() {
		if (loggerAdmin == null) {
			return;
		}
		LoggerContext rootContext = loggerAdmin.getLoggerContext(null);
		Map<String, LogLevel> rootLevels = rootContext.getLogLevels();
		rootLevels.put(loggerName, getLogLevel());
		rootContext.setLogLevels(rootLevels);
	}

	private LogLevel getLogLevel() {
		if (logLevel == 0) {
			return LogLevel.TRACE;
		}
		if ((logLevel & FrameworkLogEntry.INFO) != 0) {
			return LogLevel.INFO;
		}
		if ((logLevel & FrameworkLogEntry.WARNING) != 0) {
			return LogLevel.WARN;
		}
		if ((logLevel & FrameworkLogEntry.ERROR) != 0) {
			return LogLevel.ERROR;
		}
		// not sure what to do here; it seems it would be an error
		return LogLevel.AUDIT;
	}

	/**
	 * Determines if the log entry should be logged based on log level.
	 */
	private boolean isLoggable(int fwkEntrySeverity) {
		if (logLevel == 0)
			return true;
		return (fwkEntrySeverity & logLevel) != 0;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isLoggable(Bundle bundle, String loggableName, int loggableLevel) {
		if (!enabled)
			return false;
		if (loggerName.equals(loggableName))
			return isLoggable(convertSeverity(loggableLevel));
		if (EquinoxLogServices.PERF_LOGGER_NAME.equals(loggableName))
			// we don't want to do anything with performance logger unless
			// this is the performance logger (check done above).
			return false;
		if (!EquinoxLogServices.EQUINOX_LOGGER_NAME.equals(loggerName))
			// only the equinox log writer should pay attention to other logs
			return false;
		// for now only log errors; probably need this to be configurable
		return loggableLevel == LogService.LOG_ERROR;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void logged(LogEntry entry) {
		if (!(entry instanceof ExtendedLogEntry))
			// TODO this should never happen
			return;
		ExtendedLogEntry extended = (ExtendedLogEntry) entry;
		Object context = extended.getContext();
		if (context instanceof FrameworkLogEntry) {
			log((FrameworkLogEntry) context);
			return;
		}
		// OK we are now in a case where someone logged a normal entry to the real LogService
		log(new FrameworkLogEntry(getFwkEntryTag(entry), convertSeverity(entry.getLevel()), 0, entry.getMessage(), 0, entry.getException(), null));
	}

	private static String getFwkEntryTag(LogEntry entry) {
		Bundle b = entry.getBundle();
		if (b != null && b.getSymbolicName() != null)
			return b.getSymbolicName();
		return "unknown"; //$NON-NLS-1$
	}

	@SuppressWarnings("deprecation")
	private static int convertSeverity(int entryLevel) {
		switch (entryLevel) {
			case LogService.LOG_ERROR :
				return FrameworkLogEntry.ERROR;
			case LogService.LOG_WARNING :
				return FrameworkLogEntry.WARNING;
			case LogService.LOG_INFO :
				return FrameworkLogEntry.INFO;
			case LogService.LOG_DEBUG :
				return FrameworkLogEntry.OK;
			default :
				return 32; // unknown
		}
	}

	public String getLoggerName() {
		return loggerName;
	}
}
