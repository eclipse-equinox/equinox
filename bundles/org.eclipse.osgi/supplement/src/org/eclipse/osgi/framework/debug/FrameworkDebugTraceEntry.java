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

/**
 * A framework trace entry is a bean containing all of the attributes for a single trace message.
 */
public class FrameworkDebugTraceEntry {
	/** If a bundles symbolic name is not specified then the default value of /debug can be used */
	public final static String DEFAULT_OPTION_PATH = "/debug"; //$NON-NLS-1$

	/**
	 * The name of the thread executing the code
	 */
	private final String threadName;

	/**
	 * The date and time when the trace occurred.
	 * 
	 */
	private final long timestamp;

	/**
	 * The trace option-path
	 */
	private final String optionPath;

	/**
	 * The symbolic name of the bundle being traced
	 */
	private final String bundleSymbolicName;

	/**
	 * The class being traced
	 */
	private final String className;

	/**
	 * The method being traced
	 */
	private final String methodName;

	/**
	 * The line number
	 */
	private final int lineNumber;

	/**
	 * The trace message
	 */
	private String message;

	/**
	 * The trace exception
	 */
	private final Throwable throwable;

	/**
	 * Construct a new FrameworkTraceRecord object
	 * 
	 * @param bundleSymbolicName
	 *            The symbolic name of the bundle being traced
	 * @param optionPath
	 *            The trace optionPath
	 * @param message
	 *            The trace message
	 * @param traceClass
	 *            The class that calls the trace API
	 */
	public FrameworkDebugTraceEntry(final String bundleSymbolicName, final String optionPath, final String message, final String traceClass) {
		this(bundleSymbolicName, optionPath, message, null, traceClass);
	}

	/**
	 * Construct a new FrameworkTraceRecord object
	 * 
	 * @param bundleSymbolicName
	 *            The symbolic name of the bundle being traced
	 * @param optionPath
	 *            The trace optionPath
	 * @param message
	 *            The trace message
	 * @param error
	 *            An exception to be traced
	 * @param traceClass
	 *            The class that calls the trace API 
	 */
	public FrameworkDebugTraceEntry(String bundleSymbolicName, final String optionPath, final String message, final Throwable error, final String traceClass) {
		threadName = Thread.currentThread().getName();
		if (optionPath == null) {
			this.optionPath = FrameworkDebugTraceEntry.DEFAULT_OPTION_PATH;
		} else {
			this.optionPath = optionPath;
		}
		timestamp = System.currentTimeMillis();
		this.bundleSymbolicName = bundleSymbolicName;
		this.message = message;
		throwable = error;

		String determineClassName = null;
		String determineMethodName = null;
		int determineLineNumber = 0;
		// dynamically determine the class name, method name, and line number of the method calling the trace framework
		StackTraceElement[] stackElements = new Exception().getStackTrace();
		int i = 0;
		while (i < stackElements.length) {
			String fullClassName = stackElements[i].getClassName();
			if (!fullClassName.equals(Thread.class.getName()) && !fullClassName.equals(FrameworkDebugTraceEntry.class.getName()) && !fullClassName.equals(EclipseDebugTrace.class.getName())) {
				/*
				 * The first class which is non-JDK or framework related has been hit.
				 * If a traceClass has been specified then this current stack element
				 * is likely that class so we should find out who called it.  If a
				 * trace class has not been specified, or has been specified and this
				 * stack element is not that class, then we assume this stack element
				 * is the caller of the trace API. 
				 */
				if ((traceClass == null) || !fullClassName.equals(traceClass)) {
					determineClassName = stackElements[i].getClassName();
					determineMethodName = stackElements[i].getMethodName();
					determineLineNumber = stackElements[i].getLineNumber();
					break; // only break when the right stack element has been found; Otherwise keep trying
				}
			}
			i++;
		}

		className = determineClassName;
		methodName = determineMethodName;
		lineNumber = determineLineNumber;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {

		final StringBuffer buffer = new StringBuffer(threadName);
		buffer.append(" "); //$NON-NLS-1$
		buffer.append(timestamp);
		buffer.append(" "); //$NON-NLS-1$
		buffer.append(bundleSymbolicName);
		buffer.append(" "); //$NON-NLS-1$
		buffer.append(optionPath);
		buffer.append(" "); //$NON-NLS-1$
		buffer.append(className);
		buffer.append(" "); //$NON-NLS-1$
		buffer.append(methodName);
		buffer.append(" "); //$NON-NLS-1$
		buffer.append(lineNumber);
		if (message != null) {
			buffer.append(": "); //$NON-NLS-1$
			buffer.append(message);
		}
		if (throwable != null) {
			buffer.append(throwable);
		}
		return buffer.toString();
	}

	/**
	 * Accessor to the threads name
	 * 
	 * @return the name of the thread
	 */
	public final String getThreadName() {

		return threadName;
	}

	/**
	 * Accessor to the timestamp for this trace record
	 * 
	 * @return the date
	 */
	public final long getTimestamp() {

		return timestamp;
	}

	/**
	 * Accessor for the symbolic name of the bundle being traced
	 * 
	 * @return The symbolic name of the bundle being traced
	 */
	public final String getBundleSymbolicName() {

		return bundleSymbolicName;
	}

	/**
	 * Accessor for the trace message
	 * 
	 * @return the trace message
	 */
	public final String getMessage() {

		return message;
	}

	/**
	 * Accessor for the trace exception. This may be null if there is no exception.
	 * 
	 * @return the trace exception or null if none was defined.
	 */
	public final Throwable getThrowable() {

		return throwable;
	}

	/**
	 * Accessor for the name of the class being traced.
	 * 
	 * @return The name of the class being traced.
	 */
	public final String getClassName() {

		return className;
	}

	/**
	 * Accessor for the method being traced.
	 * 
	 * @return The name of the method being traced.
	 */
	public final String getMethodName() {

		return methodName;
	}

	/**
	 * Accessor for the option-path being traced. The <i>&lt;option-path&gt;</i> part of the debug option string
	 * required for the Eclipse debugging framework.
	 * 
	 * <pre>
	 *    Examples:
	 *       1) If a trace string com.ibm.myplugin.core/debug=true is specified then 'debug' is the option-path value.
	 *       2) If a trace string com.ibm.myplugin.core/debug/perf=true is specified then 'debug/perf' is the option-path value.
	 * </pre>
	 * 
	 * 
	 * @return The option-path being traced.
	 */
	public final String getOptionPath() {

		return optionPath;
	}

	/**
	 * Return the line number in the class/method where the trace originator
	 * 
	 * @return The line number from the class and method where the trace request originated
	 */
	public final int getLineNumber() {

		return lineNumber;
	}

	/**
	 * 
	 * @param newMessage
	 */
	void setMessage(final String newMessage) {

		message = newMessage;
	}
}