/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.dynamichelpers;

import org.eclipse.core.runtime.IExtensionPoint;

/**
 * A filter compares the given object to some pattern and returns 
 * <code>true</code> if the two match and <code>false</code> otherwise.
 * <p>
 * This interface may be implemented by clients, however factory methods are 
 * available on IExtensionTracker.
 * </p><p>
 * This interface can be used without OSGi running.
 * </p>
 * @since 3.1
 */
public interface IFilter {
	/**
	 * Return <code>true</code> if the given object matches the criteria 
	 * for this filter.
	 * 
	 * @param target the object to match
	 * @return <code>true</code> if the target matches this filter 
	 * 	and <code>false</code> otherwise
	 */
	public boolean matches(IExtensionPoint target);
}
