/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.tb1;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;

/*
 * This servlet is registered with the HttpService via the immediate DS
 * component OSGI-INF/testServlet1_component.xml.
 */
public class TestServlet1 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;
}
