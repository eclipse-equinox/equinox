/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
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
package org.eclipse.osgi.internal.container;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A helper class for doing lazy initialization 
 * 
 * @param <V> the type of object to lazy initialize
 */
public class AtomicLazyInitializer<V> {
	private final AtomicReference<V> holder = new AtomicReference<>();

	/**
	 * Gets the current value.  If the value has not been initialized then
	 * {@code null} is returned;
	 * @return the current value
	 */
	public final V get() {
		return holder.get();
	}

	/**
	 * Atomically gets the current initialized value.  If the current value is {@code null}
	 * then the supplied initializer is called to create the value returned.
	 * @param initializer the initializer to call if the current value is {@code null}
	 * @return the initialized value.  May return {@code null} if initializer returns null.
	 */
	public final V getInitialized(Callable<V> initializer) {
		V result = holder.get();
		if (result != null) {
			return result;
		}
		// Must hold a lock to ensure the operation is atomic.
		synchronized (holder) {
			result = holder.get();
			if (result != null) {
				return result;
			}
			try {
				result = initializer.call();
			} catch (Exception e) {
				unchecked(e);
			}
			holder.set(result);
			return result;
		}
	}

	/**
	 * Gets the current value and clears the value for future calls to this lazy initializer.
	 * @return the current value
	 */
	public final V getAndClear() {
		return holder.getAndSet(null);
	}

	private static <T> T unchecked(Exception exception) {
		return AtomicLazyInitializer.<T, RuntimeException> unchecked0(exception);
	}

	@SuppressWarnings("unchecked")
	private static <T, E extends Exception> T unchecked0(Exception exception) throws E {
		throw (E) exception;
	}
}