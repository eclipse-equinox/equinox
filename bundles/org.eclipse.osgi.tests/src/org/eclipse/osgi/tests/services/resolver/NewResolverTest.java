/*******************************************************************************
 * Copyright (c) 2004, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.junit.Test;
import org.osgi.framework.BundleException;

public class NewResolverTest extends AbstractStateTest {

	@Test
	public void testSkeleton() {
		State state = buildEmptyState();
		state.resolve();
	}

	@Test
	public void testBasicScenario1() throws BundleException {
		State state = buildEmptyState();

		final String MAN_A = "Bundle-SymbolicName: A\n" +
		                     "Export-Package: servlet; specification-version=2.1";
		BundleDescription bA = state.getFactory().createBundleDescription(parseManifest(MAN_A),
																		"org.eclipse.basic1A", 0);
		state.addBundle(bA);

		final String MAN_B = "Bundle-SymbolicName: B\n" +
        					 "Import-Package: servlet; specification-version=2.1";
		BundleDescription bB = state.getFactory().createBundleDescription(parseManifest(MAN_B),
													"org.eclipse.basic1B", 1);
		state.addBundle(bB);

		state.resolve();

		BundleDescription b0 = state.getBundle(0);
		assertNotNull("0.1", b0);
		assertFullyResolved("0.2", b0);

		BundleDescription b1 = state.getBundle(1);
		assertNotNull("0.3", b1);
		assertFullyResolved("0.4", b1);
	}
}


