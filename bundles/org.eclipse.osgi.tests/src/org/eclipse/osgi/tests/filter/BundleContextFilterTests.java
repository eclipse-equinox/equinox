/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.osgi.tests.filter;

import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

public class BundleContextFilterTests extends FilterTests {

	@Override
	public Filter createFilter(String filterString) throws InvalidSyntaxException {
		return OSGiTestsActivator.getContext().createFilter(filterString);
	}
}
