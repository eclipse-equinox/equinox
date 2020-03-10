/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.osgi.tests.hooks.framework.storage.a;

import java.util.Optional;
import org.osgi.framework.Bundle;
import org.osgi.framework.connect.FrameworkUtilHelper;

public class TestHelper implements FrameworkUtilHelper {
	volatile static Bundle testBundle;

	@Override
	public Optional<Bundle> getBundle(Class<?> classFromBundle) {
		return Optional.ofNullable(testBundle);
	}

	public static void setBundle(Bundle testBundle) {
		TestHelper.testBundle = testBundle;
	}
}
