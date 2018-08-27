/*******************************************************************************
 * Copyright (c) 2011 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
