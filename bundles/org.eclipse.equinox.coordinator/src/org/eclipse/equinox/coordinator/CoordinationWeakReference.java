/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.coordinator;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.eclipse.osgi.util.NLS;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.log.LogService;

public class CoordinationWeakReference extends WeakReference<CoordinationReferent> {
	private static final ReferenceQueue<CoordinationReferent> referenceQueue = new ReferenceQueue<CoordinationReferent>();
	
	public static void processOrphanedCoordinations() {
		CoordinationWeakReference r;
		while ((r = (CoordinationWeakReference)referenceQueue.poll()) != null) {
			CoordinationImpl c = r.getCoordination();
			try {
				c.fail(Coordination.ORPHANED);
			}
			catch (Throwable t) {
				c.getLogService().log(LogService.LOG_ERROR, NLS.bind(Messages.OrphanedCoordinationError, c.getName(), c.getId()), t);
			}
			finally {
				try {
					c.end();
				}
				catch (CoordinationException e) {
					// This is expected since we already failed the coordination...
					if (!Coordination.ORPHANED.equals(e.getCause()))
						// ...but only if the cause is ORPHANED.
						c.getLogService().log(LogService.LOG_ERROR, NLS.bind(Messages.OrphanedCoordinationError, c.getName(), c.getId()), e);
				}
				catch (Throwable t) {
					c.getLogService().log(LogService.LOG_ERROR, NLS.bind(Messages.OrphanedCoordinationError, c.getName(), c.getId()), t);
				}
			}
		}
	}
	
	private final CoordinationImpl coordination;
	
	public CoordinationWeakReference(CoordinationReferent referent, CoordinationImpl coordination) {
		super(referent, referenceQueue);
		if (coordination == null)
			throw new NullPointerException();
		this.coordination = coordination;
	}
	
	public CoordinationImpl getCoordination() {
		return coordination;
	}
}
