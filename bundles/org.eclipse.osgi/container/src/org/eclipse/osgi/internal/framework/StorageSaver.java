/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.osgi.internal.framework;

import java.io.IOException;
import java.util.concurrent.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

public final class StorageSaver {
	private static class StorageSaverTask implements Runnable {
		private final EquinoxContainer container;

		public StorageSaverTask(EquinoxContainer container) {
			this.container = container;
		}

		@Override
		public void run() {
			try {
				container.getStorage().save();
			} catch (IOException e) {
				container.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, "Error saving on update", e); //$NON-NLS-1$
			}
		}
	}

	private final EquinoxContainer container;
	private final long delay;
	private final ScheduledFuture<?> future;
	private final Thread hook;
	private final StorageSaverTask task;

	public StorageSaver(EquinoxContainer container) {
		this.container = container;
		task = new StorageSaverTask(container);
		delay = computeDelay();
		future = scheduleTask();
		hook = registerShutdownHook();
	}

	public void close() {
		unregisterShutdownHook();
		unscheduleTask();
	}

	public void save() {
		if (delay != 0)
			// Periodic saves are enabled or saves are disabled altogether.
			return;
		// Immediately save on request.
		task.run();
	}

	private Thread registerShutdownHook() {
		Thread thread = new Thread(task, "Equinox Shutdown Hook"); //$NON-NLS-1$
		Runtime.getRuntime().addShutdownHook(thread);
		return thread;
	}

	private long computeDelay() {
		EquinoxConfiguration configuration = container.getConfiguration();
		// Default provided by the configuration, if necessary.
		String delayProp = configuration.getConfiguration(EquinoxConfiguration.PROP_STATE_SAVE_DELAY_INTERVAL);
		// Type compatibility verified by the configuration.
		return Long.parseLong(delayProp);
	}

	private ScheduledFuture<?> scheduleTask() {
		// Negative delay disables saves. Zero delay results in immediate saves.
		if (delay <= 0)
			return null;
		ScheduledExecutorService executor = container.getScheduledExecutor();
		return executor.scheduleWithFixedDelay(task, delay, delay, TimeUnit.MILLISECONDS);
	}

	private void unregisterShutdownHook() {
		try {
			Runtime.getRuntime().removeShutdownHook(hook);
		} catch (IllegalStateException e) {
			// Ignore.
		}
	}

	private void unscheduleTask() {
		if (future != null)
			future.cancel(false);
	}
}
