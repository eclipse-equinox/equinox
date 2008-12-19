/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.composites;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.osgi.framework.Bundle;
import org.osgi.service.framework.CompositeBundle;
import org.osgi.service.framework.CompositeBundleFactory;

public class CompositeCreateTests extends AbstractCompositeTests {
	public static Test suite() {
		return new TestSuite(CompositeCreateTests.class);
	}

	public void testCreateComposite01() {
		CompositeBundle composite = createCompositeBundle(linkBundleFactory, "testCreateComposite01", null, null, true, false); //$NON-NLS-1$
		startCompositeBundle(composite, false);
		stopCompositeBundle(composite);
		uninstallCompositeBundle(composite);
	}

	public void testCreateComposite02() {
		CompositeBundle compositeLevel0 = createCompositeBundle(linkBundleFactory, "compositeLevel0", null, null, true, false); //$NON-NLS-1$
		CompositeBundleFactory factory = getFactory(compositeLevel0.getCompositeFramework());
		CompositeBundle compositeLevel1_1 = createCompositeBundle(factory, "compositeLevel1_1", null, null, true, false); //$NON-NLS-1$
		CompositeBundle compositeLevel1_2 = createCompositeBundle(factory, "compositeLevel1_2", null, null, true, false); //$NON-NLS-1$
		long idLevel1_1 = compositeLevel1_1.getBundleId();
		long idLevel1_2 = compositeLevel1_2.getBundleId();

		stopCompositeBundle(compositeLevel0);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel1_1.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel1_2.getCompositeFramework().getState()); //$NON-NLS-1$

		startCompositeBundle(compositeLevel0, false);
		// need to reget the bundles from child frameworks that were restarted
		compositeLevel1_1 = (CompositeBundle) compositeLevel0.getCompositeFramework().getBundleContext().getBundle(idLevel1_1);
		compositeLevel1_2 = (CompositeBundle) compositeLevel0.getCompositeFramework().getBundleContext().getBundle(idLevel1_2);

		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel1_1.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel1_2.getCompositeFramework().getState()); //$NON-NLS-1$

		uninstallCompositeBundle(compositeLevel0);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel1_1.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel1_1.getCompositeFramework().getState()); //$NON-NLS-1$
	}

	public void testCreateComposite03() {
		CompositeBundle compositeLevel0 = createCompositeBundle(linkBundleFactory, "compositeLevel0", null, null, true, false); //$NON-NLS-1$
		CompositeBundleFactory factoryLevel1 = getFactory(compositeLevel0.getCompositeFramework());
		// create two level 1 composites
		CompositeBundle compositeLevel1_1 = createCompositeBundle(factoryLevel1, "compositeLevel1_1", null, null, true, false); //$NON-NLS-1$
		CompositeBundle compositeLevel1_2 = createCompositeBundle(factoryLevel1, "compositeLevel1_2", null, null, true, false); //$NON-NLS-1$

		// create two level 2 composites under 1_1
		CompositeBundleFactory factoryLevel2_1 = getFactory(compositeLevel1_1.getCompositeFramework());
		CompositeBundle compositeLevel2_1 = createCompositeBundle(factoryLevel2_1, "compositeLevel2_1", null, null, true, false); //$NON-NLS-1$
		CompositeBundle compositeLevel2_2 = createCompositeBundle(factoryLevel2_1, "compositeLevel2_2", null, null, true, false); //$NON-NLS-1$
		long idLevel2_1 = compositeLevel2_1.getBundleId();
		long idLevel2_2 = compositeLevel2_2.getBundleId();

		// create two level 2 composites under 1_2
		CompositeBundleFactory factoryLevel2_2 = getFactory(compositeLevel1_2.getCompositeFramework());
		CompositeBundle compositeLevel2_3 = createCompositeBundle(factoryLevel2_2, "compositeLevel2_3", null, null, true, false); //$NON-NLS-1$
		CompositeBundle compositeLevel2_4 = createCompositeBundle(factoryLevel2_2, "compositeLevel2_4", null, null, true, false); //$NON-NLS-1$
		long idLevel2_3 = compositeLevel2_3.getBundleId();
		long idLevel2_4 = compositeLevel2_4.getBundleId();

		stopCompositeBundle(compositeLevel1_1);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel2_1.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel2_2.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_3.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_4.getCompositeFramework().getState()); //$NON-NLS-1$

		startCompositeBundle(compositeLevel1_1, false);
		// need to reget the bundles from child frameworks that were restarted
		compositeLevel2_1 = (CompositeBundle) compositeLevel1_1.getCompositeFramework().getBundleContext().getBundle(idLevel2_1);
		compositeLevel2_2 = (CompositeBundle) compositeLevel1_1.getCompositeFramework().getBundleContext().getBundle(idLevel2_2);

		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_1.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_2.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_3.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_4.getCompositeFramework().getState()); //$NON-NLS-1$

		stopCompositeBundle(compositeLevel1_2);
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_1.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_2.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel2_3.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel2_4.getCompositeFramework().getState()); //$NON-NLS-1$

		startCompositeBundle(compositeLevel1_2, false);
		// need to reget the bundles from child frameworks that were restarted
		compositeLevel2_3 = (CompositeBundle) compositeLevel1_2.getCompositeFramework().getBundleContext().getBundle(idLevel2_3);
		compositeLevel2_4 = (CompositeBundle) compositeLevel1_2.getCompositeFramework().getBundleContext().getBundle(idLevel2_4);

		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_1.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_2.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_3.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, compositeLevel2_4.getCompositeFramework().getState()); //$NON-NLS-1$

		uninstallCompositeBundle(compositeLevel0);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel2_1.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel2_2.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel2_3.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel2_4.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel1_1.getCompositeFramework().getState()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, compositeLevel1_2.getCompositeFramework().getState()); //$NON-NLS-1$
	}
}
