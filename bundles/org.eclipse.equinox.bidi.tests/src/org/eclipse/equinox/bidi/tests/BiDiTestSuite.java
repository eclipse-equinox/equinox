/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.bidi.tests;

import org.eclipse.equinox.bidi.internal.tests.ExtensibilityTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BiDiTestSuite extends TestSuite {
	public static Test suite() {
		return new BiDiTestSuite();
	}

	public BiDiTestSuite() {
		addTestSuite(ExtensibilityTest.class);
	}
}
