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
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
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
	/** BundleContext associated with this service use */
	final BundleContextImpl context;
	/** ServiceDescription of the registered service */
	final ServiceRegistrationImpl<S> registration;
	final Debug debug;

	/** bundle's use count for this service */
	/* @GuardedBy("getLock()") */
	private int useCount;

	/**
	 * ReentrantLock for this service. Use the @{@link #getLock()} method to obtain
	 * the lock.
	 */
	private final ReentrantLock lock = new ServiceUseLock();

	/**
	 * Custom ServiceException type to indicate a deadlock occurred during service
	 * registration.
	 */
	public static final int DEADLOCK = 1001;

	/**
	 * Lock timeout to allow for breaking deadlocks.
	 */
	private final Duration lockTimeout = Duration.ofSeconds(10);

	/**
	 * Constructs a service use encapsulating the service object.
	 *
	 * @param   context bundle getting the service
	 * @param   registration ServiceRegistration of the service
	 */
	ServiceUse(BundleContextImpl context, ServiceRegistrationImpl<S> registration) {
		this.context = context;
		this.registration = registration;
		this.useCount = 0;
		this.debug = context.getContainer().getConfiguration().getDebug();
	}

	/**
	 * The ReentrantLock for managing access to this ServiceUse.
	 * 
	 * @return A ReentrantLock.
	 */
	ReentrantLock getLock() {
		return lock;
	}

	/**
	 * Run the specified Function while holding the {@link #getLock() lock} for this
	 * ServiceUse.
	 * 
	 * @param callable A Function to execute. The function is passed this ServiceUse
	 *                 object.
	 * @return The return value of the specified Function.
	 */
	<T> T withLock(Function<ServiceUse<S>, T> callable) {
		ReentrantLock useLock = getLock();
		boolean interrupted = Thread.interrupted();
		try {
			boolean locked;
			try {
				locked = useLock.tryLock(lockTimeout.toNanos(), TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				interrupted = true;
				throw new ServiceException(NLS.bind(Msg.SERVICE_USE_LOCK_INTERRUPT, useLock), e);
			}
			if (locked) {
				try {
					return callable.apply(this);
				} finally {
					useLock.unlock();
				}
			}
			throw new ServiceException(
					NLS.bind(Msg.SERVICE_USE_LOCK_TIMEOUT, lockTimeout, useLock), DEADLOCK);
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
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
	 * ReentrantLock subclass with enhanced toString().
	 */
	static class ServiceUseLock extends ReentrantLock {
		private static final long serialVersionUID = 1L;

		ServiceUseLock() {
			super();
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
