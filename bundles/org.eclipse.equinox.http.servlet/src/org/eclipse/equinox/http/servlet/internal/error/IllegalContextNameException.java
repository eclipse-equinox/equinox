/*******************************************************************************
 * Copyright (c) Feb 23, 2015 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Raymond Augé <raymond.auge@liferay.com> - Bug 460639
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.error;

/**
 * @author Raymond Augé
 */
public class IllegalContextNameException extends HttpWhiteboardFailureException {

	private static final long serialVersionUID = -8790109985246626513L;

	public IllegalContextNameException(String message, int failureReason) {
		super(message, failureReason);
	}

}
