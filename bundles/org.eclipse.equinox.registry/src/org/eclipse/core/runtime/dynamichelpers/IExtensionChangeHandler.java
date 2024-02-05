/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
package org.eclipse.core.runtime.dynamichelpers;

import org.eclipse.core.runtime.IExtension;

/**
 * Extension change handlers are notified of changes for a given extension point
 * in the context of an extension tracker.
 * <p>
 * This interface can be used without OSGi running.
 * </p>
 * <p>
 * This interface is intended to be implemented by clients.
 * </p>
 * 
 * @since 3.1
 */
public interface IExtensionChangeHandler {

	/**
	 * This method is called whenever an extension conforming to the extension point
	 * filter is being added to the registry. This method does not automatically
	 * register objects to the tracker.
	 *
	 * @param tracker   a tracker to which the handler has been registered
	 * @param extension the extension being added
	 */
	public void addExtension(IExtensionTracker tracker, IExtension extension);

	/**
	 * This method is called after the removal of an extension.
	 *
	 * @param extension the extension being removed
	 * @param objects   the objects that were associated with the removed extension
	 */
	public void removeExtension(IExtension extension, Object[] objects);
}
