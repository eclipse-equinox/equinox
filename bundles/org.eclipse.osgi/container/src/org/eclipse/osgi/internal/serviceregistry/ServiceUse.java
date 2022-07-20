/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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

package org.eclipse.osgi.internal.serviceregistry;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceException;
/**
 * This class represents the use of a service by a bundle. One is created for each
 * service acquired by a bundle.
 *
 * <p>
 * This class manages a singleton service.
 *
 * @ThreadSafe
 */
public class ServiceUse<S> {

	/**
	 * Custom ServiceException type to indicate a deadlock occurred during service
	 * registration.
	 */
	public static final int DEADLOCK = 1001;

	/** BundleContext associated with this service use */
	final BundleContextImpl context;
	/** ServiceDescription of the registered service */
	final ServiceRegistrationImpl<S> registration;
	final Debug debug;

	/** bundle's use count for this service */
	/* @GuardedBy("getLock()") */
	private int useCount;

	/**
	 * ServiceUseLock for this service use. Use the @{@link #lock()} method to lock
	 * the lock and obtain an {@link AutoCloseable} object which is used to unlock
	 * the lock.
	 */
	private final ServiceUseLock lock = new ServiceUseLock();

	/**
	 * Constructs a service use encapsulating the service object.
	 *
	 * @param   context bundle getting the service
	 * @param   registration ServiceRegistration of the service
	 */
	ServiceUse(BundleContextImpl context, ServiceRegistrationImpl<S> registration) {
		this.useCount = 0;
		this.registration = registration;
		this.context = context;
		this.debug = context.getContainer().getConfiguration().getDebug();
	}

