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
package org.eclipse.osgi.framework.debug;

import java.io.*;
import java.security.AccessController;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;

/**
 * The DebugTrace implementation for Eclipse.
 * <p>
 * Clients may extend this class.
 * </p>
 * @since 3.5
 */
public class EclipseDebugTrace implements DebugTrace {

	/** The system property used to specify size a trace file can grow before it is rotated */
	public static final String PROP_TRACE_SIZE_MAX = "eclipse.trace.size.max"; //$NON-NLS-1$
	/** The system property used to specify the maximum number of backup trace files to use */
	public static final String PROP_TRACE_FILE_MAX = "eclipse.trace.backup.max"; //$NON-NLS-1$

	/**
	 * Construct a new EclipseDebugTrace for the specified bundle symbolic name and write messages to the specified
	 * trace file.  The DebugOptions object will be used to determine if tracing should occur.  
	 * 
	 * @param bundleSymbolicName The symbolic name of the bundle being traced
	 * @param debugOptions Used to determine if the specified bundle symbolic name + option-path has tracing enabled
	 */
	public EclipseDebugTrace(final String bundleSymbolicName, final DebugOptions debugOptions) {

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
	public EclipseDebugTrace(final String bundleSymbolicName, final DebugOptions debugOptions, final Class traceClass) {

		this.traceClass = traceClass;
		this.debugOptions = debugOptions;
		this.bundleSymbolicName = bundleSymbolicName;
		this.readLogProperties();
	}

	/**
	 * Is debugging enabled for the specified option-path
	 * 
	 * @param optionPath The <i>option-path</i>
	 * @return Returns true if debugging is enabled for the specified option-path on this bundle; Otherwise false.
	 */
	protected final boolean isDebuggingEnabled(final String optionPath) {
		if (optionPath == null)
			return true;
		boolean debugEnabled = false;
		if (this.debugOptions.isDebugEnabled()) {
			final String option = this.bundleSymbolicName + optionPath;
			debugEnabled = this.debugOptions.getBooleanOption(option, false);
		}
		return debugEnabled;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#trace(java.lang.String, java.lang.String)
	 */
	public void trace(final String optionPath, final String message) {

		if (this.isDebuggingEnabled(optionPath)) {
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(this.bundleSymbolicName, optionPath, message, this.traceClass);
			this.writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#trace(java.lang.String, java.lang.String, java.lang.Throwable)
	 */
	public void trace(final String optionPath, final String message, final Throwable error) {

		if (this.isDebuggingEnabled(optionPath)) {
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(this.bundleSymbolicName, optionPath, message, error, this.traceClass);
			this.writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceEntry(java.lang.String)
	 */
	public void traceEntry(final String optionPath) {

		if (this.isDebuggingEnabled(optionPath)) {
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(this.bundleSymbolicName, optionPath, EclipseDebugTrace.MESSAGE_ENTER_METHOD_NO_PARAMS, this.traceClass);
			this.writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceEntry(java.lang.String, java.lang.Object)
	 */
	public void traceEntry(final String optionPath, final Object methodArgument) {

		if (this.isDebuggingEnabled(optionPath)) {
			this.traceEntry(optionPath, new Object[] {methodArgument});
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceEntry(java.lang.String, java.lang.Object[])
	 */
	public void traceEntry(final String optionPath, final Object[] methodArguments) {

		if (this.isDebuggingEnabled(optionPath)) {
			final StringBuffer messageBuffer = new StringBuffer(EclipseDebugTrace.MESSAGE_ENTER_METHOD_WITH_PARAMS);
			if (methodArguments != null) {
				int i = 0;
				while (i < methodArguments.length) {
					if (methodArguments[i] != null) {
						messageBuffer.append(methodArguments[i].toString());
					} else {
						messageBuffer.append(EclipseDebugTrace.NULL_VALUE);
					}
					messageBuffer.append(" "); //$NON-NLS-1$
					i++;
				}
				messageBuffer.append(")"); //$NON-NLS-1$
			}
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(this.bundleSymbolicName, optionPath, messageBuffer.toString(), this.traceClass);
			this.writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceExit(java.lang.String)
	 */
	public void traceExit(final String optionPath) {

		if (this.isDebuggingEnabled(optionPath)) {
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(this.bundleSymbolicName, optionPath, EclipseDebugTrace.MESSAGE_EXIT_METHOD_NO_RESULTS, this.traceClass);
			this.writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceExit(java.lang.String, java.lang.Object)
	 */
	public void traceExit(final String optionPath, final Object result) {

		if (this.isDebuggingEnabled(optionPath)) {
			final StringBuffer messageBuffer = new StringBuffer(EclipseDebugTrace.MESSAGE_EXIT_METHOD_WITH_RESULTS);
			if (result == null) {
				messageBuffer.append(EclipseDebugTrace.NULL_VALUE);
			} else {
				messageBuffer.append(result.toString());
			}
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(this.bundleSymbolicName, optionPath, messageBuffer.toString(), this.traceClass);
			this.writeRecord(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.framework.debug.FrameworkDebugTrace#traceDumpStack(java.lang.String)
	 */
	public void traceDumpStack(final String optionPath) {

		if (this.isDebuggingEnabled(optionPath)) {
			final StringBuffer messageBuffer = new StringBuffer(EclipseDebugTrace.MESSAGE_THREAD_DUMP);
			StackTraceElement[] elements = new Exception().getStackTrace();
			// the first element in this stack trace is going to be this class, so ignore it
			// the second element in this stack trace is going to either be the caller or the trace class.  Ignore it only if a traceClass is defined
			// the rest of the elements should be included in the file array
			int firstIndex = (this.traceClass == null) ? 1 : 2;
			int endIndex = elements.length - firstIndex;
			final StackTraceElement[] newElements = new StackTraceElement[endIndex];
			int i = 0;
			while (i < endIndex) {
				newElements[i] = elements[firstIndex];
				i++;
				firstIndex++;
			}
			messageBuffer.append(this.convertStackTraceElementsToString(newElements));
			final FrameworkDebugTraceEntry record = new FrameworkDebugTraceEntry(this.bundleSymbolicName, optionPath, messageBuffer.toString(), this.traceClass);
			this.writeRecord(record);
		}
	}

	/**
	 * Utility method to convert an array of StackTraceElement objects to form a String representation of a stack dump
	 * 
	 * @param elements
	 *            The array of StackTraceElement objects
	 * @return A String of the stack dump produced by the list of elements
	 */
	protected final String convertStackTraceElementsToString(final StackTraceElement[] elements) {

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
	protected void writeRecord(final FrameworkDebugTraceEntry entry) {

		if (entry != null) {
			synchronized (EclipseDebugTrace.writeLock) {
				final File tracingFile = this.debugOptions.getFile(); // the tracing file may be null if it has not been set
				Writer traceWriter = null;
				try {
					// check to see if the file should be rotated
					this.checkTraceFileSize(tracingFile);
					// open the trace file
					traceWriter = this.openWriter(tracingFile);
					if (EclipseDebugTrace.newSession) {
						this.writeSession(traceWriter);
						EclipseDebugTrace.newSession = false;
					}
					this.writeMessage(traceWriter, entry);
					// flush the writer
					traceWriter.flush();
				} catch (Exception ex) {
					// any exceptions during tracing should be caught 
					System.err.println("An exception occurred while writing to the platform trace file: ");//$NON-NLS-1$
					ex.printStackTrace(System.err);
				} finally {
					// close the trace writer
					if (tracingFile != null) {
						this.closeWriter(traceWriter);
					}
				}
			}
		}
	}

	/**
	 * Reads the PROP_TRACE_SIZE_MAX and PROP_TRACE_FILE_MAX properties.
	 */
	protected void readLogProperties() {

		String newMaxTraceFileSize = secureAction.getProperty(PROP_TRACE_SIZE_MAX);
		if (newMaxTraceFileSize != null) {
			this.maxTraceFileSize = Integer.parseInt(newMaxTraceFileSize);
			if (this.maxTraceFileSize != 0 && this.maxTraceFileSize < DEFAULT_TRACE_FILE_MIN_SIZE) {
				// If the value is '0', then it means no size limitation.
				// Also, make sure no inappropriate(too small) assigned value.
				this.maxTraceFileSize = DEFAULT_TRACE_FILE_MIN_SIZE;
			}
		}

		String newMaxLogFiles = secureAction.getProperty(PROP_TRACE_FILE_MAX);
		if (newMaxLogFiles != null) {
			this.maxTraceFiles = Integer.parseInt(newMaxLogFiles);
			if (this.maxTraceFiles < 1) {
				// Make sure no invalid assigned value. (at least >= 1)
				this.maxTraceFiles = DEFAULT_TRACE_FILES;
			}
		}
	}

	/**
	 * Checks the trace file size.  If the file size reaches the limit then the trace file is rotated. 
	 * 
	 * @param traceFile The tracing file
	 * @return false if an error occurred trying to rotate the trace file
	 */
	protected boolean checkTraceFileSize(final File traceFile) {

		// 0 file size means there is no size limit
		boolean isBackupOK = true;
		if (this.maxTraceFileSize > 0) {
			if ((traceFile != null) && traceFile.exists()) {
				if ((traceFile.length() >> 10) > this.maxTraceFileSize) { // Use KB as file size unit.
					final String traceFileName = traceFile.getAbsolutePath();

					// Delete old backup file that will be replaced.
					String backupFilename = ""; //$NON-NLS-1$
					if (traceFileName.toLowerCase().endsWith(TRACE_FILE_EXTENSION)) {
						backupFilename = traceFileName.substring(0, traceFileName.length() - TRACE_FILE_EXTENSION.length()) + BACKUP_MARK + this.backupTraceFileIndex + TRACE_FILE_EXTENSION;
					} else {
						backupFilename = traceFileName + BACKUP_MARK + this.backupTraceFileIndex;
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
						traceWriter = this.openWriter(traceFile);
						this.writeComment(traceWriter, "This is a continuation of trace file " + backupFile.getAbsolutePath()); //$NON-NLS-1$
						this.writeComment(traceWriter, EclipseDebugTrace.TRACE_FILE_DATE + EclipseDebugTrace.TRACE_FILE_DATE_FORMATTER.format(new Date(System.currentTimeMillis())));
						traceWriter.flush();
					} catch (IOException ioEx) {
						ioEx.printStackTrace();
					} finally {
						if (traceFile != null) {
							this.closeWriter(traceWriter);
						}
					}
					this.backupTraceFileIndex = (++this.backupTraceFileIndex) % this.maxTraceFiles;
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
	protected void writeComment(final Writer traceWriter, final String comment) throws IOException {

		StringBuffer commentText = new StringBuffer(EclipseDebugTrace.TRACE_COMMENT);
		commentText.append(" "); //$NON-NLS-1$
		commentText.append(comment);
		commentText.append(EclipseDebugTrace.LINE_SEPARATOR);
		traceWriter.write(commentText.toString());
	}

	/**
	 * Accessor to retrieve the current date and time in a formatted manner.
	 * 
	 * @return A formatted time stamp based on the {@link EclipseDebugTrace#TRACE_FILE_DATE_FORMATTER} formatter
	 */
	protected final String getFormattedDate() {

		return this.getFormattedDate(System.currentTimeMillis());
	}

	/**
	 * Accessor to retrieve the time stamp in a formatted manner.
	 * 
	 * @return A formatted time stamp based on the {@link EclipseDebugTrace#TRACE_FILE_DATE_FORMATTER} formatter
	 */
	protected final String getFormattedDate(long timestamp) {

		return EclipseDebugTrace.TRACE_FILE_DATE_FORMATTER.format(new Date(timestamp));
	}

	/**
	 * Writes header information to a new trace file
	 * 
	 * @param traceWriter the trace writer
	 * @throws IOException If an error occurs while writing this session information 
	 */
	protected void writeSession(final Writer traceWriter) throws IOException {

		this.writeComment(traceWriter, EclipseDebugTrace.TRACE_NEW_SESSION + this.getFormattedDate());
		this.writeComment(traceWriter, EclipseDebugTrace.TRACE_FILE_VERSION_COMMENT + EclipseDebugTrace.TRACE_FILE_VERSION);
		this.writeComment(traceWriter, "The following option strings are specified for this debug session:"); //$NON-NLS-1$ 
		final String[] allOptions = FrameworkDebugOptions.getDefault().getAllOptions();
		for (int i = 0; i < allOptions.length; i++) {
			this.writeComment(traceWriter, "\t" + allOptions[i]); //$NON-NLS-1$
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
	protected void writeMessage(final Writer traceWriter, final FrameworkDebugTraceEntry entry) throws IOException {

		// format the trace entry
		StringBuffer message = new StringBuffer(entry.getThreadName());
		message.append(" "); //$NON-NLS-1$
		message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
		message.append(" "); //$NON-NLS-1$
		message.append(this.getFormattedDate(entry.getTimestamp()));
		message.append(" "); //$NON-NLS-1$
		message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
		message.append(" "); //$NON-NLS-1$
		message.append(entry.getBundleSymbolicName());
		message.append(" "); //$NON-NLS-1$
		message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
		message.append(" "); //$NON-NLS-1$
		message.append(entry.getOptionPath());
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
		message.append(entry.getMessage());
		if (entry.getThrowable() != null) {
			message.append(EclipseDebugTrace.TRACE_ELEMENT_DELIMITER);
			message.append(" "); //$NON-NLS-1$
			message.append(entry.getThrowable());
		}
		message.append(EclipseDebugTrace.LINE_SEPARATOR);
		// write the message
		if ((traceWriter != null) && (message != null)) {
			traceWriter.write(message.toString());
		}
	}

	/**
	 * Returns a Writer for the given OutputStream
	 * @param output an OutputStream to use for the Writer
	 * @return A Writer for the given OutputStream
	 */
	protected Writer logForStream(OutputStream output) {

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
	protected Writer openWriter(final File traceFile) {

		Writer traceWriter = null;
		if (traceFile != null) {
			try {
				traceWriter = this.logForStream(secureAction.getFileOutputStream(traceFile, true));
			} catch (IOException ioEx) {
				traceWriter = this.logForStream(System.out);
			}
		} else {
			traceWriter = this.logForStream(System.out);
		}
		return traceWriter;
	}

	/**
	 * Close the trace writer
	 * 
	 * @param traceWriter The trace writer
	 */
	protected void closeWriter(Writer traceWriter) {

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

	/** The trace message for a thread stack dump */
	protected final static String MESSAGE_THREAD_DUMP = "Thread Stack dump: "; //$NON-NLS-1$
	/** The trace message for a method completing with a return value */
	protected final static String MESSAGE_EXIT_METHOD_WITH_RESULTS = "Exiting method with result: "; //$NON-NLS-1$
	/** The trace message for a method completing with no return value */
	protected final static String MESSAGE_EXIT_METHOD_NO_RESULTS = "Exiting method with a void return"; //$NON-NLS-1$
	/** The trace message for a method starting with a set of arguments */
	protected final static String MESSAGE_ENTER_METHOD_WITH_PARAMS = "Entering method with parameters: ("; //$NON-NLS-1$
	/** The trace message for a method starting with no arguments */
	protected final static String MESSAGE_ENTER_METHOD_NO_PARAMS = "Entering method with no parameters"; //$NON-NLS-1$
	/** The version attribute written to the header of the trace file */
	protected final static String TRACE_FILE_VERSION_COMMENT = "version: "; //$NON-NLS-1$
	/** The version value written to the header of the trace file */
	protected final static String TRACE_FILE_VERSION = "1.0"; //$NON-NLS-1$
	/** The new session identifier to be written whenever a new session starts */
	protected final static String TRACE_NEW_SESSION = "!SESSION "; //$NON-NLS-1$
	/** The date attribute written to the header of the trace file to show when this file was created */
	protected final static String TRACE_FILE_DATE = "Time of creation: "; //$NON-NLS-1$
	/** Trace date formatter using the pattern: yyyy-MM-dd HH:mm:ss.SSS  */
	protected final static SimpleDateFormat TRACE_FILE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
	/** The comment character used by the trace file */
	protected final static String TRACE_COMMENT = "#"; //$NON-NLS-1$
	/** The delimiter used to separate trace elements such as the time stamp, message, etc */
	protected final static String TRACE_ELEMENT_DELIMITER = "|"; //$NON-NLS-1$
	/** OS-specific line separator */
	protected static final String LINE_SEPARATOR;
	static {
		String s = System.getProperty("line.separator"); //$NON-NLS-1$
		LINE_SEPARATOR = s == null ? "\n" : s; //$NON-NLS-1$
	}
	/** The value written to the trace file if a null object is being traced */
	public final static String NULL_VALUE = "<null>"; //$NON-NLS-1$
	/**  */
	private final static SecureAction secureAction = (SecureAction) AccessController.doPrivileged(SecureAction.createSecureAction());
	/** A lock object used to synchronize access to the trace file */
	protected final static Object writeLock = new Object();

	/** An optional argument to specify the name of the class used by clients to trace messages.  If no trace class is specified
	 * then the class calling this API is assumed to be the class being traced.
	*/
	protected Class traceClass = null;
	/** The symbolic name of the bundle being traced */
	protected String bundleSymbolicName = null;
	/** A flag to determine if the message being written is done to a new file (i.e. should the header information be written) */
	protected static boolean newSession = true;
	/** DebugOptions are used to determine if the specified bundle symbolic name + option-path has debugging enabled */
	protected DebugOptions debugOptions = null;

	/******************* Tracing file attributes **************************/
	/** The default size a trace file can grow before it is rotated */
	public static final int DEFAULT_TRACE_FILE_SIZE = 1000; // The value is in KB.
	/** The default number of backup trace files */
	public static final int DEFAULT_TRACE_FILES = 10;
	/** The minimum size limit for trace file rotation */
	public static final int DEFAULT_TRACE_FILE_MIN_SIZE = 10;
	/** The extension used for log files */
	public static final String TRACE_FILE_EXTENSION = ".trace"; //$NON-NLS-1$
	/** The extension markup to use for backup log files*/
	public static final String BACKUP_MARK = ".bak_"; //$NON-NLS-1$
	/** The maximum size that a trace file should grow (0 = unlimited) */
	protected int maxTraceFileSize = DEFAULT_TRACE_FILE_SIZE; // The value is in KB.
	/** The maximum number of trace files that should be saved */
	protected int maxTraceFiles = DEFAULT_TRACE_FILES;
	/** The index of the currently backed-up trace file */
	protected int backupTraceFileIndex = 0;
}