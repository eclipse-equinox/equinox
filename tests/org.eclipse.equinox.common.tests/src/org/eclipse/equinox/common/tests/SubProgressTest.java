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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.equinox.common.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

@SuppressWarnings("deprecation")
public class SubProgressTest {

	/**
	 * <p>Depth of the chain chain of progress monitors. In all of the tests, we create
	 * a nested chain of progress monitors rathar than a single monitor, to test its
	 * scalability under recursion. We pick a number representing a moderately deep
	 * recursion, but is still small enough that it could correspond to a real call stack
	 * without causing overflow.</p>
	 *
	 * <p>Note: changing this constant will invalidate comparisons with old performance data.</p>
	 */
	public static final int CHAIN_DEPTH = 100;
	/**
	 * <p>Number of calls to worked() within each test. This was chosen to be significantly larger
	 * than 1000 to test how well the monitor can optimize unnecessary resolution
	 * in reported progress, but small enough that the test completes in a reasonable
	 * amount of time.</p>
	 *
	 * <p>Note: changing this constant will invalidate comparisons with old performance data.</p>
	 */
	public static final int PROGRESS_SIZE = 100000;

	@Rule
	public TestName name = new TestName();

	/**
	 * Tests the style bits in SubProgressMonitor
	 * @deprecated to suppress deprecation warnings
	 */
	@Deprecated
	@Test
	public void testStyles() {

		int[] styles = new int[] {0, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK | SubProgressMonitor.SUPPRESS_SUBTASK_LABEL};

		HashMap<String, String[]> expected = new HashMap<>();
		expected.put("style 0 below style 2", new String[] {"setTaskName0", "", "setTaskName1"});
		expected.put("style 2 below style 0", new String[] {"setTaskName1", "beginTask1 ", "setTaskName1"});
		expected.put("style 6 below style 0", new String[] {"setTaskName1", "beginTask1 ", "setTaskName1"});
		expected.put("style 2 below style 4", new String[] {"setTaskName1", "beginTask0 beginTask1 ", "setTaskName1"});
		expected.put("style 0 below style 0", new String[] {"setTaskName0", "subTask1", "setTaskName1"});
		expected.put("style 6 as top-level monitor", new String[] {"", "", "setTaskName0"});
		expected.put("style 6 below style 2", new String[] {"setTaskName1", "", "setTaskName1"});
		expected.put("style 6 below style 6", new String[] {"setTaskName1", "", "setTaskName1"});
		expected.put("style 0 below style 6", new String[] {"setTaskName0", "", "setTaskName1"});
		expected.put("style 4 below style 2", new String[] {"setTaskName1", "", "setTaskName1"});
		expected.put("style 0 as top-level monitor", new String[] {"", "subTask0", "setTaskName0"});
		expected.put("style 0 below style 4", new String[] {"setTaskName0", "beginTask0 subTask1", "setTaskName1"});
		expected.put("style 4 below style 0", new String[] {"setTaskName1", "beginTask1 subTask1", "setTaskName1"});
		expected.put("style 4 as top-level monitor", new String[] {"", "beginTask0 subTask0", "setTaskName0"});
		expected.put("style 2 below style 6", new String[] {"setTaskName1", "", "setTaskName1"});
		expected.put("style 4 below style 6", new String[] {"setTaskName1", "", "setTaskName1"});
		expected.put("style 2 below style 2", new String[] {"setTaskName1", "", "setTaskName1"});
		expected.put("style 2 as top-level monitor", new String[] {"", "", "setTaskName0"});
		expected.put("style 6 below style 4", new String[] {"setTaskName1", "beginTask0 beginTask1 ", "setTaskName1"});
		expected.put("style 4 below style 4", new String[] {"setTaskName1", "beginTask0 beginTask1 subTask1", "setTaskName1"});
		HashMap<String, String[]> results = new HashMap<>();

		for (int style : styles) {
			TestProgressMonitor top = new TestProgressMonitor();
			top.beginTask("", 100);
			SubProgressMonitor child = new SubProgressMonitor(top, 100, style);

			String testName = "style " + style + " as top-level monitor";
			results.put(testName, runChildTest(0, top, child, 100 * styles.length));

			for (int innerStyle : styles) {
				SubProgressMonitor innerChild = new SubProgressMonitor(child, 100, innerStyle);
				testName = "style " + innerStyle + " below style " + style;
				results.put(testName, runChildTest(1, top, innerChild, 100));
				innerChild.done();
			}
			child.done();
		}

		// Output the code for the observed results, in case one of them has changed intentionally
		for (Map.Entry<String, String[]> entry : results.entrySet()) {
			String[] expectedResult = expected.get(entry.getKey());
			String[] value = entry.getValue();
			assertArrayEquals(value, expectedResult);
		}

	}

