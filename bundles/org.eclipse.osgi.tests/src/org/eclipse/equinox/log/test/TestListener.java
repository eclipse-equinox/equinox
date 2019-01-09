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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Assert;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

class TestListener implements LogListener {
	private final String testBundleLoc;
	private List<LogEntry> logs = new ArrayList<>();

	public TestListener() {
		this(null);
	}

	public TestListener(String testBundleLoc) {
		this.testBundleLoc = testBundleLoc == null ? OSGiTestsActivator.getContext().getBundle().getLocation() : testBundleLoc;
	}

	public synchronized void logged(LogEntry e) {
		if (!testBundleLoc.equals(e.getBundle().getLocation())) {
			return; // discard logs from all other bundles
		}
		logs.add(e);
		notifyAll();
	}

	public synchronized ExtendedLogEntry getEntryX() throws InterruptedException {
		return getEntryX(20000);
	}

	public synchronized ExtendedLogEntry getEntryX(long timeToWait) throws InterruptedException {
		LogEntry logEntry;
		long startTime = System.currentTimeMillis();
		if (logs.size() == 0 && timeToWait > 0) {
			this.wait(timeToWait);
			timeToWait = timeToWait - (System.currentTimeMillis() - startTime);
		}
		logEntry = logs.size() == 0 ? null : logs.remove(0);
		if (logEntry == null) {
			Assert.fail("No log entry logged.");
		}
		return (ExtendedLogEntry) logEntry;
	}
}