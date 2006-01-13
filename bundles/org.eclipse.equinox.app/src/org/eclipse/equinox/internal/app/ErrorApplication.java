/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import org.eclipse.equinox.app.IApplication;

/*
 * Special case class only used to throw exceptions when an application
 * cannot be found.
 */
public class ErrorApplication implements IApplication {

	static private Exception ERROR;

	public Object run(Object args) throws Exception {
		if (ERROR != null)
			throw ERROR;
		throw new IllegalStateException();
	}

	public void stop() {
		// do nothing
	}

	static void setError(Exception error) {
		ERROR = error;
	}
}
