/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public Object start(IApplicationContext context) throws Exception {
		Exception error = (Exception) context.getArguments().get(ERROR_EXCEPTION);
		if (error != null) {
			throw error;
		}
		throw new IllegalStateException();
	}

	@Override
	public void stop() {
		// do nothing
	}
}
