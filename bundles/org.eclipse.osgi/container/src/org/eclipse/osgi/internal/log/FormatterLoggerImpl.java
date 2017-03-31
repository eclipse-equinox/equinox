package org.eclipse.osgi.internal.log;

import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.LogLevel;

public class FormatterLoggerImpl extends LoggerImpl implements FormatterLogger {
	public FormatterLoggerImpl(ExtendedLogServiceImpl logServiceImpl, String name) {
		super(logServiceImpl, name);
	}

	@Override
	protected void log(LogLevel level, String format, Object... arguments) {
		Arguments processedArguments = new Arguments(arguments);
		String message = String.format(format, processedArguments.arguments());
		logServiceImpl.log(name, processedArguments.serviceReference(), level.ordinal(), message, processedArguments.throwable());
	}
}
