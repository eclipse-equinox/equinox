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

import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;

/**
 * @author Raymond Augé
 */
public class BaseServletRequestAttributeListener
	implements ServletRequestAttributeListener {

	public AtomicBoolean added = new AtomicBoolean(false);
	public AtomicBoolean replaced = new AtomicBoolean(false);
	public AtomicBoolean removed = new AtomicBoolean(false);

	@Override
	public void attributeAdded(ServletRequestAttributeEvent arg0) {
		added.set(true);
	}

	@Override
	public void attributeRemoved(ServletRequestAttributeEvent arg0) {
		removed.set(true);
	}

	@Override
	public void attributeReplaced(ServletRequestAttributeEvent arg0) {
		replaced.set(true);
	}

}