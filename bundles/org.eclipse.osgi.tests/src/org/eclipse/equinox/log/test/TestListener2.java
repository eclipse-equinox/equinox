/*******************************************************************************
 * Copyright (c) 2006, 2017 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.equinox.log.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

public class TestListener2 extends TestCase implements LogListener {
	private List<String> list;
	private AtomicInteger flag = new AtomicInteger(0);
	CountDownLatch latch;

	public TestListener2(CountDownLatch countDownLatch) {
		this.list = Collections.synchronizedList(new ArrayList());
		this.latch = countDownLatch;
	}

	@Override
	public void logged(LogEntry entry) {
		//logged is never called in parallel. Added a check to see if two threads are accessing the logged method at the same time.
		assertTrue(flag.compareAndSet(0, 1));
		if (entry.getBundle().getSymbolicName().equals("org.eclipse.osgi")) {
			list.add(entry.getMessage());
			latch.countDown();
		}
		flag.set(0);
	}

	public List<String> getLogs() {
		return this.list;
	}

}
