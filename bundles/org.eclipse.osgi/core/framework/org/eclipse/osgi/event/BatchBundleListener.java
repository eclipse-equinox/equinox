/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.event;

import org.osgi.framework.*;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
/**
 * A batch <code>BundleEvent</code> listener.
 * 
 * <p>
 * <code>BatchBundleListener</code> is a listener interface that may be
 * implemented by a bundle developer.
 * <p>
 * A <code>BatchBundleListener</code> object is registered with the
 * Framework using the {@link BundleContext#addBundleListener} method.
 * <code>BatchBundleListener</code> objects are called with a
 * <code>BundleEvent</code> object when a bundle has been installed, resolved,
 * started, stopped, updated, unresolved, or uninstalled.
 * <p>
 * <code>BatchBundleListener</code>s are synchronously called during bundle
 * life cycle processing.  Unlike <code>SynchronousBundleListener</code> the
 * framework will batch bundle events during a batching operation and deliver
 * the the list of batched events at the end of the operation.  For example,
 * the framework may batch bundle events during a refresh packages operation.
 * <p>
 * During a batching operation the framework will not deliver any events using
 * the {@link BundleListener#bundleChanged(BundleEvent)} method to the
 * <code>BatchBundleListener</code>.  During a non-batching operation the
 * <code>BatchBundleListener</code> will act like a normal <code>BundleListener</code>
 * and the {@link BundleListener#bundleChanged(BundleEvent)} method will be
 * called to deliver the non-batched <code>BundleEvent</code>s.
 * <p>
 * <code>AdminPermission</code> is required to add or remove a
 * <code>BatchBundleListener</code> object.
 * 
 * @see BundleEvent
 * @see SynchronousBundleListener
 */
public interface BatchBundleListener extends SynchronousBundleListener {
	/**
	 * Receives notification that a list of batched <code>BundleEvent</code>s have occurred.
	 * 
	 * @param events The <code>BundleEvent</code>s that have occurred.
	 */
	public abstract void bundlesChanged(BundleEvent[] events);
}
