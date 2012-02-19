/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
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
import org.osgi.service.coordinator.*;

/*
 * Ensures the Coordinator implementation honors a specified maximum timeout
 * for coordinations.
 * 
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=362137
 */
public class CoordinationMaxTimeoutTest extends CoordinatorTest {
	private static final long DEVIATION = 500;
	private static final String PROPERTY_NAME = "org.eclipse.equinox.coordinator.timeout"; //$NON-NLS-1$
	private static final long TIMEOUT = 5000;

	public void testMaxTimeoutWithNoTimeout() throws Exception {
		long start = System.currentTimeMillis();
		Coordination c = coordinator.create("c", 0); //$NON-NLS-1$
		try {
			assertTimeout(start, TIMEOUT, c);
			c.join(10000);
			assertTerminated(c);
			assertTimeoutDuration(start, TIMEOUT);
		} finally {
			try {
				c.end();
			} catch (CoordinationException e) {
				// noop
			}
		}
	}

	public void testMaxTimeoutWithGreaterTimeout() throws Exception {
		long start = System.currentTimeMillis();
		Coordination c = coordinator.create("c", 10000); //$NON-NLS-1$
		try {
			assertTimeout(start, TIMEOUT, c);
			c.join(10000);
			assertTerminated(c);
			assertTimeoutDuration(start, TIMEOUT);
		} finally {
			try {
				c.end();
			} catch (CoordinationException e) {
				// noop
			}
		}
	}

	public void testMaxTimeoutWithLesserTimeout() throws Exception {
		long start = System.currentTimeMillis();
		long timeout = 2000;
		Coordination c = coordinator.create("c", timeout); //$NON-NLS-1$
		try {
			assertTimeout(start, timeout, c);
			c.join(5000);
			assertTerminated(c);
			assertTimeoutDuration(start, timeout);
		} finally {
			try {
				c.end();
			} catch (CoordinationException e) {
				// noop
			}
		}
	}

	public void testExtendTimeoutWhenMaxTimeoutAlreadyReached() throws Exception {
		long start = System.currentTimeMillis();
		Coordination c = coordinator.create("c", 0); //$NON-NLS-1$
		try {
			assertTimeout(start, TIMEOUT, c);
			assertEquals("No change in deadline should have occurred", 0, c.extendTimeout(TIMEOUT)); //$NON-NLS-1$
			assertTimeout(start, TIMEOUT, c);
		} finally {
			try {
				c.end();
			} catch (CoordinationException e) {
				// noop
			}
		}
	}

	public void testExtendTimeoutWhenMaxTimeoutNotAlreadyReached() throws Exception {
		long start = System.currentTimeMillis();
		long timeout = 1500;
		Coordination c = coordinator.create("c", timeout); //$NON-NLS-1$
		try {
			assertTimeout(start, timeout, c);
			c.extendTimeout(timeout);
			assertTimeout(start, timeout * 2, c);
			c.extendTimeout(TIMEOUT);
			assertTimeout(start, TIMEOUT, c);
			assertEquals("No change in deadline should have occurred", 0, c.extendTimeout(TIMEOUT)); //$NON-NLS-1$
			assertTimeout(start, TIMEOUT, c);
		} finally {
			try {
				c.end();
			} catch (CoordinationException e) {
				// noop
			}
		}
	}

	public void testNoMaxTimeoutWithTimeout() throws Exception {
		Activator.getBundleContext().ungetService(coordinatorRef);
		System.setProperty(PROPERTY_NAME, String.valueOf(0));
		coordinatorRef = Activator.getBundleContext().getServiceReference(Coordinator.class.getName());
		coordinator = (Coordinator) Activator.getBundleContext().getService(coordinatorRef);
		long start = System.currentTimeMillis();
		long timeout = 2000;
		Coordination c = coordinator.create("c", timeout); //$NON-NLS-1$
		try {
			assertTimeout(start, timeout, c);
			c.join(5000);
			assertTerminated(c);
			assertTimeoutDuration(start, timeout);
		} finally {
			try {
				c.end();
			} catch (CoordinationException e) {
				// noop
			}
		}
	}

	protected void setUp() throws Exception {
		System.setProperty(PROPERTY_NAME, String.valueOf(TIMEOUT));
		assertSystemProperty(PROPERTY_NAME, String.valueOf(TIMEOUT));
		assertFrameworkProperty(PROPERTY_NAME, String.valueOf(TIMEOUT));
		// The above system property initialization must occur before calling super.setUp().
		super.setUp();
	}

	private void assertFrameworkProperty(String name, String value) {
		assertEquals("Wrong value for framework property " + name, value, Activator.getBundleContext().getProperty(name)); //$NON-NLS-1$
	}

	private void assertSystemProperty(String name, String value) {
		assertEquals("Wrong value for system property " + name, value, System.getProperty(name)); //$NON-NLS-1$
	}

	private void assertTerminated(Coordination c) {
		assertTrue("Not terminated", c.isTerminated()); //$NON-NLS-1$
	}

	private void assertTimeout(long start, long timeout, Coordination c) {
		assertTrue("Wrong timeout", c.extendTimeout(0) >= start + timeout); //$NON-NLS-1$
	}

	private void assertTimeoutDuration(long start, long timeout) {
		assertTrue("Timeout too long", System.currentTimeMillis() - start <= timeout + DEVIATION); //$NON-NLS-1$
	}
}