	private String[] runChildTest(int depth, TestProgressMonitor root, IProgressMonitor child, int ticks) {
		ArrayList<String> results = new ArrayList<>();
		child.beginTask("beginTask" + depth, ticks);
		results.add(root.getTaskName());
		child.subTask("subTask" + depth);
		results.add(root.getSubTaskName());
		child.setTaskName("setTaskName" + depth);
		results.add(root.getTaskName());
		return results.toArray(new String[results.size()]);
	}

	/**
	 * Tests SubProgressMonitor nesting when using the default constructor. (Tests
	 * parents in floating point mode)
	 * @deprecated to suppress deprecation warnings
	 */
	@Deprecated
	@Test
	public void testConstructorNestingFP() {
		TestProgressMonitor top = new TestProgressMonitor();
		top.beginTask("", 2000);

		// Create an SPM, put it in floating-point mode, and consume half its work
		SubProgressMonitor fpMonitor = new SubProgressMonitor(top, 1000);
		fpMonitor.beginTask("", 100);
		fpMonitor.internalWorked(50.0);
		fpMonitor.internalWorked(-10.0); // should have no effect

		assertEquals(500.0, top.getTotalWork(), 0.01d);

		// Create a child monitor, and ensure that it grabs the correct amount of work
		// from the parent.
		SubProgressMonitor childMonitor = new SubProgressMonitor(fpMonitor, 20);
		childMonitor.beginTask("", 100);
		childMonitor.worked(100);
		childMonitor.done();

		assertEquals(700.0, top.getTotalWork(), 0.01d);

		// Create a child monitor, and ensure that it grabs the correct amount of work
		// from the parent.
		SubProgressMonitor childMonitor2 = new SubProgressMonitor(fpMonitor, 30);
		childMonitor2.beginTask("", 100);
		childMonitor2.worked(100);
		childMonitor2.done();

		assertEquals(1000.0, top.getTotalWork(), 0.01d);

		// Ensure that creating another child will have no effect
		SubProgressMonitor childMonitor3 = new SubProgressMonitor(fpMonitor, 10);
		childMonitor3.beginTask("", 100);
		childMonitor3.worked(100);
		childMonitor3.done();

		assertEquals(1000.0, top.getTotalWork(), 0.01d);
		fpMonitor.worked(100);
		assertEquals(1000.0, top.getTotalWork(), 0.01d);
		fpMonitor.done();
		assertEquals(1000.0, top.getTotalWork(), 0.01d);
	}

	/**
	 * Tests SubProgressMonitor nesting when using the default constructor. Tests constructors
	 * in int mode.
	 * @deprecated to suppress deprecation warnings
	 */
	@Deprecated
	@Test
	public void testConstructorNestingInt() {
		TestProgressMonitor top = new TestProgressMonitor();
		top.beginTask("", 2000);

		// Create an SPM leave it in int mode, and consume half its work
		SubProgressMonitor fpMonitor = new SubProgressMonitor(top, 1000);
		fpMonitor.beginTask("", 100);
		fpMonitor.worked(50);

		assertEquals(500.0, top.getTotalWork(), 0.01d);

		// Create a child monitor, and ensure that it grabs the correct amount of work
		// from the parent.
		SubProgressMonitor childMonitor = new SubProgressMonitor(fpMonitor, 20);
		childMonitor.beginTask("", 100);
		childMonitor.worked(100);
		childMonitor.done();

		assertEquals(700.0, top.getTotalWork(), 0.01d);

		// Create a child monitor, and ensure that it grabs the correct amount of work
		// from the parent.
		SubProgressMonitor childMonitor2 = new SubProgressMonitor(fpMonitor, 30);
		childMonitor2.beginTask("", 100);
		childMonitor2.worked(100);
		childMonitor2.done();

		assertEquals(1000.0, top.getTotalWork(), 0.01d);

		// Ensure that creating another child will have no effect
		SubProgressMonitor childMonitor3 = new SubProgressMonitor(fpMonitor, 10);
		childMonitor3.beginTask("", 100);
		childMonitor3.worked(100);
		childMonitor3.done();

		assertEquals(1000.0, top.getTotalWork(), 0.01d);
		fpMonitor.worked(100);
		assertEquals(1000.0, top.getTotalWork(), 0.01d);
		fpMonitor.done();
		assertEquals(1000.0, top.getTotalWork(), 0.01d);
	}

