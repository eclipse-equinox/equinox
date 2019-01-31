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

import org.osgi.service.http.NamespaceException;

/**
 * @author Raymond Augé
 */
public class PatternInUseException extends NamespaceException {

	private static final long serialVersionUID = -4196149175131735927L;

	public PatternInUseException(String pattern) {
		super("Pattern already in use: " + pattern); //$NON-NLS-1$
	}
}