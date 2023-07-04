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

import java.io.IOException;

import org.eclipse.equinox.spi.tests.service.TestService;

/**
 * A second, self-contained {@code TestService} provider implementation,
 * distinct from {@link TestServiceImpl}. Needed whenever a test installs two
 * provider bundles for the same service type at once: a consumer's class
 * loader caches classes by name, so two providers advertising the very same
 * class name would collapse into a single, already-loaded {@code Class} the
 * second time around. Kept independent of {@link TestServiceImpl} (rather
 * than subclassing it) so its bundle doesn't need cross-bundle access to the
 * other provider's class.
 */
public class TestServiceImpl2 implements TestService {

	@Override
	public String getValue() {
		try (var resource = getClass().getClassLoader().getResource("/value.txt").openStream()) {
			return new String(resource.readAllBytes()).trim();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
