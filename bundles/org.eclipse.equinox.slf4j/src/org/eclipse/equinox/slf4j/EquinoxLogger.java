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
 *******************************************************************************/
package org.eclipse.equinox.slf4j;

import org.osgi.service.log.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

class EquinoxLogger extends org.slf4j.helpers.MarkerIgnoringBase {

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
	public void trace(String msg) {
		if (isTraceEnabled()) {
			logger.trace(msg);
		}
	}

	@Override
	public void trace(String msg, Throwable t) {
		if (isTraceEnabled()) {
			logger.trace(msg, t);
		}
	}

	@Override
	public void trace(String format, Object arg1) {
		if (isTraceEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1);
			logger.trace(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		if (isTraceEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
			logger.trace(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void trace(String format, Object... argArray) {
		if (isTraceEnabled()) {
			FormattingTuple tp = MessageFormatter.arrayFormat(format, argArray);
			logger.trace(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public boolean isDebugEnabled() {
		return logger != null && logger.isDebugEnabled();
	}

	@Override
	public void debug(String msg) {
		if (isDebugEnabled()) {
			logger.debug(msg);
		}
	}

	@Override
	public void debug(String msg, Throwable t) {
		if (isDebugEnabled()) {
			logger.debug(msg, t);
		}
	}

	@Override
	public void debug(String format, Object arg1) {
		if (isDebugEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1);
			logger.debug(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		if (isDebugEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
			logger.debug(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void debug(String format, Object... argArray) {
		if (isDebugEnabled()) {
			FormattingTuple tp = MessageFormatter.arrayFormat(format, argArray);
			logger.debug(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public boolean isInfoEnabled() {
		return logger != null && logger.isInfoEnabled();
	}

	@Override
	public void info(String msg) {
		if (isInfoEnabled()) {
			logger.info(msg);
		}
	}

	@Override
	public void info(String msg, Throwable t) {
		if (isInfoEnabled()) {
			logger.info(msg, t);
		}
	}

	@Override
	public void info(String format, Object arg1) {
		if (isInfoEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1);
			logger.info(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		if (isInfoEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
			logger.info(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void info(String format, Object... argArray) {
		if (isInfoEnabled()) {
			FormattingTuple tp = MessageFormatter.arrayFormat(format, argArray);
			logger.info(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public boolean isWarnEnabled() {
		return logger != null && logger.isWarnEnabled();
	}

	@Override
	public void warn(String msg) {
		if (isWarnEnabled()) {
			logger.warn(msg);
		}
	}

	@Override
	public void warn(String msg, Throwable t) {
		if (isWarnEnabled()) {
			logger.warn(msg, t);
		}
	}

	@Override
	public void warn(String format, Object arg1) {
		if (isWarnEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1);
			logger.warn(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		if (isWarnEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
			logger.warn(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void warn(String format, Object... argArray) {
		if (isWarnEnabled()) {
			FormattingTuple tp = MessageFormatter.arrayFormat(format, argArray);
			logger.warn(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public boolean isErrorEnabled() {
		return logger != null && logger.isErrorEnabled();
	}

	@Override
	public void error(String msg) {
		if (isErrorEnabled()) {
			logger.error(msg);
		}
	}

	@Override
	public void error(String msg, Throwable t) {
		if (isErrorEnabled()) {
			logger.error(msg, t);
		}
	}

	@Override
	public void error(String format, Object arg1) {
		if (isErrorEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1);
			logger.error(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		if (isErrorEnabled()) {
			FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
			logger.error(tp.getMessage(), tp.getThrowable());
		}
	}

	@Override
	public void error(String format, Object... argArray) {
		if (isErrorEnabled()) {
			FormattingTuple tp = MessageFormatter.arrayFormat(format, argArray);
			logger.error(tp.getMessage(), tp.getThrowable());
		}
	}

}