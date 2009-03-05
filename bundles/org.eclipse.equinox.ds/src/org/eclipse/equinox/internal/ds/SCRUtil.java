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
import java.lang.reflect.Modifier;
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

	/**
	 * Checks whether the method can be accessed according to the DS v1.1 specification rules (112.8.4 Locating Component Methods)
	 * @param implClass the component implementation class
	 * @param currentClass the class where the method is located. This class is in the component imlpementation class hierarchy
	 * @param methodToCheck the method to be checked
	 * @param isComponent11 specifies whether the component is according to schema 1.1 or 1.0. Its value is true in case it is a v1.1 component
	 * @return true, if the method can be executed
	 */
	public static boolean checkMethodAccess(Class implClass, Class currentClass, Method methodToCheck, boolean isComponent11) {
		int modifiers = methodToCheck.getModifiers();
		boolean result = true;
		if (isComponent11) {
			if (currentClass == implClass) {
				//the method is located in the component impl class
				//allow all types of modifiers
			} else {
				//the method is located in a super class of the component impl class
				if (Modifier.isPrivate(modifiers)) {
					// private method - no access
					result = false;
				} else if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)) {
					// not protected neither public neither private - this is private package case
					if (currentClass.getPackage() != implClass.getPackage()) {
						//do not accept the method if its class package differs the package of the component impl class 
						result = false;
					}
				}
			}
		} else {
			result = Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
		}
		return result;
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
