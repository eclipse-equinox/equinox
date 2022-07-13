/*******************************************************************************
 * Copyright (C) 2020, Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.common.tests.text;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	StringMatcherFindTest.class,
	StringMatcherPlainTest.class,
	StringMatcherWildcardTest.class,
	StringMatcherPrefixTest.class,
	StringMatcherOtherTest.class
})
public class StringMatcherTests {
	// empty
}
