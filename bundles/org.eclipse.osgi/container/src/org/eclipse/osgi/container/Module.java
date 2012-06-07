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
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.resource.Capability;
import org.osgi.service.resolver.ResolutionException;

/**
 * A module represents a set of revisions installed in a
 * module {@link ModuleContainer container}.
 */
public abstract class Module implements BundleReference, BundleStartLevel, Comparable<Module> {
	/**
	 * The possible start options for a module
	 */
	public static enum StartOptions {
		/**
		 * The module start operation is transient and the persistent 
		 * autostart or activation policy setting of the module is not modified.
		 */
		TRANSIENT,
		/**
		 * The module start operation must activate the module according to the module's declared
		 * activation policy.
		 */
		USE_ACTIVATION_POLICY,
		/**
		 * The module start operation is transient and the persistent activation policy
		 * setting will be used.
		 */
		TRANSIENT_RESUME,
		/**
		 * The module start operation is transient and will only happen if {@link Settings#AUTO_START auto start}
		 * setting is persistent.
		 */
		TRANSIENT_IF_AUTO_START,
		/**
		 * The module start operation that indicates the module is being started because of a
		 * lazy start trigger class load.  This option must be used with the 
		 * {@link StartOptions#TRANSIENT transient} options.
		 */
		LAZY_TRIGGER;

