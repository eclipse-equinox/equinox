/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.bidi.tests;

import org.eclipse.equinox.bidi.internal.tests.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({StructuredTextExtensibilityTest.class, StructuredTextMethodsTest.class, StructuredTextFullToLeanTest.class, StructuredTextExtensionsTest.class, StructuredTextMathTest.class, StructuredTextSomeMoreTest.class, StructuredTextProcessorTest.class, StructuredTextStringRecordTest.class})
public class StructuredTextTestSuite {
	//intentionally left blank
}
