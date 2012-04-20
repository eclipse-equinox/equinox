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

import java.util.concurrent.locks.*;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * A read/write lock that may be upgraded from a read lock to a write lock without 
 * the risk of allowing another thread to obtain the write lock.
 * <p>
 * A read lock may only be upgraded to a write lock if the read lock requested 
 * an upgrade reservation.  When a read lock requests a upgrade reservation then
 * it is indicating the intention to upgrade to a write lock.  Once the upgrade
 * reservation is held no other thread is allowed to obtain the write lock until
 * the upgrade reservation is revoked.
 */
public class UpgradeableReadWriteLock {
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Object reservationMonitor = new Object();
	private Thread reserveUpgradeThread = null;
	private int readLocksHeldByUpgrade = 0;

	/**
	 * Acquires the write lock.  
	 * <p>
	 * Acquires the write lock if neither the read nor write nor the upgrade reservation lock are held by another thread.
	 * If the current thread already holds the write lock
	 * then an {@link IllegalStateException} is thrown, write locks are not reentrant.
	 * If the upgrade reservation is held by the current thread then all of the 
	 * currently held read locks are released before obtaining the write lock.
	 * @see WriteLock#lock()
	 */
	public void lockWrite() {
		boolean success = lock.writeLock().tryLock();
		if (success) {
			if (lock.writeLock().getHoldCount() > 1) {
				lock.writeLock().unlock();
				throw new IllegalStateException("Attempted re-entrant write lock."); //$NON-NLS-1$
			}
			synchronized (reservationMonitor) {
				// locked on reservation monitor here only if the current thread holds the write lock
				if (reserveUpgradeThread != null) {
					// unlock write to allow the reserve thread to lock write or give up reservation
					lock.writeLock().unlock();
					while (reserveUpgradeThread != null)
						try {
							reservationMonitor.wait();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					// OK we are clear to attempt write lock now;
					// reservation lock is held to prevent other reservations
					lock.writeLock().lock();
				}
			}
			return;
		}

		int readCount = lock.getReadHoldCount();
		synchronized (reservationMonitor) {
			// locked on reservation monitor here and may hold read locks but does NOT hold the write lock yet
			if (reserveUpgradeThread == Thread.currentThread()) {
				if (readCount > 0) {
					// requesting upgrade; must free read locks before waiting.
					for (int i = 0; i < readCount; i++)
						lock.readLock().unlock();
				}
			} else {
				if (readCount > 0) {
					// attempting write upgrade without reservation
					throw new IllegalStateException("Attempting upgrade to write without reservation."); //$NON-NLS-1$
				}
				while (reserveUpgradeThread != null)
					try {
						reservationMonitor.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
			}
			// OK we are clear to attempt write lock now;
			// reservation lock is held to prevent other reservations
			lock.writeLock().lock();
			readLocksHeldByUpgrade = readCount;
		}
	}

	/**
	 * Attempts to release the write lock. 
	 * <p>
	 * If the upgrade reservation is held by the current thread
	 * then all read locks that were released when acquiring the write lock are re-acquired before
	 * releasing the write lock and the current thread will retain the upgrade reservation.
	 * @see WriteLock#unlock()
	 */
	public void unlockWrite() {
		synchronized (reservationMonitor) {
			if (reserveUpgradeThread == Thread.currentThread()) {
				// re-obtain the read locks that got released during the write upgrade
				for (int i = 0; i < readLocksHeldByUpgrade; i++)
					lockRead();
				readLocksHeldByUpgrade = 0;
			}
		}
		// now unlock the write lock
		lock.writeLock().unlock();
	}

	/**
	 * Acquires the upgradeable read lock.
	 * <p>
	 * Acquires an upgradeable read lock if the write lock and upgradeable read lock are not held by another thread.
	 * <p>
	 * If the write lock or upgradeable read lock are held by another thread then the current thread 
	 * becomes disabled for thread scheduling purposes and lies dormant until the upgradeable read lock has been acquired. 
	 * <p>
	 * Only a single thread may hold the upgradeable read lock
	 * @see ReadLock#lock()
	 */
	public void lockUpgradeableRead() {
		synchronized (reservationMonitor) {
			if (reserveUpgradeThread == Thread.currentThread())
				throw new IllegalStateException("Thread already requested write upgrade."); //$NON-NLS-1$
			while (reserveUpgradeThread != null)
				try {
					// wait for the reserve thread to give up its upgrade request
					reservationMonitor.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			reserveUpgradeThread = Thread.currentThread();
		}
		lock.readLock().lock();
	}

	/**
	 * Acquires the read lock.
	 * <p>
	 * Acquires the read lock if the write lock is not held by another thread.
	 * <p>
	 * If the write lock is held by another thread then the current thread 
	 * becomes disabled for thread scheduling purposes and lies dormant until the read lock has been acquired. 
	 * @see ReadLock#lock()
	 */
	public void lockRead() {
		lock.readLock().lock();
	}

	/**
	 * Attempts to release the upgradeable read lock.
	 * <p>
	 * If the number of readers is now zero then the lock is made available for write lock and upgradeable read lock attempts.
	 * @see ReadLock#unlock()
	 */
	public void unlockUpgradeableRead() {
		synchronized (reservationMonitor) {
			if (reserveUpgradeThread != Thread.currentThread())
				throw new IllegalStateException("The current thread does not own the upgrade reservation."); //$NON-NLS-1$
			reserveUpgradeThread = null;
			reservationMonitor.notifyAll();
		}
		lock.readLock().unlock();
	}

	/**
	 * Attempts to release the read lock.
	 * <p>
	 * If the number of readers is now zero then the lock is made available for write lock attempts.
	 * @see ReadLock#unlock()
	 */
	public void unlockRead() {
		lock.readLock().unlock();
	}

	/**
	 * @return the number of read holds the current thread has.
	 * @see ReentrantReadWriteLock#getReadHoldCount()
	 */
	int getReadHoldCount() {
		return lock.getReadHoldCount();
	}
}
