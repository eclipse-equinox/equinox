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

import org.osgi.service.log.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

class EquinoxLogger extends org.slf4j.helpers.AbstractLogger {

	private static final long serialVersionUID = 1L;
	private final Logger logger;

	EquinoxLogger(String name, Logger logger) {
		this.name = name;
		this.logger = logger;
	}

	@Override
	public boolean isTraceEnabled() {
		return logger != null && logger.isTraceEnabled();
	}

	@Override
	public boolean isDebugEnabled() {
		return logger != null && logger.isDebugEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return logger != null && logger.isInfoEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return logger != null && logger.isWarnEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return logger != null && logger.isErrorEnabled();
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

		if(level == Level.TRACE && logger.isTraceEnabled()) {
			String formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments);
			logger.trace(formattedMessage,throwable);
		}
		if(level == Level.DEBUG && logger.isDebugEnabled()) {
			String formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments);
			logger.debug(formattedMessage,throwable);
		}
		if(level == Level.WARN && logger.isWarnEnabled()) {
			String formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments);
			logger.warn(formattedMessage,throwable);
		}
		if(level == Level.INFO && logger.isInfoEnabled()) {
			String formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments);
			logger.info(formattedMessage, throwable);
		}
		if(level == Level.ERROR && logger.isInfoEnabled()) {
			String formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments);
			logger.info(formattedMessage, throwable);
		}
	}

}