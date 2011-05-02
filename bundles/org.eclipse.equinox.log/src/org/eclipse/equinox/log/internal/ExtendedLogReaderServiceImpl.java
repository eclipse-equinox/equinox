/*******************************************************************************
 * Copyright (c) 2006, 2009 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

import java.util.*;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.LogFilter;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogListener;

public class ExtendedLogReaderServiceImpl implements ExtendedLogReaderService {

	private final ExtendedLogReaderServiceFactory factory;
	private final Bundle bundle;
	private Set listeners = new HashSet();

	ExtendedLogReaderServiceImpl(ExtendedLogReaderServiceFactory factory, Bundle bundle) {
		this.factory = factory;
		this.bundle = bundle;
	}

	public synchronized void addLogListener(LogListener listener, LogFilter filter) {
		checkShutdown();
		if (listener == null)
			throw new IllegalArgumentException("LogListener must not be null"); //$NON-NLS-1$

		if (filter == null)
			throw new IllegalArgumentException("LogFilter must not be null"); //$NON-NLS-1$		

		listeners.add(listener);
		factory.addLogListener(listener, filter);
	}

	public void addLogListener(LogListener listener) {
		addLogListener(listener, ExtendedLogReaderServiceFactory.NULL_LOGGER_FILTER);
	}

	public Enumeration getLog() {
		checkShutdown();
		return factory.getLog();
	}

	public synchronized void removeLogListener(LogListener listener) {
		checkShutdown();
		if (listener == null)
			throw new IllegalArgumentException("LogListener must not be null"); //$NON-NLS-1$

		factory.removeLogListener(listener);
		listeners.remove(listener);
	}

	private synchronized void checkShutdown() {
		if (listeners == null)
			throw new IllegalStateException("LogReaderService for " + bundle.getSymbolicName() + " (id=" + bundle.getBundleId() + ") is shutdown."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	synchronized void shutdown() {
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			factory.removeLogListener((LogListener) it.next());
		}
		listeners = null;
	}
}
