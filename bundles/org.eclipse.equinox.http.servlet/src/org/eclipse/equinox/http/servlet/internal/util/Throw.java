/*******************************************************************************
 * Copyright (c) 2016 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 497271
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.util;

public class Throw {

	public static <T> T unchecked(Throwable throwable) {
		return Throw.<T, RuntimeException>unchecked0(throwable);
	}

	@SuppressWarnings("unchecked")
	private static <T, E extends Throwable> T unchecked0(Throwable throwable) throws E {
		throw (E) throwable;
	}

}
