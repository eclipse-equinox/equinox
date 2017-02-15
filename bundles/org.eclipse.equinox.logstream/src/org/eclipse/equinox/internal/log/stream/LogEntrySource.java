package org.eclipse.equinox.internal.log.stream;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushEventConsumer;
import org.osgi.util.pushstream.PushEventSource;
import org.osgi.util.tracker.ServiceTracker;

public class LogEntrySource implements PushEventSource<LogEntry> {
	private final Executor executor;
	private final ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> withHistory;
	private final Set<PushEventConsumer<? super LogEntry>> consumers = new CopyOnWriteArraySet<>();
	public LogEntrySource(Executor executor, ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> withHistory) {
		this.executor = executor;
		this.withHistory = withHistory;
	}


	@Override
	public Closeable open(PushEventConsumer<? super LogEntry> aec) throws Exception {
		if (!consumers.add(aec)){
			throw new IllegalStateException("Cannot add the same consumer multiple times");
		}
		return () -> {
			if (consumers.remove(aec)) {
				try {
					aec.accept(PushEvent.close());
				} catch (Exception e) {
					// ignore here for log stream
				}
			}
		};
	}

	public void logged(LogEntry entry) {
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
	}

}
