/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Alexander Fedorov (ArSysOp) - Bug 561712
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.util.Optional;
import org.eclipse.core.internal.runtime.LocalizationUtils;
import org.osgi.framework.FrameworkUtil;

/**
 * A concrete status implementation, suitable either for 
 * instantiating or subclassing.
 * <p>
 * This class can be used without OSGi running.
 * </p>
 * <p>
 * For performance critical code, such as when {@link Status} objects are not created
 * for logging or error handling, there may be a small performance penalty when using
 * static methods or constructors that automatically determine the plug-in id from
 * the provided {@link Class} or by using {@link StackWalker}.
 * </p>
 */
public class Status implements IStatus {

	/**
	 * Constant used to indicate an unknown plugin id.
	 */
	private static final String unknownId = "unknown"; //$NON-NLS-1$

	/**
	 * A standard OK status with an "ok" message.
	 *
	 * @since 3.0
	 */
	public static final IStatus OK_STATUS = new Status(OK, unknownId, OK, LocalizationUtils.safeLocalize("ok"), null); //$NON-NLS-1$
	/**
	 * A standard CANCEL status with no message.
	 * 
	 * @since 3.0
	 */
	public static final IStatus CANCEL_STATUS = new Status(CANCEL, unknownId, 1, "", null); //$NON-NLS-1$
	/**
	 * The severity. One of
	 * <ul>
	 * <li><code>CANCEL</code></li>
	 * <li><code>ERROR</code></li>
	 * <li><code>WARNING</code></li>
	 * <li><code>INFO</code></li>
	 * <li>or <code>OK</code> (0)</li>
	 * </ul>
	 */
	private int severity = OK;

	/** Unique identifier of plug-in.
	 */
	private String pluginId;

	/** Plug-in-specific status code.
	 */
	private int code;

	/** Message, localized to the current locale.
	 */
	private String message;

	/** Wrapped exception, or <code>null</code> if none.
	 */
	private Throwable exception = null;

	/** Constant to avoid generating garbage.
	 */
	private static final IStatus[] theEmptyStatusArray = new IStatus[0];

	private static StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	/**
	 * Construct a new status object with severity {@link IStatus#INFO},
	 * code {@link IStatus#OK}, and pluginId determined automatically
	 * from the calling class using {@link StackWalker}.
	 * The created status has no children.
	 *
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @return the newly created status
	 * @since 3.15
	 */
	public static IStatus info(String message) {
		Class<?> callerClass = null;
		try {
			callerClass = walker.getCallerClass();
		} catch (IllegalCallerException e) {
			// ignored - use null class
		}
		return new Status(INFO, callerClass, OK, message, null);
	}

	/**
	 * Construct a new status object with severity {@link IStatus#WARNING},
	 * code {@link IStatus#OK}, and pluginId determined automatically
	 * from the calling class using {@link StackWalker}.
	 * The created status has no children.
	 *
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @return the newly created status
	 * @since 3.15
	 */
	public static IStatus warning(String message) {
		Class<?> callerClass = null;
		try {
			callerClass = walker.getCallerClass();
		} catch (IllegalCallerException e) {
			// ignored - use null class
		}
		return new Status(WARNING, callerClass, OK, message, null);
	}

	/**
	 * Construct a new status object with severity {@link IStatus#WARNING},
	 * code {@link IStatus#OK}, and pluginId determined automatically
	 * from the calling class using {@link StackWalker}.
	 * The created status has no children.
	 *
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @param exception a low-level exception, or <code>null</code> if not
	 *    applicable
	 * @return the newly created status
	 * @since 3.15
	 */
	public static Status warning(String message, Throwable exception) {
		Class<?> callerClass = null;
		try {
			callerClass = walker.getCallerClass();
		} catch (IllegalCallerException e) {
			// ignored - use null class
		}
		return new Status(WARNING, callerClass, OK, message, exception);
	}

	/**
	 * Construct a new status object with severity {@link IStatus#ERROR},
	 * code {@link IStatus#OK}, and pluginId determined automatically
	 * from the calling class using {@link StackWalker}.
	 * The created status has no children.
	 *
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @return the newly created status
	 * @since 3.15
	 */
	public static IStatus error(String message) {
		Class<?> callerClass = null;
		try {
			callerClass = walker.getCallerClass();
		} catch (IllegalCallerException e) {
			// ignored - use null class
		}
		return new Status(ERROR, callerClass, OK, message, null);
	}

	/**
	 * Construct a new status object with severity {@link IStatus#ERROR},
	 * code {@link IStatus#OK}, and pluginId determined automatically
	 * from the calling class using {@link StackWalker}.
	 * The created status has no children.
	 *
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @param exception a low-level exception, or <code>null</code> if not
	 *    applicable
	 * @return the newly created status
	 * @since 3.15
	 */
	public static IStatus error(String message, Throwable exception) {
		Class<?> callerClass = null;
		try {
			callerClass = walker.getCallerClass();
		} catch (IllegalCallerException e) {
			// ignored - use null class
		}
		return new Status(ERROR, callerClass, OK, message, exception);
	}

	/**
	 * Creates a new status object.  The created status has no children.
	 *
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 * @param caller the relevant class to build unique identifier from
	 * @param code the caller-specific status code, or <code>OK</code>
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @param exception a low-level exception, or <code>null</code> if not
	 *    applicable
	 *    
	 * @since 3.12
	 */
	public Status(int severity, Class<?> caller, int code, String message, Throwable exception) {
		setSeverity(severity);
		setPlugin(identifier(caller));
		setCode(code);
		setMessage(message);
		setException(exception);
	}

