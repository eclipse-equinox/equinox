/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
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
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.runnable.StartupMonitor;

public class DefaultStartupMonitor implements StartupMonitor {

	private final Method updateMethod;
	private final Runnable splashHandler;
	private final EquinoxConfiguration equinoxConfig;

	/**
	 * Create a new startup monitor using the given splash handler.  The splash handle must
	 * have an updateSplash method.
	 * 
	 * @param splashHandler
	 * @throws IllegalStateException
	 */
	public DefaultStartupMonitor(Runnable splashHandler, EquinoxConfiguration equinoxConfig) throws IllegalStateException {
		this.splashHandler = splashHandler;
		this.equinoxConfig = equinoxConfig;

		try {
			updateMethod = splashHandler.getClass().getMethod("updateSplash", (Class[]) null); //$NON-NLS-1$
		} catch (SecurityException e) {
			throw (IllegalStateException) new IllegalStateException(e.getMessage()).initCause(e);
		} catch (NoSuchMethodException e) {
			//TODO maybe we could do something else in the update method in this case, like print something to the console?
			throw (IllegalStateException) new IllegalStateException(e.getMessage()).initCause(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.adaptor.StartupMonitor#update()
	 */
	public void update() {
		if (updateMethod != null) {
			try {
				updateMethod.invoke(splashHandler, (Object[]) null);
			} catch (Throwable e) {
				// ignore, this is best effort
			}
		} else {
			//TODO maybe we could print something interesting to the console?
		}
	}

	public void applicationRunning() {
		if (EclipseStarter.debug) {
			String timeString = equinoxConfig.getConfiguration("eclipse.startTime"); //$NON-NLS-1$ 
			long time = timeString == null ? 0L : Long.parseLong(timeString);
			Debug.println("Application Started: " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
		}
		splashHandler.run();
	}
}
