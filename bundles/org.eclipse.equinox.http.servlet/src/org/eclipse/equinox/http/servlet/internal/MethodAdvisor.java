/*******************************************************************************
 * Copyright (c) 2011, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

class MethodAdvisor extends Object {
	private final Class subject;
	private final Set methodsCache;
	private final boolean methodCacheEnabled;

	// Property to turn off method caching.
	private static final String DISABLE_METHOD_CACHE = "org.eclipse.equinox.http.servlet.internal.disable.method.cache"; //$NON-NLS-1$

	private static boolean isMethodCacheEnabled() {
		return Boolean.getBoolean(MethodAdvisor.DISABLE_METHOD_CACHE) == false;
	}

	MethodAdvisor(Class subject) {
		super();
		if (subject == null)
			throw new IllegalArgumentException("subject must not be null"); //$NON-NLS-1$
		if (subject.isInterface())
			throw new IllegalArgumentException("subject must not be an interface"); //$NON-NLS-1$
		this.subject = subject;
		this.methodsCache = new HashSet(17);
		this.methodCacheEnabled = MethodAdvisor.isMethodCacheEnabled();
	}

	private boolean equals(Method left, Method right) {
		boolean match;
		match = hasEqualMethodNames(left, right);
		if (match == false)
			return false;
		match = hasEqualReturnTypes(left, right);
		if (match == false)
			return false;
		match = hasEqualParameterTypes(left, right);
		if (match == false)
			return false;
		return true;
	}

	private boolean hasEqualMethodNames(Method left, Method right) {
		String leftName = left.getName();
		String rightName = right.getName();
		boolean equal = leftName.equals(rightName);
		return equal;
	}

	private boolean hasEqualParameterTypes(Method left, Method right) {
		Class[] leftParameterTypes = left.getParameterTypes();
		Class[] rightMethodParameterTypes = right.getParameterTypes();
		boolean equal = leftParameterTypes.length == rightMethodParameterTypes.length;
		int i = 0;
		int count = leftParameterTypes.length - 1;
		while (equal && i <= count) {
			Class leftClass = leftParameterTypes[i];
			Class rightClass = rightMethodParameterTypes[i];
			equal = leftClass.equals(rightClass);
			i++;
		}
		return equal;
	}

	private boolean hasEqualReturnTypes(Method left, Method right) {
		Class leftClass = left.getReturnType();
		Class rightClass = right.getReturnType();
		boolean equal = leftClass.equals(rightClass);
		return equal;
	}

	private boolean hasValidModifiers(Method declaredMethod) {
		int modifiers = declaredMethod.getModifiers();
		boolean valid;
		valid = Modifier.isPublic(modifiers);
		if (valid == false)
			return false;
		valid = Modifier.isAbstract(modifiers) == false;
		if (valid == false)
			return false;
		return true;
	}

	private boolean isImplemented(Class clazz, Method method) {
		if (clazz == null)
			return false;
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (int i = 0; i < declaredMethods.length; i++) {
			Method declaredMethod = declaredMethods[i];
			boolean valid = hasValidModifiers(declaredMethod);
			if (valid == false)
				continue;
			boolean match = equals(method, declaredMethod);
			if (match == false)
				continue;
			methodsCache.add(method);
			return true; // Implemented and added to cache.
		}
		Class parent = clazz.getSuperclass();
		return isImplemented(parent, method);
	}

	boolean isImplemented(Method method) {
		if (method == null)
			throw new IllegalArgumentException("method must not be null"); //$NON-NLS-1$
		synchronized (methodsCache) {
			if (methodCacheEnabled) {
				boolean exists = methodsCache.contains(method);
				if (exists)
					return true; // Implemented and exists in cache.
			}
			return isImplemented(subject, method);
		}
	}
}
