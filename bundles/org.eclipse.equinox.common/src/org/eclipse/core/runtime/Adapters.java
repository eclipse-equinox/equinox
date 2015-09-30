/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 478685
 *******************************************************************************/
package org.eclipse.core.runtime;

import org.eclipse.core.internal.runtime.AdapterManager;

/**
 * Provides a standard way to request adapters from adaptable objects
 * 
 * @see IAdaptable
 * @see IAdapterManager
 * @since 3.8
 */
public class Adapters {
	/**
	 * If it is possible to adapt the given object to the given type, this
	 * returns the adapter. Performs the following checks:
	 * 
	 * <ol>
	 * <li>Returns <code>sourceObject</code> if it is an instance of the
	 * adapter type.</li>
	 * <li>If sourceObject implements IAdaptable, it is queried for adapters.</li>
	 * <li>Finally, the adapter manager is consulted for adapters</li>
	 * </ol>
	 * 
	 * Otherwise returns null.
	 * 
	 * @param sourceObject
	 *            object to adapt
	 * @param adapter
	 *            type to adapt to
	 * @param allowActivation
	 *            if true, plugins may be activated if necessary to provide the requested adapter.
	 *            if false, the method will return null if they cannot be provided from activated plugins.
	 * @return a representation of sourceObject that is assignable to the
	 *         adapter type, or null if no such representation exists
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAdapter(Object sourceObject, Class<T> adapter, boolean allowActivation) {
		if (sourceObject == null) {
			return null;
		}
		if (adapter.isInstance(sourceObject)) {
			return (T) sourceObject;
		}

		if (sourceObject instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) sourceObject;

			Object result = adaptable.getAdapter(adapter);
			if (result != null) {
				// Sanity-check
				Assert.isTrue(adapter.isInstance(result));
				return (T) result;
			}
		}

		// If the source object is a platform object then it's already tried calling AdapterManager.getAdapter,
		// so there's no need to try it again.
		if ((sourceObject instanceof PlatformObject) && !allowActivation) {
			return null;
		}

		String adapterId = adapter.getName();
		Object result = queryAdapterManager(sourceObject, adapterId, allowActivation);
		if (result != null) {
			// Sanity-check
			Assert.isTrue(adapter.isInstance(result));
			return (T) result;
		}

		return null;
	}

	/**
	 * If it is possible to adapt the given object to the given type, this
	 * returns the adapter.
	 * <p>
	 * Convenient method for calling <code>getAdapter(Object, Class, true)</code>.
	 * <p>
	 * See  {@link #getAdapter(Object, Class, boolean)}.
	 *
	 */
	public static <T> T adapt(Object sourceObject, Class<T> adapter) {
		return getAdapter(sourceObject, adapter, true);
	}

	private static Object queryAdapterManager(Object sourceObject, String adapterId, boolean allowActivation) {
		Object result;
		if (allowActivation) {
			result = AdapterManager.getDefault().loadAdapter(sourceObject, adapterId);
		} else {
			result = AdapterManager.getDefault().getAdapter(sourceObject, adapterId);
		}
		return result;
	}
}