	/**
	 * Get a service's service object and increment the use count.
	 *
	 * @return The service object.
	 */
	/* @GuardedBy("getLock()") */
	S getService() {
		assert getLock().isHeldByCurrentThread();
		if (debug.DEBUG_SERVICES) {
			Debug.println("[" + Thread.currentThread().getName() + "] getService[factory=" + registration.getBundle() //$NON-NLS-1$ //$NON-NLS-2$
					+ "](" + context.getBundleImpl() + "," + registration + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		incrementUse();
		return registration.getServiceObject();
	}

	/**
	 * Unget a service's service object.
	 *
	 * <p>
	 * Decrements the use count if the service was being used.
	 *
	 * @return true if the service was ungotten; otherwise false.
	 */
	/* @GuardedBy("getLock()") */
	boolean ungetService() {
		assert getLock().isHeldByCurrentThread();
		if (!inUse()) {
			return false;
		}
		if (debug.DEBUG_SERVICES) {
			Debug.println("[" + Thread.currentThread().getName() + "] ungetService[factory=" + registration.getBundle() //$NON-NLS-1$ //$NON-NLS-2$
					+ "](" + context.getBundleImpl() + "," + registration + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		decrementUse();
		return true;
	}

	/**
	 * Return the service object for this service use.
	 *
	 * @return The service object.
	 */
	/* @GuardedBy("getLock()") */
	S getCachedService() {
		return registration.getServiceObject();
	}

	/**
	 * Get a new service object for the service.
	 *
	 * <p>
	 * By default, this returns the result of {@link #getService()}.
	 *
	 * @return The service object.
	 */
	/* @GuardedBy("getLock()") */
	S newServiceObject() {
		return getService();
	}

	/**
	 * Release a service object for the service.
	 *
	 * <p>
	 * By default, this returns the result of {@link #ungetService()}.
	 *
	 * @param service The service object to release.
	 * @return true if the service was released; otherwise false.
	 * @throws IllegalArgumentException If the specified service was not
	 *         provided by this object.
	 */
	/* @GuardedBy("getLock()") */
	boolean releaseServiceObject(final S service) {
		if ((service == null) || (service != getCachedService())) {
			throw new IllegalArgumentException(Msg.SERVICE_OBJECTS_UNGET_ARGUMENT_EXCEPTION);
		}
		if (debug.DEBUG_SERVICES) {
			Debug.println("[" + Thread.currentThread().getName() + "] releaseServiceObject[factory=" //$NON-NLS-1$ //$NON-NLS-2$
					+ registration.getBundle() + "](" + context.getBundleImpl() + "," + registration + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return ungetService();
	}

	/**
	 * Release all uses of the service and reset the use count to zero.
	 */
	/* @GuardedBy("getLock()") */
	void release() {
		assert getLock().isHeldByCurrentThread();
		resetUse();
	}

	/**
	 * Is this service use using any services?
	 *
	 * @return true if no services are being used and this service use can be discarded.
	 */
	/* @GuardedBy("getLock()") */
	boolean isEmpty() {
		assert getLock().isHeldByCurrentThread();
		return !inUse();
	}

	/**
	 * Is the use count non zero?
	 *
	 * @return true if the use count is greater than zero.
	 */
	/* @GuardedBy("getLock()") */
	boolean inUse() {
		return useCount > 0;
	}

	/**
	 * Incrementing the use count.
	 */
	/* @GuardedBy("getLock()") */
	void incrementUse() {
		if (useCount == Integer.MAX_VALUE) {
			throw new ServiceException(Msg.SERVICE_USE_OVERFLOW);
		}
		useCount++;
	}

	/**
	 * Decrementing the use count.
	 */
	/* @GuardedBy("getLock()") */
	void decrementUse() {
		assert inUse();
		useCount--;
	}

	/**
	 * Reset the use count to zero.
	 */
	/* @GuardedBy("getLock()") */
	void resetUse() {
		useCount = 0;
	}

	/**
	 * The ServiceUseLock for managing access to this ServiceUse.
	 * <p>
	 * Use {@link #lock()} to lock the ServiceUseLock in a try-with-resources
	 * statement.
	 * 
	 * @return The ServiceUseLock for this ServiceUse.
	 */
	ServiceUseLock getLock() {
		return lock;
	}

	/**
	 * Acquires the ServiceUseLock of this ServiceUse.
	 * 
	 * If this ServiceUse is locked by another thread then the current thread lies
	 * dormant until the lock has been acquired.
	 * 
	 * @return The unlocker, which is {@link AutoCloseable}, to unlock the
	 *         ServiceUseLock of this ServiceUse.
	 * @throws ServiceException If a deadlock with another ServiceUseLock is
	 *                          detected.
	 */
	ServiceUseLock lock() {
		Thread awaitingThread = null;
		boolean interrupted = false;
		try {
			final ServiceUseLock useLock = getLock(); // local var to avoid multiple getfields
			while (true) {
				try {
					if (useLock.tryLock(100_000_000L, TimeUnit.NANOSECONDS)) { // 100ms (but prevent conversion)
						return useLock;
					}
					awaitingThread = Thread.currentThread();
					checkDeadLock(awaitingThread, useLock);
				} catch (InterruptedException e) {
					interrupted = true;
					// Clear interrupted status and try again to lock, just like a plain
					// synchronized. Re-interrupted before returning to the caller.
				}
			}
		} finally {
			if (awaitingThread != null) {
				registration.getAwaitedUseLocks().remove(awaitingThread);
			}
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void checkDeadLock(final Thread currentThread, final ServiceUseLock currentLock) {
		final ConcurrentMap<Thread, ServiceUseLock> awaitedUseLocks = registration.getAwaitedUseLocks();
		awaitedUseLocks.put(currentThread, currentLock);
		// Check if current thread is in the cycle of mutually awaiting thread-lock
		// pairs
		ServiceUseLock useLock = currentLock;
		int maxCycles = awaitedUseLocks.size();
		for (int i = 0; i < maxCycles; i++) { // Prevent infinite loop
			Thread owner = useLock.getOwner();
			if (owner == currentThread) {
				throw new ServiceException(NLS.bind(Msg.SERVICE_USE_DEADLOCK, currentLock), DEADLOCK);
			}
			if (owner == null || (useLock = awaitedUseLocks.get(owner)) == null) {
				break; // lock could be released in the meantime
			}
		}
		// Not (yet) a dead-lock. Lock was regularly hold by another thread.
		// Race conditions are not an issue here. A deadlock is a static situation and
		// if we closely missed the other thread putting its awaited lock it will be
		// noticed in the next loop-pass.
	}

	/**
	 * ReentrantLock subclass that allows for {@link AutoCloseable} unlocking.
	 * <p>
	 * This lock is unlocked if the close method on {@link ServiceUseUnlock} is
	 * invoked. ServiceUseUnlock objects can therefore can be used as a resource in
	 * a try-with-resources statement.
	 * <p>
	 * Also exposes {@link #getOwner()} and has an enhanced {@link #toString()}.
	 * 
	 * @see ServiceUse#lock()
	 */
	static class ServiceUseLock extends ReentrantLock implements AutoCloseable {
		private static final long serialVersionUID = 1L;

		/**
		 * Unlock this lock.
		 * <p>
		 * This method is not idempotent and should be called only once for each lock
		 * acquisition.
		 * </p>
		 * 
		 * @see #unlock()
		 */
		@Override
		public void close() {
			unlock();
		}

		@Override
		protected Thread getOwner() {
			return super.getOwner();
		}

		/**
		 * Returns a lock state description for this lock. This adds additional
		 * information over the default implementation when the lock is held.
		 *
		 * @return The lock state description.
		 */
		@SuppressWarnings("nls")
		@Override
		public String toString() {
			Thread o = getOwner();
			if (o != null) {
				try {
					ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
					ThreadInfo threadInfo = threadMXBean.getThreadInfo(o.getId(), Integer.MAX_VALUE);
					StackTraceElement[] trace = threadInfo.getStackTrace();
					StringBuilder sb = new StringBuilder(super.toString()).append(", Details:\n");
					if (o.isDaemon()) {
						sb.append("daemon ");
					}
					sb.append("prio=").append(o.getPriority()).append(" id=").append(o.getId()).append(" ")
							.append(o.getState());
					for (StackTraceElement traceElement : trace) {
						sb.append("\tat ").append(traceElement).append("\n");
					}
					return sb.toString();
				} catch (Exception e) {
					// do nothing and fall back to just the default, thread might be gone
				}
			}
			return super.toString();
		}
	}
}
