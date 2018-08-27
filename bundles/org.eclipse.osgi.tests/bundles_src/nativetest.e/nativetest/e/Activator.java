/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package nativetest.e;

import java.lang.reflect.Method;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		Method findLibrary = ClassLoader.class.getDeclaredMethod("findLibrary", new Class[] {String.class});
		findLibrary.setAccessible(true);
		AbstractBundleTests.simpleResults.addEvent(findLibrary.invoke(this.getClass().getClassLoader(), new Object[] {"nativecode.txt"}));
	}

	public void stop(BundleContext context) throws Exception {
		// nothing
	}

}
