/*******************************************************************************
 * Copyright (c) 2015 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 460720
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.util;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;

public class BaseHttpSessionIdListener implements HttpSessionIdListener {

	public AtomicBoolean changed = new AtomicBoolean(false);

	@Override
	public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
		changed.set(true);
	}

}
