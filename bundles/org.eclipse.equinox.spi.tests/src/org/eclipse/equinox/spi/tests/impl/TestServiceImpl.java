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
import java.net.URL;

import org.eclipse.equinox.spi.tests.service.TestService;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class TestServiceImpl implements TestService {

	@Override
	public String getValue() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		URL entry = bundle.getEntry("/value.txt");
		try (var resource = getClass().getClassLoader().getResource("/value.txt").openStream()) {
			return new String(resource.readAllBytes()).trim();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
