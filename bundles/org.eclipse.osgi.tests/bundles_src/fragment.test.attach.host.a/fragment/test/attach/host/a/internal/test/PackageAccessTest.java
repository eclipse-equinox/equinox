/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
