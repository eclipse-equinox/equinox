/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import org.eclipse.equinox.log.ExtendedLogEntry;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

class TestListener implements LogListener {
	LogEntry entry;
	final Bundle logImpl = Activator.getBundle("org.eclipse.equinox.log");

	public synchronized void logged(LogEntry entry) {
		if (entry.getBundle() == logImpl)
			return; // discard logs from the logImpl
		this.entry = entry;
		notifyAll();
	}

	public synchronized LogEntry getEntry() {
		return entry;
	}

	public synchronized ExtendedLogEntry getEntryX() {
		return (ExtendedLogEntry) entry;
	}
}