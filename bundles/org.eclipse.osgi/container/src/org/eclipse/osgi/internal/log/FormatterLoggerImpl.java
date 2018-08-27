/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
