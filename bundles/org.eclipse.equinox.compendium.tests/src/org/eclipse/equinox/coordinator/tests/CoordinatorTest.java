/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others
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
package org.eclipse.equinox.coordinator.tests;

import org.eclipse.equinox.compendium.tests.Activator;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.ServiceReference;
import org.osgi.service.coordinator.Coordinator;

public abstract class CoordinatorTest {
	protected Coordinator coordinator;
	protected ServiceReference<Coordinator> coordinatorRef;

	@Before
	public void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_COORDINATOR).start();
		coordinatorRef = Activator.getBundleContext().getServiceReference(Coordinator.class);
		coordinator = Activator.getBundleContext().getService(coordinatorRef);
	}

	@After
	public void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(coordinatorRef);
		Activator.getBundle(Activator.BUNDLE_COORDINATOR).stop();
	}
}
