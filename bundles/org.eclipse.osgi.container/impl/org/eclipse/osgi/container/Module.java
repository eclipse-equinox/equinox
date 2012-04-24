/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.service.resolver.ResolutionException;

/**
 * A module represents a set of revisions installed in a
 * module {@link ModuleContainer container}.
 */
public abstract class Module implements BundleReference {
	public static enum START_OPTIONS {
		TRANSIENT, ACTIVATION_POLICY, LAZY_TRIGGER
	}

	public static enum STOP_OPTIONS {
		TRANSIENT
	}

	public static enum State {
		/**
		 * 
		 */
		INSTALLED,
		/**
		 * 
		 */
		RESOLVED,
		/**
		 * 
		 */
		LAZY_STARTING,
		/**
		 * 
		 */
		STARTING,
		/**
		 * 
		 */
		ACTIVE,
		/**
		 * 
		 */
		STOPPING,
		/**
		 * 
		 */
		UNINSTALLED
	}

	public static final EnumSet<State> ACTIVE_SET = EnumSet.of(State.STARTING, State.LAZY_STARTING, State.ACTIVE, State.STOPPING);
	public static final EnumSet<State> RESOLVED_SET = EnumSet.of(State.RESOLVED, State.STARTING, State.LAZY_STARTING, State.ACTIVE, State.STOPPING);

	public static enum Event {
		/**
		 * 
		 */
		INSTALLED,
		/**
		 * 
		 */
		LAZY_ACTIVATION,
		/**
		 * 
		 */
		RESOLVED,
		/**
		 * 
		 */
		STARTED,
		/**
		 * 
		 */
		STARTING,
		/**
		 * 
		 */
		STOPPED,
		/**
		 * 
		 */
		STOPPING,
		/**
		 * 
		 */
		UNINSTALLED,
		/**
		 * 
		 */
		UNRESOLVED,
		/**
		 * 
		 */
		UPDATED
	}

	private final Long id;
	private final String location;
	private final ModuleRevisions revisions;
	private final ReentrantLock stateChangeLock = new ReentrantLock();
	private final EnumSet<Event> stateTransitionEvents = EnumSet.noneOf(Event.class);
	private volatile State state = State.INSTALLED;

	public Module(Long id, String location, ModuleContainer container) {
		this.id = id;
		this.location = location;
		this.revisions = new ModuleRevisions(this, container);
	}

	/**
	 * Returns the module id.
	 * @return the module id.
	 */
	public Long getId() {
		return id;
	}

	/** Returns the module location
	 * @return the module location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Returns the {@link ModuleRevisions} associated with this module.
	 * @return the {@link ModuleRevisions} associated with this module
	 */
	public final ModuleRevisions getRevisions() {
		return revisions;
	}

	/**
	 * Returns the current {@link ModuleRevision revision} associated with this module.
	 * @return the current {@link ModuleRevision revision} associated with this module.
	 */
	public final ModuleRevision getCurrentRevision() {
		List<ModuleRevision> revisionList = revisions.getModuleRevisions();
		return revisionList.isEmpty() || revisions.isUninstalled() ? null : revisionList.get(0);
	}

	public State getState() {
		return state;
	}

	void setState(State state) {
		this.state = state;
	}

	private static final EnumSet<Event> VALID_RESOLVED_TRANSITION = EnumSet.of(Event.STARTED);
	private static final EnumSet<Event> VALID_STOPPED_TRANSITION = EnumSet.of(Event.UPDATED, Event.UNRESOLVED, Event.UNINSTALLED);

	protected void lockStateChange(Event transitionEvent) throws BundleException {
		try {
			boolean acquired = stateChangeLock.tryLock(5, TimeUnit.SECONDS);
			if (acquired) {
				boolean isValidTransition = true;
				switch (transitionEvent) {
					case STARTED :
					case UPDATED :
					case UNINSTALLED :
					case UNRESOLVED :
						// These states must be initiating transition states
						// no other transition state is allowed when these are kicked off
						isValidTransition = stateTransitionEvents.isEmpty();
						break;
					case RESOLVED :
						isValidTransition = VALID_RESOLVED_TRANSITION.containsAll(stateTransitionEvents);
						break;
					case STOPPED :
						isValidTransition = VALID_STOPPED_TRANSITION.containsAll(stateTransitionEvents);
						break;
					default :
						isValidTransition = false;
						break;
				}
				if (!isValidTransition) {
					stateChangeLock.unlock();
				} else {
					stateTransitionEvents.add(transitionEvent);
					return;
				}
			}
			throw new BundleException("Unable to acquire the state change lock for the module: " + transitionEvent, BundleException.STATECHANGE_ERROR);
		} catch (InterruptedException e) {
			throw new BundleException("Unable to acquire the state change lock for the module.", BundleException.STATECHANGE_ERROR, e);
		}
	}

