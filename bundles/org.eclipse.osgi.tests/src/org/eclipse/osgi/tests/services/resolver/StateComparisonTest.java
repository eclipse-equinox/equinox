/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.resolver;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;

public class StateComparisonTest extends AbstractStateTest {

	public StateComparisonTest(String testName) {
		super(testName);
	}

	public void testAddition() throws BundleException {
		State state1 = buildEmptyState();
		State state2 = state1.getFactory().createState(state1);
		StateDelta delta = state1.compare(state2);
		assertEquals("1.0", 0, delta.getChanges().length);
		delta = state2.compare(state1);
		assertEquals("1.1", 0, delta.getChanges().length);
		String A_MANIFEST = "Bundle-SymbolicName: org.eclipse.a\nBundle-Version: 1.0\n";
		BundleDescription bundleA = state2.getFactory().createBundleDescription(parseManifest(A_MANIFEST), "org.eclipse.a", -1);
		assertTrue("2.0", state2.addBundle(bundleA));
		delta = state1.compare(state2);
		assertEquals("2.1", 1, delta.getChanges().length);
		BundleDelta removal = delta.getChanges()[0];
		assertEquals("2.2", bundleA, removal.getBundle());
		assertEquals("2.3", BundleDelta.REMOVED, removal.getType());
		delta = state2.compare(state1);
		assertEquals("3.1", 1, delta.getChanges().length);
		BundleDelta addition = delta.getChanges()[0];
		assertEquals("3.2", bundleA, addition.getBundle());
		assertEquals("3.3", BundleDelta.ADDED, addition.getType());
	}

	public void testRemoval() throws BundleException {
		State state1 = buildSimpleState();
		State state2 = state1.getFactory().createState(state1);
		StateDelta delta = state1.compare(state2);
		assertEquals("1.0", 0, delta.getChanges().length);
		delta = state2.compare(state1);
		assertEquals("1.1", 0, delta.getChanges().length);
		BundleDescription bundle1 = state1.getBundleByLocation("org.eclipse.b1");
		assertNotNull("1.9", bundle1);
		assertTrue("2.0", state1.removeBundle(bundle1));
		delta = state1.compare(state2);
		assertEquals("2.1", 1, delta.getChanges().length);
		BundleDelta removal = delta.getChanges()[0];
		assertEquals("2.2", bundle1, removal.getBundle());
		assertEquals("2.3", BundleDelta.REMOVED, removal.getType());
		delta = state2.compare(state1);
		assertEquals("3.1", 1, delta.getChanges().length);
		BundleDelta addition = delta.getChanges()[0];
		assertEquals("3.2", bundle1, addition.getBundle());
		assertEquals("3.3", BundleDelta.ADDED, addition.getType());
	}

	public void testUpdate() throws BundleException {
		State state1 = buildSimpleState();
		State state2 = state1.getFactory().createState(state1);
		StateDelta delta = state1.compare(state2);
		assertEquals("1.0", 0, delta.getChanges().length);
		delta = state2.compare(state1);
		assertEquals("1.1", 0, delta.getChanges().length);
		assertNotNull("1.9", state1.getBundleByLocation("org.eclipse.b1"));
		String A_MANIFEST = "Bundle-SymbolicName: org.eclipse.b1\nBundle-Version: 2.0\n";
		BundleDescription bundle1 = state1.getFactory().createBundleDescription(parseManifest(A_MANIFEST), "org.eclipse.b1", 1);
		assertTrue("2.0", state1.updateBundle(bundle1));
		delta = state1.compare(state2);
		assertEquals("2.1", 1, delta.getChanges().length);
		BundleDelta update = delta.getChanges()[0];
		assertEquals("2.2", bundle1, update.getBundle());
		assertEquals("2.3", BundleDelta.UPDATED, update.getType());
		delta = state2.compare(state1);
		assertEquals("3.1", 0, delta.getChanges().length);
	}

	public static Test suite() {
		return new TestSuite(StateComparisonTest.class);
	}
}