	/**
	 * Tests the automatic cleanup when progress monitors are created via their constructor
	 * @deprecated to suppress deprecation warnings
	 */
	@Deprecated
	@Test
	public void testParallelChildren() {
		TestProgressMonitor top = new TestProgressMonitor();
		top.beginTask("", 1000);
		SubProgressMonitor mon = new SubProgressMonitor(top, 1000);
		mon.beginTask("", 1000);

		SubProgressMonitor monitor1 = new SubProgressMonitor(mon, 200);
		SubProgressMonitor monitor2 = new SubProgressMonitor(mon, 200);

		assertEquals("Ensure no work has been reported yet", 0.0, top.getTotalWork(), 0.01d);
		monitor1.beginTask("", 1000);
		assertEquals("Ensure no work has been reported yet", 0.0, top.getTotalWork(), 0.01d);
		monitor2.beginTask("", 1000);
		assertEquals("Should not have cleaned up monitor 1", 0.0, top.getTotalWork(), 0.01d);
		monitor1.done();

		assertEquals("Should have cleaned up monitor 1", 200.0, top.getTotalWork(), 0.01d);
		monitor1.worked(1000);
		assertEquals("Monitor1 shouldn't report work once it's complete", 200.0, top.getTotalWork(), 0.01d);
		monitor2.worked(500);
		assertEquals(300.0, top.getTotalWork(), 0.01d);

		// Create a monitor that will leak - monitors won't be auto-completed until their done methods are
		// called
		SubProgressMonitor monitor3 = new SubProgressMonitor(mon, 300);
		assertEquals("Monitor2 should not have been cleaned up yet", 300.0, top.getTotalWork(), 0.01d);
		SubProgressMonitor monitor4 = new SubProgressMonitor(mon, 300);
		monitor4.beginTask("", 100);
		mon.done();
		Assert.assertNotNull(monitor3);

		assertEquals("All leaked work should have been collected", 1000.0, top.getTotalWork(), 0.01d);
	}

	/**
	 * @deprecated to suppress deprecation warnings
	 */
	@Deprecated
	@Test
	public void testCancellation() {
		TestProgressMonitor root = new TestProgressMonitor();
		root.beginTask("", 1000);

		SubProgressMonitor spm = new SubProgressMonitor(root, 1000);

		// Test that changes at the root propogate to the child
		root.setCanceled(true);
		assertTrue(spm.isCanceled());
		root.setCanceled(false);
		assertFalse(spm.isCanceled());

		// Test that changes to the child propogate to the root
		spm.setCanceled(true);
		assertTrue(root.isCanceled());
		spm.setCanceled(false);
		assertFalse(root.isCanceled());

		// Test a chain of depth 2
		spm.beginTask("", 1000);
		SubProgressMonitor spm2 = new SubProgressMonitor(spm, 1000);

		// Test that changes at the root propogate to the child
		root.setCanceled(true);
		assertTrue(spm2.isCanceled());
		root.setCanceled(false);
		assertFalse(spm2.isCanceled());

		// Test that changes to the child propogate to the root
		spm2.setCanceled(true);
		assertTrue(root.isCanceled());
		spm2.setCanceled(false);
		assertFalse(root.isCanceled());
	}

	/**
	 * Tests creating progress monitors under a custom progress monitor parent. This
	 * is the same as the performance test as the same name, but it verifies
	 * correctness rather than performance.
	 */
	@Test
	public void testCreateChildrenUnderCustomParent() {
		TestProgressMonitor monitor = new TestProgressMonitor();
		createChildrenUnderParent(monitor, SubProgressTest.PROGRESS_SIZE);

		// We don't actually expect the progress to be optimal in this case since the progress monitor wouldn't
		// know what it was rooted under and would have had to report more progress than necessary... but we
		// should be able to check that there was no redundancy.

		assertEquals(0, monitor.getRedundantWorkCalls());
		assertTrue(monitor.getWorkCalls() >= 100);
	}

	/**
	 * Creates and destroys the given number of child progress monitors under the given parent.
	 *
	 * @param monitor monitor to create children under. The caller must call done on this monitor
	 * if necessary.
	 * @param progressSize total number of children to create.
	 *
	 * @deprecated to suppress deprecation warnings
	 */
	@Deprecated
	private static void createChildrenUnderParent(IProgressMonitor monitor, int progressSize) {
		monitor.beginTask("", progressSize);

		for (int count = 0; count < progressSize; count++) {
			SubProgressMonitor mon = new SubProgressMonitor(monitor, 1);
			mon.beginTask("", 100);
			mon.done();
		}
	}

	/**
	 * Test SubProgressMonitor's created with negative a work value.
	 */
	@Test
	public void testNegativeWorkValues() {
		TestProgressMonitor top = new TestProgressMonitor();
		top.beginTask("", 10);

		SubProgressMonitor childMonitor = new SubProgressMonitor(top, IProgressMonitor.UNKNOWN); // -1
		childMonitor.beginTask("", 10);
		childMonitor.worked(5);
		assertEquals(0.0, top.getTotalWork(), 0.01d);
		childMonitor.done();
		assertEquals(0.0, top.getTotalWork(), 0.01d);

		top.done();
	}

}
