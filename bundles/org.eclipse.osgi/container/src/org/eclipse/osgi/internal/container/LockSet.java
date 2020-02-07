/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.container;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
	static final class LockHolder {
		private final AtomicInteger useCount = new AtomicInteger(0);
		private final ReentrantLock lock = new ReentrantLock();

		int incrementUseCount() {
			return useCount.incrementAndGet();
		}

		int decremementUseCount() {
			return useCount.decrementAndGet();
		}

		boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			return !lock.isHeldByCurrentThread() && lock.tryLock(time, unit);
		}

		void unlock() {
			lock.unlock();
		}

		@Override
		public String toString() {
			return lock.toString();
		}
	}

	private final Map<T, LockHolder> locks = new HashMap<>();

	public boolean tryLock(T t, long time, TimeUnit unit) throws InterruptedException {
		final boolean previousInterruption = Thread.interrupted();
		try {
			LockHolder lock;
			synchronized (locks) {
				lock = locks.get(t);
				if (lock == null) {
					lock = new LockHolder();
					locks.put(t, lock);
				}
				lock.incrementUseCount();
			}
			// all interested threads have the lock object and the use count is the number of such threads
			boolean acquired = false;
			try {
				acquired = lock.tryLock(time, unit);
				return acquired;
			} finally {
				if (!acquired) {
					synchronized (locks) {
						// If, after failing to acquire the lock, no other thread is using the lock, discard it.
						if (lock.decremementUseCount() == 0) {
							locks.remove(t);
						}
					}
				}
			}
		} finally {
			if (previousInterruption) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public void unlock(T t) {
		synchronized (locks) {
			LockHolder lock = locks.get(t);
			if (lock == null)
				throw new IllegalStateException("No lock found."); //$NON-NLS-1$
			lock.unlock();
			// If, after unlocking, no other thread is using the lock, discard it.
			if (lock.decremementUseCount() == 0) {
				locks.remove(t);
			}
		}
	}

	public String getLockInfo(T t) {
		synchronized (locks) {
			return String.valueOf(locks.get(t));
		}
	}
}
