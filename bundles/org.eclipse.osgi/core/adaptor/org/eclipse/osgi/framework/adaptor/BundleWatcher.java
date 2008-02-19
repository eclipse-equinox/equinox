/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.adaptor;

import org.osgi.framework.Bundle;

/**
 * Watches bundle lifecyle processes.  This interface is different than that of
 * a BundleLisener because it gets notified before and after all lifecycle 
 * changes.  A bundle watcher acts as the main entry point for logging 
 * bundle activity.
 * <p>
 * Note that a bundle watcher is always notified of when a lifecycle processes 
 * has ended even in cases where the lifecycle process may have failed.  For 
 * example, if activating a bundle fails the {@link #END_ACTIVATION} flag will
 * still be sent to the bundle watcher to notify them that the activation 
 * process has ended.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.1
 */
public interface BundleWatcher {
	/**
	 * The install process is beginning for a bundle
	 * @since 3.2
	 */
	public static final int START_INSTALLING = 0x0001;
	/**
	 * The install process has ended for a bundle
	 * @since 3.2
	 */
	public static final int END_INSTALLING = 0x0002;
	/**
	 * The activation process is beginning for a bundle
	 * @since 3.2
	 */
	public static final int START_ACTIVATION = 0x0004;
	/**
	 * The activation process has ended for a bundle
	 * @since 3.2
	 */
	public static final int END_ACTIVATION = 0x0008;
	/**
	 * The deactivation process is beginning for a bundle
	 * @since 3.2
	 */
	public static final int START_DEACTIVATION = 0x0010;
	/**
	 * The deactivation process has ended for a bundle
	 * @since 3.2
	 */
	public static final int END_DEACTIVATION = 0x0020;
	/**
	 * The uninstallation process is beginning for a bundle
	 * @since 3.2
	 */
	public static final int START_UNINSTALLING = 0x0040;
	/**
	 * The uninstallation process has ended for a bundle
	 * @since 3.2
	 */
	public static final int END_UNINSTALLING = 0x0080;

	/**
	 * Receives notification that a lifecycle change is going to start or has
	 * ended.
	 * @param bundle the bundle for which the lifecycle change is occurring on.
	 * @param type the type of lifecycle change which is occurring.
	 * @see #START_INSTALLING
	 * @see #END_INSTALLING
	 * @see #START_ACTIVATION
	 * @see #END_ACTIVATION
	 * @see #START_DEACTIVATION
	 * @see #END_DEACTIVATION
	 * @see #START_UNINSTALLING
	 * @see #END_UNINSTALLING
	 * @since 3.2
	 */
	public void watchBundle(Bundle bundle, int type);
}
