/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log;

import java.io.*;
import java.lang.reflect.*;

/**
 * Encapsulate an <code>Exception</code>
 * or <code>Error</code> in a logging message.
 * <p>
 * The stack trace information of a <code>Throwable</code>
 * is transient and therefore this information is lost
 * if the throwable is serialized or cloned.<br>
 * This class extracts the stack trace information from the throwable
 * and keeps it for later retrieval.
 * <p>
 * The <code>Throwable</code> instance itself and any nested exceptions
 * are not kept in the <code>LoggedException</code> for 2 reasons:<ol>
 * <li>so no reference to that throwable is kept that would prevent
 *  the removal of the memory space that hosts the throwable
 *  with <em>NOFORCE</em>.</li>
 * <li>the class of the throwable can be removed from the
 *  system and the class loader segments can be freed.</li>
 * </ol>
 * <p>
 */

/* This class MUST be public for nested exception printing to work */
public class LoggedException extends Throwable implements Cloneable {

	/** The fully qualified name of exception or error type. */
	String exceptionClassName;

	/**
	 * The detail message.
	 * Don't use super class field because we need to be able to
	 * copy it into another memory space.
	 */
	String message;

	/**
	 * The stack trace.
	 * The super class field is transient.
	 */
	String stackTrace;

	/**
	 * The nested exception if any.
	 */
	LoggedException nestedException;

	/**
	 * Creates a wrapper of the given <code>Throwable</code>,
	 *
	 * @param	t		the exception or error
	 */
	public LoggedException(Throwable t) {
		super();

		// copy original exception message
		if (t.getMessage() != null) {
			message = new String(t.getMessage().toCharArray());
		}
		if (t instanceof LoggedException) {
			LoggedException lex = (LoggedException) t;
			exceptionClassName = lex.exceptionClassName;
			// copy stack trace
			stackTrace = new String(lex.stackTrace.toCharArray());
		} else {
			exceptionClassName = t.getClass().getName();
			// save stack trace
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream s = new PrintStream(baos);
			t.printStackTrace(s);
			s.close();
			stackTrace = baos.toString();

		}
		// copy original exception class name
		exceptionClassName = new String(exceptionClassName.toCharArray());

		// search for a nested exception

		//      Field[] fields = t.getClass().getFields();
		//      for (int i = 0; i < fields.length; i++) {
		//      	try {
		//      		Object o = fields[i].get(t);
		//      		if (o != null && o instanceof Throwable) {
		//      			nestedException = new LoggedException((Throwable) o);
		//      			break;
		//      		}
		//      	} catch (IllegalAccessException e) {}
		//      }

		Method[] methods = t.getClass().getMethods();

		int size = methods.length;
		Class throwable = Throwable.class;

		for (int i = 0; i < size; i++) {
			Method method = methods[i];

			if (Modifier.isPublic(method.getModifiers()) && method.getName().startsWith("get") && //$NON-NLS-1$
					throwable.isAssignableFrom(method.getReturnType()) && (method.getParameterTypes().length == 0)) {
				try {
					Throwable nested = (Throwable) method.invoke(t, null);

					if ((nested != null) && (nested != t)) {
						nestedException = new LoggedException(nested);
						break;
					}
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
				}
			}
		}
	}

	/**
	 * Does nothing.
	 * <p>
	 * This is a wrapper of an <code>Exception</code> or <code>Error</code>.
	 * Hence it does not need it's own stack trace filled in.
	 */
	public Throwable fillInStackTrace() {
		return this;
	}

	/**
	 * Returns the class name of the original
	 * <code>Exception</code> or <code>Error</code>.
	 */
	public String getExceptionClassName() {
		return exceptionClassName;
	}

	/**
	 * Returns the human readable message describing the condition
	 * of the original <code>Exception</code> or <code>Error</code>.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the nested <code>LoggedException</code>
	 * or <code>null/code> if there is none.
	 */
	public Throwable getNestedException() {
		return nestedException;
	}

	/**
	 * Prints the backtrace of the original <code>Exception</code> or <code>Error</code>.
	 */
	public void printStackTrace() {
		printStackTrace(System.err);
	}

	/**
	 * Prints the backtrace of the original <code>Exception</code> or <code>Error</code>.
	 */
	public void printStackTrace(PrintStream s) {
		synchronized (s) {
			s.println(stackTrace);
		}
	}

	/**
	 * Prints the backtrace of the original <code>Exception</code> or <code>Error</code>.
	 */
	public void printStackTrace(PrintWriter w) {
		synchronized (w) {
			w.println(stackTrace);
		}
	}

	/**
	 * Returns a string representation of the original
	 * <code>Exception</code> or <code>Error</code>.
	 *
	 * @return	The string representation of the receiver.
	 */
	public String toString() {
		if (message == null) {
			return exceptionClassName;
		} else {
			int length = exceptionClassName.length() + message.length() + 2;
			return new StringBuffer(length).append(exceptionClassName).append(": ").append(message).toString(); //$NON-NLS-1$
		}
	}
}