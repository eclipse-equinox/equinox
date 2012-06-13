/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.adaptor;

/**
 * An exception my implement the StatusException interface to give more status information about the type of exception.
 */
public interface StatusException {
	/**
	 * The exception is ok and expected
	 */
	public static final int CODE_OK = 0x01;
	/**
	 * The exception is for informational purposes
	 */
	public static final int CODE_INFO = 0x02;
	/**
	 * The exception is unexpected by may be handled, but a warning should be logged
	 */
	public static final int CODE_WARNING = 0x04;
	/**
	 * The exception is unexpected and should result in an error.
	 */
	public static final int CODE_ERROR = 0x08;

	/**
	 * Returns the status object
	 * @return the status object
	 */
	public Object getStatus();

	/**
	 * Returns the status code
	 * @return the status code
	 */
	public int getStatusCode();
}
