/*******************************************************************************
 * Copyright (c) 2026 Patrick Ziegler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Ziegler - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.tests.resolver;

import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Properties;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.tests.services.resolver.AbstractStateTest;
import org.junit.Test;
import org.osgi.framework.BundleException;

@SuppressWarnings("deprecation") // StateObjectFactory.createBundleDescription()
public class TestSubstitution extends AbstractStateTest {

	BundleDescription bundle_1 = null;
	BundleDescription bundle_2 = null;
	BundleDescription bundle_3 = null;

	@Test
	public void testSubstitutePackage() throws BundleException {
		State state = buildEmptyState();
		StateObjectFactory sof = StateObjectFactory.defaultFactory;

		bundle_1 = create_bundle_1(sof);
		bundle_2 = create_bundle_2(sof);
		bundle_3 = create_bundle_3(sof);

		assertTrue("failed to add bundle", state.addBundle(bundle_1));
		assertTrue("failed to add bundle", state.addBundle(bundle_2));
		assertTrue("failed to add bundle", state.addBundle(bundle_3));

		state.resolve();

		assertTrue("unexpected bundle resolution state", bundle_1.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_2.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_3.isResolved());
	}

	public BundleDescription create_bundle_1(StateObjectFactory sof) throws BundleException {
		Dictionary dictionary_1 = new Properties();
		dictionary_1.put("Bundle-ManifestVersion", "2");
		dictionary_1.put("Bundle-SymbolicName", "A");
		dictionary_1.put("Import-Package", "test;version=\"1.0.0\"");
		dictionary_1.put("Export-Package", "test;version=\"1.0.0\"");
		return sof.createBundleDescription(dictionary_1, "bundle_1", 1);
	}

	public BundleDescription create_bundle_2(StateObjectFactory sof) throws BundleException {
		Dictionary dictionary_2 = new Properties();
		dictionary_2.put("Bundle-ManifestVersion", "2");
		dictionary_2.put("Bundle-SymbolicName", "B");
		dictionary_2.put("Import-Package", "test;version=\"2.0.0\"");
		dictionary_2.put("Export-Package", "test;version=\"2.0.0\"");
		return sof.createBundleDescription(dictionary_2, "bundle_2", 2);
	}

	public BundleDescription create_bundle_3(StateObjectFactory sof) throws BundleException {
		Dictionary dictionary_3 = new Properties();
		dictionary_3.put("Bundle-ManifestVersion", "2");
		dictionary_3.put("Bundle-SymbolicName", "C");
		dictionary_3.put("Import-Package", "test;version=\"[1.0,2)\"");
		return sof.createBundleDescription(dictionary_3, "bundle_3", 3);
	}
}
