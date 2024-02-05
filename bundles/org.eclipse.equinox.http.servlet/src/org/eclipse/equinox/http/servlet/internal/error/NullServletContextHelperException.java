/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.error;

/**
 * @author Raymond Augé
 */
public class NullServletContextHelperException extends IllegalArgumentException {

	private static final long serialVersionUID = 8516340810740210836L;

	public NullServletContextHelperException() {
		super("ServletContexHelper cannot be null."); //$NON-NLS-1$
	}

}
