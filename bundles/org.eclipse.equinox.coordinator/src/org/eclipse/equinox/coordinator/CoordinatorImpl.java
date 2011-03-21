/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.coordinator;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
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

	private static ThreadLocal<WeakCoordinationStack> coordinationStack = new ThreadLocal<WeakCoordinationStack>() {
		@Override 
		protected WeakCoordinationStack initialValue() {
			return new WeakCoordinationStack();
		}
	};
	
	private static class WeakCoordinationStack {
		private final LinkedList<Reference<CoordinationImpl>> coordinations = new LinkedList<Reference<CoordinationImpl>>();
		private final ReferenceQueue<CoordinationImpl> queue = new ReferenceQueue<CoordinationImpl>();
		
		public WeakCoordinationStack() {
		}

		public boolean contains(CoordinationImpl c) {
			purge();
			for (Reference<CoordinationImpl> r : coordinations) {
				if (c.equals(r.get()))
					return true;
			}
			return false;
		}
		
		public CoordinationImpl peek() {
			purge();
			if (coordinations.isEmpty())
				return null;
			return coordinations.getFirst().get();
		}
		
		public CoordinationImpl pop() {
			purge();
			if (coordinations.isEmpty())
				return null;
			CoordinationImpl c = coordinations.removeFirst().get();
			if (c != null)
				c.setThreadAndEnclosingCoordination(null, null);
			return c;
		}
		
		public void push(CoordinationImpl c) {
			purge();
			if (contains(c))
				throw new CoordinationException(Messages.CoordinatorImpl_3, c, CoordinationException.ALREADY_PUSHED);
			Reference<CoordinationImpl> r = new WeakReference<CoordinationImpl>(c, queue);
			c.setThreadAndEnclosingCoordination(Thread.currentThread(), coordinations.isEmpty() ? null : coordinations.getFirst());
			coordinations.addFirst(r);
		}
		
		private void purge() {
			Reference<? extends CoordinationImpl> r;
			while ((r = queue.poll()) != null) {
				int index = coordinations.indexOf(r);
				coordinations.remove(r);
				if (index > 0) {
					r = coordinations.get(index - 1);
					CoordinationImpl c = r.get();
					if (c != null)
						c.setThreadAndEnclosingCoordination(Thread.currentThread(), coordinations.get(index));
				}
			}
		}
	}
	
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

	public Coordination begin(String name, long timeout) {
		CoordinationImpl coordination = (CoordinationImpl) create(name, timeout);
		coordination.push();
		return coordination;
	}

	public Coordination create(String name, long timeout) {
		// This method requires the INITIATE permission. No bundle check is done.
		checkPermission(CoordinationPermission.INITIATE, name);
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
		if (result != null && !result.isTerminated()) {
			try {
				checkPermission(CoordinationPermission.ADMIN, result.getName());
			} catch (SecurityException e) {
				logService.log(LogService.LOG_DEBUG, Messages.CoordinatorImpl_1, e);
				result = null;
			}
		}
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
		return coordinationStack.get().peek();
	}

	public Coordination pop() {
		Coordination c = coordinationStack.get().peek();
		if (c == null) return null;
		checkPermission(CoordinationPermission.INITIATE, c.getName());
		return coordinationStack.get().pop();
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

	LogService getLogService() {
		return logService;
	}

	void purge() {
		// Purge the timer of all canceled tasks if we're running on a supportive JCL.
		try {
			Timer.class.getMethod("purge", (Class<?>[]) null).invoke(timer, (Object[]) null); //$NON-NLS-1$
		} catch (Exception e) {
			logService.log(LogService.LOG_DEBUG, Messages.CoordinatorImpl_4, e);
		}
	}

	void push(CoordinationImpl coordination) throws CoordinationException {
		coordinationStack.get().push(coordination);
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
		for (Coordination coordination : coords) {
			coordination.fail(Coordination.RELEASED);
		}
	}

	/*
	 * This procedure must occur when a coordination is being failed or ended.
	 */
	void terminate(Coordination coordination, List<Participant> participants) {
		// A coordination has been terminated and needs to be removed from the thread local stack.
		synchronized (this) {
			synchronized (CoordinatorImpl.class) {
				this.coordinations.remove(coordination);
				idToCoordination.remove(new Long(coordination.getId()));
				participantToCoordination.keySet().removeAll(participants);
			}
		}
	}
}
