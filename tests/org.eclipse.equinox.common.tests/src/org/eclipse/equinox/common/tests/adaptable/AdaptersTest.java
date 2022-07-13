/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.tests.adaptable;

import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.eclipse.core.runtime.Adapters;
import org.junit.Test;

public class AdaptersTest {

	@Test
	public void testOptionalObjectIsNull() {
		Optional<?> optional = Adapters.of(null, Object.class);
		assertTrue(optional.isEmpty());
	}

	@Test(expected = NullPointerException.class)
	public void testOptionalAdapterTypeIsNull() {
		Adapters.of(new Object(), null);
	}

	@Test
	public void testOptionalOfNotAdaptableIsEmpty() {
		Optional<?> optional = Adapters.of(new ThisWillNotAdapt(), Runnable.class);
		assertTrue(optional.isEmpty());
	}

	@Test
	public void testOptionalOfAdaptable() {
		Optional<?> optional = Adapters.of(new ThisWillAdaptToRunnable(), Runnable.class);
		assertTrue(optional.isPresent());
	}

	private static final class ThisWillNotAdapt {

	}

	private static final class ThisWillAdaptToRunnable implements Runnable {

		@Override
		public void run() {

		}

	}

}
