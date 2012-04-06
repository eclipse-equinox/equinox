/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LockSet<T> {
	private final HashMap<T, ReentrantLock> locks = new HashMap<T, ReentrantLock>();
	private final Object monitor = new Object();
	private final boolean reentrant;

	public LockSet(boolean reentrant) {
		this.reentrant = reentrant;
	}

	public boolean lock(T t) {
		ReentrantLock lock = getLock(t);
		lock.lock();
		if (reentrant)
			return true;
		if (lock.getHoldCount() > 1) {
			lock.unlock();
			return false;
		}
		return true;
	}

	public boolean tryLock(T t) {
		ReentrantLock lock = getLock(t);
		boolean obtained = lock.tryLock();
		if (obtained) {
			if (lock.getHoldCount() > 1) {
				lock.unlock();
				return false;
			}
		}
		return obtained;
	}

	public boolean tryLock(T t, long time, TimeUnit unit) throws InterruptedException {
		ReentrantLock lock = getLock(t);
		boolean obtained = lock.tryLock(time, unit);
		if (obtained) {
			if (lock.getHoldCount() > 1) {
				lock.unlock();
				return false;
			}
		}
		return obtained;
	}

	public void unlock(T t) {
		getLock(t).unlock();
	}

	private ReentrantLock getLock(T t) {
		synchronized (monitor) {
			ReentrantLock lock = locks.get(t);
			if (lock == null) {
				lock = new ReentrantLock();
				locks.put(t, lock);
			}
			return lock;
		}
	}
}
