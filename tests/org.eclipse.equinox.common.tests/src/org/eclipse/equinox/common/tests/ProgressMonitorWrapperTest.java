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

import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.CoreTest;

/**
 * Test cases for the Path class.
 */
public class ProgressMonitorWrapperTest extends CoreTest {
	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public ProgressMonitorWrapperTest() {
		super(null);
	}

	public ProgressMonitorWrapperTest(String name) {
		super(name);
	}

	/**
	 * @deprecated to suppress deprecation warnings
	 */
	@Deprecated
	public void testProgressMonitorWrapper() {
		NullProgressMonitor nullMonitor = new NullProgressMonitor();
		SubProgressMonitor wrapped = new SubProgressMonitor(nullMonitor, 10);
		ProgressMonitorWrapper wrapper = new ProgressMonitorWrapper(wrapped) {};

		assertSame("1.0", nullMonitor, wrapped.getWrappedProgressMonitor());
		assertSame("1.1", wrapped, wrapper.getWrappedProgressMonitor());

		assertTrue("1.2", !nullMonitor.isCanceled());
		assertTrue("1.3", !wrapped.isCanceled());
		assertTrue("1.4", !wrapper.isCanceled());

		nullMonitor.setCanceled(true);
		assertTrue("1.5", nullMonitor.isCanceled());
		assertTrue("1.6", wrapped.isCanceled());
		assertTrue("1.7", wrapper.isCanceled());
	}
}
