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

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Test;

/**
 * Test cases for the Path class.
 */
public class OperationCanceledExceptionTest {

	@Test
	public void testCoreException() {
		final String MESSAGE_STRING = "An exception has occurred";
		OperationCanceledException e = new OperationCanceledException(MESSAGE_STRING);

		assertEquals("1.0", MESSAGE_STRING, e.getMessage());
	}
}
