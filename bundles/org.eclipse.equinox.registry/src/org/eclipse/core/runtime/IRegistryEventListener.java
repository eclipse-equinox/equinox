/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.util.EventListener;

/**
 * A registry event listener is notified of changes to extension points. Changes
 * include modifications of extension points and their extensions. Listeners will
 * only receive a notification if the extension point they are registered for is
 * modified. (Which includes modifications of extensions under the extension point.)
 * <p>
 * This interface can be used without OSGi running.
 * </p><p>
 * Clients may implement this interface.
 * </p>
 * @see IExtensionRegistry#addListener(IRegistryEventListener, String)
 * @since 3.4
 */
public interface IRegistryEventListener extends EventListener {

	/**
	 * Notifies this listener that extensions were added to the registry.
	 * <p>
	 * The extensions supplied as the argument are valid only for the duration of the 
	 * invocation of this method.
	 * </p>
	 * @param extensions extensions added to the registry
	 */
	public void added(IExtension[] extensions);

	/**
	 * Notifies this listener that extensions were removed from the registry.
	 * <p>
	 * The extensions supplied as the argument are valid only for the duration of the 
	 * invocation of this method.
	 * </p>
	 * @param extensions extensions removed from the registry
	 */
	public void removed(IExtension[] extensions);

	/**
	 * Notifies this listener that extension points were added to the registry.
	 * <p>
	 * The extension points supplied as the argument are valid only for the duration of the 
	 * invocation of this method.
	 * </p>
	 * @param extensionPoints extension points added to the registry
	 */
	public void added(IExtensionPoint[] extensionPoints);

	/**
	 * Notifies this listener that extension points were removed from the registry.
	 * <p>
	 * The extension points supplied as the argument are valid only for the duration of the 
	 * invocation of this method.
	 * </p>
	 * @param extensionPoints extension points removed from the registry
	 */
	public void removed(IExtensionPoint[] extensionPoints);

}