	/**
	 * Creates a new status object.  The created status has no children.
	 *
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param code the plug-in-specific status code, or <code>OK</code>
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @param exception a low-level exception, or <code>null</code> if not
	 *    applicable 
	 */
	public Status(int severity, String pluginId, int code, String message, Throwable exception) {
		setSeverity(severity);
		setPlugin(pluginId);
		setCode(code);
		setMessage(message);
		setException(exception);
	}

	/**
	 * Simplified constructor of a new status object; assumes that code is <code>OK</code>.
	 * The created status has no children.
	 *
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 * @param caller the relevant class to build unique identifier from
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @param exception a low-level exception, or <code>null</code> if not
	 *    applicable
	 *     
	 * @since 3.12
	 */
	public Status(int severity, Class<?> caller, String message, Throwable exception) {
		setSeverity(severity);
		setPlugin(identifier(caller));
		setMessage(message);
		setException(exception);
		setCode(OK);
	}

	/**
	 * Simplified constructor of a new status object; assumes that code is <code>OK</code>.
	 * The created status has no children.
	 *
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @param exception a low-level exception, or <code>null</code> if not
	 *    applicable
	 *     
	 * @since org.eclipse.equinox.common 3.3
	 */
	public Status(int severity, String pluginId, String message, Throwable exception) {
		setSeverity(severity);
		setPlugin(pluginId);
		setMessage(message);
		setException(exception);
		setCode(OK);
	}

	/**
	 * Simplified constructor of a new status object; assumes that code is <code>OK</code> and
	 * exception is <code>null</code>. The created status has no children.
	 *
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 * @param caller the relevant class to build unique identifier from
	 * @param message a human-readable message, localized to the
	 *    current locale
	 *    
	 * @since 3.12
	 */
	public Status(int severity, Class<?> caller, String message) {
		setSeverity(severity);
		setPlugin(identifier(caller));
		setMessage(message);
		setCode(OK);
		setException(null);
	}

	/**
	 * Simplified constructor of a new status object; assumes that code is <code>OK</code> and
	 * exception is <code>null</code>. The created status has no children.
	 *
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param message a human-readable message, localized to the
	 *    current locale
	 *    
	 * @since org.eclipse.equinox.common 3.3
	 */
	public Status(int severity, String pluginId, String message) {
		setSeverity(severity);
		setPlugin(pluginId);
		setMessage(message);
		setCode(OK);
		setException(null);
	}

	/**
	 * Extracts an identifier from the given class: either bundle symbolic name or class name, never returns <code>null</code>
	 * 
	 * @param caller the relevant class to build unique identifier from
	 * @return identifier extracted for the given class
	 */
	private static String identifier(Class<?> caller) {
		return Optional.ofNullable(caller)//
				.flatMap(c -> Optional.ofNullable(FrameworkUtil.getBundle(c)))//
				.map(b -> b.getSymbolicName())//
				.orElseGet(() -> Optional.ofNullable(caller)//
						.map(c -> c.getName())//
						.orElse(unknownId));
	}

	@Override
	public IStatus[] getChildren() {
		return theEmptyStatusArray;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public Throwable getException() {
		return exception;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getPlugin() {
		return pluginId;
	}

	@Override
	public int getSeverity() {
		return severity;
	}

	@Override
	public boolean isMultiStatus() {
		return false;
	}

	@Override
	public boolean isOK() {
		return severity == OK;
	}

	@Override
	public boolean matches(int severityMask) {
		return (severity & severityMask) != 0;
	}

	/**
	 * Sets the status code.
	 *
	 * @param code the plug-in-specific status code, or <code>OK</code>
	 */
	protected void setCode(int code) {
		this.code = code;
	}

	/**
	 * Sets the exception.
	 *
	 * @param exception a low-level exception, or <code>null</code> if not
	 *    applicable 
	 */
	protected void setException(Throwable exception) {
		this.exception = exception;
	}

	/**
	 * Sets the message. If null is passed, message is set to an empty
	 * string.
	 *
	 * @param message a human-readable message, localized to the
	 *    current locale
	 */
	protected void setMessage(String message) {
		if (message == null)
			this.message = ""; //$NON-NLS-1$
		else
			this.message = message;
	}

	/**
	 * Sets the plug-in id.
	 *
	 * @param pluginId the unique identifier of the relevant plug-in
	 */
	protected void setPlugin(String pluginId) {
		Assert.isLegal(pluginId != null && pluginId.length() > 0);
		this.pluginId = pluginId;
	}

	/**
	 * Sets the severity.
	 *
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 */
	protected void setSeverity(int severity) {
		Assert.isLegal(severity == OK || severity == ERROR || severity == WARNING || severity == INFO || severity == CANCEL);
		this.severity = severity;
	}

	/**
	 * Returns a string representation of the status, suitable 
	 * for debugging purposes only.
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("Status "); //$NON-NLS-1$
		if (severity == OK) {
			buf.append("OK"); //$NON-NLS-1$
		} else if (severity == ERROR) {
			buf.append("ERROR"); //$NON-NLS-1$
		} else if (severity == WARNING) {
			buf.append("WARNING"); //$NON-NLS-1$
		} else if (severity == INFO) {
			buf.append("INFO"); //$NON-NLS-1$
		} else if (severity == CANCEL) {
			buf.append("CANCEL"); //$NON-NLS-1$
		} else {
			buf.append("severity="); //$NON-NLS-1$
			buf.append(severity);
		}
		buf.append(": "); //$NON-NLS-1$
		buf.append(pluginId);
		buf.append(" code="); //$NON-NLS-1$
		buf.append(code);
		buf.append(' ');
		buf.append(message);
		if (exception != null) {
			buf.append(' ');
			buf.append(exception);
		}
		return buf.toString();
	}
}
