/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.loader.buddy;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;

public class SystemPolicy implements IBuddyPolicy {

	private static class ParentClassLoader extends ClassLoader {
		protected ParentClassLoader() {
			super(Object.class.getClassLoader());
		}
	}

	public static final byte BOOT = 0;
	public static final byte EXT = 1;
	public static final byte APP = 2;

	private static SystemPolicy[] instances = new SystemPolicy[3];

	private ClassLoader classLoader;

	public static SystemPolicy getInstance(final byte type) {
		if (instances[type] == null) {
			instances[type] = new SystemPolicy();
			instances[type].classLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
				public ClassLoader run() {
					return createClassLoader(type);
				}
			});
		}
		return instances[type];
	}

	public SystemPolicy() {
		//Nothing to do
	}

	public SystemPolicy(ClassLoader parent) {
		classLoader = parent;
	}

	static ClassLoader createClassLoader(byte type) {
		switch (type) {
			case APP :
				if (ClassLoader.getSystemClassLoader() != null)
					return ClassLoader.getSystemClassLoader();
				return new ParentClassLoader();

			case BOOT :
				return new ParentClassLoader();

			case EXT :
				if (ClassLoader.getSystemClassLoader() != null)
					return ClassLoader.getSystemClassLoader().getParent();
				return new ParentClassLoader();
		}
		return null;
	}

	public Class<?> loadClass(String name) {
		try {
			return classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public URL loadResource(String name) {
		return classLoader.getResource(name);
	}

	public Enumeration<URL> loadResources(String name) {
		try {
			return classLoader.getResources(name);
		} catch (IOException e) {
			return null;
		}
	}

}