	protected void unlockStateChange(Event transitionEvent) {
		if (stateChangeLock.getHoldCount() == 0 || !stateTransitionEvents.contains(transitionEvent))
			throw new IllegalMonitorStateException("Current thread does not hold the state change lock for: " + transitionEvent);
		stateTransitionEvents.remove(transitionEvent);
		stateChangeLock.unlock();
	}

	public void start(EnumSet<START_OPTIONS> options) throws BundleException {
		if (options == null)
			options = EnumSet.noneOf(START_OPTIONS.class);
		if (options.contains(START_OPTIONS.LAZY_TRIGGER) && !options.contains(START_OPTIONS.TRANSIENT))
			throw new IllegalArgumentException("Cannot use lazy trigger option without the transient option.");
		Event event;
		lockStateChange(Event.STARTED);
		try {
			checkValid();
			// TODO need a check to see if the current revision is valid for start (e.g. is fragment).
			if (!options.contains(START_OPTIONS.TRANSIENT)) {
				persistStartOptions(options);
			}
			if (State.ACTIVE.equals(getState()))
				return;
			if (getState().equals(State.INSTALLED)) {
				try {
					getRevisions().getContainer().resolve(Arrays.asList(this), true);
				} catch (ResolutionException e) {
					throw new BundleException("Could not resolve module.", BundleException.RESOLVE_ERROR, e);
				}
			}
			if (getState().equals(State.INSTALLED)) {
				throw new BundleException("Could not resolve module.", BundleException.RESOLVE_ERROR);
			}
			event = doStart(options);
		} finally {
			unlockStateChange(Event.STARTED);
		}

		if (event != null) {
			if (!EnumSet.of(Event.STARTED, Event.LAZY_ACTIVATION, Event.STOPPED).contains(event))
				throw new IllegalStateException("Wrong event type: " + event);
			fireEvent(event);
		}
	}

	public void stop(EnumSet<STOP_OPTIONS> options) throws BundleException {
		if (options == null)
			options = EnumSet.noneOf(STOP_OPTIONS.class);
		Event event;
		BundleException stopError = null;
		lockStateChange(Event.STOPPED);
		try {
			checkValid();
			if (!options.contains(STOP_OPTIONS.TRANSIENT)) {
				persistStopOptions(options);
			}
			if (!Module.ACTIVE_SET.contains(getState()))
				return;
			try {
				event = doStop(options);
			} catch (BundleException e) {
				stopError = e;
				// must always fire the STOPPED event
				event = Event.STOPPED;
			}
		} finally {
			unlockStateChange(Event.STOPPED);
		}

		if (event != null) {
			if (!Event.STOPPED.equals(event))
				throw new IllegalStateException("Wrong event type: " + event);
			fireEvent(event);
		}
		if (stopError != null)
			throw stopError;
	}

	private void checkValid() {
		if (getState().equals(State.UNINSTALLED))
			throw new IllegalStateException("Module has been uninstalled.");
	}

	private Event doStart(EnumSet<START_OPTIONS> options) throws BundleException {
		if (isLazyActivate(options)) {
			setState(State.LAZY_STARTING);
			return Event.LAZY_ACTIVATION;
		}
		setState(State.STARTING);
		fireEvent(Event.STARTING);
		try {
			startWorker(options);
			setState(State.ACTIVE);
			return Event.STARTED;
		} catch (Throwable t) {
			if (t instanceof BundleException)
				throw (BundleException) t;
			throw new BundleException("Error starting module.", BundleException.ACTIVATOR_ERROR, t);
		}
	}

	protected void startWorker(EnumSet<START_OPTIONS> options) {
		// do nothing
	}

	private Event doStop(EnumSet<STOP_OPTIONS> options) throws BundleException {
		setState(State.STOPPING);
		fireEvent(Event.STOPPING);
		try {
			stopWorker(options);
			return Event.STOPPED;
		} catch (Throwable t) {
			if (t instanceof BundleException)
				throw (BundleException) t;
			throw new BundleException("Error stopping module.", BundleException.ACTIVATOR_ERROR, t);
		} finally {
			// must always set the state to stopped
			setState(State.RESOLVED);
		}
	}

	/**
	 * @throws BundleException  
	 */
	protected void stopWorker(EnumSet<STOP_OPTIONS> options) throws BundleException {
		// do nothing
	}

	/**
	 * @throws BundleException  
	 */
	protected void updateWorker(ModuleRevisionBuilder builder) throws BundleException {
		// do nothing
	}

	public String toString() {
		return "[id=" + id + "]";
	}

	abstract protected void fireEvent(Event event);

	abstract protected void persistStartOptions(EnumSet<START_OPTIONS> options);

	abstract protected void persistStopOptions(EnumSet<STOP_OPTIONS> options);

	abstract protected void cleanup(ModuleRevision revision);

	abstract protected boolean isLazyActivate(EnumSet<START_OPTIONS> options);
}
