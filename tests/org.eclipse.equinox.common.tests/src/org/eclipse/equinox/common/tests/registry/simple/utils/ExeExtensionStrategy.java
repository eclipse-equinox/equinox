/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.equinox.common.tests.registry.simple.utils;

import java.io.File;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.core.runtime.spi.RegistryStrategy;

/**
 * Registry strategy that uses class loader from this bundle to process executable
 * extensions.
 * @since 3.2
 */
public class ExeExtensionStrategy extends RegistryStrategy {

	public ExeExtensionStrategy(File[] theStorageDir, boolean[] cacheReadOnly) {
		super(theStorageDir, cacheReadOnly);
	}

	@Override
	public Object createExecutableExtension(RegistryContributor defaultContributor, String className, String requestedContributorName) {
		Class<?> classInstance = null;
		try {
			classInstance = Class.forName(className);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}

		// create a new instance
		Object result = null;
		try {
			result = classInstance.getDeclaredConstructor().newInstance();
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}

		return result;
	}
}
