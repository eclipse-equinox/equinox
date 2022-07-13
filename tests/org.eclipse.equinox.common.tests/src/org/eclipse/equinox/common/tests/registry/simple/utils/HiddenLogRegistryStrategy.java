/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.equinox.common.tests.registry.simple.utils;

import java.io.File;
import org.eclipse.core.internal.registry.RegistryMessages;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.spi.RegistryStrategy;

/**
 * Registry strategy that keeps log output in an accessible string.
 * @since 3.2
 */
public class HiddenLogRegistryStrategy extends RegistryStrategy {

	public static String output;

	public HiddenLogRegistryStrategy(File[] theStorageDir, boolean[] cacheReadOnly) {
		super(theStorageDir, cacheReadOnly);
	}

	@Override
	public boolean debug() {
		return true;
	}

	@Override
	public void log(IStatus status) {
		log(status, null);
	}

	// Same as RegistryStrategy, but logs into String
	public void log(IStatus status, String prefix) {
		String message = status.getMessage();
		int severity = status.getSeverity();

		String statusMsg;
		switch (severity) {
			case IStatus.ERROR :
				statusMsg = RegistryMessages.log_error;
				break;
			case IStatus.WARNING :
				statusMsg = RegistryMessages.log_warning;
				break;
			default :
				statusMsg = RegistryMessages.log_log;
				break;
		}
		statusMsg += message;

		if (prefix != null)
			statusMsg = prefix + statusMsg;
		output += statusMsg;

		// print out children as well
		IStatus[] children = status.getChildren();
		if (children.length != 0) {
			String newPrefix;
			if (prefix == null)
				newPrefix = "\t"; //$NON-NLS-1$
			else
				newPrefix = prefix + "\t"; //$NON-NLS-1$
			for (IStatus child : children) {
				log(child, newPrefix);
			}
		}
	}
}
