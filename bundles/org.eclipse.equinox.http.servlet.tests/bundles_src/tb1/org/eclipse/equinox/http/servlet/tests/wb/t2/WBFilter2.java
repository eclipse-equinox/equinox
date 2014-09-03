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

package org.eclipse.equinox.http.servlet.tests.wb.t2;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractWhiteboardTestFilter;

import org.osgi.service.component.ComponentContext;

/**
 * @author Raymond Augé
 */
public class WBFilter2 extends AbstractWhiteboardTestFilter {

	char c;

	private void activate(ComponentContext componentContext) {
		c = (Character)componentContext.getProperties().get("char");
	}

	@Override
	public char getChar() {
		return c;
	}

}
