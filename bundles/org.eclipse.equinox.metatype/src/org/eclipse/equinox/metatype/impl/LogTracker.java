/*******************************************************************************
 * Copyright (c) 1998, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.metatype.impl;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * LogTracker class. This class encapsulates the LogService and handles all
 * issues such as the service coming and going.
 */

public class LogTracker extends ServiceTracker<LogService, LogService> {

	@SuppressWarnings("deprecation")
	final static int LOG_ERROR = LogService.LOG_ERROR;

	@SuppressWarnings("deprecation")
	final static int LOG_WARNING = LogService.LOG_WARNING;

	@SuppressWarnings("deprecation")
	final static int LOG_INFO = LogService.LOG_INFO;

	@SuppressWarnings("deprecation")
	final static int LOG_DEBUG = LogService.LOG_DEBUG;

	/** LogService interface class name */
	protected final static String clazz = "org.osgi.service.log.LogService"; //$NON-NLS-1$

	/** PrintStream to use if LogService is unavailable */
	private final PrintStream out;

	/**
	 * Create new LogTracker.
	 *
	 * @param context BundleContext of parent bundle.
	 * @param out     Default PrintStream to use if LogService is unavailable.
	 */
	public LogTracker(BundleContext context, PrintStream out) {
		super(context, clazz, null);
		this.out = out;
	}

	/*
	 * ----------------------------------------------------------------------
	 * LogService Interface implementation
	 * ----------------------------------------------------------------------
	 */

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	public void log(ServiceReference<?> reference, int level, String message) {
		log(reference, level, message, null);
	}

	@SuppressWarnings("deprecation")
	public void log(ServiceReference<?> reference, int level, String message, Throwable exception) {
		ServiceReference<LogService>[] references = getServiceReferences();

		if (references != null) {
			int size = references.length;

			for (int i = 0; i < size; i++) {
				LogService service = getService(references[i]);
				if (service != null) {
					try {
						service.log(reference, level, message, exception);
					} catch (Exception e) {
						// TODO: consider printing to System Error
					}
				}
			}

			return;
		}

		noLogService(level, message, exception, reference);
	}

	/**
	 * The LogService is not available so we write the message to a PrintStream.
	 *
	 * @param level     Logging level
	 * @param message   Log message.
	 * @param throwable Log exception or null if none.
	 * @param reference ServiceReference associated with message or null if none.
	 */
	protected void noLogService(int level, String message, Throwable throwable, ServiceReference<?> reference) {
		if (out != null) {
			synchronized (out) {
				// Bug #113286. If no log service present and messages are being
				// printed to stdout, prepend message with a timestamp.
				String timestamp = getDate(new Date());
				out.print(timestamp + " "); //$NON-NLS-1$

				switch (level) {
				case LOG_DEBUG: {
					out.print(LogTrackerMsg.Debug);

					break;
				}
				case LOG_INFO: {
					out.print(LogTrackerMsg.Info);

					break;
				}
				case LOG_WARNING: {
					out.print(LogTrackerMsg.Warning);

					break;
				}
				case LOG_ERROR: {
					out.print(LogTrackerMsg.Error);

					break;
				}
				default: {
					out.print("["); //$NON-NLS-1$
					out.print(LogTrackerMsg.Unknown_Log_level);
					out.print("]: "); //$NON-NLS-1$

					break;
				}
				}

				out.println(message);

				if (reference != null) {
					out.println(reference);
				}

				if (throwable != null) {
					throwable.printStackTrace(out);
				}
			}
		}
	}

	// from EclipseLog to avoid using DateFormat -- see bug 149892#c10
	private String getDate(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		StringBuffer sb = new StringBuffer();
		appendPaddedInt(c.get(Calendar.YEAR), 4, sb).append('-');
		appendPaddedInt(c.get(Calendar.MONTH) + 1, 2, sb).append('-');
		appendPaddedInt(c.get(Calendar.DAY_OF_MONTH), 2, sb).append(' ');
		appendPaddedInt(c.get(Calendar.HOUR_OF_DAY), 2, sb).append(':');
		appendPaddedInt(c.get(Calendar.MINUTE), 2, sb).append(':');
		appendPaddedInt(c.get(Calendar.SECOND), 2, sb).append('.');
		appendPaddedInt(c.get(Calendar.MILLISECOND), 3, sb);
		return sb.toString();
	}

	private StringBuffer appendPaddedInt(int value, int pad, StringBuffer buffer) {
		pad = pad - 1;
		if (pad == 0)
			return buffer.append(Integer.toString(value));
		int padding = (int) Math.pow(10, pad);
		if (value >= padding)
			return buffer.append(Integer.toString(value));
		while (padding > value && padding > 1) {
			buffer.append('0');
			padding = padding / 10;
		}
		buffer.append(value);
		return buffer;
	}
}
