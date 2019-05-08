/*******************************************************************************
 * Copyright (c) 2006, 2018 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.equinox.log.LogFilter;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.eclipse.osgi.framework.util.ArrayMap;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.log.OrderedExecutor.OrderedTaskQueue;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;

public class ExtendedLogReaderServiceFactory implements ServiceFactory<ExtendedLogReaderServiceImpl> {

	static final int MAX_RECURSIONS = 50;

	static final class LogTask implements Runnable {
		private final LogEntry logEntry;
		private final LogListener listener;

		LogTask(LogEntry logEntry, LogListener listener) {
			this.logEntry = logEntry;
			this.listener = listener;
		}

		public void run() {
			safeLogged(listener, logEntry);
		}
	}

	@SuppressWarnings("unchecked")
	private static final Enumeration<LogEntry> EMPTY_ENUMERATION = Collections.enumeration(Collections.EMPTY_LIST);

	static final LogFilter NULL_LOGGER_FILTER = new LogFilter() {
		public boolean isLoggable(Bundle b, String loggerName, int logLevel) {
			return true;
		}
	};

	private static final LogFilter[] ALWAYS_LOG = new LogFilter[0];

	private static PrintStream errorStream;

	private final ReentrantReadWriteLock listenersLock = new ReentrantReadWriteLock();
	private ArrayMap<LogListener, Object[]> listeners = new ArrayMap<>(5);
	private LogFilter[] filters = null;
	private final ThreadLocal<int[]> nestedCallCount = new ThreadLocal<>();
	private final LinkedList<LogEntry> history;
	private final int maxHistory;
	private final LogLevel defaultLevel;

	private OrderedExecutor executor;

	static boolean safeIsLoggable(LogFilter filter, Bundle bundle, String name, int level) {
		try {
			return filter.isLoggable(bundle, name, level);
		} catch (RuntimeException | LinkageError e) {
			// "listener.logged" calls user code and might throw an unchecked exception
			// we catch the error here to gather information on where the problem occurred.
			// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
			getErrorStream().println("LogFilter.isLoggable threw a non-fatal unchecked exception as follows:"); //$NON-NLS-1$
			e.printStackTrace(getErrorStream());
		}
		return false;
	}

	private static synchronized PrintStream getErrorStream() {
		if (errorStream == null)
			return System.err;

		return errorStream;
	}

	public static synchronized void setErrorStream(PrintStream ps) {
		errorStream = ps;
	}

	static void safeLogged(LogListener listener, LogEntry logEntry) {
		try {
			listener.logged(logEntry);
		} catch (RuntimeException | LinkageError e) {
			// "listener.logged" calls user code and might throw an unchecked exception
			// we catch the error here to gather information on where the problem occurred.
			// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
			getErrorStream().println("LogListener.logged threw a non-fatal unchecked exception as follows:"); //$NON-NLS-1$
			e.printStackTrace(getErrorStream());
		} 	
	}

	public ExtendedLogReaderServiceFactory(int maxHistory, LogLevel defaultLevel) {
		this.defaultLevel = defaultLevel;
		this.maxHistory = maxHistory;
		if (maxHistory > 0) {
			history = new LinkedList<>();
		} else {
			history = null;
		}
	}

	public void start(EquinoxContainer equinoxContainer) {
		executor = new OrderedExecutor(equinoxContainer);
	}

	public void stop() {
		executor.shutdown();
	}

	public LogLevel getDefaultLogLevel() {
		return defaultLevel;
	}

	public ExtendedLogReaderServiceImpl getService(Bundle bundle, ServiceRegistration<ExtendedLogReaderServiceImpl> registration) {
		return new ExtendedLogReaderServiceImpl(this);
	}

	public void ungetService(Bundle bundle, ServiceRegistration<ExtendedLogReaderServiceImpl> registration, ExtendedLogReaderServiceImpl service) {
		service.shutdown();
	}

	boolean isLoggable(final Bundle bundle, final String name, final int level) {
		if (System.getSecurityManager() != null) {
			return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
				public Boolean run() {
					return isLoggablePrivileged(bundle, name, level);
				}
			});
		}
		return isLoggablePrivileged(bundle, name, level);
	}

	boolean isLoggablePrivileged(Bundle bundle, String name, int level) {
		LogFilter[] filtersCopy;
		listenersLock.readLock().lock();
		try {
			filtersCopy = filters;
		} finally {
			listenersLock.readLock().unlock();
		}
		try {
			if (incrementNestedCount() == MAX_RECURSIONS)
				return false;
			if (filtersCopy == null)
				return false;

			if (filtersCopy == ALWAYS_LOG)
				return true;

			int filtersLength = filtersCopy.length;
			for (int i = 0; i < filtersLength; i++) {
				LogFilter filter = filtersCopy[i];
				if (safeIsLoggable(filter, bundle, name, level))
					return true;
			}
		} finally {
			decrementNestedCount();
		}
		return false;
	}

	private int incrementNestedCount() {
		int[] count = getCount();
		count[0] = count[0] + 1;
		return count[0];
	}

	private void decrementNestedCount() {
		int[] count = getCount();
		if (count[0] == 0)
			return;
		count[0] = count[0] - 1;
	}

	private int[] getCount() {
		int[] count = nestedCallCount.get();
		if (count == null) {
			count = new int[] {0};
			nestedCallCount.set(count);
		}
		return count;
	}

	void log(final Bundle bundle, final String name, final StackTraceElement stackTraceElement, final Object context, final LogLevel logLevelEnum, final int level, final String message, final ServiceReference<?> ref, final Throwable exception) {
		if (System.getSecurityManager() != null) {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				public Void run() {
					logPrivileged(bundle, name, stackTraceElement, context, logLevelEnum, level, message, ref, exception);
					return null;
				}
			});
		} else {
			logPrivileged(bundle, name, stackTraceElement, context, logLevelEnum, level, message, ref, exception);
		}
	}

	void logPrivileged(Bundle bundle, String name, StackTraceElement stackTraceElement, Object context, LogLevel logLevelEnum, int level, String message, ServiceReference<?> ref, Throwable exception) {
		LogEntry logEntry = new ExtendedLogEntryImpl(bundle, name, stackTraceElement, context, logLevelEnum, level, message, ref, exception);
		storeEntry(logEntry);
		ArrayMap<LogListener, Object[]> listenersCopy;
		listenersLock.readLock().lock();
		try {
			listenersCopy = listeners;
		} finally {
			listenersLock.readLock().unlock();
		}
		try {
			if (incrementNestedCount() >= MAX_RECURSIONS)
				return;
			int size = listenersCopy.size();
			for (int i = 0; i < size; i++) {
				Object[] listenerObjects = listenersCopy.getValue(i);
				LogFilter filter = (LogFilter) listenerObjects[0];
				if (safeIsLoggable(filter, bundle, name, level)) {
					LogListener listener = listenersCopy.getKey(i);
					OrderedTaskQueue orderedTaskQueue = (OrderedTaskQueue) listenerObjects[1];
					if (orderedTaskQueue != null) {
						orderedTaskQueue.execute(new LogTask(logEntry, listener), size);
					} else {
						// log synchronously
						safeLogged(listener, logEntry);
					}
				}
			}
		} finally {
			decrementNestedCount();
		}
	}

	private void storeEntry(LogEntry logEntry) {
		if (history != null) {
			synchronized (history) {
				if (history.size() == maxHistory) {
					history.removeLast();
				}
				history.addFirst(logEntry);
			}
		}
	}

	void addLogListener(LogListener listener, LogFilter filter) {
		listenersLock.writeLock().lock();
		try {
			ArrayMap<LogListener, Object[]> listenersCopy = new ArrayMap<>(listeners.getKeys(), listeners.getValues());
			Object[] listenerObjects = listenersCopy.get(listener);
			if (listenerObjects == null) {
				// Only create a task queue for non-SynchronousLogListeners
				OrderedTaskQueue taskQueue = (listener instanceof SynchronousLogListener) ? null : executor.createQueue();
				listenerObjects = new Object[] {filter, taskQueue};
			} else if (filter != listenerObjects[0]) {
				// update the filter
				listenerObjects[0] = filter;
			}
			listenersCopy.put(listener, listenerObjects);
			recalculateFilters(listenersCopy);
			listeners = listenersCopy;
		} finally {
			listenersLock.writeLock().unlock();
		}
	}

	private void recalculateFilters(ArrayMap<LogListener, Object[]> listenersCopy) {
		List<LogFilter> filtersList = new ArrayList<>();
		int size = listenersCopy.size();
		for (int i = 0; i < size; i++) {
			Object[] listenerObjects = listenersCopy.getValue(i);
			LogFilter filter = (LogFilter) listenerObjects[0];
			if (filter == NULL_LOGGER_FILTER) {
				filters = ALWAYS_LOG;
				return;
			}
			filtersList.add(filter);
		}

		if (filtersList.isEmpty())
			filters = null;

		filters = filtersList.toArray(new LogFilter[filtersList.size()]);
	}

	void removeLogListener(LogListener listener) {
		listenersLock.writeLock().lock();
		try {
			ArrayMap<LogListener, Object[]> listenersCopy = new ArrayMap<>(listeners.getKeys(), listeners.getValues());
			listenersCopy.remove(listener);
			recalculateFilters(listenersCopy);
			listeners = listenersCopy;
		} finally {
			listenersLock.writeLock().unlock();
		}
	}

	Enumeration<LogEntry> getLog() {
		if (history == null) {
			return EMPTY_ENUMERATION;
		}
		synchronized (history) {
			return Collections.enumeration(new ArrayList<>(history));
		}
	}

}

/**
* This Executor uses OrderedTaskQueue to execute tasks in a FIFO order.
*/
class OrderedExecutor implements ThreadFactory {
	private final int nThreads = Math.min(Runtime.getRuntime().availableProcessors(), 10);
	private final String logThreadName;
	private final ThreadPoolExecutor delegate;
	private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
	private int coreSize = 0;

