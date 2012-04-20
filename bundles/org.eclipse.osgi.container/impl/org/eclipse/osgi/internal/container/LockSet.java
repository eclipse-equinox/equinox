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
package org.eclipse.osgi.internal.container;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LockSet<T> {
	private final Map<T, ReentrantLock> locks = new WeakHashMap<T, ReentrantLock>();
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
			if (reentrant)
				return true;
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
			if (reentrant)
				return true;
			if (lock.getHoldCount() > 1) {
				lock.unlock();
				return false;
			}
		}
		return obtained;
	}

	public void unlock(T t) {
		synchronized (monitor) {
			ReentrantLock lock = locks.get(t);
			if (lock == null)
				throw new IllegalStateException("No lock found."); //$NON-NLS-1$
			lock.unlock();
		}
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
