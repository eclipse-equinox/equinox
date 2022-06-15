/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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
@FunctionalInterface
public interface HookContext<T> {

	/**
	 * Call the specified hook.
	 *
	 * @param hook The hook object to call. The hook object must be of the type
	 * supported by this hook context. If it is not, then this method will
	 * simply return.
	 * @param hookRegistration the registration for the hook object
	 * @throws Exception An exception thrown by the hook object.
	 */
	public void call(T hook, ServiceRegistration<T> hookRegistration) throws Exception;

	/**
	 * Returns true if the given registration should be skipped.
	 * @param hookRegistration the registration to check
	 * @return true if the given registration should be skipped.
	 */
	public default boolean skipRegistration(ServiceRegistration<?> hookRegistration) {
		return false;
	}
}
