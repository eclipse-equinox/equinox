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

package org.eclipse.equinox.http.servlet.tests.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

/**
 * @author Raymond Augé
 */
public class BaseServletRequestListener implements ServletRequestListener {

	public AtomicBoolean initialized = new AtomicBoolean(false);
	public AtomicBoolean destroyed = new AtomicBoolean(false);

	public AtomicInteger number = new AtomicInteger();

	@Override
	public void requestDestroyed(ServletRequestEvent arg0) {
		destroyed.set(true);
		number.decrementAndGet();
	}

	@Override
	public void requestInitialized(ServletRequestEvent arg0) {
		initialized.set(true);
		number.incrementAndGet();
	}

}
