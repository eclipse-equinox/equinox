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

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UpgradeableReadWriteLock {
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Object reservationMonitor = new Object();
	private Thread reserveUpgradeThread = null;

	public int lockWrite() {
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
				}
				// OK we are clear to attempt write lock now;
				// reservation lock is held to prevent other reservations
				lock.writeLock().lock();
			}
			return 0;
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
			} else if (reserveUpgradeThread != null) {
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
		}
		return readCount;
	}

	public void unlockWrite(int readLocks) {
		// re-obtain the read locks that got released
		for (int i = 0; i < readLocks; i++)
			lockRead(false);
		// now unlock the write lock
		lock.writeLock().unlock();
	}

	public void lockRead(boolean reserveUpgrade) {
		if (reserveUpgrade) {
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
		}
		lock.readLock().lock();
	}

	public void unlockRead(boolean unreserveUpgrade) {
		if (unreserveUpgrade) {
			synchronized (reservationMonitor) {
				if (reserveUpgradeThread != Thread.currentThread())
					throw new IllegalStateException("The current thread does not own the upgrade reservation."); //$NON-NLS-1$
				reserveUpgradeThread = null;
				reservationMonitor.notifyAll();
			}
		}
		lock.readLock().unlock();
	}

	public int getReadHoldCount() {
		return lock.getReadHoldCount();
	}
}
