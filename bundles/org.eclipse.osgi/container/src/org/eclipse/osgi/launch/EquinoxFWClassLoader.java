/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.launch;

import java.net.URL;
import java.net.URLClassLoader;

class EquinoxFWClassLoader extends URLClassLoader {

	private static final String[] DELEGATE_PARENT_FIRST = {"java.", "org.osgi.", "org.eclipse.osgi.launch.", "org.eclipse.osgi.service.", "org.eclipse.osgi.framework.log", "org.eclipse.osgi.framework.adaptor", "org.eclipse.osgi.framework.internal.core.ReferenceInputStream"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	private static final String[] DELEGATE_CHILD_FIRST = new String[0]; // nothing right now is skipped

	private final ClassLoader parent;

	public EquinoxFWClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		this.parent = parent;
	}

	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> clazz = findLoadedClass(name);
		if (clazz != null)
			return clazz;

		boolean childFirst = childFirst(name);
		ClassNotFoundException cnfe = null;

		if (childFirst)
			try {
				clazz = findClass(name);
			} catch (ClassNotFoundException e) {
				// continue
				cnfe = e;
			}

		if (clazz == null)
			try {
				clazz = parent.loadClass(name);
			} catch (ClassNotFoundException e) {
				// continue
			}

		if (clazz == null && cnfe != null)
			throw cnfe;
		if (clazz == null && !childFirst)
			clazz = findClass(name);

		if (resolve)
			resolveClass(clazz);
		return clazz;
	}

	private boolean childFirst(String name) {
		for (int i = DELEGATE_CHILD_FIRST.length - 1; i >= 0; i--)
			if (name.startsWith(DELEGATE_CHILD_FIRST[i]))
				return true;
		for (int i = DELEGATE_PARENT_FIRST.length - 1; i >= 0; i--)
			if (name.startsWith(DELEGATE_PARENT_FIRST[i]))
				return false;
		return true;

	}
}
