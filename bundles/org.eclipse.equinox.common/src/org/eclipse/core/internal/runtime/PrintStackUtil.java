/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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

public class PrintStackUtil {

	static public void printChildren(IStatus status, PrintStream output) {
		IStatus[] children = status.getChildren();
		if (children == null || children.length == 0)
			return;
		for (int i = 0; i < children.length; i++) {
			output.println("Contains: " + children[i].getMessage()); //$NON-NLS-1$
			Throwable exception = children[i].getException();
			if (exception != null)
				exception.printStackTrace(output);
			printChildren(children[i], output);
		}
	}

	static public void printChildren(IStatus status, PrintWriter output) {
		IStatus[] children = status.getChildren();
		if (children == null || children.length == 0)
			return;
		for (int i = 0; i < children.length; i++) {
			output.println("Contains: " + children[i].getMessage()); //$NON-NLS-1$
			output.flush(); // call to synchronize output
			Throwable exception = children[i].getException();
			if (exception != null)
				exception.printStackTrace(output);
			printChildren(children[i], output);
		}
	}

}
