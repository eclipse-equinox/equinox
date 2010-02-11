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
package org.eclipse.osgi.service.debug;

/**
 * A DebugTrace is used to record debug trace statements, based on the current
 * option settings in a corresponding {@link DebugOptions} class. The trace implementation
 * will automatically insert additional contextual information such as the bundle, class, 
 * and method performing the tracing. 
 * <p>
 * Trace statements may be written to a file, or onto standard output, depending on 
 * how the {@link DebugOptions} is configured.
 * </p>
 * <p>
 * All methods on this class have an optional <code>option</code> argument.
 * When specified, this argument will cause the tracing to be conditional on the value
 * of {@link DebugOptions#getBooleanOption(String, boolean)}, where the bundle's
 * symbolic name will automatically be prepended to the provided option string. For example,
 * if your bundle symbolic name is "com.acme.bundle", and you provide an option argument
 * of "/debug/parser", the trace will only be printed if the option "com.acme.bundle/debug/parser"
 * has a value of "true".
 * </p>
 * <p>
 * Note that the pipe character ("&#124;") is reserved for internal use. If this character 
 * happens to occur in any of the thread name, the option, the message or an Exception
 * message, it will be escaped to the corresponding HTML representation ("&amp&#35;124&#59;").   
 * </p>
 *  
 * @since 3.5
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface DebugTrace {

	/**
	 * Traces a message for the specified option.
	 * 
	 * @param option The name of the boolean option that will control whether the
	 * trace statement is printed (e.g., "/debug/myComponent"), or <code>null</code>
	 * @param message The trace message to display
	 */
	public void trace(final String option, final String message);

	/**
	 * Traces a message and exception for the specified option.
	 * 
	 * @param option The name of the boolean option that will control whether the
	 * trace statement is printed (e.g., "/debug/myComponent"), or <code>null</code>
	 * @param message The trace message to display
	 * @param error The exception to trace
	 */
	public void trace(final String option, final String message, final Throwable error);

	/**
	 * Adds a trace message showing a thread stack dump for the current class and 
	 * method being executed for the specified option.
	 *
	 * @param option The name of the boolean option that will control whether the
	 * trace statement is printed (e.g., "/debug/myComponent"), or <code>null</code>
	 */
	public void traceDumpStack(final String option);

	/**
	 * Add a trace message level stating that a method is being executed for the specified option.
	 * 
	 * @param option The name of the boolean option that will control whether the
	 * trace statement is printed (e.g., "/debug/myComponent"), or <code>null</code>
	 */
	public void traceEntry(final String option);

	/**
	 * Add a trace message level stating that a method with the specified argument 
	 * values is being executed for the specified option. The result of {@link String#valueOf(Object)} 
	 * on the methodArgument will be written to the trace file.
	 * 
	 * @param option The name of the boolean option that will control whether the
	 * trace statement is printed (e.g., "/debug/myComponent"), or <code>null</code>
	 * @param methodArgument
	 *            The single argument for the method being executed
	 */
	public void traceEntry(final String option, final Object methodArgument);

	/**
	 * Add a trace message level stating that a method with the specified arguments 
	 * values is being executed for the specified option. The result of {@link String#valueOf(Object)} 
	 * on each argument will be written to the trace file.
	 * 
	 * @param option The name of the boolean option that will control whether the
	 * trace statement is printed (e.g., "/debug/myComponent"), or <code>null</code>
	 * @param methodArguments
	 *            A list of object arguments for the method being executed
	 */
	public void traceEntry(final String option, final Object[] methodArguments);

	/**
	 * Add a trace message level stating that a method has completed execution for the specified option.
	 * 
	 * @param option The name of the boolean option that will control whether the
	 * trace statement is printed (e.g., "/debug/myComponent"), or <code>null</code>
	 */
	public void traceExit(final String option);

	/**
	 * Add a trace message level stating that a method with the specified result value 
	 * has completed execution for the specified option. The result of {@link String#valueOf(Object)} 
	 * on the result object will be written to the trace file.
	 * 
	 * @param option The name of the boolean option that will control whether the
	 * trace statement is printed (e.g., "/debug/myComponent"), or <code>null</code>
	 * @param result The result being returned from the method that was executed
	 */
	public void traceExit(final String option, final Object result);
}