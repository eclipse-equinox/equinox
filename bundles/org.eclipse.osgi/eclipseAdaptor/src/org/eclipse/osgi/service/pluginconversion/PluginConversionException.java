/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.pluginconversion;


/**
 * Custom exception for errors that can happen during plugin conversion.
 */
public class PluginConversionException extends Exception {
	private static final long serialVersionUID = 3258130258472284472L;
	/**
	 * Nested exception.
	 */
	private transient Throwable cause;

	public PluginConversionException() {
		super();
	}

	public PluginConversionException(String message) {
		super(message);
	}

	public PluginConversionException(String message, Throwable cause) {
		super(message);
		this.cause = cause;
	}

	public PluginConversionException(Throwable cause) {
		this.cause = cause;
	}

	public Throwable getCause() {
		return cause;
	}
}
