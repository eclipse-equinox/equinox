/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.util;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

/**
 * @author Raymond Augé
 */
public class BaseHttpSessionAttributeListener
	implements HttpSessionAttributeListener {

	public AtomicBoolean added = new AtomicBoolean(false);
	public AtomicBoolean replaced = new AtomicBoolean(false);
	public AtomicBoolean removed = new AtomicBoolean(false);

	@Override
	public void attributeAdded(HttpSessionBindingEvent arg0) {
		added.set(true);
	}

	@Override
	public void attributeRemoved(HttpSessionBindingEvent arg0) {
		removed.set(true);
	}

	@Override
	public void attributeReplaced(HttpSessionBindingEvent arg0) {
		replaced.set(true);
	}

}
