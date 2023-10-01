/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.eclipse.osgi.util.NLS;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.log.LogService;

public class CoordinationWeakReference extends WeakReference<CoordinationReferent> {
	private static final ReferenceQueue<CoordinationReferent> referenceQueue = new ReferenceQueue<>();

	public static void processOrphanedCoordinations() {
		CoordinationWeakReference r;
		while ((r = (CoordinationWeakReference) referenceQueue.poll()) != null) {
			CoordinationImpl c = r.getCoordination();
			if (!c.isEnding()) {
				try {
					c.fail(Coordination.ORPHANED);
				} catch (Exception e) {
					c.getLogService().log(LogService.LOG_WARNING,
							NLS.bind(Messages.OrphanedCoordinationError, c.getName(), c.getId()), e);
				} finally {
					try {
						c.end();
					} catch (CoordinationException e) {
						// This is expected since we already failed the coordination...
						if (!Coordination.ORPHANED.equals(e.getCause()))
							// ...but only if the cause is ORPHANED.
							c.getLogService().log(LogService.LOG_DEBUG,
									NLS.bind(Messages.OrphanedCoordinationError, c.getName(), c.getId()), e);
					} catch (Exception e) {
						c.getLogService().log(LogService.LOG_WARNING,
								NLS.bind(Messages.OrphanedCoordinationError, c.getName(), c.getId()), e);
					}
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
