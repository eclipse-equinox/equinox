/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.composites;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllCompositeTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllCompositeTests.class.getName());
		suite.addTest(CompositeCreateTests.suite());
		suite.addTest(CompositeShareTests.suite());
		suite.addTest(CompositeSecurityTests.suite());
		return suite;
	}
}
