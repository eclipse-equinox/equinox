/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.log.stream;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.stream.LogStreamProvider;
import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamBuilder;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.tracker.ServiceTracker;

public class LogStreamProviderImpl implements LogStreamProvider {
	private final PushStreamProvider pushStreamProvider = new PushStreamProvider();
	private final ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> logReaderService;
	private final WeakHashMap<LogEntrySource, Boolean> weakMap = new WeakHashMap<>();
	private final Set<LogEntrySource> logEntrySources = Collections.newSetFromMap(weakMap);

	private final ReentrantReadWriteLock historyLock = new ReentrantReadWriteLock();

	public LogStreamProviderImpl(ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> logReaderService) {
		this.logReaderService = logReaderService;
	}

	/* Create a PushStream of {@link LogEntry} objects.
	 * The returned PushStream is an unbuffered stream with a parallelism of one.
	 * (non-Javadoc)
	 * @see org.osgi.service.log.stream.LogStreamProvider#createStream(org.osgi.service.log.stream.LogStreamProvider.Options[])
	 */

	@Override
	public PushStream<LogEntry> createStream(Options... options) {
		ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> withHistory = null;
		if (options != null) {
			for (Options option : options) {
				if (Options.HISTORY.equals(option)) {
					withHistory = logReaderService;
				}
			}
		}

		// A write lock is acquired in order to add logEntrySource into the Set of logEntrySources.
		historyLock.writeLock().lock();
		try {
			LogEntrySource logEntrySource = new LogEntrySource(withHistory);
			PushStreamBuilder<LogEntry, BlockingQueue<PushEvent<? extends LogEntry>>> streamBuilder = pushStreamProvider.buildStream(logEntrySource);
			//creating an unbuffered stream
			PushStream<LogEntry> logStream = streamBuilder.unbuffered().create();
			logEntrySource.setLogStream(logStream);
			// Adding to sources makes the source start listening for new entries
			logEntrySources.add(logEntrySource);
			return logStream;
		} finally {
			historyLock.writeLock().unlock();
		}
	}

	/*
	 * Send the incoming log entries to the logEntrySource.logged(entry) for the consumer to accept it.
	 */
	public void logged(LogEntry entry) {
		historyLock.readLock().lock();
		try {
			for (LogEntrySource logEntrySource : logEntrySources) {
				logEntrySource.logged(entry);
			}
		} finally {
			historyLock.readLock().unlock();
		}
	}

	/*
	 * Closing the stream for each source.
	 */
	public void close() {
		PushStream<LogEntry> logStream;
		historyLock.readLock().lock();
		try {
			for (LogEntrySource logEntrySource : logEntrySources) {
				logStream = logEntrySource.getLogStream();
				try {
					logStream.close();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			historyLock.readLock().unlock();
		}
	}

}
