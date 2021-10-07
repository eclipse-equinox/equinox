/*******************************************************************************
 *  Copyright (c) 2021 Red Hat, Inc. and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.common.tests.adaptable;

import org.eclipse.core.runtime.IAdapterFactory;

public class NullAdapter2Factory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { TestAdaptable.class };
	}

}
