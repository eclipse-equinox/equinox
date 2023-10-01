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

import java.util.Objects;

/**
 * @author Raymond Augé
 */
public class HttpWhiteboardFailureException extends IllegalArgumentException {

	private static final long serialVersionUID = 1944632136470074075L;

	public HttpWhiteboardFailureException(String message, int failureReason) {
		super(Objects.requireNonNull(message));

		this.failureReason = failureReason;
	}

	public int getFailureReason() {
		return failureReason;
	}

	private final int failureReason;

}
