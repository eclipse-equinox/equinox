/*******************************************************************************
 * Copyright (c) 2015 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
