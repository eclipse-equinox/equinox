/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.osgi.tests.services.resolver;

import java.io.File;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class DisabledInfoTest extends AbstractStateTest {
	private final String B1_LOCATION = "b1"; //$NON-NLS-1$
	private final String B2_LOCATION = "b2"; //$NON-NLS-1$
	private final String B3_LOCATION = "b3"; //$NON-NLS-1$
	private final String POLICY = "test.policy"; //$NON-NLS-1$

	public static Test suite() {
		return new TestSuite(DisabledInfoTest.class);
	}

	public DisabledInfoTest(String name) {
		super(name);
	}

	public void testDisabledInfo01() throws BundleException {
		State state = buildTestState();
		BundleDescription b1 = state.getBundleByLocation(B1_LOCATION);
		BundleDescription b2 = state.getBundleByLocation(B2_LOCATION);
		BundleDescription b3 = state.getBundleByLocation(B3_LOCATION);

		DisabledInfo info1 = new DisabledInfo(POLICY, "message 1", b1); //$NON-NLS-1$
		DisabledInfo info2 = new DisabledInfo(POLICY, "message 1", b2); //$NON-NLS-1$
		DisabledInfo info3 = new DisabledInfo(POLICY, "message 1", b3); //$NON-NLS-1$

		state.addDisabledInfo(info1);
		state.addDisabledInfo(info2);
		state.addDisabledInfo(info3);

		State copy = state.getFactory().createState(state);
		BundleDescription copyB1 = copy.getBundleByLocation(B1_LOCATION);
		BundleDescription copyB2 = copy.getBundleByLocation(B2_LOCATION);
		BundleDescription copyB3 = copy.getBundleByLocation(B3_LOCATION);

		DisabledInfo copyInfo1 = copy.getDisabledInfo(copyB1, POLICY);
		DisabledInfo copyInfo2 = copy.getDisabledInfo(copyB2, POLICY);
		DisabledInfo copyInfo3 = copy.getDisabledInfo(copyB3, POLICY);

		assertNotNull("copyInfo1", copyInfo1); //$NON-NLS-1$
		assertNotNull("copyInfo2", copyInfo2); //$NON-NLS-1$
		assertNotNull("copyInfo3", copyInfo3); //$NON-NLS-1$

		assertTrue("copyInfo1 bundle", copyInfo1.getBundle() == copyB1); //$NON-NLS-1$
		assertTrue("copyInfo2 bundle", copyInfo2.getBundle() == copyB2); //$NON-NLS-1$
		assertTrue("copyInfo3 bundle", copyInfo3.getBundle() == copyB3); //$NON-NLS-1$
	}

	public void testDisabledInfo02() throws BundleException {
		State state = buildTestState();
		BundleDescription b1 = state.getBundleByLocation(B1_LOCATION);
		BundleDescription b2 = state.getBundleByLocation(B2_LOCATION);
		BundleDescription b3 = state.getBundleByLocation(B3_LOCATION);

		DisabledInfo info1 = new DisabledInfo(POLICY, "message 1", b1); //$NON-NLS-1$
		DisabledInfo info2 = new DisabledInfo(POLICY, "message 1", b2); //$NON-NLS-1$
		DisabledInfo info3 = new DisabledInfo(POLICY, "message 1", b3); //$NON-NLS-1$

		state.resolve();
		assertTrue("b1 resolved", b1.isResolved()); //$NON-NLS-1$
		assertTrue("b2 resolved", b2.isResolved()); //$NON-NLS-1$
		assertTrue("b3 resolved", b3.isResolved()); //$NON-NLS-1$

		state.addDisabledInfo(info1);
		state.addDisabledInfo(info2);
		state.addDisabledInfo(info3);

		state.resolve(false);
		assertFalse("b1 resolved", b1.isResolved()); //$NON-NLS-1$
		assertFalse("b2 resolved", b2.isResolved()); //$NON-NLS-1$
		assertFalse("b3 resolved", b3.isResolved()); //$NON-NLS-1$
	}

	public void testDisabledInfo03() throws BundleException {
		State state = buildTestState();
		BundleDescription b1 = state.getBundleByLocation(B1_LOCATION);
		BundleDescription b2 = state.getBundleByLocation(B2_LOCATION);
		BundleDescription b3 = state.getBundleByLocation(B3_LOCATION);

		DisabledInfo info1 = new DisabledInfo(POLICY, "message 1", b1); //$NON-NLS-1$
		DisabledInfo info2 = new DisabledInfo(POLICY, "message 1", b2); //$NON-NLS-1$
		DisabledInfo info3 = new DisabledInfo(POLICY, "message 1", b3); //$NON-NLS-1$

		state.addDisabledInfo(info1);
		state.addDisabledInfo(info2);
		state.addDisabledInfo(info3);
		BundleContext context = OSGiTestsActivator.getContext();
		File stateDir = context.getDataFile("testDisabledInfo03"); //$NON-NLS-1$
		stateDir.mkdirs();
		try {
			state.getFactory().writeState(state, stateDir);
			State copy = state.getFactory().readState(stateDir);
			BundleDescription copyB1 = copy.getBundleByLocation(B1_LOCATION);
			BundleDescription copyB2 = copy.getBundleByLocation(B2_LOCATION);
			BundleDescription copyB3 = copy.getBundleByLocation(B3_LOCATION);
			DisabledInfo copyInfo1 = copy.getDisabledInfo(copyB1, POLICY);
			DisabledInfo copyInfo2 = copy.getDisabledInfo(copyB2, POLICY);
			DisabledInfo copyInfo3 = copy.getDisabledInfo(copyB3, POLICY);

			assertNotNull("copyInfo1", copyInfo1); //$NON-NLS-1$
			assertNotNull("copyInfo2", copyInfo2); //$NON-NLS-1$
			assertNotNull("copyInfo3", copyInfo3); //$NON-NLS-1$

			assertTrue("copyInfo1 bundle", copyInfo1.getBundle() == copyB1); //$NON-NLS-1$
			assertTrue("copyInfo2 bundle", copyInfo2.getBundle() == copyB2); //$NON-NLS-1$
			assertTrue("copyInfo3 bundle", copyInfo3.getBundle() == copyB3); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testDisabledInfo04() throws BundleException {
		State state = buildTestState();
		BundleDescription b1 = state.getBundleByLocation(B1_LOCATION);
		BundleDescription b2 = state.getBundleByLocation(B2_LOCATION);
		BundleDescription b3 = state.getBundleByLocation(B3_LOCATION);

		DisabledInfo info1 = new DisabledInfo(POLICY, "message 1", b1); //$NON-NLS-1$
		DisabledInfo info2 = new DisabledInfo(POLICY, "message 1", b2); //$NON-NLS-1$
		DisabledInfo info3 = new DisabledInfo(POLICY, "message 1", b3); //$NON-NLS-1$

		state.resolve();
		assertTrue("b1 resolved", b1.isResolved()); //$NON-NLS-1$
		assertTrue("b2 resolved", b2.isResolved()); //$NON-NLS-1$
		assertTrue("b3 resolved", b3.isResolved()); //$NON-NLS-1$

		state.addDisabledInfo(info1);
		state.addDisabledInfo(info2);
		state.addDisabledInfo(info3);

		BundleDescription[] disabledBundles = state.getDisabledBundles();
		assertNotNull("disabledBundles", disabledBundles); //$NON-NLS-1$
		assertEquals("disabledBundles length", 3, disabledBundles.length); //$NON-NLS-1$
		assertTrue("b1 not found", disabledBundles[0] == b1 || disabledBundles[1] == b1 || disabledBundles[2] == b1); //$NON-NLS-1$
		assertTrue("b2 not found", disabledBundles[0] == b2 || disabledBundles[1] == b2 || disabledBundles[2] == b2); //$NON-NLS-1$
		assertTrue("b3 not found", disabledBundles[0] == b3 || disabledBundles[1] == b3 || disabledBundles[2] == b3); //$NON-NLS-1$

		state.removeDisabledInfo(info1);
		disabledBundles = state.getDisabledBundles();
		assertNotNull("disabledBundles", disabledBundles); //$NON-NLS-1$
		assertEquals("disabledBundles length", 2, disabledBundles.length); //$NON-NLS-1$
		assertTrue("b2 not found", disabledBundles[0] == b2 || disabledBundles[1] == b2); //$NON-NLS-1$
		assertTrue("b3 not found", disabledBundles[0] == b3 || disabledBundles[1] == b3); //$NON-NLS-1$

		state.removeDisabledInfo(info2);
		disabledBundles = state.getDisabledBundles();
		assertNotNull("disabledBundles", disabledBundles); //$NON-NLS-1$
		assertEquals("disabledBundles length", 1, disabledBundles.length); //$NON-NLS-1$
		assertTrue("b3 not found", disabledBundles[0] == b3); //$NON-NLS-1$

		state.removeDisabledInfo(info3);
		disabledBundles = state.getDisabledBundles();
		assertNotNull("disabledBundles", disabledBundles); //$NON-NLS-1$
		assertEquals("disabledBundles length", 0, disabledBundles.length); //$NON-NLS-1$
	}

	public void testBug251427() throws BundleException {
		State state = buildTestState();
		BundleDescription b1 = state.getBundleByLocation(B1_LOCATION);
		BundleDescription b2 = state.getBundleByLocation(B2_LOCATION);

		DisabledInfo info11 = new DisabledInfo(POLICY, "message 1", b1); //$NON-NLS-1$
		DisabledInfo info12 = new DisabledInfo(POLICY, "message 1", b1); //$NON-NLS-1$
		DisabledInfo info21 = new DisabledInfo(POLICY, "message 1", b2); //$NON-NLS-1$
		DisabledInfo info22 = new DisabledInfo(POLICY, "message 1", b2); //$NON-NLS-1$

		assertEquals("infos not equal", info11, info12); //$NON-NLS-1$
		assertEquals("infos hashCode not equal", info11.hashCode(), info12.hashCode()); //$NON-NLS-1$
		assertEquals("infos not equal", info21, info22); //$NON-NLS-1$
		assertEquals("infos hashCode not equal", info21.hashCode(), info22.hashCode()); //$NON-NLS-1$

	}

	private State buildTestState() throws BundleException {
		State state = buildEmptyState();
		final String B1_MANIFEST = "Bundle-ManifestVersion: 1\n" + "Bundle-SymbolicName: b1\n" + "Bundle-Version: 1.0\n" + "Import-Package: b2"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		BundleDescription b1 = state.getFactory().createBundleDescription(state, parseManifest(B1_MANIFEST), B1_LOCATION, 1);
		state.addBundle(b1);
		final String B2_MANIFEST = "Bundle-ManifestVersion: 1\n" + "Bundle-SymbolicName: b2\n" + "Bundle-Version: 1.0\n" + "Export-Package: b2\n" + "Import-Package: b3"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		BundleDescription b2 = state.getFactory().createBundleDescription(state, parseManifest(B2_MANIFEST), B2_LOCATION, 2);
		state.addBundle(b2);
		final String B3_MANIFEST = "Bundle-ManifestVersion: 1\n" + "Bundle-SymbolicName: b3\n" + "Bundle-Version: 1.0\n" + "Export-Package: b3; version=1.0"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		BundleDescription b3 = state.getFactory().createBundleDescription(state, parseManifest(B3_MANIFEST), B3_LOCATION, 3);
		state.addBundle(b3);
		return state;
	}
}
