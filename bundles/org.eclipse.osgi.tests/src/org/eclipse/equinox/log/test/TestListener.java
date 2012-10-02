/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import junit.framework.Assert;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

class TestListener implements LogListener {
	LogEntry entry;

	public synchronized void logged(LogEntry entry) {
		if (entry.getBundle().getBundleId() == 0)
			return; // discard logs from the logImpl in the framework
		this.entry = entry;
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