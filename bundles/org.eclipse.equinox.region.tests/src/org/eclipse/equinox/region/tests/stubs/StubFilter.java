/*******************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.region.tests.stubs;

import java.util.Dictionary;
import java.util.Map;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * A simple stub implementation of Filter for testing purposes. This
 * implementation always returns true for matches.
 */
public class StubFilter implements Filter {
	private final String filterString;

	public StubFilter(String filterString) {
		this.filterString = filterString;
	}

	@Override
	public boolean match(ServiceReference<?> reference) {
		return true;
	}

	@Override
	public boolean match(Dictionary<String, ?> dictionary) {
		return true;
	}

	@Override
	public boolean matchCase(Dictionary<String, ?> dictionary) {
		return true;
	}

	@Override
	public boolean matches(Map<String, ?> map) {
		return true;
	}

	@Override
	public String toString() {
		return filterString;
	}
}
