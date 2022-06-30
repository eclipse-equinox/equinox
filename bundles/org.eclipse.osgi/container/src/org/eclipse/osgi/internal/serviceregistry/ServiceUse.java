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
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.messages.Msg;
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
	/* @GuardedBy("this") */
	private int useCount;

	/** lock for this service */
	private final ReentrantLock lock = new ReentrantLockExt();
	public static final int DEADLOCK = 1001;

	private Duration lockTimeout = Duration.ofSeconds(10);

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

	void runWithLock(Runnable runnable) {
		callWithLock(Executors.callable(runnable));
	}

	<T> T callWithLock(Callable<T> callable) {
		try {
			if (lock.tryLock(getLockTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
				try {
					return callable.call();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt(); // To appease sonar
					return null;
				} catch (Exception e) {
					throw new RuntimeException("Callable threw exception", e); //$NON-NLS-1$
				} finally {
					lock.unlock();
				}
			}
			throw new ServiceException(
					"Failed to acquire lock within try period, Lock, id:" + System.identityHashCode(lock) + ", " //$NON-NLS-1$ //$NON-NLS-2$
							+ lock.toString(),
					DEADLOCK); // $NON-NLS-1$
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // To appease sonar
			return null;
		}
	}

	Duration getLockTimeout() {
		return lockTimeout;
	}

	ReentrantLock getLock() {
		return lock;
	}

	/**
	 * Get a service's service object and increment the use count.
	 *
	 * @return The service object.
	 */
	/* @GuardedBy("this") */
	S getService() {
		assert lock.isHeldByCurrentThread();
		if (debug.DEBUG_SERVICES) {
			Debug.println('[' + Thread.currentThread().getName() + "] getService[factory=" + registration.getBundle() //$NON-NLS-1$
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
	/* @GuardedBy("this") */
	boolean ungetService() {
		assert lock.isHeldByCurrentThread();
		if (!inUse()) {
			return false;
		}
		if (debug.DEBUG_SERVICES) {
			Debug.println('[' + Thread.currentThread().getName() + "] ungetService[factory=" + registration.getBundle() //$NON-NLS-1$
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
	/* @GuardedBy("this") */
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
	/* @GuardedBy("this") */
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
	/* @GuardedBy("this") */
	boolean releaseServiceObject(final S service) {
		if ((service == null) || (service != getCachedService())) {
			throw new IllegalArgumentException(Msg.SERVICE_OBJECTS_UNGET_ARGUMENT_EXCEPTION);
		}
		if (debug.DEBUG_SERVICES) {
			Debug.println('[' + Thread.currentThread().getName() + "] releaseServiceObject[factory=" //$NON-NLS-1$
					+ registration.getBundle() + "](" + context.getBundleImpl() + "," + registration + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return ungetService();
	}

	/**
	 * Release all uses of the service and reset the use count to zero.
	 */
	/* @GuardedBy("this") */
	void release() {
		assert lock.isHeldByCurrentThread();
		resetUse();
	}

	/**
	 * Is this service use using any services?
	 *
	 * @return true if no services are being used and this service use can be discarded.
	 */
	/* @GuardedBy("this") */
	boolean isEmpty() {
		assert lock.isHeldByCurrentThread();
		return !inUse();
	}

	/**
	 * Is the use count non zero?
	 *
	 * @return true if the use count is greater than zero.
	 */
	/* @GuardedBy("this") */
	boolean inUse() {
		return useCount > 0;
	}

	/**
	 * Incrementing the use count.
	 */
	/* @GuardedBy("this") */
	void incrementUse() {
		if (useCount == Integer.MAX_VALUE) {
			throw new ServiceException(Msg.SERVICE_USE_OVERFLOW);
		}
		useCount++;
	}

	/**
	 * Decrementing the use count.
	 */
	/* @GuardedBy("this") */
	void decrementUse() {
		assert inUse();
		useCount--;
	}

	/**
	 * Reset the use count to zero.
	 */
	/* @GuardedBy("this") */
	void resetUse() {
		useCount = 0;
	}

	private static class ReentrantLockExt extends ReentrantLock {
		private static final long serialVersionUID = 161034782199227436L;

		public ReentrantLockExt() {
			super();
		}

		public ReentrantLockExt(boolean fair) {
			super(fair);
		}

		/**
		 * Returns a string identifying this lock, as well as its lock state. The state,
		 * in brackets, includes either the String {@code "Unlocked"} or the String
		 * {@code "Locked by"} followed by the {@linkplain Thread#getName name} of the
		 * owning thread.
		 *
		 * @return a string identifying this lock, as well as its lock state
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
					StringBuilder sb = new StringBuilder("\"" + o.getName() + "\"" + (o.isDaemon() ? " daemon" : "")
							+ " prio=" + o.getPriority() + " Id=" + o.getId() + " " + o.getState());

					for (StackTraceElement traceElement : trace)
						sb.append("\tat " + traceElement + "\n");

					return super.toString() + "[Locked by thread " + o.getName() + "], Details:\n" + sb.toString();
				} catch (Exception e) {
					// do nothing and fall back to just the default, thread might be gone
				}
			}
			return super.toString() + "[Unlocked]";
		}
	}
}