	public OrderedExecutor(final EquinoxContainer equinoxContainer) {
		this.logThreadName = "Equinox Log Thread - " + equinoxContainer.toString(); //$NON-NLS-1$
		this.delegate = new ThreadPoolExecutor(0, nThreads, 10L, TimeUnit.SECONDS, queue, this);
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, logThreadName);
		t.setDaemon(true);
		return t;
	}

	void executeOrderedTask(Runnable task, OrderedTaskQueue dependencyQueue, int numListeners) {
		OrderedTaskQueue.OrderedTask firstOrderedTask;
		synchronized (this) {
			// This will either queue the task to wait for existing ones to finish
			// or it will return the first ordered task so it can kick off the execution
			// for ordered task queue
			firstOrderedTask = dependencyQueue.addTaskAndReturnIfFirst(task);
			if (firstOrderedTask != null) {
				// Check that we are at the optimal target for core pool size
				int targetSize = Math.min(nThreads, numListeners);
				if (coreSize < targetSize) {
					coreSize = targetSize;
					delegate.setCorePoolSize(coreSize);
				}
			}
		}

		// execute the first ordered task now
		if (firstOrderedTask != null) {
			delegate.execute(firstOrderedTask);
		}
	}

	OrderedTaskQueue createQueue() {
		return new OrderedTaskQueue();
	}

	void shutdown() {
		delegate.shutdown();
	}

	void executeNextTask(OrderedTaskQueue taskQueue) {
		OrderedTaskQueue.OrderedTask nextTask;
		synchronized (this) {
			nextTask = taskQueue.getNextTask();
			if (nextTask == null && queue.isEmpty()) {
				// The event storm has ended, let the threads be reclaimed
				delegate.setCorePoolSize(0);
				coreSize = 0;
			}
		}
		if (nextTask != null) {
			delegate.execute(nextTask);
		}
	}

	/**
	 * Keeps an list of ordered tasks and guarantees the tasks are run in the order
	 * they are queued.  Tasks executed with this queue will always be run
	 * in FIFO order and will never run in parallel to guarantee events are
	 * received in the proper order by the listener.  Each log listener
	 * has its own ordered task queue.
	 * <p>
	 * Note that only the execute method is thread safe.  All other methods
	 * must be guarded by the OrderedExecutor monitor.
	 */
	class OrderedTaskQueue {
		private final Queue<OrderedTask> dependencyQueue = new LinkedList<>();
		private AtomicReference<OrderedTask> firstTask = new AtomicReference<>();

		void execute(Runnable task, int numListeners) {
			executeOrderedTask(task, this, numListeners);
		}

		OrderedTask addTaskAndReturnIfFirst(Runnable task) {
			OrderedTask orderedTask = new OrderedTask(task);
			if (firstTask.compareAndSet(null, orderedTask)) {
				return orderedTask;
			}
			dependencyQueue.add(orderedTask);
			return null;
		}

		OrderedTask getNextTask() {
			OrderedTask nextTask = dependencyQueue.poll();
			if (nextTask == null) {
				// The queue has been drained reset the first task.
				// the next task for this ordered task queue will become the first again
				firstTask.set(null);
			}
			return nextTask;
		}

		class OrderedTask implements Runnable {
			private final Runnable task;

			public OrderedTask(Runnable task) {
				this.task = task;
			}

			@Override
			public void run() {
				try {
					task.run();
				} finally {
					executeNextTask(OrderedTaskQueue.this);
				}
			}
		}
	}
}
