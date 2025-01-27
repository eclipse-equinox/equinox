/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package test.bug375784;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		final ClassLoader parent = getClass().getClassLoader();
		// create a custom class loader that uses a bundle's class loader as the parent
		ClassLoader testCL = new ClassLoader(parent) {

			@Override
			protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				if ("test.bug375784.Test".equals(name)) {
					URL classURL = this.getClass().getResource("Test.class");
					if (classURL == null) {
						throw new ClassNotFoundException(name);
					}
					byte[] bytes;
					try {
						bytes = getBytes(classURL);
						return defineClass(name, bytes, 0, bytes.length);
					} catch (IOException e) {
						throw new ClassNotFoundException(name, e);
					}
				}
				return super.loadClass(name, resolve);
			}

		};

		// Load a class that expects to be able to have free access to SaxParserFactory
		Class clazz;
		try {
			clazz = testCL.loadClass("test.bug375784.Test");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		try {
			clazz.getDeclaredConstructor().newInstance();
			throw new RuntimeException("Should have failed to create object from class: " + clazz);
		} catch (InvocationTargetException e) {
			if (!(e.getCause() instanceof NoClassDefFoundError)) {
				throw e;
			}
		} catch (NoClassDefFoundError e) {
			// expected
		}
	}

	public static byte[] getBytes(URL url) throws IOException {
		InputStream in = url.openStream();
		byte[] classbytes;
		int bytesread = 0;
		int readcount;
		try {
			int length = 1024;
			classbytes = new byte[length];
			readloop: while (true) {
				for (; bytesread < length; bytesread += readcount) {
					readcount = in.read(classbytes, bytesread, length - bytesread);
					if (readcount <= 0) /* if we didn't read anything */
						break readloop; /* leave the loop */
				}
				byte[] oldbytes = classbytes;
				length += 1024;
				classbytes = new byte[length];
				System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
			}
			if (classbytes.length > bytesread) {
				byte[] oldbytes = classbytes;
				classbytes = new byte[bytesread];
				System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
			}
		} finally {
			try {
				in.close();
			} catch (IOException ee) {
				// nothing to do here
			}
		}
		return classbytes;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// Nothing
	}

}
