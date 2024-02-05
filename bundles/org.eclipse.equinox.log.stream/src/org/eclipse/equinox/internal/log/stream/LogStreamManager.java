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

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.stream.LogStreamProvider;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/* LogStreamManager is used to start and stop the bundle and keeps the track of the logs using the 
 * ServiceTrackerCustomizer<LogReaderService, AtomicReference<LogReaderService>> which listens to 
 * the incoming logs using the LogListener. It is also responsible to provide service tracker 
 * and each log entry to the LogStreamProviderFactory.
 */
public class LogStreamManager implements BundleActivator,
		ServiceTrackerCustomizer<LogReaderService, AtomicReference<LogReaderService>>, LogListener {
	private ServiceRegistration<LogStreamProvider> logStreamServiceRegistration;
	private LogStreamProviderFactory logStreamProviderFactory;
	private ServiceTracker<LogReaderService, AtomicReference<LogReaderService>> logReaderService;
	private BundleContext context;
	private final ReentrantLock eventProducerLock = new ReentrantLock();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bc) throws Exception {
		this.context = bc;
		logReaderService = new ServiceTracker<>(context, LogReaderService.class, this);
		logReaderService.open();
		logStreamProviderFactory = new LogStreamProviderFactory(logReaderService);
		logStreamServiceRegistration = context.registerService(LogStreamProvider.class, logStreamProviderFactory, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		logReaderService.close();
		logStreamServiceRegistration.unregister();
		logStreamServiceRegistration = null;
		logStreamProviderFactory.shutdownExecutor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.
	 * framework.ServiceReference)
	 */

	@Override
	public AtomicReference<LogReaderService> addingService(ServiceReference<LogReaderService> reference) {
		AtomicReference<LogReaderService> tracked = new AtomicReference<>();
		modifiedService(reference, tracked);
		return tracked;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.
	 * framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void modifiedService(ServiceReference<LogReaderService> modifiedServiceRef,
			AtomicReference<LogReaderService> modifiedTracked) {
		eventProducerLock.lock();
		try {
			// Check if the currently used reader service is lower ranked that the modified
			// serviceRef
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
						AtomicReference<LogReaderService> currentTracked = logReaderService
								.getService(currentServiceRef);
						if (currentTracked != null) {
							LogReaderService currentLogReader = currentTracked.get();
							if (currentLogReader != null) {
								// we were really using this service;
								// remove our listener and unget the service
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.
	 * framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(ServiceReference<LogReaderService> removedRef,
			AtomicReference<LogReaderService> removedTracked) {
		eventProducerLock.lock();
		try {
			LogReaderService removedLogReader = removedTracked.get();
			if (removedLogReader != null) {
				// remove the listener
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
		} finally {
			eventProducerLock.unlock();
		}
	}

	/*
	 * It is used to post each log entry to the LogStreamProviderFactory
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.log.LogListener#logged(org.osgi.service.log.LogEntry)
	 */

	@Override
	public void logged(LogEntry entry) {
		logStreamProviderFactory.postLogEntry(entry);
	}

}
