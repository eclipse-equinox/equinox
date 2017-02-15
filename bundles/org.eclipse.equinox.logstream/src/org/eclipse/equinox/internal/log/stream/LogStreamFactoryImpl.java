package org.eclipse.equinox.internal.log.stream;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.stream.LogStreamProvider;
import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamBuilder;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class LogStreamFactoryImpl implements BundleActivator, LogStreamProvider, ServiceTrackerCustomizer<LogReaderService, AtomicReference<LogReaderService>>, LogListener {
	private final PushStreamProvider pushStreamProvider = new PushStreamProvider();
	private final Set<LogEntrySource> logEntrySources = new CopyOnWriteArraySet<>();
	private final ExecutorService executor = Executors.newFixedThreadPool(1, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "LogStream thread");
		}
	});
	private ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> logReaderService;
	BundleContext context;
	ReentrantLock eventProducerLock = new ReentrantLock();
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		this.context = context;
		logReaderService = new ServiceTracker<>(context, LogReaderService.class, this);
		logReaderService.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		logReaderService.close();
		executor.shutdown();
	}

	@Override
	public AtomicReference<LogReaderService> addingService(ServiceReference<LogReaderService> reference) {
		AtomicReference<LogReaderService> tracked = new AtomicReference<>();
		modifiedService(reference, tracked);
		return tracked;
	}

	@Override
	public void modifiedService(ServiceReference<LogReaderService> modifiedServiceRef, AtomicReference<LogReaderService> modifiedTracked) {
		eventProducerLock.lock();
		try {
			// Check if the currently used reader service is lower ranked that the modified serviceRef
			ServiceReference<LogReaderService> currentServiceRef = logReaderService.getServiceReference();
			if (currentServiceRef == null || modifiedServiceRef.compareTo(currentServiceRef) > 0) {
				// The modified service reference is higher ranked than the currently used one;
				// Use the modified service reference instead.
				LogReaderService readerService = context.getService(modifiedServiceRef);
				if (readerService != null) {
					if (modifiedTracked.get() == null) {
						// update our tracked object for the reference with the real service
						modifiedTracked.set(readerService);
					}
					// remove our listener from the currently used service
					if (currentServiceRef != null) {
						AtomicReference<LogReaderService> currentTracked = logReaderService.getService(currentServiceRef);
						if (currentTracked != null) {
							LogReaderService currentLogReader = currentTracked.get();
							if (currentLogReader != null) {
								// we were really using this service;
								// remove the our listener and unget the service
								currentLogReader.removeLogListener(this);
								context.ungetService(currentServiceRef);
								// finally null out our tracked reference
								currentTracked.set(null);
							}
						}
					}
					readerService.addLogListener(this);
				}
			}
		} finally {
			eventProducerLock.unlock();
		}
	}

	@Override
	public void removedService(ServiceReference<LogReaderService> removedRef, AtomicReference<LogReaderService> removedTracked) {
		eventProducerLock.lock();
		try {
		} finally {
			LogReaderService removedLogReader = removedTracked.get();
			if (removedLogReader != null) {
				removedLogReader.removeLogListener(this);
				context.ungetService(removedRef);
				removedTracked.set(null);
			}
			ServiceReference<LogReaderService> currentRef = logReaderService.getServiceReference();
			if (currentRef != null) {
				AtomicReference<LogReaderService> currentTracked = logReaderService.getService(currentRef);
				if (currentTracked != null) {
					LogReaderService currentLogReader = currentTracked.get();
					if (currentLogReader == null) {
						currentLogReader = context.getService(currentRef);
						currentTracked.set(currentLogReader);
					}
					if (currentLogReader != null) {
						currentLogReader.addLogListener(this);
					}
				}
			}
			eventProducerLock.unlock();
		}
	}

	@Override
	public PushStream<LogEntry> createStream(Options... options) {
		ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> withHistory = null;
		if (options != null) {
			for (Options option : options) {
				if (Options.HISTORY.equals(option)) {
					withHistory = logReaderService;
					break;
				}
			}
		}
		LogEntrySource logEntrySource = new LogEntrySource(executor, withHistory);
		PushStreamBuilder<LogEntry, BlockingQueue<PushEvent<? extends LogEntry>>> streamBuilder = pushStreamProvider.buildStream(logEntrySource);
		PushStream<LogEntry> logStream = streamBuilder.unbuffered().withExecutor(executor).create();
		logEntrySources.add(logEntrySource);
		return logStream;
	}

	@Override
	public void logged(LogEntry entry) {
		for (LogEntrySource logEntrySource : logEntrySources) {
			logEntrySource.logged(entry);
		}
	}

}