		/**
		 * Tests if this option is contained in the specified options
		 */
		public boolean isContained(StartOptions... options) {
			for (StartOptions option : options) {
				if (equals(option)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * The possible start options for a module
	 */
	public static enum StopOptions {
		/**
		 * The module stop operation is transient and the persistent 
		 * autostart setting of the module is not modified.
		 */
		TRANSIENT;

		/**
		 * Tests if this option is contained in the specified options
		 */
		public boolean isContained(StopOptions... options) {
			for (StopOptions option : options) {
				if (equals(option)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * An enumeration of the possible {@link Module#getState() states} a module may be in.
	 */
	public static enum State {
		/**
		 * The module is installed but not yet resolved.
		 */
		INSTALLED,
		/**
		 * The module is resolved and able to be started.
		 */
		RESOLVED,
		/**
		 * The module is waiting for a {@link StartOptions#LAZY_TRIGGER trigger}
		 * class load to proceed with starting.
		 */
		LAZY_STARTING,
		/**
		 * The module is in the process of starting.
		 */
		STARTING,
		/**
		 * The module is now running.
		 */
		ACTIVE,
		/**
		 * The module is in the process of stopping
		 */
		STOPPING,
		/**
		 * The module is uninstalled and may not be used.
		 */
		UNINSTALLED
	}

	/**
	 * Event types that may be {@link Module#publishEvent(Event) published} for a module
	 * indicating a {@link Module#getState() state} change has occurred for a module.
	 */
	public static enum Event {
		/**
		 * The module has been installed
		 */
		INSTALLED,
		/**
		 * The module has been activated with the lazy activation policy and
		 * is waiting a {@link StartOptions#LAZY_TRIGGER trigger} class load.
		 */
		LAZY_ACTIVATION,
		/**
		 * The module has been resolved.
		 */
		RESOLVED,
		/**
		 * The module has beens started.
		 */
		STARTED,
		/**
		 * The module is about to be activated.
		 */
		STARTING,
		/**
		 * The module has been stopped.
		 */
		STOPPED,
		/**
		 * The module is about to be deactivated.
		 */
		STOPPING,
		/**
		 * The module has been uninstalled.
		 */
		UNINSTALLED,
		/**
		 * The module has been unresolved.
		 */
		UNRESOLVED,
		/**
		 * The module has been updated.
		 */
		UPDATED
	}

	/**
	 * An enumeration of persistent settings for a module
	 */
	public static enum Settings {
		/**
		 * The module has been set to auto start.
		 */
		AUTO_START,
		/**
		 * The module has been set to use its activation policy
		 */
		USE_ACTIVATION_POLICY,
	}

	/**
	 * A set of {@link State states} that indicate a module is active.
	 */
	public static final EnumSet<State> ACTIVE_SET = EnumSet.of(State.STARTING, State.LAZY_STARTING, State.ACTIVE, State.STOPPING);
	/**
	 * A set of {@link State states} that indicate a module is resolved.
	 */
	public static final EnumSet<State> RESOLVED_SET = EnumSet.of(State.RESOLVED, State.STARTING, State.LAZY_STARTING, State.ACTIVE, State.STOPPING);

	private final Long id;
	private final String location;
	private final ModuleRevisions revisions;
	private final ReentrantLock stateChangeLock = new ReentrantLock();
	private final EnumSet<Event> stateTransitionEvents = EnumSet.noneOf(Event.class);
	private final EnumSet<Settings> settings;
	private volatile State state = State.INSTALLED;
	private volatile int startlevel;
	private volatile long lastModified;

	/**
	 * Constructs a new module with the specified id, location and
	 * container.
	 * @param id the new module id
	 * @param location the new module location
	 * @param container the container for the new module
	 * @param settings the persisted settings.  May be {@code null} if there are no settings.
	 * @param startlevel the persisted start level or initial start level.
	 */
	public Module(Long id, String location, ModuleContainer container, EnumSet<Settings> settings, int startlevel) {
		this.id = id;
		this.location = location;
		this.revisions = new ModuleRevisions(this, container);
		this.settings = settings == null ? EnumSet.noneOf(Settings.class) : settings;
		this.startlevel = startlevel;
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
	 * If the module is uninstalled then {@code null} is returned.
	 * @return the current {@link ModuleRevision revision} associated with this module.
	 */
	public final ModuleRevision getCurrentRevision() {
		return revisions.getCurrentRevision();
	}

	/**
	 * Returns the current {@link State state} of this module.
	 * @return the current state of this module.
	 */
	public State getState() {
		return state;
	}

	void setState(State state) {
		this.state = state;
	}

	@Override
	public int getStartLevel() {
		return this.startlevel;
	}

	@Override
	public void setStartLevel(int startLevel) {
		revisions.getContainer().setStartLevel(this, startLevel);
	}

	@Override
	public boolean isPersistentlyStarted() {
		return settings.contains(Settings.AUTO_START);
	}

	@Override
	public boolean isActivationPolicyUsed() {
		return settings.contains(Settings.USE_ACTIVATION_POLICY);
	}

	void storeStartLevel(int newStartLevel) {
		this.startlevel = newStartLevel;
	}

	/**
	 * Returns the time when this module was last modified.  A module is considered
	 * to be modified when it is installed, updated or uninstalled.
	 * <p>
	 * The time value is a the number of milliseconds since January 1, 1970, 00:00:00 UTC.
	 * @return the time when this bundle was last modified.
	 */
	public long getLastModified() {
		return this.lastModified;
	}

	void setlastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	private static final EnumSet<Event> VALID_RESOLVED_TRANSITION = EnumSet.of(Event.STARTED);
	private static final EnumSet<Event> VALID_STOPPED_TRANSITION = EnumSet.of(Event.UPDATED, Event.UNRESOLVED, Event.UNINSTALLED);

	/**
	 * Acquires the module lock for state changes by the current thread for the specified
	 * transition event.  Certain transition events locks may be nested within other
	 * transition event locks.  For example, a resolved transition event lock may be
	 * nested within a started transition event lock.  A stopped transition lock
	 * may be nested within an updated, unresolved or uninstalled transition lock.
	 * @param transitionEvent the transition event to acquire the lock for.
	 * @throws BundleException
	 */
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

	/**
	 * Releases the lock for state changes for the specified transition event.
	 * @param transitionEvent
	 */
	protected void unlockStateChange(Event transitionEvent) {
		if (stateChangeLock.getHoldCount() == 0 || !stateTransitionEvents.contains(transitionEvent))
			throw new IllegalMonitorStateException("Current thread does not hold the state change lock for: " + transitionEvent);
		stateTransitionEvents.remove(transitionEvent);
		stateChangeLock.unlock();
	}

	/**
	 * Returns true if the current thread holds the state change lock for the specified transition event.
	 * @param transitionEvent
	 * @return true if the current thread holds the state change lock for the specified transition event.
	 */
	protected boolean holdsTransitionEventLock(Event transitionEvent) {
		return stateChangeLock.getHoldCount() > 0 && stateTransitionEvents.contains(transitionEvent);
	}

	/**
	 * Starts this module
	 * @param options the options for starting
	 * @throws BundleException if an errors occurs while starting
	 */
	public void start(StartOptions... options) throws BundleException {
		revisions.getContainer().checkAdminPermission(getBundle(), AdminPermission.EXECUTE);
		if (options == null) {
			options = new StartOptions[0];
		}
		Event event;
		if (StartOptions.LAZY_TRIGGER.isContained(options)) {
			if (stateChangeLock.getHoldCount() > 0 && stateTransitionEvents.contains(Event.STARTED)) {
				// nothing to do here; the current thread is activating the bundle.
			}
		}
		BundleException startError = null;
		lockStateChange(Event.STARTED);
		try {
			checkValid();
			if (StartOptions.TRANSIENT_IF_AUTO_START.isContained(options) && !settings.contains(Settings.AUTO_START)) {
				// Do nothing
				return;
			}
			persistStartOptions(options);
			if (getStartLevel() > getRevisions().getContainer().getStartLevel()) {
				if (StartOptions.TRANSIENT.isContained(options)) {
					throw new BundleException("Cannot transiently start a module whose start level is not met.", BundleException.START_TRANSIENT_ERROR);
				}
				// DO nothing
				return;
			}
			// TODO need a check to see if the current revision is valid for start (e.g. is fragment).
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
			try {
				event = doStart(options);
			} catch (BundleException e) {
				// must return state to resolved
				setState(State.RESOLVED);
				startError = e;
				// must always publish the STOPPED event on error
				event = Event.STOPPED;
			}
		} finally {
			unlockStateChange(Event.STARTED);
		}

		if (event != null) {
			if (!EnumSet.of(Event.STARTED, Event.LAZY_ACTIVATION, Event.STOPPED).contains(event))
				throw new IllegalStateException("Wrong event type: " + event);
			publishEvent(event);
		}

		if (startError != null) {
			throw startError;
		}
	}

	/**
	 * Stops this module.
	 * @param options options for stopping
	 * @throws BundleException if an error occurs while stopping
	 */
	public void stop(StopOptions... options) throws BundleException {
		revisions.getContainer().checkAdminPermission(getBundle(), AdminPermission.EXECUTE);
		if (options == null)
			options = new StopOptions[0];
		Event event;
		BundleException stopError = null;
		lockStateChange(Event.STOPPED);
		try {
			checkValid();
			persistStopOptions(options);
			if (!Module.ACTIVE_SET.contains(getState()))
				return;
			try {
				event = doStop();
			} catch (BundleException e) {
				stopError = e;
				// must always publish the STOPPED event
				event = Event.STOPPED;
			}
		} finally {
			unlockStateChange(Event.STOPPED);
		}

		if (event != null) {
			if (!Event.STOPPED.equals(event))
				throw new IllegalStateException("Wrong event type: " + event);
			publishEvent(event);
		}
		if (stopError != null)
			throw stopError;
	}

	@Override
	public int compareTo(Module o) {
		int slcomp = getStartLevel() - o.getStartLevel();
		if (slcomp != 0) {
			return slcomp;
		}
		long idcomp = getId() - o.getId();
		return (idcomp < 0L) ? -1 : ((idcomp > 0L) ? 1 : 0);
	}

	void checkValid() {
		if (getState().equals(State.UNINSTALLED))
			throw new IllegalStateException("Module has been uninstalled.");
	}

	private Event doStart(StartOptions... options) throws BundleException {
		boolean isLazyTrigger = StartOptions.LAZY_TRIGGER.isContained(options);
		if (isLazyTrigger) {
			if (!State.LAZY_STARTING.equals(getState())) {
				// need to make sure we transition through the lazy starting state
				setState(State.LAZY_STARTING);
				// need to publish the lazy event
				unlockStateChange(Event.STARTED);
				try {
					publishEvent(Event.LAZY_ACTIVATION);
				} finally {
					lockStateChange(Event.STARTED);
				}
				if (State.ACTIVE.equals(getState())) {
					// A sync listener must have caused the bundle to activate
					return null;
				}
				// continue on to normal starting
			}
		} else {
			if (settings.contains(Settings.USE_ACTIVATION_POLICY) && isLazyActivate()) {
				if (State.LAZY_STARTING.equals(getState())) {
					// a sync listener must have tried to start this module again with the lazy option
					return null; // no event to publish; nothing to do
				}
				// set the lazy starting state and return lazy activation event for firing
				setState(State.LAZY_STARTING);
				return Event.LAZY_ACTIVATION;
			}
		}

		// time to actual start the module
		if (!State.STARTING.equals(getState())) {
			// TODO this starting state check should not be needed
			// but we do it because of the way the system module init works
			setState(State.STARTING);
			publishEvent(Event.STARTING);
		}
		try {
			startWorker();
			setState(State.ACTIVE);
			return Event.STARTED;
		} catch (Throwable t) {
			// must fire stopping event
			setState(State.STOPPING);
			publishEvent(Event.STOPPING);
			if (t instanceof BundleException)
				throw (BundleException) t;
			throw new BundleException("Error starting module.", BundleException.ACTIVATOR_ERROR, t);
		}
	}

	/**
	 * Performs any work associated with starting a module.  For example,
	 * loading and calling start on an activator.
	 * @throws BundleException 
	 */
	protected void startWorker() throws BundleException {
		// Do nothing
	}

	private Event doStop() throws BundleException {
		setState(State.STOPPING);
		publishEvent(Event.STOPPING);
		try {
			stopWorker();
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
	protected void stopWorker() throws BundleException {
		// Do nothing
	}

	/**
	 * @throws BundleException  
	 */
	protected void updateWorker(ModuleRevisionBuilder builder) throws BundleException {
		// do nothing
	}

	@Override
	public String toString() {
		return "[id=" + id + "]";
	}

	/**
	 * Publishes the specified event for this module.
	 * @param event the event type to publish
	 */
	abstract protected void publishEvent(Event event);

	private void persistStartOptions(StartOptions... options) {
		if (StartOptions.TRANSIENT_RESUME.isContained(options) || StartOptions.LAZY_TRIGGER.isContained(options)) {
			return;
		}

		// Always set the use acivation policy setting
		if (StartOptions.USE_ACTIVATION_POLICY.isContained(options)) {
			settings.add(Settings.USE_ACTIVATION_POLICY);
		} else {
			settings.remove(Settings.USE_ACTIVATION_POLICY);
		}

		if (StartOptions.TRANSIENT.isContained(options)) {
			return;
		}
		settings.add(Settings.AUTO_START);
		revisions.getContainer().moduleDataBase.persistSettings(settings, this);
	}

	private void persistStopOptions(StopOptions... options) {
		if (StopOptions.TRANSIENT.isContained(options))
			return;
		settings.clear();
		revisions.getContainer().moduleDataBase.persistSettings(settings, this);
	}

	/**
	 * The container is done with the revision and it has been complete removed.
	 * This method allows the resources behind the revision to be cleaned up.
	 * @param revision the revision to clean up
	 */
	abstract protected void cleanup(ModuleRevision revision);

	boolean isLazyActivate() {
		ModuleRevision current = getCurrentRevision();
		if (current == null)
			return false;
		List<Capability> capabilities = current.getCapabilities(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE);
		if (capabilities.isEmpty())
			return false;
		Capability moduleData = capabilities.get(0);
		return EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY_LAZY.equals(moduleData.getAttributes().get(EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY));
	}
}
