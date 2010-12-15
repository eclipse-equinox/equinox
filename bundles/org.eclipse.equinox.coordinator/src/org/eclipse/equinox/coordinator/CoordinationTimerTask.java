/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.coordinator;

import java.util.TimerTask;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.log.LogService;

public class CoordinationTimerTask extends TimerTask {
	private final CoordinationImpl coordination;

	public CoordinationTimerTask(CoordinationImpl coordination) {
		if (coordination == null)
			throw new IllegalArgumentException(Messages.CoordinationImpl_0);
		this.coordination = coordination;
	}

	@Override
	public void run() {
		// Catch all exceptions and errors in order to prevent the timer 
		// thread from stopping.
		try {
			coordination.fail(Coordination.TIMEOUT);
		} catch (Throwable t) {
			coordination.getLogService().log(LogService.LOG_ERROR, Messages.CoordinationImpl_9, t);
		}
	}
}
