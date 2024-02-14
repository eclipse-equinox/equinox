/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
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
package org.eclipse.equinox.coordinator;

import java.util.TimerTask;

import org.eclipse.osgi.util.NLS;
import org.osgi.service.coordinator.Coordination;

public class CoordinationTimerTask extends TimerTask {
	private final CoordinationImpl coordination;

	public CoordinationTimerTask(CoordinationImpl coordination) {
		if (coordination == null)
			throw new NullPointerException(NLS.bind(Messages.NullParameter, "coordination")); //$NON-NLS-1$
		this.coordination = coordination;
	}

	@Override
	public void run() {
		// Catch all exceptions and errors in order to prevent the timer
		// thread from stopping.
		try {
			coordination.fail(Coordination.TIMEOUT);
		} catch (Throwable t) {
			coordination.getLogService().error(NLS.bind(Messages.CoordinationTimedOutError,
					new Object[] { coordination.getName(), coordination.getId(), Thread.currentThread() }), t);
		}
	}
}
