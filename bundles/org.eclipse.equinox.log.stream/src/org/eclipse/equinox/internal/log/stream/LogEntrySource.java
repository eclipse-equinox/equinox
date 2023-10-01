/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package org.eclipse.equinox.internal.log.stream;

import java.io.Closeable;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushEventConsumer;
import org.osgi.util.pushstream.PushEventSource;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.tracker.ServiceTracker;

public class LogEntrySource implements PushEventSource<LogEntry> {
	private final Set<PushEventConsumer<? super LogEntry>> consumers = new CopyOnWriteArraySet<>();
	private final ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> withHistory;
	private volatile PushStream<LogEntry> logStream;
	private final ReentrantLock historyLock = new ReentrantLock();

	public LogEntrySource(ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> withHistory) {
		this.withHistory = withHistory;

	}

	public PushStream<LogEntry> getLogStream() {
		return logStream;
	}

	public void setLogStream(PushStream<LogEntry> logStream) {
		this.logStream = logStream;
	}

	/*
	 * Open method isused to connect to the source and begin receiving a stream of
	 * events. It returns an AutoCloseable which can be used to close the event
	 * stream. If the close method is called on this object then the stream is
	 * terminated by sending a close event. (non-Javadoc)
	 * 
	 * @see org.osgi.util.pushstream.PushEventSource#open(org.osgi.util.pushstream.
	 * PushEventConsumer)
	 */

	@Override
	public Closeable open(PushEventConsumer<? super LogEntry> aec) throws Exception {

		LinkedBlockingDeque<LogEntry> historyList = new LinkedBlockingDeque<>();

		if (!consumers.add(aec)) {
			throw new IllegalStateException("Cannot add the same consumer multiple times"); //$NON-NLS-1$
		}

		/*
		 * when history is not equal to null then we acquire a lock to provide the full
		 * history to the consumer first before any other new entries
		 */
		if (withHistory != null) {
			historyLock.lock();
			try {
				AtomicReference<LogReaderService> readerRef = withHistory.getService();
				LogReaderService reader = readerRef.get();
				if (reader != null) {
					// Enumeration has the most recent entry first
					Enumeration<LogEntry> e = reader.getLog();
					if (e != null) {
						while (e.hasMoreElements()) {
							historyList.add(e.nextElement());
						}
					}
					// Logging the history in the order of their appearance
					if (historyList != null) {
						while (!historyList.isEmpty()) {
							LogEntry logEntry = historyList.removeLast();
							logged(logEntry);
						}
					}
				}
			} finally {
				historyLock.unlock();
			}
		}

		Closeable result = () -> {
			if (consumers.remove(aec)) {
				try {
					aec.accept(PushEvent.close());
				} catch (Exception e) {
					// ignore here for log stream
				}
			}
		};

		return result;
	}

	public void logged(LogEntry entry) {
		if (withHistory != null) {
			historyLock.lock();
		}

		/*
		 * consumer accepts the incoming log entries and returns a back pressure. A
		 * return of zero indicates that event delivery may continue immediately. A
		 * positive return value indicates that the source should delay sending any
		 * further events for the requested number of milliseconds. A return value of -1
		 * indicates that no further events should be sent and that the stream can be
		 * closed.
		 * 
		 * @see org.osgi.util.pushstream.PushEventConsumer<T>
		 */
		try {
			for (PushEventConsumer<? super LogEntry> consumer : consumers) {
				try {
					long status = consumer.accept(PushEvent.data(entry));

					if (status < 0) {
						consumer.accept(PushEvent.close());
					}

				} catch (Exception e) {
					// we ignore exceptions here for log stream
				}
			}
		} finally {
			if (withHistory != null) {
				historyLock.unlock();
			}

		}
	}
}
