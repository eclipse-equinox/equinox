/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

/**
 * Aggregated registry timestamp. Corresponds to the current contents of the registry.
 * <p>
 * This class may be instantiated.
 * </p><p>
 * This class is not indended to be subclassed.
 * </p>
 * @since org.eclipse.equinox.registry 3.3
 */
public final class RegistryTimestamp {
	/**
	 * Current aggregated timestamp
	 */
	private long aggregateTimestamp;

	private boolean modified;

	/**
	 * Public constructor.
	 */
	public RegistryTimestamp() {
		reset();
	}

	/**
	 * Returns value of the aggregated timestamp.
	 * @return value of the aggregated timestamp
	 */
	public long getContentsTimestamp() {
		return aggregateTimestamp;
	}

	/**
	 * Set value of the aggregated timestamp.
	 * @param timestamp the aggregated timestamp of the current registry contents 
	 */
	public void set(long timestamp) {
		aggregateTimestamp = timestamp;
		modified = false;
	}

	/**
	 * Sets aggregated timestamp to the value corresponding to an empty registry.
	 */
	public void reset() {
		aggregateTimestamp = 0;
		modified = false;
	}

	/**
	 * Determines if the aggregate timestamp was modified using add() or remove()
	 * methods.
	 * @return true: the timestamp was modified after the last set/reset 
	 */
	public boolean isModifed() {
		return modified;
	}

	/**
	 * Add individual contribution timestamp to the aggregated timestamp. 
	 * @param timestamp the time stamp of the contribution being added to the registry
	 */
	public void add(long timestamp) {
		aggregateTimestamp ^= timestamp;
		modified = true;
	}

	/**
	 * Remove individual contribution timestamp from the aggregated timestamp.
	 * @param timestamp the time stamp of the contribution being removed from the registry
	 */
	public void remove(long timestamp) {
		aggregateTimestamp ^= timestamp;
		modified = true;
	}
}
