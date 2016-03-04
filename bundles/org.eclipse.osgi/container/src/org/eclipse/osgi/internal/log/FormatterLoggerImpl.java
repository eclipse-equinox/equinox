/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.admin.LoggerContext;

public class FormatterLoggerImpl extends LoggerImpl implements FormatterLogger {
	public FormatterLoggerImpl(ExtendedLogServiceImpl logServiceImpl, String name, LoggerContext loggerContext) {
		super(logServiceImpl, name, loggerContext);
	}

	@Override
	String formatMessage(String format, Arguments processedArguments) {
		return String.format(format, processedArguments.arguments());
	}

}
