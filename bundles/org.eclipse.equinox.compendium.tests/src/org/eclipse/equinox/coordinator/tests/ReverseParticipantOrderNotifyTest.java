package org.eclipse.equinox.coordinator.tests;

import java.util.*;
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
		private final List participants;

		public TestParticipant(List participants) {
			this.participants = participants;
		}

		public void ended(Coordination coordination) throws Exception {
			participants.add(this);
		}

		public void failed(Coordination coordination) throws Exception {
			participants.add(this);
		}
	}

	public void testReverseParticipateOrderNotifyEnded() {
		List before = new ArrayList();
		List after = Collections.synchronizedList(new ArrayList());
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

	public void testReverseParticipateOrderNotifyFailed() {
		List before = new ArrayList();
		List after = Collections.synchronizedList(new ArrayList());
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
