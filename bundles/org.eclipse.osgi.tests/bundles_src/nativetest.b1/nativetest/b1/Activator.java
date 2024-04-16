/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package nativetest.b1;

import java.lang.reflect.Method;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public Method findDeclaredMethod(Class<?> clazz, String method, Class... args) throws NoSuchMethodException {
		do {
			try {
				return clazz.getDeclaredMethod(method, args);
			} catch (NoSuchMethodException e) {
				clazz = clazz.getSuperclass();
			}
		} while (clazz != null);
		throw new NoSuchMethodException(method);
	}

	public void start(BundleContext context) throws Exception {
		Method findLibrary = findDeclaredMethod(this.getClass().getClassLoader().getClass(), "findLibrary",
				String.class);
		findLibrary.setAccessible(true);
		AbstractBundleTests.simpleResults
				.addEvent(findLibrary.invoke(this.getClass().getClassLoader(), new Object[] { "nativefile.txt" }));
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
