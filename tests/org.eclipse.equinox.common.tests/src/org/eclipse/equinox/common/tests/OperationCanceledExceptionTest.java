/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.common.tests;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.tests.harness.CoreTest;

/**
 * Test cases for the Path class.
 */
public class OperationCanceledExceptionTest extends CoreTest {
	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public OperationCanceledExceptionTest() {
		super(null);
	}

	public OperationCanceledExceptionTest(String name) {
		super(name);
	}

	public void testCoreException() {
		final String MESSAGE_STRING = "An exception has occurred";
		OperationCanceledException e = new OperationCanceledException(MESSAGE_STRING);

		assertEquals("1.0", MESSAGE_STRING, e.getMessage());
	}
}
