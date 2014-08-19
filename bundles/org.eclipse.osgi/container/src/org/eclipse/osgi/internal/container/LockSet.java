/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.container;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Implementation note: This class does not pool ReentrantLocks for the objects
 * that are being locked.  This means that if the same object is locked and unlocked
 * over and over then new ReentrantLocks are created each time.  This set should be
 * used with care.  If the same object is going to be locked/unlocked over and over then
 * consider using a different locking strategy.
 * 
 * Previous implementations of this class attempted to use a WeakHashMap to cache
 * the locks, but this proved to be a flawed approach because of the unpredictable
 * timing of garbage collection, particularly with autoboxed types (e.g. bundle
 * long ids).
 */
public class LockSet<T> {
	private final Map<T, ReentrantLock> locks = new HashMap<T, ReentrantLock>();
	private final Object monitor = new Object();

	public boolean tryLock(T t, long time, TimeUnit unit) throws InterruptedException {
		ReentrantLock lock;
		synchronized (monitor) {
			lock = locks.get(t);
			if (lock == null) {
				lock = new ReentrantLock();
				locks.put(t, lock);
			}
		}
		boolean obtained = !lock.isHeldByCurrentThread() && lock.tryLock(time, unit);
		if (obtained) {
			synchronized (monitor) {
				// must check that another thread did not remove the lock
				// when unlocking while we were waiting to obtain the lock
				if (!locks.containsKey(t)) {
					locks.put(t, lock);
				}
			}
		}
		return obtained;
	}

	public void unlock(T t) {
		synchronized (monitor) {
			ReentrantLock lock = locks.get(t);
			if (lock == null)
				throw new IllegalStateException("No lock found."); //$NON-NLS-1$
			if (lock.getHoldCount() == 1) {
				// We are about to remove the last hold;
				// Clear out the lock from the map;
				// This forces a new lock to get created if the same object is locked again;
				// Must remove before unlocking to avoid removing a lock that may be waiting to
				// be obtained by another thread in tryLock
				locks.remove(t);
			}
			lock.unlock();
		}
	}

}
