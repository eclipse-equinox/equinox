/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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

import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.CoordinationPermission;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;

public class CoordinatorImpl implements Coordinator {
	// IDs must be positive integers and monotonically increasing.
	private static long lastId;

	private synchronized static long getNextId() {
		if (Long.MAX_VALUE == lastId)
			throw new IllegalStateException(NLS.bind(Messages.MaxCoordinationIdExceeded, lastId));
		// First ID will be 1.
		return ++lastId;
	}

	// Coordination IDs must be unique across all using bundles.
	private static final Map<Long, CoordinationImpl> idToCoordination = new HashMap<>();
	// Coordination participation must be tracked across all using bundles.
	private static final Map<Participant, CoordinationImpl> participantToCoordination = Collections
			.synchronizedMap(new IdentityHashMap<Participant, CoordinationImpl>());

	private static ThreadLocal<WeakCoordinationStack> coordinationStack = new ThreadLocal<WeakCoordinationStack>() {
		@Override
		protected WeakCoordinationStack initialValue() {
			return new WeakCoordinationStack();
		}
	};

	private static class WeakCoordinationStack {
		private final LinkedList<CoordinationImpl> coordinations = new LinkedList<>();

		public WeakCoordinationStack() {
		}

		public boolean contains(CoordinationImpl c) {
			return coordinations.contains(c);
		}

		public CoordinationImpl peek() {
			if (coordinations.isEmpty())
				return null;
			return coordinations.getFirst();
		}

		public CoordinationImpl pop() {
			if (coordinations.isEmpty())
				return null;
			CoordinationImpl c = coordinations.removeFirst();
			if (c != null)
				c.setThreadAndEnclosingCoordination(null, null);
			return c;
		}

		public void push(CoordinationImpl c) {
			if (contains(c))
				throw new CoordinationException(
						NLS.bind(Messages.CoordinationAlreadyExists,
								new Object[] { c.getName(), c.getId(), Thread.currentThread() }),
						c.getReferent(), CoordinationException.ALREADY_PUSHED);
			c.setThreadAndEnclosingCoordination(Thread.currentThread(),
					coordinations.isEmpty() ? null : coordinations.getFirst());
			coordinations.addFirst(c);
		}
	}

	private final Bundle bundle;
	private final List<CoordinationImpl> coordinations;
	private final LogTracker logTracker;
	private final long maxTimeout;
	private final Timer timer;

	private boolean shutdown;

	public CoordinatorImpl(Bundle bundle, LogTracker logService, Timer timer, long maxTimeout) {
		this.bundle = bundle;
		this.logTracker = logService;
		this.timer = timer;
		coordinations = new ArrayList<>();
		if (maxTimeout < 0)
			throw new IllegalArgumentException(NLS.bind(Messages.InvalidTimeInterval, maxTimeout));
		this.maxTimeout = maxTimeout;
	}

	public boolean addParticipant(Participant participant) throws CoordinationException {
		CoordinationWeakReference.processOrphanedCoordinations();
		Coordination coordination = peek();
		if (coordination == null)
			return false;
		coordination.addParticipant(participant);
		return true;
	}

	public Coordination begin(String name, long timeout) {
		Coordination coordination = create(name, timeout);
		coordination.push();
		return coordination;
	}

	public Coordination create(String name, long timeout) {
		CoordinationWeakReference.processOrphanedCoordinations();
		// This method requires the INITIATE permission. No bundle check is done.
		checkPermission(CoordinationPermission.INITIATE, name);
		// Override the requested timeout with the max timeout, if necessary.
		if (maxTimeout != 0) {
			if (timeout == 0 || maxTimeout < timeout) {
				logTracker.warn(NLS.bind(Messages.MaximumTimeout, new Object[] { timeout, maxTimeout, name }));
				timeout = maxTimeout;
			}
		}
		// Create the coordination object itself, which will store its own instance
		// of a referent to be returned to clients other than the initiator.
		CoordinationImpl coordination = new CoordinationImpl(getNextId(), name, timeout, this);
		// Create the referent to be returned to the initiator.
		CoordinationReferent referent = new CoordinationReferent(coordination);
		// Create a weak reference to the referent returned to the initiator. No other
		// references to the initiator's referent must be maintained outside of this
		// method. A strong reference to the CoordinationWeakReference must be
		// maintained
		// by the coordination in order to avoid garbage collection. It serves no other
		// purpose. Just "set it and forget it".
		coordination.reference = new CoordinationWeakReference(referent, coordination);
		synchronized (this) {
			if (shutdown)
				throw new IllegalStateException(NLS.bind(Messages.CoordinatorShutdown, name, timeout));
			synchronized (CoordinatorImpl.class) {
				coordinations.add(coordination);
				idToCoordination.put(Long.valueOf(coordination.getId()), coordination);
			}
		}
		if (timeout > 0) {
			TimerTask timerTask = new CoordinationTimerTask(coordination);
			coordination.setTimerTask(timerTask);
		}
		// Make sure to return the referent targeted towards the initiator here.
		return referent;
	}

