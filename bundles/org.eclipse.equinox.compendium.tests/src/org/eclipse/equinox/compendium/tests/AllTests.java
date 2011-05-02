/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.compendium.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for Equinox Compendium"); //$NON-NLS-1$
		suite.addTest(org.eclipse.equinox.metatype.tests.AllTests.suite());
		suite.addTest(org.eclipse.equinox.useradmin.tests.AllTests.suite());
		suite.addTest(org.eclipse.equinox.event.tests.AllTests.suite());
		return suite;
	}

}
