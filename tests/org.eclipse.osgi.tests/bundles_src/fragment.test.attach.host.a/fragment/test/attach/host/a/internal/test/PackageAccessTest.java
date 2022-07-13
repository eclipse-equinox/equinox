/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package fragment.test.attach.host.a.internal.test;

import org.eclipse.osgi.tests.bundles.AbstractBundleTests;

public class PackageAccessTest {
	void packageLevelAccess(String className) {
		AbstractBundleTests.simpleResults.addEvent(className);
	}
}
