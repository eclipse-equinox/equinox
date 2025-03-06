/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.PrintStream;
import java.io.PrintWriter;
import org.eclipse.core.runtime.IStatus;

public class PrintStackUtil {

	static public void printChildren(IStatus status, PrintStream output) {
		IStatus[] children = status.getChildren();
		if (children == null || children.length == 0) {
			return;
		}
		for (IStatus child : children) {
			output.println("Contains: " + child.getMessage()); //$NON-NLS-1$
			Throwable exception = child.getException();
			if (exception != null) {
				exception.printStackTrace(output);
			}
			printChildren(child, output);
		}
	}

	static public void printChildren(IStatus status, PrintWriter output) {
		IStatus[] children = status.getChildren();
		if (children == null || children.length == 0) {
			return;
		}
		for (IStatus child : children) {
			output.println("Contains: " + child.getMessage()); //$NON-NLS-1$
			output.flush(); // call to synchronize output
			Throwable exception = child.getException();
			if (exception != null) {
				exception.printStackTrace(output);
			}
			printChildren(child, output);
		}
	}

}
