/*******************************************************************************
 * Copyright (c) 2014, 2017 Raymond Augé and others.
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

package org.eclipse.equinox.http.servlet.tests.wb.t2;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractWhiteboardTestFilter;

import org.osgi.service.component.ComponentContext;

/**
 * @author Raymond Augé
 */
public class WBFilter2 extends AbstractWhiteboardTestFilter {

	char c;

	protected void activate(ComponentContext componentContext) {
		String s = (String) componentContext.getProperties().get("char");
		c = s.charAt(0);
	}

	@Override
	public char getChar() {
		return c;
	}

}
