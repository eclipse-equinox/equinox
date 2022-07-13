/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.util.NLS;

public class NLSTestCase extends CoreTest {

	public void testEmptyMessageBug200296() {
		try {
			NLS.bind("", Integer.valueOf(0));
		} catch (NegativeArraySizeException e) {
			fail("1.0", e);
		}
	}

}
