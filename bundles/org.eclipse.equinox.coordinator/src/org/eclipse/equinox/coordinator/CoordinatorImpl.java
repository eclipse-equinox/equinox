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

import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceException;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.CoordinationPermission;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.log.LogService;

public class CoordinatorImpl implements Coordinator {
	// IDs must be positive integers and monotonically increasing.
	private static long lastId;

	private synchronized static long getNextId() {
		if (Long.MAX_VALUE == lastId)
			throw new IllegalStateException(Messages.CoordinatorImpl_0);
		// First ID will be 1.
		return ++lastId;
	}

	// Coordination IDs must be unique across all using bundles.
	private static final Map<Long, CoordinationImpl> idToCoordination = new HashMap<Long, CoordinationImpl>();
	// Coordination participation must be tracked across all using bundles.
	private static final Map<Participant, CoordinationImpl> participantToCoordination = new IdentityHashMap<Participant, CoordinationImpl>();
	/* ThreadLocal, as opposed to the following Map, was considered and rejected 
	 * because a coordination may be terminated from a thread other than the one 
	 * it's currently associated with. Termination requires the coordination to be 
	 * removed from the stack. There would be no access to the ThreadLocal under 
	 * such circumstances.
	 * 
	 * The thread local stack must be tracked across all using bundles.
	 */
	private static final Map<Thread, LinkedList<CoordinationImpl>> threadToCoordinations = new HashMap<Thread, LinkedList<CoordinationImpl>>();

	private final Bundle bundle;
	private final List<CoordinationImpl> coordinations;
	private final LogService logService;
	private final Timer timer;

	private boolean shutdown;

	public CoordinatorImpl(Bundle bundle, LogService logService, Timer timer) {
		this.bundle = bundle;
		this.logService = logService;
		this.timer = timer;
		coordinations = new ArrayList<CoordinationImpl>();
	}

	public boolean addParticipant(Participant participant) throws CoordinationException {
		Coordination coordination = peek();
		if (coordination == null)
			return false;
		coordination.addParticipant(participant);
		return true;
	}

	public Coordination begin(String name, int timeout) {
		CoordinationImpl coordination = (CoordinationImpl) create(name, timeout);
		coordination.push();
		return coordination;
	}

	public Coordination create(String name, int timeout) {
		// This method requires the INITIATE permission. No bundle check is done.
		checkPermission(CoordinationPermission.INITIATE, name, false);
		CoordinationImpl coordination = new CoordinationImpl(getNextId(), name, timeout, this);
		synchronized (this) {
			if (shutdown)
				throw new IllegalStateException(Messages.CoordinatorImpl_2);
			synchronized (CoordinatorImpl.class) {
				coordinations.add(coordination);
				idToCoordination.put(new Long(coordination.getId()), coordination);
			}
		}
		if (timeout > 0) {
			TimerTask timerTask = new CoordinationTimerTask(coordination);
			coordination.setTimerTask(timerTask);
			schedule(timerTask, coordination.getDeadline());
		}
		return coordination;
	}

	public boolean fail(Throwable reason) {
		Coordination coordination = peek();
		if (coordination == null)
			return false;
		return coordination.fail(reason);
	}

	public Coordination getCoordination(long id) {
		CoordinationImpl result = null;
		synchronized (CoordinatorImpl.class) {
			result = idToCoordination.get(new Long(id));
		}
		if (result == null || result.isTerminated())
			return null;
		checkPermission(CoordinationPermission.ADMIN, result.getName());
		return result;
	}

	public Collection<Coordination> getCoordinations() {
		ArrayList<Coordination> result;
		synchronized (CoordinatorImpl.class) {
			result = new ArrayList<Coordination>(idToCoordination.size());
			for (CoordinationImpl coordination : idToCoordination.values()) {
				// Ideally, we're only interested in coordinations that have not terminated.
				// It's okay, however, if the coordination terminates from this point forward.
				if (coordination.isTerminated())
					continue;
				try {
					checkPermission(CoordinationPermission.ADMIN, coordination.getName());
					result.add(coordination);
				} catch (SecurityException e) {
					logService.log(LogService.LOG_DEBUG, Messages.CoordinatorImpl_1, e);
				}
			}
		}
		result.trimToSize();
		return result;
	}

