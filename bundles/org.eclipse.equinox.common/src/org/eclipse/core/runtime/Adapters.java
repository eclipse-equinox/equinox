/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 478685, 478864, 479849
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
	 * @param <T> class type to adapt to
	 * @param sourceObject
	 *            object to adapt, can be null
	 * @param adapter
	 *            type to adapt to
	 * @param allowActivation
	 *            if true, plug-ins may be activated if necessary to provide the requested adapter.
	 *            if false, the method will return null if an adapter cannot be provided from activated plug-ins.
	 * @return a representation of sourceObject that is assignable to the
	 *         adapter type, or null if no such representation exists
	 */
	@SuppressWarnings("unchecked")
	public static <T> T adapt(Object sourceObject, Class<T> adapter, boolean allowActivation) {
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
				if (!adapter.isInstance(result)) {
					throw new AssertionFailedException(adaptable.getClass().getName() + ".getAdapter(" + adapter.getName() + ".class) returned " //$NON-NLS-1$//$NON-NLS-2$
							+ result.getClass().getName() + " that is not an instance the requested type"); //$NON-NLS-1$
				}
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
			if (!adapter.isInstance(result)) {
				throw new AssertionFailedException("An adapter factory for " //$NON-NLS-1$
						+ sourceObject.getClass().getName() + " returned " + result.getClass().getName() //$NON-NLS-1$
						+ " that is not an instance of " + adapter.getName()); //$NON-NLS-1$
			}
			return (T) result;
		}

		return null;
	}

	/**
	 * If it is possible to adapt the given object to the given type, this
	 * returns the adapter.
	 * <p>
	 * Convenience method for calling <code>adapt(Object, Class, true)</code>.
	 * <p>
	 * See {@link #adapt(Object, Class, boolean)}.
	 * 
	 * @param <T> class type to adapt to
	 * @param sourceObject
	 *            object to adapt, can be null
	 * @param adapter
	 *            type to adapt to
	 * @return a representation of sourceObject that is assignable to the
	 *         adapter type, or null if no such representation exists
	 */
	public static <T> T adapt(Object sourceObject, Class<T> adapter) {
		return adapt(sourceObject, adapter, true);
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
