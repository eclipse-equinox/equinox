/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julian Chen - fix for bug #92572, jclRM
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;

/**
 * NOT API!!!  This log infrastructure was split from the InternalPlatform.
 * 
 * @since org.eclipse.equinox.common 1.0
 */
// XXX this must be removed and replaced with something more reasonable
public final class RuntimeLog {

	private static ArrayList logListeners = new ArrayList(5);

	/**
	 * @see Platform#addLogListener(ILogListener)
	 */
	public static void addLogListener(ILogListener listener) {
		synchronized (logListeners) {
			// replace if already exists (Set behaviour but we use an array
			// since we want to retain order)
			logListeners.remove(listener);
			logListeners.add(listener);
		}
	}

	/**
	 * @see Platform#removeLogListener(ILogListener)
	 */
	public static void removeLogListener(ILogListener listener) {
		synchronized (logListeners) {
			logListeners.remove(listener);
		}
	}

	/**
	 * Checks if the given listener is present
	 */
	public static boolean contains(ILogListener listener) {
		synchronized (logListeners) {
			return logListeners.contains(listener);
		}
	}

	/**
	 * Notifies all listeners of the platform log.
	 */
	public static void log(final IStatus status) {
		// create array to avoid concurrent access
		ILogListener[] listeners;
		synchronized (logListeners) {
			listeners = (ILogListener[]) logListeners.toArray(new ILogListener[logListeners.size()]);
		}
		for (int i = 0; i < listeners.length; i++) {
			try {
				listeners[i].logging(status, IRuntimeConstants.PI_RUNTIME);
			} catch (Exception e) {
				handleException(e);
			} catch (LinkageError e) {
				handleException(e);
			}
		}
	}

	private static void handleException(Throwable e) {
		if (!(e instanceof OperationCanceledException)) {
			// Got a error while logging. Don't try to log again, just put it into stderr 
			e.printStackTrace();
		}
	}

	/**
	 * Helps determine if any listeners are registered with the logging mechanism.
	 * @return true if no listeners are registered
	 */
	public static boolean isEmpty() {
		synchronized (logListeners) {
			return (logListeners.size() == 0);
		}
	}

}
