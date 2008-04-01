/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.PrintStream;
import java.io.PrintWriter;
import org.eclipse.core.runtime.IStatus;

/**
 * There are entry methods for both PrintStream and PrintWriter. The PrintStream 
 * access point does flush() on the derived PrintWriter after text has been 
 * printed out.
 */
public class PrintStackUtil {

	static public void printChildren(IStatus status, PrintStream output) {
		Throwable cause = status.getException();
		StackTraceElement[] topStack = (cause == null) ? null : cause.getStackTrace();

		PrintWriter writer = new PrintWriter(output);
		printChildren(status, writer, topStack, 1);
		writer.flush();
	}

	static public void printChildren(IStatus status, PrintWriter output) {
		Throwable cause = status.getException();
		StackTraceElement[] topStack = (cause == null) ? null : cause.getStackTrace();
		printChildren(status, output, topStack, 1);
	}

	static private void printChildren(IStatus parent, PrintWriter output, StackTraceElement[] topStack, int level) {
		IStatus[] children = parent.getChildren();
		if (children == null || children.length == 0)
			return;

		for (int i = 0; i < children.length; i++) {
			output.println("Caused by: " + children[i].getMessage()); //$NON-NLS-1$
			Throwable exception = children[i].getException();
			if (exception != null) {
				StackTraceElement[] stack = exception.getStackTrace();
				if (stack == null || stack.length == 0)
					return;
				// eliminate common trails
				int stackMatch = stack.length;
				int topPosition = (topStack == null) ? 0 : topStack.length;
				for (int j = stack.length; j > 0 && topPosition > 0; j--, topPosition--) {
					if (!stack[j - 1].equals(topStack[topPosition - 1])) {
						stackMatch = j;
						break;
					}
				}
				for (int j = 0; j < stackMatch; j++)
					output.println("\tat " + stack[j]); //$NON-NLS-1$
				if (stackMatch != stack.length)
					output.println("\t... " + Integer.toString(stack.length - stackMatch) + " more"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			printChildren(children[i], output, topStack, level + 1);
		}
	}

}
