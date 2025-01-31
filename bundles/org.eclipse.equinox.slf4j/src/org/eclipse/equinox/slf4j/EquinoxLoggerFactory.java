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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.internal.runtime.PlatformLogWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;
import org.slf4j.ILoggerFactory;

/**
 * An implementation of the SLF4J {@link ILoggerFactory} that is using the
 * equinox platform log like done with the ILog interface
 */
public class EquinoxLoggerFactory implements org.slf4j.ILoggerFactory {

	private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
	private static final Bundle FACTORY_BUNDLE = FrameworkUtil.getBundle(EquinoxLoggerFactory.class);
	private static final Bundle SLF4J_BUNDLE = FrameworkUtil.getBundle(org.slf4j.ILoggerFactory.class);
	static final ConcurrentMap<Bundle, EquinoxLogger> LOGGER_MAP = new ConcurrentHashMap<>();
	static final AtomicReference<LogService> logService = new AtomicReference<>();

	@Override
	public org.slf4j.Logger getLogger(String name) {
		Bundle bundle = getBundle();
		if (bundle == null) {
			return new EquinoxLogger(name, null);
		}
		return LOGGER_MAP.computeIfAbsent(bundle, b -> {
			LogService service = logService.get();
			@SuppressWarnings("restriction")
			Logger bundleLogger = service == null ? null
					: service.getLogger(b, PlatformLogWriter.EQUINOX_LOGGER_NAME, Logger.class);
			return new EquinoxLogger(name, bundleLogger);
		});
	}

	private Bundle getBundle() {
		return STACK_WALKER.walk(s -> s.map(frame -> FrameworkUtil.getBundle(frame.getDeclaringClass()))
				.dropWhile(stackFrameBundle -> stackFrameBundle == FACTORY_BUNDLE || stackFrameBundle == SLF4J_BUNDLE)
				.findFirst().orElse(null));
	}

}
