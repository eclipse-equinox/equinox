/*******************************************************************************
 * Copyright (c) 2026, 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.spi.tests.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import org.eclipse.equinox.spi.tests.service.TestService;
import org.eclipse.equinox.spi.tests.service.TestServiceConsumer;
import org.osgi.service.component.annotations.Component;

@Component
public class TestServiceConsumerImpl implements TestServiceConsumer {

	@Override
	public Optional<TestService> findFirst() {
		ServiceLoader<TestService> serviceLoader = ServiceLoader.load(TestService.class);
		return serviceLoader.findFirst();
	}

	@Override
	public Iterator<TestService> iterator() {
		ServiceLoader<TestService> serviceLoader = ServiceLoader.load(TestService.class);
		// Drain the (lazy) ServiceLoader iterator right here, rather than returning
		// it directly: java.util.ServiceLoader's internal provider discovery relies
		// on inspecting the calling stack (Reflection.getCallerClass()-style checks),
		// which only works correctly while still inside this method's call frame.
		List<TestService> services = serviceLoader.stream().map(Provider::get).toList();
		return services.iterator();
	}
}
