/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Christoph Laeubrich - Bug 567344
 *******************************************************************************/
package org.eclipse.equinox.common.tests.adaptable;

import java.util.function.Supplier;

import org.eclipse.core.runtime.IAdapterFactory;
import org.junit.Assert;

public class TestAdapterFactory2 extends Assert implements IAdapterFactory {

	private final Supplier<TestAdapter2> supplier;

	public TestAdapterFactory2(Supplier<TestAdapter2> supplier) {
		this.supplier = supplier;
	}

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		assertTrue("Request for wrong adapter", adaptableObject instanceof TestAdaptable2);
		return adapterType.cast(supplier.get());
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { TestAdapter2.class };
	}
}