	public boolean fail(Throwable reason) {
		CoordinationWeakReference.processOrphanedCoordinations();
		Coordination coordination = peek();
		if (coordination == null)
			return false;
		return coordination.fail(reason);
	}

	public Coordination getCoordination(long id) {
		CoordinationWeakReference.processOrphanedCoordinations();
		CoordinationReferent result = null;
		synchronized (CoordinatorImpl.class) {
			CoordinationImpl c = idToCoordination.get(Long.valueOf(id));
			if (c != null)
				result = c.getReferent();
		}
		if (result != null && !result.isTerminated()) {
			try {
				checkPermission(CoordinationPermission.ADMIN, result.getName());
			} catch (SecurityException e) {
				logTracker.debug(NLS.bind(Messages.GetCoordinationNotPermitted,
						new Object[] { Thread.currentThread(), result.getName(), result.getId() }), e);
				result = null;
			}
		}
		return result;
	}

	public Collection<Coordination> getCoordinations() {
		CoordinationWeakReference.processOrphanedCoordinations();
		ArrayList<Coordination> result;
		synchronized (CoordinatorImpl.class) {
			result = new ArrayList<>(idToCoordination.size());
			for (CoordinationImpl coordination : idToCoordination.values()) {
				// Ideally, we're only interested in coordinations that have not terminated.
				// It's okay, however, if the coordination terminates from this point forward.
				if (coordination.isTerminated())
					continue;
				try {
					checkPermission(CoordinationPermission.ADMIN, coordination.getName());
					result.add(coordination.getReferent());
				} catch (SecurityException e) {
					logTracker.debug(NLS.bind(Messages.GetCoordinationNotPermitted,
							new Object[] { Thread.currentThread(), coordination.getName(), coordination.getId() }), e);
				}
			}
		}
		result.trimToSize();
		return result;
	}

	public Coordination peek() {
		CoordinationWeakReference.processOrphanedCoordinations();
		CoordinationImpl c = coordinationStack.get().peek();
		if (c == null)
			return null;
		return c.getReferent();
	}

	public Coordination pop() {
		CoordinationWeakReference.processOrphanedCoordinations();
		CoordinationImpl c = coordinationStack.get().peek();
		if (c == null)
			return null;
		checkPermission(CoordinationPermission.INITIATE, c.getName());
		return coordinationStack.get().pop().getReferent();
	}

	CoordinationImpl addParticipant(Participant participant, CoordinationImpl coordination) {
		CoordinationImpl result = null;
		synchronized (participantToCoordination) {
			result = participantToCoordination.get(participant);
			if (result == null) {
				participantToCoordination.put(participant, coordination);
			}
		}
		return result;
	}

	void checkPermission(String permissionType, String coordinationName) {
		checkPermission(new CoordinationPermission(coordinationName, bundle, permissionType));
	}

	void checkPermission(Permission permission) {
		SecurityManager securityManager = System.getSecurityManager();
		if (securityManager == null)
			return;
		securityManager.checkPermission(permission);
	}

	Bundle getBundle() {
		return bundle;
	}

	LogTracker getLogService() {
		return logTracker;
	}

	long getMaxTimeout() {
		return maxTimeout;
	}

	void purge() {
		// Purge the timer of all canceled tasks if we're running on a supportive JCL.
		try {
			Timer.class.getMethod("purge", (Class<?>[]) null).invoke(timer, (Object[]) null); //$NON-NLS-1$
		} catch (Exception e) {
			logTracker.debug(Messages.CanceledTaskNotPurged, e);
		}
	}

	void push(CoordinationImpl coordination) throws CoordinationException {
		coordinationStack.get().push(coordination);
	}

	void schedule(TimerTask task, Date deadline) {
		timer.schedule(task, deadline);
	}

	void shutdown() {
		CoordinationWeakReference.processOrphanedCoordinations();
		List<CoordinationImpl> coords;
		synchronized (this) {
			shutdown = true;
			// Make a copy so the removal of the coordination from the list during
			// termination does not interfere with the iteration.
			coords = new ArrayList<>(this.coordinations);
		}
		for (CoordinationImpl coordination : coords) {
			coordination.fail(Coordination.RELEASED);
		}
	}

	/*
	 * This procedure must occur when a coordination is being failed or ended.
	 */
	void terminate(CoordinationImpl coordination, List<Participant> participants) {
		// A coordination has been terminated and needs to be removed from the thread
		// local stack.
		synchronized (this) {
			synchronized (CoordinatorImpl.class) {
				this.coordinations.remove(coordination);
				idToCoordination.remove(Long.valueOf(coordination.getId()));
				participantToCoordination.keySet().removeAll(participants);
			}
		}
	}
}
