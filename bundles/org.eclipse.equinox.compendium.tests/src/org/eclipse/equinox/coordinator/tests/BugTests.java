/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.ref.WeakReference;
import org.junit.Test;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;

public class BugTests extends CoordinatorTest {

	@Test
	public void testBug421487() throws Exception {
		// Begin a thread coordination on this thread.
		Coordination c1 = coordinator.begin("c1", 0); //$NON-NLS-1$
		// Begin a second thread coordination on this thread.
		Coordination c2 = coordinator.begin("c2", 0); //$NON-NLS-1$
		// c2's enclosing coordination will be c1.
		assertEquals("Wrong enclosing coordination", c1, c2.getEnclosingCoordination()); //$NON-NLS-1$
		WeakReference<Coordination> reference = new WeakReference<Coordination>(c1);
		// Set c1 to null so it will become weakly reachable and enqueued.
		c1 = null;
		// Ensure c1 becomes weakly reachable.
		for (int i = 0; i < 100 && reference.get() != null; i++)
			// Force garbage collection.
			System.gc();
		assertNull("The enclosing coordination never became weakly reachable", reference.get()); //$NON-NLS-1$
		// For some reason, this delay is necessary to force the failure
		// condition to occur when running "normally". The failure will occur
		// without this delay when running in debug mode with or without
		// breakpoints.
		Thread.sleep(1000);
		try {
			// End the enclosed coordination.
			c2.end();
		} catch (CoordinationException e) {
			// A CoordinationException of type ALREADY_ENDED is expected since
			// the coordination was failed.
			assertEquals("Wrong type", CoordinationException.ALREADY_ENDED, e.getType()); //$NON-NLS-1$
		} catch (NullPointerException e) {
			e.printStackTrace();
			fail("Received NPE while ending the coordination"); //$NON-NLS-1$
		}
	}
}
