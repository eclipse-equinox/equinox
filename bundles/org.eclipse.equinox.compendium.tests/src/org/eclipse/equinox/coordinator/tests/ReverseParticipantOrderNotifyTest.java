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

import static org.junit.Assert.assertEquals;

import java.util.*;
import org.junit.Test;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;

/*
 * Ensures participants are notified in reverse participation order when ending
 * or failing a coordination.
 * 
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=371980
 */
public class ReverseParticipantOrderNotifyTest extends CoordinatorTest {
	private static class TestParticipant implements Participant {
		private final List<TestParticipant> participants;

		public TestParticipant(List<TestParticipant> participants) {
			this.participants = participants;
		}

		public void ended(Coordination coordination) throws Exception {
			participants.add(this);
		}

		public void failed(Coordination coordination) throws Exception {
			participants.add(this);
		}
	}

	@Test
	public void testReverseParticipateOrderNotifyEnded() {
		List<Participant> before = new ArrayList<Participant>();
		List<TestParticipant> after = Collections.synchronizedList(new ArrayList<TestParticipant>());
		Coordination c = coordinator.create("c", 0); //$NON-NLS-1$
		for (int i = 0; i < 10; i++) {
			Participant p = new TestParticipant(after);
			before.add(p);
			c.addParticipant(p);
		}
		c.end();
		Collections.reverse(before);
		assertEquals("Not notified in reverse participation order", before, after); //$NON-NLS-1$
	}

	@Test
	public void testReverseParticipateOrderNotifyFailed() {
		List<Participant> before = new ArrayList<Participant>();
		List<TestParticipant> after = Collections.synchronizedList(new ArrayList<TestParticipant>());
		Coordination c = coordinator.begin("c", 0); //$NON-NLS-1$
		for (int i = 0; i < 10; i++) {
			Participant p = new TestParticipant(after);
			before.add(p);
			c.addParticipant(p);
		}
		c.fail(new Exception());
		Collections.reverse(before);
		assertEquals("Not notified in reverse participation order", before, after); //$NON-NLS-1$
	}
}
