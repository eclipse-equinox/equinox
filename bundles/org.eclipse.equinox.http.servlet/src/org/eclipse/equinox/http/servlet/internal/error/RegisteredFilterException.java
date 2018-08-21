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

import javax.servlet.Filter;
import javax.servlet.ServletException;

/**
 * @author Raymond Augé
 */
public class RegisteredFilterException extends ServletException {

	private static final long serialVersionUID = 4321327145573490998L;

	public RegisteredFilterException(Filter filter) {
		super("Filter has already been registered: " + filter);
	}
}