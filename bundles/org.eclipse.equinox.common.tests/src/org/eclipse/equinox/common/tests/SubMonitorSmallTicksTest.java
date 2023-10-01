/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.SubMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Ensures that creating a SubMonitor with a small number of ticks will not
 * prevent it from reporting accurate progress.
 */
public class SubMonitorSmallTicksTest {

	private TestProgressMonitor topmostMonitor;
	private SubMonitor smallTicksChild;

	private static int TOTAL_WORK = 1000;

	@Before
	public void setUp() throws Exception {
		topmostMonitor = new TestProgressMonitor();
		smallTicksChild = SubMonitor.convert(topmostMonitor, 10);
	}

	@Test
	public void testWorked() {
		SubMonitor bigTicksChild = smallTicksChild.newChild(10).setWorkRemaining(TOTAL_WORK);
		for (int i = 0; i < TOTAL_WORK; i++) {
			bigTicksChild.worked(1);
		}
		bigTicksChild.done();
	}

	@Test
	public void testInternalWorked() {
		double delta = 10.0d / TOTAL_WORK;

		for (int i = 0; i < TOTAL_WORK; i++) {
			smallTicksChild.internalWorked(delta);
		}
	}

	@Test
	public void testSplit() {
		SubMonitor bigTicksChild = smallTicksChild.newChild(10).setWorkRemaining(TOTAL_WORK);
		for (int i = 0; i < TOTAL_WORK; i++) {
			bigTicksChild.split(1);
		}
		bigTicksChild.done();
	}

	@After
	public void tearDown() throws Exception {
		smallTicksChild.done();
		topmostMonitor.done();
		topmostMonitor.assertOptimal();
	}

}
