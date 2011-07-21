/*******************************************************************************
 * Copyright (c) 2011 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllExtendedLogServiceTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("Test extended log service"); //$NON-NLS-1$
		suite.addTestSuite(ExtendedLogServiceTest.class);
		suite.addTestSuite(ExtendedLogReaderServiceTest.class);
		return suite;
	}
}
