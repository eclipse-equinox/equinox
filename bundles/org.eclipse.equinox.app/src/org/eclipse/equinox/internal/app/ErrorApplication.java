/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/*
 * Special case class only used to throw exceptions when an application
 * cannot be found.
 */
public class ErrorApplication implements IApplication {
	static final String ERROR_EXCEPTION = "error.exception"; //$NON-NLS-1$

	public Object start(IApplicationContext context) throws Exception {
		Exception error = (Exception) context.getArguments().get(ERROR_EXCEPTION);
		if (error != null)
			throw error;
		throw new IllegalStateException();
	}

	public void stop() {
		// do nothing
	}
}
