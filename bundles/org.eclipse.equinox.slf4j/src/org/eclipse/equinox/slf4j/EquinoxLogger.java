/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *     IBM Corporation -  Update deprecated logger
 *******************************************************************************/
package org.eclipse.equinox.slf4j;

import java.util.Locale;

import org.osgi.service.log.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

class EquinoxLogger extends org.slf4j.helpers.AbstractLogger {

	private static final long serialVersionUID = 1L;
	private final Logger logger;

	/**
	 * The minimal level a log message must have in order to be forwarded to the
	 * underlying Equinox logger. Messages with lower level are discarded.
	 */
	private static final String LOG_LEVEL_THRESHOLD = System.getProperty("equinox.slf4j.logThreshold", "INFO");
	private final Level logLevelThreshold;

	EquinoxLogger(String name, Logger logger) {
		this.name = name;
		this.logger = logger;
		Level level;
		try {
			level = Level.valueOf(LOG_LEVEL_THRESHOLD.toUpperCase(Locale.ENGLISH));
		} catch (Exception e) {
			level = Level.INFO;
		}
		this.logLevelThreshold = level;
	}

	private boolean isEnabled(Level level) {
		return this.logLevelThreshold.compareTo(level) >= 0;
	}

	@Override
	public boolean isTraceEnabled() {
		return logger != null && isEnabled(Level.TRACE) && logger.isTraceEnabled();
	}

	@Override
	public boolean isDebugEnabled() {
		return logger != null && isEnabled(Level.DEBUG) && logger.isDebugEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return logger != null && isEnabled(Level.INFO) && logger.isInfoEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return logger != null && isEnabled(Level.WARN) && logger.isWarnEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return logger != null && isEnabled(Level.ERROR) && logger.isErrorEnabled();
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return isTraceEnabled();
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return isDebugEnabled();
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return isInfoEnabled();
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return isWarnEnabled();
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return isErrorEnabled();
	}

	@Override
	protected String getFullyQualifiedCallerName() {
		return null;
	}

	@Override
	protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments,
			Throwable throwable) {
		if (logger == null) {
			return;
		}
		//Must not pass a null argument to OSGi Logger
		Object[] loggerArguments = throwable != null ? new Object[] { throwable } : new Object[0];

		if (level == Level.TRACE && isTraceEnabled()) {
			String formattedMessage = safeBasicArrayFormat(messagePattern, arguments);
			logger.trace(formattedMessage, loggerArguments);
		}
		if (level == Level.DEBUG && isDebugEnabled()) {
			String formattedMessage = safeBasicArrayFormat(messagePattern, arguments);
			logger.debug(formattedMessage, loggerArguments);
		}
		if (level == Level.WARN && isWarnEnabled()) {
			String formattedMessage = safeBasicArrayFormat(messagePattern, arguments);
			logger.warn(formattedMessage, loggerArguments);
		}
		if (level == Level.INFO && isInfoEnabled()) {
			String formattedMessage = safeBasicArrayFormat(messagePattern, arguments);
			logger.info(formattedMessage, loggerArguments);
		}
		if (level == Level.ERROR && isErrorEnabled()) {
			String formattedMessage = safeBasicArrayFormat(messagePattern, arguments);
			logger.error(formattedMessage, loggerArguments);
		}
	}

	/**
	 * Wrapper around SLF4J basicArrayFormat that guards against a null message pattern.
	 * OSGi does not allow logging a null message
	 * @param messagePattern
	 * @param arguments
	 */
	private String safeBasicArrayFormat(String messagePattern, Object[] arguments) {
		if(messagePattern == null) {
			return String.valueOf(messagePattern);
		} else {
			return MessageFormatter.basicArrayFormat(messagePattern, arguments);
		}
	}

}