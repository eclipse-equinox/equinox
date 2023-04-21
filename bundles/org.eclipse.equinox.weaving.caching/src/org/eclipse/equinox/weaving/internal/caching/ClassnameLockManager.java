/*******************************************************************************
 * Copyright (c) 2023 Stefan Winkler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *     Stefan Winkler - initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A name-based locking facility that makes it possible to execute code in the
 * scope of a given class name in read or write locked state (with respect to a
 * ReadWriteLock).
 *
 * This makes it possible to read a woven class from the cache concurrently
 * while preventing a write operation to the cache for the same class file.
 */
public class ClassnameLockManager {
	/**
	 * Helper class that combines a {@link ReadWriteLock} with a lock counter. By
	 * using this counter, we can clean up any locks that are no longer used.
	 */
	private static class LockWithCount {
		/**
		 * The counter.
		 */
		final AtomicInteger count;
		/**
		 * The lock.
		 */
		final ReadWriteLock lock;

		/**
		 * Create a new lock instance with a counter initialized to zero.
		 */
		LockWithCount() {
			this.lock = new ReentrantReadWriteLock();
			this.count = new AtomicInteger(0);
		}
	}

	/**
	 * Functional interface for read operations. A cache read operation returns the
	 * read byte[] array.
	 */
	@FunctionalInterface
	public interface ReadOperation {
		/**
		 * The logic to read the array of class bytes from the cache.
		 *
		 * @return the bytes read from the cache
		 */
		byte[] read();
	}

	/**
	 * Functional interface for write operations. Since we are writing to the file
	 * system, IOExceptions can occor. Those can be propagated by declaring the
	 * exception.
	 */
	@FunctionalInterface
	public interface WriteOperation {
		/**
		 * The logic to write a class file to the cache.
		 *
		 * @throws IOException in case of a file system error in the logic, this can be
		 *                     propagated to the caller.
		 */
		void write() throws IOException;
	}

	/**
	 * The map of classnames to {@link LockWithCount} instances.
	 */
	private final ConcurrentHashMap<String, LockWithCount> classnameLocks = new ConcurrentHashMap<>(
			IBundleConstants.QUEUE_CAPACITY);

	/**
	 * Acquire a read or write lock for the given classname.
	 *
	 * @param classname the classname
	 * @param writeLock {@code true} if a write lock shall be acquired,
	 *                  {@code false} for a read lock
	 * @return the lock that has been acquired
	 */
	private Lock acquireLock(final String classname, final boolean writeLock) {
		// we need to return the result from inside the closure, so wrap the value in an
		// array.
		final Lock[] lockWrapper = new Lock[1];

		// we use compute(), so we can have the complete logic in a single atomic
		// transaction.
		// If there is not yet a lock, we create it and lock with it. If not, we lock
		// with the one that exists.
		classnameLocks.compute(classname, (key, existingEntry) -> {
			// existingEntry == null means there is no lock for the given classname, so we
			// create one.
			final LockWithCount resultingEntry = existingEntry != null ? existingEntry : new LockWithCount();
			// ge the lock that we need (read or write, depending on the parameter).
			lockWrapper[0] = writeLock ? resultingEntry.lock.writeLock() : resultingEntry.lock.readLock();
			// increment the lock counter
			resultingEntry.count.incrementAndGet();
			// return either the already existing lock (=leave the map unchanged) or, if
			// there was none, the new one (adding it to the map)
			return resultingEntry;
		});

		// now activate the lock - this must happen outside of the compute() because
		// otherwise we would end up in a deadlock with the classnameLocks
		// ConcurrentHashMap lock in releaseLock()
		lockWrapper[0].lock();

		// return the acquired lock
		return lockWrapper[0];
	}

	/**
	 * Execute a read operation.
	 *
	 * @param classname     the classname defining the scope of the lock to acquire
	 * @param readOperation the operation to execute
	 *
	 * @return the byte[] array read by the readOperation
	 */
	public byte[] executeRead(final String classname, final ReadOperation readOperation) {
		final Lock lock = acquireLock(classname, false);
		try {
			return readOperation.read();
		} finally {
			releaseLock(classname, lock);
		}
	}

	/**
	 * Execute a write operation.
	 *
	 * @param classname      the classname defining the scope of the lock to acquire
	 * @param writeOperation the operation to execute
	 *
	 * @throws IOException any exception thrown by the writeOperation will be
	 *                     propagated to the caller
	 */
	public void executeWrite(final String classname, final WriteOperation writeOperation) throws IOException {
		final Lock lock = acquireLock(classname, true);
		try {
			writeOperation.write();
		} finally {
			releaseLock(classname, lock);
		}
	}

	/**
	 * Release the acquired lock and perform cleanup actions.
	 *
	 * @param classname the classname to which lock applies
	 * @param lock      the lock the was acquired before
	 */
	private void releaseLock(final String classname, final Lock lock) {
		// We know that the lock is in the map, otherwise it could not have been
		// acquired. So we use computeIfPresent() here.
		classnameLocks.computeIfPresent(classname, (key, existingEntry) -> {
			// we need to unlock inside the atomic compute operation, so that nothing can
			// occur between the unlock operation and
			// the counter decrement
			lock.unlock();
			// decrement the counter and if this was the last lock, remove it (by returning
			// null), otherwise keep the
			// existing entry as it was
			return existingEntry.count.decrementAndGet() == 0 ? null : existingEntry;
		});
	}
}
