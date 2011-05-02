/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

public class BasicReadWriteLock {
	private int currentReaders = 0;
	private int writersWaiting = 0;
	private boolean writing = false;

	public synchronized void readLock() {
		while (writing || writersWaiting != 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				// reset interrupted state but keep waiting
				Thread.currentThread().interrupt();
			}
		}
		currentReaders++;
	}

	public synchronized void readUnlock() {
		currentReaders--;
		notifyAll();
	}

	public synchronized void writeLock() {
		writersWaiting++;
		while (writing || currentReaders != 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				// reset interrupted state but keep waiting
				Thread.currentThread().interrupt();
			}
		}
		writersWaiting--;
		writing = true;
	}

	public synchronized void writeUnlock() {
		writing = false;
		notifyAll();
	}
}
