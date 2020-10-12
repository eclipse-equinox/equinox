/*******************************************************************************
 * Copyright (c) 2020 Christoph Laeubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Laeubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;


public class SliceProgressMonitorTest {

	@Test
	public void testIgnoresCancel() {
		TestProgressMonitor monitor = new TestProgressMonitor();
		monitor.beginTask("Test", 100);
		IProgressMonitor slice = monitor.slice(10);
		slice.setCanceled(true);
		assertTrue(slice.isCanceled());
		assertFalse(monitor.isCanceled());
	}

	@Test
	public void testPropagatesCancel() {
		TestProgressMonitor monitor = new TestProgressMonitor();
		monitor.beginTask("Test", 100);
		IProgressMonitor slice = monitor.slice(10);
		monitor.setCanceled(true);
		assertTrue(slice.isCanceled());
	}

	@Test
	public void testDuplicateBeginTask() {
		TestProgressMonitor monitor = new TestProgressMonitor();
		monitor.beginTask("Test", 100);
		IProgressMonitor slice = monitor.slice(10);
		slice.beginTask(null, 0);
		assertThrows(IllegalStateException.class, () -> slice.beginTask(null, 0));
	}

	@Test
	public void testConsumeOnDone() {
		TestProgressMonitor monitor = new TestProgressMonitor();
		monitor.beginTask("Test", 100);
		IProgressMonitor slice = monitor.slice(10);
		slice.done();
		assertEquals(10, (int) monitor.getTotalWork());
	}

	@Test
	public void testSubProgressIsBigger() {
		TestProgressMonitor monitor = new TestProgressMonitor();
		monitor.beginTask("Test", 100);
		IProgressMonitor slice = monitor.slice(10);
		slice.beginTask(null, 50);
		slice.worked(25);
		assertEquals(5, (int) monitor.getTotalWork());
		slice.done();
		assertEquals(10, (int) monitor.getTotalWork());
	}

	@Test
	public void testSubProgressIsSmaller() {
		TestProgressMonitor monitor = new TestProgressMonitor();
		monitor.beginTask("Test", 100);
		IProgressMonitor slice = monitor.slice(50);
		slice.beginTask(null, 10);
		slice.worked(5);
		assertEquals(25, (int) monitor.getTotalWork());
		slice.worked(5);
		assertEquals(50, (int) monitor.getTotalWork());
	}

	@Test
	public void testSubProgressIsNeverGreater() {
		TestProgressMonitor monitor = new TestProgressMonitor();
		monitor.beginTask("Test", 100);
		IProgressMonitor slice = monitor.slice(50);
		slice.beginTask(null, 50);
		slice.worked(60);
		assertEquals(50, (int) monitor.getTotalWork());
		slice.done();
		assertEquals(50, (int) monitor.getTotalWork());
	}

	@Test
	public void testSubProgressIsIndependent() {
		TestProgressMonitor monitor = new TestProgressMonitor();
		monitor.beginTask("Test", 100);
		IProgressMonitor slice1 = monitor.slice(50);
		IProgressMonitor slice2 = monitor.slice(50);
		assertEquals(0, (int) monitor.getTotalWork());
		slice1.beginTask(null, 100);
		slice2.beginTask(null, 200);
		slice2.worked(10);
		assertEquals(2, (int) monitor.getTotalWork());
		slice1.worked(5);
		assertEquals(4, (int) monitor.getTotalWork());
		slice2.done();
		assertEquals(52, (int) monitor.getTotalWork());
		slice1.done();
		assertEquals(100, (int) monitor.getTotalWork());
	}
}
