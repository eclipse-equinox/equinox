/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Enumeration;

import org.eclipse.equinox.internal.util.pool.ObjectCreator;
import org.eclipse.equinox.internal.util.pool.ObjectPool;

/**
 * Util.java
 * 
 * @author Valentin Valchev
 * @author Pavlin Dobrev
 * @version 1.0
 */

public final class SCRUtil implements ObjectCreator {

	private static ObjectPool objectArrayPool;

	static {
		SCRUtil u = new SCRUtil();
		// FIXME: use some kind of logging for the object pool to determine
		// the optimal solution!
		objectArrayPool = new ObjectPool(u, 10, 2);
	}

	private SCRUtil() {
	}

	public static Object[] getObjectArray() {
		return (Object[]) objectArrayPool.getObject();
	}

	public static void release(Object[] objectArray) {
		for (int j = 0; j < objectArray.length; j++) {
			objectArray[j] = null;
		}
		objectArrayPool.releaseObject(objectArray);
	}

	public Object getInstance() throws Exception {
		return new Object[1];
	}

	public static void copyTo(Dictionary dst, Dictionary src) {
		if (src == null || dst == null) {
			return;
		}
		Object key;
		for (Enumeration e = src.keys(); e.hasMoreElements();) {
			key = e.nextElement();
			dst.put(key, src.get(key));
		}
	}

	private static Method setAccessibleMethod = null;
	private static Object[] args = null;
	private static boolean failed = false;

	/**
	 * This method is added only for JVM compatibility. Actually setAccessible()
	 * is available since jdk1.2. The older java runtimes don't have this
	 * method.
	 * 
	 * However, you can call this method. It is guaranteed that it will do the
	 * right job.
	 * 
	 * @param method
	 *            the method to set accessible.
	 */
	public static final void setAccessible(Method method) {
		try {
			if (setAccessibleMethod == null && !failed) {
				setAccessibleMethod = Class.forName("java.lang.reflect.AccessibleObject").getMethod("setAccessible", new Class[] {boolean.class});
				args = new Object[] {Boolean.TRUE};
			}
			if (setAccessibleMethod != null)
				setAccessibleMethod.invoke(method, args);
		} catch (Exception e) {
			failed = true;
		}
	}
}
