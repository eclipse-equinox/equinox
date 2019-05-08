/*******************************************************************************
 * Copyright (c) 2006, 2018 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.equinox.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;
import org.osgi.service.log.LoggerConsumer;
import org.osgi.service.log.admin.LoggerContext;

public class LoggerImpl implements Logger {
	static final String THIS_PACKAGE_NAME = LoggerImpl.class.getName().substring(0, LoggerImpl.class.getName().length() - LoggerImpl.class.getSimpleName().length());
	static final Object[] EMPTY = new Object[0];
	protected final ExtendedLogServiceImpl logServiceImpl;
	protected final String name;

	private LogLevel enabledLevel = LogLevel.TRACE;

	public LoggerImpl(ExtendedLogServiceImpl logServiceImpl, String name, LoggerContext loggerContext) {
		this.logServiceImpl = logServiceImpl;
		this.name = name;
		applyLoggerContext(loggerContext);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isLoggable(int level) {
		return logServiceImpl.isLoggable(name, level);
	}

	@Override
	public void log(int level, String message) {
		log(null, level, message, null);
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void log(ServiceReference sr, int level, String message) {
		log(sr, null, level, message, sr, null);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		log(sr, null, level, message, sr, exception);
	}

	@Override
	public void log(Object context, int level, String message) {
		log(context, level, message, null);
	}

	@Override
	public void log(Object context, int level, String message, Throwable exception) {
		log(context, null, level, message, null, exception);
	}

	private void log(Object context, LogLevel logLevelEnum, int level, String message, ServiceReference<?> ref, Throwable exception) {
		log(logServiceImpl.getBundle(), context, logLevelEnum, level, message, ref, exception);
	}

	void log(Bundle entryBundle, Object context, LogLevel logLevelEnum, int level, String message, ServiceReference<?> ref, Throwable exception) {
		if (logLevelEnum == null) {
			logLevelEnum = getLogLevel(level);
		}
		if (enabledLevel.implies(logLevelEnum)) {
			logServiceImpl.getFactory().log(entryBundle, name, getLocation(), context, logLevelEnum, level, message, ref, exception);
		}
	}

	@SuppressWarnings("deprecation")
	private LogLevel getLogLevel(int level) {
		switch (level) {
			case LogService.LOG_DEBUG :
				return LogLevel.DEBUG;
			case LogService.LOG_ERROR :
				return LogLevel.ERROR;
			case LogService.LOG_INFO :
				return LogLevel.INFO;
			case LogService.LOG_WARNING :
				return LogLevel.WARN;
			default :
				return LogLevel.TRACE;
		}
	}

	@Override
	public boolean isTraceEnabled() {
		return enabledLevel.implies(LogLevel.TRACE);
	}

	@Override
	public void trace(String message) {
		trace(message, EMPTY);
	}

	@Override
	public void trace(String format, Object arg) {
		trace(format, new Object[] {arg});
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		trace(format, new Object[] {arg1, arg2});
	}

	@Override
	public void trace(String format, Object... arguments) {
		log(LogLevel.TRACE, format, arguments);
	}

	@Override
	public boolean isDebugEnabled() {
		return enabledLevel.implies(LogLevel.DEBUG);
	}

	@Override
	public void debug(String message) {
		debug(message, EMPTY);
	}

	@Override
	public void debug(String format, Object arg) {
		debug(format, new Object[] {arg});
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		debug(format, new Object[] {arg1, arg2});
	}

	@Override
	public void debug(String format, Object... arguments) {
		log(LogLevel.DEBUG, format, arguments);
	}

	@Override
	public boolean isInfoEnabled() {
		return enabledLevel.implies(LogLevel.INFO);
	}

	@Override
	public void info(String message) {
		info(message, EMPTY);
	}

	@Override
	public void info(String format, Object arg) {
		info(format, new Object[] {arg});
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		info(format, new Object[] {arg1, arg2});
	}

	@Override
	public void info(String format, Object... arguments) {
		log(LogLevel.INFO, format, arguments);
	}

	@Override
	public boolean isWarnEnabled() {
		return enabledLevel.implies(LogLevel.WARN);
	}

	@Override
	public void warn(String message) {
		warn(message, EMPTY);
	}

	@Override
	public void warn(String format, Object arg) {
		warn(format, new Object[] {arg});
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		warn(format, new Object[] {arg1, arg2});
	}

	@Override
	public void warn(String format, Object... arguments) {
		log(LogLevel.WARN, format, arguments);
	}

	@Override
	public boolean isErrorEnabled() {
		return enabledLevel.implies(LogLevel.ERROR);
	}

	@Override
	public void error(String message) {
		error(message, EMPTY);
	}

	@Override
	public void error(String format, Object arg) {
		error(format, new Object[] {arg});
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		error(format, new Object[] {arg1, arg2});
	}

	@Override
	public void error(String format, Object... arguments) {
		log(LogLevel.ERROR, format, arguments);
	}

	@Override
	public void audit(String message) {
		audit(message, EMPTY);
	}

	@Override
	public void audit(String format, Object arg) {
		audit(format, new Object[] {arg});
	}

	@Override
	public void audit(String format, Object arg1, Object arg2) {
		audit(format, new Object[] {arg1, arg2});
	}

	@Override
	public void audit(String format, Object... arguments) {
		log(LogLevel.AUDIT, format, arguments);
	}

	@Override
	public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
		if (isTraceEnabled()) {
			consumer.accept(this);
		}
	}

	@Override
	public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
		if (isDebugEnabled()) {
			consumer.accept(this);
		}
	}

	@Override
	public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
		if (isInfoEnabled()) {
			consumer.accept(this);
		}
	}

	@Override
	public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
		if (isWarnEnabled()) {
			consumer.accept(this);
		}
	}

	@Override
	public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
		if (isErrorEnabled()) {
			consumer.accept(this);
		}
	}

	private static final Pattern pattern = Pattern.compile("(\\\\?)(\\\\?)(\\{\\})"); //$NON-NLS-1$

	private void log(LogLevel level, String format, Object... arguments) {
		if (!enabledLevel.implies(level)) {
			return;
		}
		StackTraceElement location = getLocation();
		Arguments processedArguments = new Arguments(arguments);
		String message = processedArguments.isEmpty() ? format : formatMessage(format, processedArguments);
		logServiceImpl.getFactory().log(logServiceImpl.getBundle(), name, location, processedArguments.serviceReference(), level, level.ordinal(), message.toString(), processedArguments.serviceReference(), processedArguments.throwable());
	}

	private StackTraceElement getLocation() {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		if (elements.length == 0) {
			return null;
		}
		for (int i = 1; i < elements.length; i++) {
			if (!elements[i].getClassName().startsWith(THIS_PACKAGE_NAME)) {
				return elements[i];
			}
		}
		return elements[1];
	}

	String formatMessage(String format, Arguments processedArguments) {
		Matcher matcher = pattern.matcher(format);
		char[] chars = format.toCharArray();
		int offset = 0;
		StringBuilder message = new StringBuilder(format.length() * 2);
		for (Object argument : processedArguments.arguments()) {
			// By design, the message will continue to be formatted for as long as arguments remain.
			// Once all arguments have been processed, formatting stops. This means some escape characters
			// and place holders may remain unconsumed. This matches SLF4J behavior.
			while (matcher.find()) {
				if (matcher.group(2).isEmpty()) {
					if (matcher.group(1).isEmpty()) {
						// {} Handle curly brackets as normal.
						offset = append(message, matcher, chars, offset, matcher.start(3) - offset, argument);
						break;
					}
					// \{} Ignore curly brackets. Consume backslash.
					offset = append(message, matcher, chars, offset, matcher.start(3) - offset - 1, matcher.group(3));
				} else {
					// \\{} Handle curly brackets as normal. Consume one backslash.
					offset = append(message, matcher, chars, offset, matcher.start(3) - offset - 1, argument);
					break;
				}
			}
		}
		message.append(chars, offset, chars.length - offset);
		return message.toString();
	}

	private static int append(StringBuilder builder, Matcher matcher, char[] chars, int offset, int length, Object argument) {
		builder.append(chars, offset, length);
		builder.append(argument);
		return matcher.end(3);
	}

	void applyLoggerContext(LoggerContext loggerContext) {
		enabledLevel = loggerContext == null ? LogLevel.WARN : loggerContext.getEffectiveLogLevel(name);
	}
}
