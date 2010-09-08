/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.debug;

import java.io.*;
import java.security.AccessController;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.service.debug.DebugTrace;

/**
 * The DebugTrace implementation for Eclipse.
 */
class EclipseDebugTrace implements DebugTrace {

	/** The system property used to specify size a trace file can grow before it is rotated */
	private static final String PROP_TRACE_SIZE_MAX = "eclipse.trace.size.max"; //$NON-NLS-1$
	/** The system property used to specify the maximum number of backup trace files to use */
	private static final String PROP_TRACE_FILE_MAX = "eclipse.trace.backup.max"; //$NON-NLS-1$
	/** The trace message for a thread stack dump */
	private final static String MESSAGE_THREAD_DUMP = "Thread Stack dump: "; //$NON-NLS-1$
	/** The trace message for a method completing with a return value */
	private final static String MESSAGE_EXIT_METHOD_WITH_RESULTS = "Exiting method {0}with result: "; //$NON-NLS-1$
	/** The trace message for a method completing with no return value */
	private final static String MESSAGE_EXIT_METHOD_NO_RESULTS = "Exiting method {0}with a void return"; //$NON-NLS-1$
	/** The trace message for a method starting with a set of arguments */
	private final static String MESSAGE_ENTER_METHOD_WITH_PARAMS = "Entering method {0}with parameters: ("; //$NON-NLS-1$
	/** The trace message for a method starting with no arguments */
	private final static String MESSAGE_ENTER_METHOD_NO_PARAMS = "Entering method {0}with no parameters"; //$NON-NLS-1$
	/** The version attribute written in the header of a new session */
	private final static String TRACE_FILE_VERSION_COMMENT = "version: "; //$NON-NLS-1$
	/** The verbose attribute written in the header of a new session */
	private final static String TRACE_FILE_VERBOSE_COMMENT = "verbose: "; //$NON-NLS-1$
	/** The version value written in the header of a new session */
	private final static String TRACE_FILE_VERSION = "1.1"; //$NON-NLS-1$
	/** The new session identifier to be written whenever a new session starts */
	private final static String TRACE_NEW_SESSION = "!SESSION "; //$NON-NLS-1$
	/** The date attribute written to the header of the trace file to show when this file was created */
	private final static String TRACE_FILE_DATE = "Time of creation: "; //$NON-NLS-1$
	/** Trace date formatter using the pattern: yyyy-MM-dd HH:mm:ss.SSS  */
	private final static SimpleDateFormat TRACE_FILE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
	/** The comment character used by the trace file */
	private final static String TRACE_COMMENT = "#"; //$NON-NLS-1$
	/** The delimiter used to separate trace elements such as the time stamp, message, etc */
	private final static String TRACE_ELEMENT_DELIMITER = "|"; //$NON-NLS-1$
	/** The string written in place of the {@link EclipseDebugTrace#TRACE_TRACE_ELEMENT_DELIMITER} in entries */
	private final static String TRACE_ELEMENT_DELIMITER_ENCODED = "&#124;"; //$NON-NLS-1$
	/** OS-specific line separator */
	private static final String LINE_SEPARATOR;
	static {
		String s = System.getProperty("line.separator"); //$NON-NLS-1$
		LINE_SEPARATOR = s == null ? "\n" : s; //$NON-NLS-1$
	}
	/** The value written to the trace file if a null object is being traced */
	private final static String NULL_VALUE = "<null>"; //$NON-NLS-1$
	/**  */
	private final static SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());
	/** A lock object used to synchronize access to the trace file */
	private final static Object writeLock = new Object();

	/******************* Tracing file attributes **************************/
	/** The default size a trace file can grow before it is rotated */
	private static final int DEFAULT_TRACE_FILE_SIZE = 1000; // The value is in KB.
	/** The default number of backup trace files */
	private static final int DEFAULT_TRACE_FILES = 10;
	/** The minimum size limit for trace file rotation */
	private static final int DEFAULT_TRACE_FILE_MIN_SIZE = 10;
	/** The extension used for log files */
	private static final String TRACE_FILE_EXTENSION = ".trace"; //$NON-NLS-1$
	/** The extension markup to use for backup log files*/
	private static final String BACKUP_MARK = ".bak_"; //$NON-NLS-1$
	/** The maximum size that a trace file should grow (0 = unlimited) */
	private int maxTraceFileSize = DEFAULT_TRACE_FILE_SIZE; // The value is in KB.
	/** The maximum number of trace files that should be saved */
	private int maxTraceFiles = DEFAULT_TRACE_FILES;
	/** The index of the currently backed-up trace file */
	private int backupTraceFileIndex = 0;

	/** An optional argument to specify the name of the class used by clients to trace messages.  If no trace class is specified
	 * then the class calling this API is assumed to be the class being traced.
	*/
	private String traceClass = null;
	/** The symbolic name of the bundle being traced */
	private String bundleSymbolicName = null;
	/** A flag to determine if the message being written is done to a new file (i.e. should the header information be written) */
	static boolean newSession = true;
	/** DebugOptions are used to determine if the specified bundle symbolic name + option-path has debugging enabled */
	private FrameworkDebugOptions debugOptions = null;

	/**
	 * Construct a new EclipseDebugTrace for the specified bundle symbolic name and write messages to the specified
	 * trace file.  The DebugOptions object will be used to determine if tracing should occur.  
	 * 
	 * @param bundleSymbolicName The symbolic name of the bundle being traced
	 * @param debugOptions Used to determine if the specified bundle symbolic name + option-path has tracing enabled
	 */
	EclipseDebugTrace(final String bundleSymbolicName, final FrameworkDebugOptions debugOptions) {

		this(bundleSymbolicName, debugOptions, null);
	}

	/**
	 * Construct a new EclipseDebugTrace for the specified bundle symbolic name and write messages to the specified
	 * trace file.  
	 * 
	 * @param bundleSymbolicName The symbolic name of the bundle being traced
	 * @param debugOptions Used to determine if the specified bundle symbolic name + option-path has tracing enabled
	 * @param traceClass The class that the client is using to perform trace API calls
	 */
	EclipseDebugTrace(final String bundleSymbolicName, final FrameworkDebugOptions debugOptions, final Class<?> traceClass) {

		this.traceClass = traceClass != null ? traceClass.getName() : null;
		this.debugOptions = debugOptions;
		this.bundleSymbolicName = bundleSymbolicName;
		readLogProperties();
	}

	/**
	 * Is debugging enabled for the specified option-path
	 * 
	 * @param optionPath The <i>option-path</i>
	 * @return Returns true if debugging is enabled for the specified option-path on this bundle; Otherwise false.
	 */
	private final boolean isDebuggingEnabled(final String optionPath) {
		if (optionPath == null)
			return true;
		boolean debugEnabled = false;
		if (debugOptions.isDebugEnabled()) {
			final String option = bundleSymbolicName + optionPath;
			debugEnabled = debugOptions.getBooleanOption(option, false);
		}
		return debugEnabled;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#trace(java.lang.String, java.lang.String)
	 */
	public void trace(final String optionPath, final String message) {

		if (isDebuggingEnabled(optionPath)) {
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(bundleSymbolicName, optionPath, message, traceClass);
			writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#trace(java.lang.String, java.lang.String, java.lang.Throwable)
	 */
	public void trace(final String optionPath, final String message, final Throwable error) {

		if (isDebuggingEnabled(optionPath)) {
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(bundleSymbolicName, optionPath, message, error, traceClass);
			writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceEntry(java.lang.String)
	 */
	public void traceEntry(final String optionPath) {

		if (isDebuggingEnabled(optionPath)) {
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(bundleSymbolicName, optionPath, null, traceClass);
			setMessage(record, EclipseDebugTrace.MESSAGE_ENTER_METHOD_NO_PARAMS);
			writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceEntry(java.lang.String, java.lang.Object)
	 */
	public void traceEntry(final String optionPath, final Object methodArgument) {

		if (isDebuggingEnabled(optionPath)) {
			traceEntry(optionPath, new Object[] {methodArgument});
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceEntry(java.lang.String, java.lang.Object[])
	 */
	public void traceEntry(final String optionPath, final Object[] methodArguments) {

		if (isDebuggingEnabled(optionPath)) {
			final StringBuffer messageBuffer = new StringBuffer(EclipseDebugTrace.MESSAGE_ENTER_METHOD_WITH_PARAMS);
			if (methodArguments != null) {
				int i = 0;
				while (i < methodArguments.length) {
					if (methodArguments[i] != null) {
						messageBuffer.append(methodArguments[i].toString());
					} else {
						messageBuffer.append(EclipseDebugTrace.NULL_VALUE);
					}
					i++;
					if (i < methodArguments.length) {
						messageBuffer.append(" "); //$NON-NLS-1$
					}
				}
				messageBuffer.append(")"); //$NON-NLS-1$
			}
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(bundleSymbolicName, optionPath, null, traceClass);
			setMessage(record, messageBuffer.toString());
			writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceExit(java.lang.String)
	 */
	public void traceExit(final String optionPath) {

		if (isDebuggingEnabled(optionPath)) {
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(bundleSymbolicName, optionPath, null, traceClass);
			setMessage(record, EclipseDebugTrace.MESSAGE_EXIT_METHOD_NO_RESULTS);
			writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceExit(java.lang.String, java.lang.Object)
	 */
	public void traceExit(final String optionPath, final Object result) {

		if (isDebuggingEnabled(optionPath)) {
			final StringBuffer messageBuffer = new StringBuffer(EclipseDebugTrace.MESSAGE_EXIT_METHOD_WITH_RESULTS);
			if (result == null) {
				messageBuffer.append(EclipseDebugTrace.NULL_VALUE);
			} else {
				messageBuffer.append(result.toString());
			}
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(bundleSymbolicName, optionPath, null, traceClass);
			setMessage(record, messageBuffer.toString());
			writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceDumpStack(java.lang.String)
	 */
	public void traceDumpStack(final String optionPath) {

		if (isDebuggingEnabled(optionPath)) {
			final StringBuffer messageBuffer = new StringBuffer(EclipseDebugTrace.MESSAGE_THREAD_DUMP);
			StackTraceElement[] elements = new Exception().getStackTrace();
			// the first element in this stack trace is going to be this class, so ignore it
			// the second element in this stack trace is going to either be the caller or the trace class.  Ignore it only if a traceClass is defined
			// the rest of the elements should be included in the file array
			int firstIndex = (traceClass == null) ? 1 : 2;
			int endIndex = elements.length - firstIndex;
			final StackTraceElement[] newElements = new StackTraceElement[endIndex];
			int i = 0;
			while (i < endIndex) {
				newElements[i] = elements[firstIndex];
				i++;
				firstIndex++;
			}
			messageBuffer.append(convertStackTraceElementsToString(newElements));
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(bundleSymbolicName, optionPath, messageBuffer.toString(), traceClass);
			writeRecord(record);
		}
	}

	/**
	 * Set the trace message for the specified record to include class and method information
	 * if verbose debugging is disabled.
	 *  
	 * @param record The {@link FrameworkDebugTraceEntry} containing the information to persist to the trace file.
	 * @param originalMessage The original tracing message
	 */
	private final void setMessage(final FrameworkDebugTraceEntry record, final String originalMessage) {

		String argument = null;
		if (!debugOptions.isVerbose()) {
			final StringBuffer classMethodName = new StringBuffer(record.getClassName());
			classMethodName.append("#"); //$NON-NLS-1$
			classMethodName.append(record.getMethodName());
			classMethodName.append(" "); //$NON-NLS-1$
			argument = classMethodName.toString();
		} else {
			argument = ""; //$NON-NLS-1$
		}
		String newMessage = MessageFormat.format(originalMessage, new Object[] {argument});
		record.setMessage(newMessage);
	}

	/**
	 * Utility method to convert an array of StackTraceElement objects to form a String representation of a stack dump
	 * 
	 * @param elements
	 *            The array of StackTraceElement objects
	 * @return A String of the stack dump produced by the list of elements
	 */
	private final String convertStackTraceElementsToString(final StackTraceElement[] elements) {

		final StringBuffer buffer = new StringBuffer();
		if (elements != null) {
			buffer.append("java.lang.Throwable: "); //$NON-NLS-1$
			buffer.append(EclipseDebugTrace.LINE_SEPARATOR);
			int i = 0;
			while (i < elements.length) {
				if (elements[i] != null) {
					buffer.append("\tat "); //$NON-NLS-1$
					buffer.append(elements[i].toString());
					buffer.append(EclipseDebugTrace.LINE_SEPARATOR);
				}
				i++;
			}
		}
		return buffer.toString();
	}

	/**
	 * Write the specified FrameworkTraceEntry to trace file
	 * 
	 * @param entry The FrameworkTraceEntry to write to the log file.
	 */
	private void writeRecord(final FrameworkDebugTraceEntry entry) {

		if (entry != null) {
			synchronized (EclipseDebugTrace.writeLock) {
				final File tracingFile = debugOptions.getFile(); // the tracing file may be null if it has not been set
				Writer traceWriter = null;
				try {
					// check to see if the file should be rotated
					checkTraceFileSize(tracingFile, entry.getTimestamp());
					// open the trace file
					traceWriter = openWriter(tracingFile);
					if (EclipseDebugTrace.newSession) {
						writeSession(traceWriter, entry.getTimestamp());
						EclipseDebugTrace.newSession = false;
					}
					writeMessage(traceWriter, entry);
					// flush the writer
					traceWriter.flush();
				} catch (Exception ex) {
					// any exceptions during tracing should be caught 
					System.err.println("An exception occurred while writing to the platform trace file: ");//$NON-NLS-1$
					ex.printStackTrace(System.err);
				} finally {
					// close the trace writer
					if (tracingFile != null) {
						closeWriter(traceWriter);
					}
				}
			}
		}
	}

	/**
	 * Reads the PROP_TRACE_SIZE_MAX and PROP_TRACE_FILE_MAX properties.
	 */
	private void readLogProperties() {

		String newMaxTraceFileSize = secureAction.getProperty(PROP_TRACE_SIZE_MAX);
		if (newMaxTraceFileSize != null) {
			maxTraceFileSize = Integer.parseInt(newMaxTraceFileSize);
			if (maxTraceFileSize != 0 && maxTraceFileSize < DEFAULT_TRACE_FILE_MIN_SIZE) {
				// If the value is '0', then it means no size limitation.
				// Also, make sure no inappropriate(too small) assigned value.
				maxTraceFileSize = DEFAULT_TRACE_FILE_MIN_SIZE;
			}
		}

		String newMaxLogFiles = secureAction.getProperty(PROP_TRACE_FILE_MAX);
		if (newMaxLogFiles != null) {
			maxTraceFiles = Integer.parseInt(newMaxLogFiles);
			if (maxTraceFiles < 1) {
				// Make sure no invalid assigned value. (at least >= 1)
				maxTraceFiles = DEFAULT_TRACE_FILES;
			}
		}
	}

	/**
	 * Checks the trace file size.  If the file size reaches the limit then the trace file is rotated. 
	 * 
	 * @param traceFile The tracing file
	 * @param timestamp the timestamp for the session; this is the same timestamp as the first entry
	 * @return false if an error occurred trying to rotate the trace file
	 */
	private boolean checkTraceFileSize(final File traceFile, long timestamp) {

		// 0 file size means there is no size limit
		boolean isBackupOK = true;
		if (maxTraceFileSize > 0) {
			if ((traceFile != null) && traceFile.exists()) {
				if ((traceFile.length() >> 10) > maxTraceFileSize) { // Use KB as file size unit.
					final String traceFileName = traceFile.getAbsolutePath();

					// Delete old backup file that will be replaced.
					String backupFilename = ""; //$NON-NLS-1$
					if (traceFileName.toLowerCase().endsWith(TRACE_FILE_EXTENSION)) {
						backupFilename = traceFileName.substring(0, traceFileName.length() - TRACE_FILE_EXTENSION.length()) + BACKUP_MARK + backupTraceFileIndex + TRACE_FILE_EXTENSION;
					} else {
						backupFilename = traceFileName + BACKUP_MARK + backupTraceFileIndex;
					}
					final File backupFile = new File(backupFilename);
					if (backupFile.exists()) {
						if (!backupFile.delete()) {
							System.err.println("Error when trying to delete old trace file: " + backupFile.getName());//$NON-NLS-1$ 
							if (backupFile.renameTo(new File(backupFile.getAbsolutePath() + System.currentTimeMillis()))) {
								System.err.println("So we rename it to filename: " + backupFile.getName()); //$NON-NLS-1$
							} else {
								System.err.println("And we also cannot rename it!"); //$NON-NLS-1$
								isBackupOK = false;
							}
						}
					}

					// Rename current log file to backup one.
					boolean isRenameOK = traceFile.renameTo(backupFile);
					if (!isRenameOK) {
						System.err.println("Error when trying to rename trace file to backup one."); //$NON-NLS-1$
						isBackupOK = false;
					}
					/*
					 * Write a header to new log file stating that this new file is a continuation file. 
					 * This method should already be called with the file lock set so we should be safe 
					 * to update it here.
					*/
					Writer traceWriter = null;
					try {
						traceWriter = openWriter(traceFile);
						writeComment(traceWriter, "This is a continuation of trace file " + backupFile.getAbsolutePath()); //$NON-NLS-1$
						writeComment(traceWriter, EclipseDebugTrace.TRACE_FILE_VERSION_COMMENT + EclipseDebugTrace.TRACE_FILE_VERSION);
						writeComment(traceWriter, EclipseDebugTrace.TRACE_FILE_VERBOSE_COMMENT + debugOptions.isVerbose());
						writeComment(traceWriter, EclipseDebugTrace.TRACE_FILE_DATE + getFormattedDate(timestamp));
						traceWriter.flush();
					} catch (IOException ioEx) {
						ioEx.printStackTrace();
					} finally {
						if (traceFile != null) {
							closeWriter(traceWriter);
						}
					}
					backupTraceFileIndex = (++backupTraceFileIndex) % maxTraceFiles;
				}
			}
		}
		return isBackupOK;
	}

	/**
	 * Writes a comment to the trace file
	 *
	 * @param traceWriter the trace writer
	 * @param comment the comment to be written to the trace file
	 * @throws IOException If an error occurs while writing the comment
	 */
	private void writeComment(final Writer traceWriter, final String comment) throws IOException {

		StringBuffer commentText = new StringBuffer(EclipseDebugTrace.TRACE_COMMENT);
		commentText.append(" "); //$NON-NLS-1$
		commentText.append(comment);
		commentText.append(EclipseDebugTrace.LINE_SEPARATOR);
		traceWriter.write(commentText.toString());
	}

	/**
	 * Accessor to retrieve the time stamp in a formatted manner.
	 * 
	 * @return A formatted time stamp based on the {@link EclipseDebugTrace#TRACE_FILE_DATE_FORMATTER} formatter
	 */
	private final String getFormattedDate(long timestamp) {

		return EclipseDebugTrace.TRACE_FILE_DATE_FORMATTER.format(new Date(timestamp));
	}

	/**
	 * Accessor to retrieve the text of a {@link Throwable} in a formatted manner so that it can be written to the
	 * trace file. 
	 * 
	 * @param error The {@lnk Throwable} to format
	 * @return The complete text of a {@link Throwable} as a {@link String} or null if the input error is null.
	 */
	private final String getFormattedThrowable(Throwable error) {

		String result = null;
		if (error != null) {
			PrintStream throwableStream = null;
			try {
				ByteArrayOutputStream throwableByteOutputStream = new ByteArrayOutputStream();
				throwableStream = new PrintStream(throwableByteOutputStream, false);
				error.printStackTrace(throwableStream);
				result = encodeText(throwableByteOutputStream.toString());
			} finally {
				if (throwableStream != null) {
					throwableStream.close();
				}
			}
		}
		return result;
	}

	/**
	 * Writes header information to a new trace file
	 * 
	 * @param traceWriter the trace writer
	 * @param timestamp the timestamp for the session; this is the same timestamp as the first entry
	 * @throws IOException If an error occurs while writing this session information 
	 */
	private void writeSession(final Writer traceWriter, long timestamp) throws IOException {

		writeComment(traceWriter, EclipseDebugTrace.TRACE_NEW_SESSION + this.getFormattedDate(timestamp));
		writeComment(traceWriter, EclipseDebugTrace.TRACE_FILE_VERSION_COMMENT + EclipseDebugTrace.TRACE_FILE_VERSION);
		writeComment(traceWriter, EclipseDebugTrace.TRACE_FILE_VERBOSE_COMMENT + debugOptions.isVerbose());
		writeComment(traceWriter, "The following option strings are specified for this debug session:"); //$NON-NLS-1$ 
		final String[] allOptions = FrameworkDebugOptions.getDefault().getAllOptions();
		for (int i = 0; i < allOptions.length; i++) {
			writeComment(traceWriter, "\t" + allOptions[i]); //$NON-NLS-1$
		}
	}

	/**
	 * Writes the specified trace entry object to the trace file using the 
	 * {@link EclipseDebugTrace#TRACE_ELEMENT_DELIMITER} as the delimiter between
	 * each element of the entry.
	 * 
	 * @param traceWriter the trace writer
	 * @param entry The trace entry object to write to the trace file
	 * @throws IOException If an error occurs while writing this message
	 */
	private void writeMessage(final Writer traceWriter, final FrameworkDebugTraceEntry entry) throws IOException {

		final StringBuffer message = new StringBuffer(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
		message.append(" "); //$NON-NLS-1$
		message.append(encodeText(entry.getThreadName()));
		message.append(" "); //$NON-NLS-1$
		message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
		message.append(" "); //$NON-NLS-1$
		message.append(this.getFormattedDate(entry.getTimestamp()));
		message.append(" "); //$NON-NLS-1$
		message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
		message.append(" "); //$NON-NLS-1$
		if (!debugOptions.isVerbose()) {
			// format the trace entry for quiet tracing: only the thread name, timestamp, trace message, and exception (if necessary)
			message.append(encodeText(entry.getMessage()));
		} else {
			// format the trace entry for verbose tracing
			message.append(entry.getBundleSymbolicName());
			message.append(" "); //$NON-NLS-1$
			message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
			message.append(" "); //$NON-NLS-1$
			message.append(encodeText(entry.getOptionPath()));
			message.append(" "); //$NON-NLS-1$
			message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
			message.append(" "); //$NON-NLS-1$
			message.append(entry.getClassName());
			message.append(" "); //$NON-NLS-1$
			message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
			message.append(" "); //$NON-NLS-1$
			message.append(entry.getMethodName());
			message.append(" "); //$NON-NLS-1$
			message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
			message.append(" "); //$NON-NLS-1$
			message.append(entry.getLineNumber());
			message.append(" "); //$NON-NLS-1$
			message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
			message.append(" "); //$NON-NLS-1$
			message.append(encodeText(entry.getMessage()));
		}
		if (entry.getThrowable() != null) {
			message.append(" "); //$NON-NLS-1$
			message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
			message.append(" "); //$NON-NLS-1$
			message.append(this.getFormattedThrowable(entry.getThrowable()));
		}
		message.append(" "); //$NON-NLS-1$
		message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
		message.append(EclipseDebugTrace.LINE_SEPARATOR);
		// write the message
		if ((traceWriter != null) && (message != null)) {
			traceWriter.write(message.toString());
		}
	}

	/**
	 * Encodes the specified string to replace any occurrence of the {@link EclipseDebugTrace#TRACE_ELEMENT_DELIMITER}
	 * string with the {@link EclipseDebugTrace#TRACE_ELEMENT_DELIMITER_ENCODED}
	 * string.  This can be used to ensure that the delimiter character does not break parsing when
	 * the entry text contains the delimiter character. 
	 * 
	 * @param inputString The original string to be written to the trace file. 
	 * @return The original input string with all occurrences of
	 * {@link EclipseDebugTrace#TRACE_ELEMENT_DELIMITER} replaced with 
	 * {@link EclipseDebugTrace#TRACE_ELEMENT_DELIMITER_ENCODED}. A <code>null</code> value will be
	 * returned if the input string is <code>null</code>.
	 */
	private static String encodeText(final String inputString) {
		if (inputString == null || inputString.indexOf(TRACE_ELEMENT_DELIMITER) < 0)
			return inputString;
		final StringBuffer tempBuffer = new StringBuffer(inputString);
		int currentIndex = tempBuffer.indexOf(TRACE_ELEMENT_DELIMITER);
		while (currentIndex >= 0) {
			tempBuffer.replace(currentIndex, currentIndex + TRACE_ELEMENT_DELIMITER.length(), TRACE_ELEMENT_DELIMITER_ENCODED);
			currentIndex = tempBuffer.indexOf(TRACE_ELEMENT_DELIMITER);
		}
		return tempBuffer.toString();
	}

	/**
	 * Returns a Writer for the given OutputStream
	 * @param output an OutputStream to use for the Writer
	 * @return A Writer for the given OutputStream
	 */
	private Writer logForStream(OutputStream output) {

		try {
			return new BufferedWriter(new OutputStreamWriter(output, "UTF-8")); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			return new BufferedWriter(new OutputStreamWriter(output));
		}
	}

	/**
	 * Creates the trace writer.
	 * If the tracing file is null then the writer will use System.out to print any messages.
	 * 
	 * @param traceFile The tracing file
	 * @return Returns a new Writer object  
	 */
	private Writer openWriter(final File traceFile) {

		Writer traceWriter = null;
		if (traceFile != null) {
			try {
				traceWriter = logForStream(secureAction.getFileOutputStream(traceFile, true));
			} catch (IOException ioEx) {
				traceWriter = logForStream(System.out);
			}
		} else {
			traceWriter = logForStream(System.out);
		}
		return traceWriter;
	}

	/**
	 * Close the trace writer
	 * 
	 * @param traceWriter The trace writer
	 */
	private void closeWriter(Writer traceWriter) {

		if (traceWriter != null) {
			try {
				traceWriter.close();
			} catch (IOException ioEx) {
				// we cannot log here; just print the stacktrace.
				ioEx.printStackTrace();
			}
			traceWriter = null;
		}
	}
}