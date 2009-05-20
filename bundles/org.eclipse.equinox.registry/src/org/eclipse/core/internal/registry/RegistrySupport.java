/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.util.ResourceBundle;
import org.eclipse.core.runtime.IStatus;

/**
 * Simple implementation if the registry support functionality.
 * The logging output is done onto System.out (for both specific and generic logs) 
 * in the following format:
 * 
 * [Error|Warning|Log]: Main error message
 * [Error|Warning|Log]: Child error message 1
 * 	...
 * [Error|Warning|Log]: Child error message N
 * 
 * The translation routine assumes that keys are prefixed with '%'. If no resource
 * bundle is present, the key itself (without leading '%') is returned. There is
 * no decoding for the leading '%%'.
 */
public class RegistrySupport {

	static public String translate(String key, ResourceBundle resources) {
		if (key == null)
			return null;
		if (resources == null)
			return key;
		String trimmedKey = key.trim();
		if (trimmedKey.length() == 0)
			return key;
		if (trimmedKey.charAt(0) != '%')
			return key;
		return resources.getString(trimmedKey.substring(1));
	}

	static public void log(IStatus status, String prefix) {
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
		System.out.println(statusMsg);

		// print out children as well
		IStatus[] children = status.getChildren();
		if (children.length != 0) {
			String newPrefix;
			if (prefix == null)
				newPrefix = "\t"; //$NON-NLS-1$
			else
				newPrefix = prefix + "\t"; //$NON-NLS-1$
			for (int i = 0; i < children.length; i++) {
				log(children[i], newPrefix);
			}
		}
	}
}
