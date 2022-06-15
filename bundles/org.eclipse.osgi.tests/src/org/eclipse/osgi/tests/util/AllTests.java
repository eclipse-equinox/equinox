/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
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

package org.eclipse.osgi.tests.util;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ //
		TestCaseinsensitiveMap.class, //
		ObjectPoolTestCase.class, //
		ManifestElementTestCase.class, //
		NLSTestCase.class, //
		StorageUtilTestCase.class, //
		BidiTextProcessorTestCase.class, //
		LatinTextProcessorTestCase.class, //
})
public class AllTests {
}
