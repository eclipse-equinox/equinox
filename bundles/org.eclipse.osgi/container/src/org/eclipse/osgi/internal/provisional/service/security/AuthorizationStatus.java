/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
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
package org.eclipse.osgi.internal.provisional.service.security;

/**
 * Constants for status codes in the Authorization engine.
 * <p>
 * This class is not intended to be extended by clients.
 * </p>
 *
 * @since 3.4
 */
public class AuthorizationStatus {

	/**
	 * This code means that the system is functioning normally - no bundles
	 * are currently experiencing authorization problems.
	 */
	public static final int OK = 0x00;

	/**
	 * This code means that there are bundles in the system that are being
	 * disabled due to authorization constraints.
	 */
	public static final int ERROR = 0x01;

}
