/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.bidi.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.bidi.internal.tests.*;

public class StructuredTextTestSuite extends TestSuite {
	public static Test suite() {
		return new StructuredTextTestSuite();
	}

	public StructuredTextTestSuite() {
		addTestSuite(StructuredTextExtensibilityTest.class);
		addTestSuite(StructuredTextMethodsTest.class);
		addTestSuite(StructuredTextFullToLeanTest.class);
		addTestSuite(StructuredTextExtensionsTest.class);
		addTestSuite(StructuredTextMathTest.class);
		addTestSuite(StructuredTextSomeMoreTest.class);
		addTestSuite(StructuredTextProcessorTest.class);
		addTestSuite(StructuredTextStringRecordTest.class);
	}
}