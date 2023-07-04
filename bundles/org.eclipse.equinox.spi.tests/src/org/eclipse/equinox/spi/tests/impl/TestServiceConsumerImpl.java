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

import java.util.ServiceLoader;

import org.eclipse.equinox.spi.tests.service.TestService;
import org.eclipse.equinox.spi.tests.service.TestServiceConsumer;
import org.osgi.service.component.annotations.Component;

@Component
public class TestServiceConsumerImpl implements TestServiceConsumer {

	@Override
	public boolean findFirst() {
		ServiceLoader<TestService> serviceLoader = ServiceLoader.load(TestService.class);
		return serviceLoader.findFirst().isPresent();
	}

	@Override
	public Object iterator() {
		ServiceLoader<TestService> serviceLoader = ServiceLoader.load(TestService.class);
		return serviceLoader.iterator();
	}
}
