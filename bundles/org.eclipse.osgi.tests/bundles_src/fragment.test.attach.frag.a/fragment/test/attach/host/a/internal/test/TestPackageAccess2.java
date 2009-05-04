/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fragment.test.attach.host.a.internal.test;

import org.eclipse.osgi.tests.bundles.ITestRunner;

public class TestPackageAccess2 implements ITestRunner {

	public Object testIt() throws Exception {
		new PackageAccessTest2().packageLevelAccess(TestPackageAccess2.class.getName());
		return null;
	}
}
