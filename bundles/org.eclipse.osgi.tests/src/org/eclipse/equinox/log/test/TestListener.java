/*******************************************************************************
n
n This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

class TestListener implements LogListener {
	private final Bundle testBundle;
	LogEntry entry;

	public TestListener() {
		this(null);
	}

	public TestListener(Bundle testBundle) {
		this.testBundle = testBundle == null ? OSGiTestsActivator.getContext().getBundle() : testBundle;
	}

	public synchronized void logged(LogEntry e) {
		if (!testBundle.equals(e.getBundle()))
			return; // discard logs from all other bundles
		this.entry = e;
		notifyAll();
	}

	public synchronized ExtendedLogEntry getEntryX() {
		ExtendedLogEntry current = (ExtendedLogEntry) entry;
		entry = null;
		return current;
	}

	public void waitForLogEntry() throws InterruptedException {
		synchronized (this) {
			long timeToWait = 20000;
			long startTime = System.currentTimeMillis();
			while (this.entry == null && timeToWait > 0) {
				this.wait(timeToWait);
				timeToWait = timeToWait - (System.currentTimeMillis() - startTime);
			}
			if (this.entry == null) {
				Assert.fail("No log entry logged.");
			}
		}
	}
}