	public Coordination peek() {
		Coordination result = null;
		Thread thread = Thread.currentThread();
		synchronized (CoordinationImpl.class) {
			LinkedList<CoordinationImpl> coords = threadToCoordinations.get(thread);
			// Be sure to avoid EmptyStackException with this check.
			if (coords != null && !coords.isEmpty()) {
				result = coords.getFirst();
			}
		}
		return result;
	}

	public Coordination pop() {
		Thread thread = Thread.currentThread();
		CoordinationImpl result = null;
		synchronized (CoordinatorImpl.class) {
			LinkedList<CoordinationImpl> coords = threadToCoordinations.get(thread);
			if (coords != null && !coords.isEmpty()) {
				result = coords.removeFirst();
				if (coords.isEmpty()) {
					// Clean the map up if there are no more coordinations
					// associated with the current thread.
					threadToCoordinations.remove(thread);
				}
			}
		}
		if (result != null)
			result.pop();
		return result;
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
		checkPermission(permissionType, coordinationName, true);
	}

	void checkPermission(String permissionType, String coordinationName, boolean bundleCheckRequired) {
		checkPermission(new CoordinationPermission(bundleCheckRequired ? bundle : null, coordinationName, permissionType));
	}

	void checkPermission(Permission permission) {
		SecurityManager securityManager = System.getSecurityManager();
		if (securityManager == null)
			return;
		securityManager.checkPermission(permission);
	}

	LogService getLogService() {
		return logService;
	}

	void purge() {
		// Purge the timer of all canceled tasks if  we're running on a supportive JCL.
		try {
			Timer.class.getMethod("purge", (Class<?>[]) null).invoke(timer, (Object[]) null); //$NON-NLS-1$
		} catch (Exception e) {
			logService.log(LogService.LOG_DEBUG, Messages.CoordinatorImpl_4, e);
		}
	}

	void push(CoordinationImpl coordination) throws CoordinationException {
		synchronized (CoordinatorImpl.class) {
			LinkedList<CoordinationImpl> coords = threadToCoordinations.get(coordination.getThread());
			if (coords == null) {
				coords = new LinkedList<CoordinationImpl>();
				threadToCoordinations.put(coordination.getThread(), coords);
			}
			if (coords.contains(coordination))
				throw new CoordinationException(Messages.CoordinatorImpl_3, coordination, CoordinationException.ALREADY_PUSHED);
			coords.addFirst(coordination);
		}
	}

	void schedule(TimerTask task, Date deadline) {
		timer.schedule(task, deadline);
	}

	void shutdown() {
		List<Coordination> coords;
		synchronized (this) {
			shutdown = true;
			// Make a copy so the removal of the coordination from the list during
			// termination does not interfere with the iteration.
			coords = new ArrayList<Coordination>(this.coordinations);
		}
		ServiceException serviceException = new ServiceException(Messages.CoordinationImpl_4, ServiceException.UNREGISTERED);
		for (Coordination coordination : coords) {
			coordination.fail(serviceException);
		}
	}

	void terminate(Coordination coordination, List<Participant> participants) {
		// A coordination has been terminated and needs to be removed from the thread local stack.
		synchronized (this) {
			synchronized (CoordinatorImpl.class) {
				this.coordinations.remove(coordination);
				LinkedList<CoordinationImpl> coords = threadToCoordinations.get(coordination.getThread());
				if (coords != null && !coords.isEmpty()) {
					coords.remove(coordination);
					if (coords.isEmpty()) {
						// Clean the map up if there are no more coordinations
						// associated with the current thread.
						threadToCoordinations.remove(coordination.getThread());
					}
				}
				idToCoordination.remove(new Long(coordination.getId()));
				participantToCoordination.keySet().removeAll(participants);
			}
		}
	}
}
