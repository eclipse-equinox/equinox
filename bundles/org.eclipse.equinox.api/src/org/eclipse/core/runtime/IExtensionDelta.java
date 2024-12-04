/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
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
package org.eclipse.core.runtime;

/**
 * An extension delta represents changes to the extension registry.
 * <p>
 * This interface can be used without OSGi running.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * 
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IExtensionDelta {
	/**
	 * Delta kind constant indicating that an extension has been added to an
	 * extension point.
	 * 
	 * @see IExtensionDelta#getKind()
	 */
	public int ADDED = 1;
	/**
	 * Delta kind constant indicating that an extension has been removed from an
	 * extension point.
	 * 
	 * @see IExtensionDelta#getKind()
	 */
	public int REMOVED = 2;

	/**
	 * The kind of this extension delta.
	 *
	 * @return the kind of change this delta represents
	 * @see #ADDED
	 * @see #REMOVED
	 */
	public int getKind();

	/**
	 * Returns the affected extension.
	 *
	 * @return the affected extension
	 */
	public IExtension getExtension();

	/**
	 * Returns the affected extension point.
	 *
	 * @return the affected extension point
	 */
	public IExtensionPoint getExtensionPoint();
}
