/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

import java.util.ArrayList;
import java.util.List;

public class BasicReadWriteLock {
	private List<Thread> currentReaders = new ArrayList<Thread>(2);
	private int writersWaiting = 0;
	private Thread writing = null;

	public synchronized int readLock() {
		while (writing != null || writersWaiting != 0) {
			try {
				if (writing == Thread.currentThread())
					throw new IllegalStateException("Attempted to nest read lock inside a write lock"); //$NON-NLS-1$
				wait();
			} catch (InterruptedException e) {
				// reset interrupted state but keep waiting
				Thread.currentThread().interrupt();
			}
		}
		currentReaders.add(Thread.currentThread());
		if (currentReaders.size() == 1)
			return 1;
		Thread current = Thread.currentThread();
		int result = 0;
		for (Thread reader : currentReaders) {
			if (reader == current)
				result++;
		}
		return result;
	}

	public synchronized void readUnlock() {
		currentReaders.remove(Thread.currentThread());
		notifyAll();
	}

	public synchronized void writeLock() {
		writersWaiting++;
		try {
			while (writing != null || currentReaders.size() != 0) {
				try {
					if (writing == Thread.currentThread() || currentReaders.contains(Thread.currentThread()))
						throw new IllegalStateException("Attempted to nest write lock inside a read or write lock"); //$NON-NLS-1$
					wait();
				} catch (InterruptedException e) {
					// reset interrupted state but keep waiting
					Thread.currentThread().interrupt();
				}
			}
		} finally {
			writersWaiting--;
		}
		writing = Thread.currentThread();
	}

	public synchronized void writeUnlock() {
		writing = null;
		notifyAll();
	}
}
