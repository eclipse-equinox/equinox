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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.junit.Test;

/**
 * Test cases for the Path class.
 */
public class CoreExceptionTest {

	@Test
	public void testCoreException() {
		final String MESSAGE_STRING = "An exception has occurred";
		IStatus status = new Status(IStatus.ERROR, "org.eclipse.core.tests.runtime", 31415, MESSAGE_STRING,
				new NumberFormatException());

		CoreException e = new CoreException(status);

		assertEquals("1.0", status, e.getStatus());
		assertEquals("1.1", MESSAGE_STRING, e.getMessage());
	}
}
