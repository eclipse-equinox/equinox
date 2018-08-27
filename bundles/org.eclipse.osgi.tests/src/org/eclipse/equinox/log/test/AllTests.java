/*******************************************************************************
s
s This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.eclipse.equinox.log.test"); //$NON-NLS-1$
		suite.addTest(AllLogServiceTests.suite());
		suite.addTest(AllExtendedLogServiceTests.suite());
		return suite;
	}

}
