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
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.util;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author Raymond Augé
 */
public class BaseServletContextListener implements ServletContextListener {

	public AtomicBoolean initialized = new AtomicBoolean(false);
	public AtomicBoolean destroyed = new AtomicBoolean(false);
	public ServletContext servletContext;

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		destroyed.set(true);
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		initialized.set(true);
		servletContext = servletContextEvent.getServletContext();
	}

}