/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.log;

/**
 * A framework log entry used to log information to a FrameworkLog
 * @since 3.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class FrameworkLogEntry {
	/**
	 * Severity constant (value 0) indicating this log entry represents the nominal case.
	 * @see #getSeverity()
	 * @since 3.2
	 */
	public static final int OK = 0;

	/** 
	 * Severity constant (bit mask, value 1) indicating this log entry is informational only.
	 * @see #getSeverity()
	 * @since 3.2
	 */
	public static final int INFO = 0x01;

	/** 
	 * Severity constant (bit mask, value 2) indicating this log entry represents a warning.
	 * @see #getSeverity()
	 * @since 3.2
	 */
	public static final int WARNING = 0x02;

	/** 
	 * Severity constant (bit mask, value 4) indicating this log entry represents an error.
	 * @see #getSeverity()
	 * @since 3.2
	 */
	public static final int ERROR = 0x04;

	/**
	 * Status type severity (bit mask, value 8) indicating this log entry represents a cancellation.
	 * @see #getSeverity()
	 * @since 3.2
	 */
	public static final int CANCEL = 0x08;

	// It would be nice to rename some of these fields but we cannot change the getter method
	// names without breaking clients.  Changing only the field names would be confusing.
	//TODO "entry" has another meaning here - title, summary, tag are better names 
	private final String entry;
	private final String message;
	//TODO get rid of this
	private final int stackCode;
	//TODO: use "reason" or "cause" instead
	private final Throwable throwable;
	private final FrameworkLogEntry[] children;
	private final int severity;
	private final int bundleCode;
	private final Object context;

	/**
	 * Constructs a new FrameworkLogEntry
	 * @param entry the entry
	 * @param message the message
	 * @param stackCode the stack code
	 * @param throwable the throwable
	 * @param children the children
	 */
	public FrameworkLogEntry(String entry, String message, int stackCode, Throwable throwable, FrameworkLogEntry[] children) {
		this(null, entry, 0, 0, message, stackCode, throwable, children);
	}

	/**
	 * Constructs a new FrameworkLogEntry
	 * @param entry the entry
	 * @param severity the severity
	 * @param bundleCode the bundle code
	 * @param message the message
	 * @param stackCode the stack code
	 * @param throwable the throwable
	 * @param children the children
	 * @since 3.2
	 */
	public FrameworkLogEntry(String entry, int severity, int bundleCode, String message, int stackCode, Throwable throwable, FrameworkLogEntry[] children) {
		this(null, entry, severity, bundleCode, message, stackCode, throwable, children);
	}

	/**
	 * Constructs a new FrameworkLogEntry
	 * @param context the context
	 * @param entry the entry
	 * @param severity the severity
	 * @param bundleCode the bundle code
	 * @param message the message
	 * @param stackCode the stack code
	 * @param throwable the throwable
	 * @param children the children
	 * @since 3.7
	 */
	public FrameworkLogEntry(Object context, String entry, int severity, int bundleCode, String message, int stackCode, Throwable throwable, FrameworkLogEntry[] children) {
		this.context = context;
		this.entry = entry;
		this.message = message;
		this.stackCode = stackCode;
		this.throwable = throwable;
		this.children = children;
		this.severity = severity;
		this.bundleCode = bundleCode;
	}

	/**
	 * 
	 * @return Returns the children.
	 */
	public FrameworkLogEntry[] getChildren() {
		return children;
	}

	/**
	 * @return Returns the entry.
	 */
	public String getEntry() {
		return entry;
	}

	/**
	 * @return Returns the message.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return Returns the stackCode.
	 */
	public int getStackCode() {
		return stackCode;
	}

	/**
	 * @return Returns the throwable.
	 */
	public Throwable getThrowable() {
		return throwable;
	}

	/**
	 * Returns the severity. The severities are as follows (in descending order):
	 * <ul>
	 * <li><code>CANCEL</code> - cancelation occurred</li>
	 * <li><code>ERROR</code> - a serious error (most severe)</li>
	 * <li><code>WARNING</code> - a warning (less severe)</li>
	 * <li><code>INFO</code> - an informational ("fyi") message (least severe)</li>
	 * <li><code>OK</code> - everything is just fine</li>
	 * </ul>
	 * <p>
	 * The severity of a multi-entry log is defined to be the maximum
	 * severity of any of its children, or <code>OK</code> if it has
	 * no children.
	 * </p>
	 *
	 * @return the severity: one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 * @since 3.2
	 */
	public int getSeverity() {
		return severity;
	}

	/**
	 * Returns the bundle-specific code describing the outcome.
	 *
	 * @return bundle-specific code
	 * @since 3.2
	 */
	public int getBundleCode() {
		return bundleCode;
	}

	/**
	 * Returns the context associated with this <code>FrameworkLogEntry</code>
	 * object.
	 * 
	 * @return <code>Object</code> containing the context associated with this
	 *         <code>FrameworkLogEntry</code> object;<code>null</code> if no context is
	 *         associated with this <code>FrameworkLogEntry</code> object.
	 * @since 3.7
	 */
	public Object getContext() {
		return context;
	}
}
