/*******************************************************************************
 * Copyright (c) 2006, 2016 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.util.*;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.LogFilter;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

public class ExtendedLogReaderServiceImpl implements ExtendedLogReaderService {

	private final ExtendedLogReaderServiceFactory factory;
	private Set<LogListener> listeners = new HashSet<>();

	ExtendedLogReaderServiceImpl(ExtendedLogReaderServiceFactory factory) {
		this.factory = factory;
	}

	@Override
	public synchronized void addLogListener(LogListener listener, LogFilter filter) {
		checkShutdown();
		if (listener == null)
			throw new IllegalArgumentException("LogListener must not be null"); //$NON-NLS-1$

		if (filter == null)
			throw new IllegalArgumentException("LogFilter must not be null"); //$NON-NLS-1$		

		listeners.add(listener);
		factory.addLogListener(listener, filter);
	}

	@Override
	public void addLogListener(LogListener listener) {
		addLogListener(listener, ExtendedLogReaderServiceFactory.NULL_LOGGER_FILTER);
	}

	@Override
	public Enumeration<LogEntry> getLog() {
		checkShutdown();
		return factory.getLog();
	}

	@Override
	public synchronized void removeLogListener(LogListener listener) {
		checkShutdown();
		if (listener == null)
			throw new IllegalArgumentException("LogListener must not be null"); //$NON-NLS-1$

		factory.removeLogListener(listener);
		listeners.remove(listener);
	}

	private synchronized void checkShutdown() {
		if (listeners == null)
			throw new IllegalStateException("LogReaderService is shutdown."); //$NON-NLS-1$
	}

	synchronized void shutdown() {
		for (LogListener listener : listeners) {
			factory.removeLogListener(listener);
		}
		listeners = null;
	}
}
