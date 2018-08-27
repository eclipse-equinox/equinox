/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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

package org.eclipse.osgi.internal.serviceregistry;

import org.osgi.framework.ServiceRegistration;

/**
 * A callable hook that contains the context for call a collection of hooks.
 * This is effectively a "closure" for calling each hook. The hook context
 * must know the type of the hook object, the method to call on the hook
 * as well as all the parameters which need to be passed to the hook method.
 *
 */
public interface HookContext {

	/**
	 * Call the specified hook.
	 * 
	 * @param hook The hook object to call. The hook object must be of the type
	 * supported by this hook context. If it is not, then this method will
	 * simply return.
	 * @param hookRegistration the registration for the hook object
	 * @throws Exception An exception thrown by the hook object.
	 */
	public void call(Object hook, ServiceRegistration<?> hookRegistration) throws Exception;

	/**
	 * Return the class name of the hook type supported by this hook context.
	 * 
	 * @return The class name of the hook type supported by this hook context.
	 */
	public String getHookClassName();

	/**
	 * Return the hook method name called by this hook context.
	 * 
	 * @return The hook method name called by this hook context.
	 */
	public String getHookMethodName();

	/**
	 * Returns true if the given registration should be skipped.
	 * @param hookRegistration the registration to check
	 * @return true if the given registration should be skipped.
	 */
	public boolean skipRegistration(ServiceRegistration<?> hookRegistration);
}
