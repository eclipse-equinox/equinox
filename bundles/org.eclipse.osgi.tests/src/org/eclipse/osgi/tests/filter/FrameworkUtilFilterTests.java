/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.filter;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.osgi.framework.*;

public class FrameworkUtilFilterTests extends FilterTests {
	public static Test suite() {
		return new TestSuite(FrameworkUtilFilterTests.class);
	}

	public Filter createFilter(String filterString) throws InvalidSyntaxException {
		return FrameworkUtil.createFilter(filterString);
	}
}
