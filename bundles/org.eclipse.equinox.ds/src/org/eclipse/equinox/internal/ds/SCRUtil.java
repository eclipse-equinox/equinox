/*******************************************************************************
 * Copyright (c) 1997, 2010 by ProSyst Software GmbH
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
import org.apache.felix.scr.Component;
import org.eclipse.equinox.internal.util.pool.ObjectCreator;
import org.eclipse.equinox.internal.util.pool.ObjectPool;

/**
 * Holds useful methods used by SCR.
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
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
		//
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
	 * @param currentClass the class where the method is located. This class is in the component implementation class hierarchy
	 * @param methodToCheck the method to be checked
	 * @param isComponent11 specifies whether the component is according to schema 1.1 or higher. Its value is true in case the component version is v1.1 or higher
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
				setAccessibleMethod = Class.forName("java.lang.reflect.AccessibleObject").getMethod("setAccessible", new Class[] {boolean.class}); //$NON-NLS-1$ //$NON-NLS-2$
				args = new Object[] {Boolean.TRUE};
			}
			if (setAccessibleMethod != null)
				setAccessibleMethod.invoke(method, args);
		} catch (Exception e) {
			failed = true;
		}
	}

	/**
	 * Gets the string representation of an <code>Object</code>. This method is
	 * helpful if the passed parameter is a type of array. If the objects is not
	 * recognized as array, the method will simply return the toString() value
	 * 
	 * @param value
	 *          an object which should be represented as string
	 * @return the string representation of the
	 */
	public static String getStringRepresentation(Object value) {
		if (value == null)
			return "null"; //$NON-NLS-1$
		//this would speedup many cases
		if (value instanceof String)
			return (String) value;

		StringBuffer res = new StringBuffer(200);

		if (value instanceof String[]) {
			res.append("String["); //$NON-NLS-1$
			String[] arr = (String[]) value;
			for (int i = 0; i < arr.length; i++) {
				res.append(arr[i]);
				if (i != arr.length - 1) {
					res.append(","); //$NON-NLS-1$
				}
			}
			res.append("]"); //$NON-NLS-1$
		} else if (value instanceof int[]) {
			res.append("int["); //$NON-NLS-1$
			int[] arr = (int[]) value;
			for (int i = 0; i < arr.length; i++) {
				res.append(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					res.append(","); //$NON-NLS-1$
				}
			}
			res.append("]"); //$NON-NLS-1$
		} else if (value instanceof long[]) {
			res.append("long["); //$NON-NLS-1$
			long[] arr = (long[]) value;
			for (int i = 0; i < arr.length; i++) {
				res.append(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					res.append(","); //$NON-NLS-1$
				}
			}
			res.append("]"); //$NON-NLS-1$
		} else if (value instanceof char[]) {
			res.append("char["); //$NON-NLS-1$
			char[] arr = (char[]) value;
			for (int i = 0; i < arr.length; i++) {
				res.append(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					res.append(","); //$NON-NLS-1$
				}
			}
			res.append("]"); //$NON-NLS-1$
		} else if (value instanceof boolean[]) {
			res.append("boolean["); //$NON-NLS-1$
			boolean[] arr = (boolean[]) value;
			for (int i = 0; i < arr.length; i++) {
				res.append(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					res.append(","); //$NON-NLS-1$
				}
			}
			res.append("]"); //$NON-NLS-1$
		} else if (value instanceof double[]) {
			res.append("double["); //$NON-NLS-1$
			double[] arr = (double[]) value;
			for (int i = 0; i < arr.length; i++) {
				res.append(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					res.append(","); //$NON-NLS-1$
				}
			}
			res.append("]"); //$NON-NLS-1$
		} else if (value instanceof float[]) {
			res.append("float["); //$NON-NLS-1$
			float[] arr = (float[]) value;
			for (int i = 0; i < arr.length; i++) {
				res.append(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					res.append(","); //$NON-NLS-1$
				}
			}
			res.append("]"); //$NON-NLS-1$
		} else if (value instanceof Object[]) {
			res.append("Object["); //$NON-NLS-1$
			Object[] arr = (Object[]) value;
			for (int i = 0; i < arr.length; i++) {
				res.append(getStringRepresentation(arr[i]));
				if (i != arr.length - 1) {
					res.append(","); //$NON-NLS-1$
				}
			}
			res.append("]"); //$NON-NLS-1$
		} else {
			return value.toString();
		}
		return res.toString();
	}

	/**
	 * Gets the string presentation of a state defined by the interface org.apache.felix.scr.Component
	 * @param state the specified state
	 * @return the string representation of the specified state
	 */
	public static String getStateStringRepresentation(int state) {
		String result = "Unknown"; //$NON-NLS-1$
		switch (state) {
			case Component.STATE_ACTIVATING :
				result = "Activating"; //$NON-NLS-1$
				break;
			case Component.STATE_ACTIVE :
				result = "Active"; //$NON-NLS-1$
				break;
			case Component.STATE_DEACTIVATING :
				result = "Deactivating"; //$NON-NLS-1$
				break;
			case Component.STATE_DISABLED :
				result = "Disabled"; //$NON-NLS-1$
				break;
			case Component.STATE_DISPOSED :
				result = "Disposed"; //$NON-NLS-1$
				break;
			case Component.STATE_DISPOSING :
				result = "Disposing"; //$NON-NLS-1$
				break;
			case Component.STATE_ENABLING :
				result = "Enabling"; //$NON-NLS-1$
				break;
			case Component.STATE_FACTORY :
				result = "Factory"; //$NON-NLS-1$
				break;
			case Component.STATE_REGISTERED :
				result = "Registered"; //$NON-NLS-1$
				break;
			case Component.STATE_UNSATISFIED :
				result = "Unsatisfied"; //$NON-NLS-1$
				break;

		}
		return result;
	}
}
