/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Andrew Niefer - IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.internal.adaptor;

import java.lang.reflect.Method;
import org.eclipse.osgi.service.runnable.StartupMonitor;

public class DefaultStartupMonitor implements StartupMonitor {

	private Method updateMethod = null;
	private Object splashHandler = null;

	/**
	 * Create a new startup monitor using the given splash handler.  The splash handle must
	 * have an updateSplash method.
	 * 
	 * @param splashHandler
	 * @throws IllegalStateException
	 */
	public DefaultStartupMonitor(Object splashHandler) throws IllegalStateException {
		this.splashHandler = splashHandler;

		try {
			updateMethod = splashHandler.getClass().getMethod("updateSplash", null); //$NON-NLS-1$
		} catch (SecurityException e) {
			throw new IllegalStateException(e.getMessage());
		} catch (NoSuchMethodException e) {
			//TODO maybe we could do something else in the update method in this case, like print something to the console?
			throw new IllegalStateException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.adaptor.StartupMonitor#update()
	 */
	public void update() {
		if (updateMethod != null) {
			try {
				updateMethod.invoke(splashHandler, null);
			} catch (Throwable e) {
				// ignore, this is best effort
			}
		} else {
			//TODO maybe we could print something interesting to the console?
		}
	}
}
