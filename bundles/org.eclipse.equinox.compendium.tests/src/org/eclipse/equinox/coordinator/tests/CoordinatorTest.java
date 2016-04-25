/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
