/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.internal.container.EquinoxReentrantLock;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.resolver.ResolutionException;

/**
 * A module represents a set of revisions installed in a
 * module {@link ModuleContainer container}.
 * @since 3.10
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
		 * lazy start trigger class load.
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
	 * An enumeration of persistent settings for a module
	 */
	public static enum Settings {
		/**
		 * The module has been set to auto start.
		 */
		AUTO_START,
		/**
		 * The module has been set to use its activation policy.
		 */
		USE_ACTIVATION_POLICY
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
	final EquinoxReentrantLock stateChangeLock = new EquinoxReentrantLock();
	private final EnumSet<ModuleEvent> stateTransitionEvents = EnumSet.noneOf(ModuleEvent.class);
	private final EnumSet<Settings> settings;
	private final ThreadLocal<Boolean> inStartResolve = new ThreadLocal<Boolean>();
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
		this.settings = settings == null ? EnumSet.noneOf(Settings.class) : EnumSet.copyOf(settings);
		this.startlevel = startlevel;
	}

	/**
	 * Returns the module id.
	 * @return the module id.
	 */
	public final Long getId() {
		return id;
	}

	/** Returns the module location
	 * @return the module location
	 */
	public final String getLocation() {
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
	 * Returns the module container this module is contained in.
	 * @return the module container.
	 */
	public final ModuleContainer getContainer() {
		return revisions.getContainer();
	}

	/**
	 * Returns the current {@link ModuleRevision revision} associated with this module.
	 * If the module is uninstalled then the last current revision is returned.
	 * @return the current {@link ModuleRevision revision} associated with this module.
	 */
	public final ModuleRevision getCurrentRevision() {
		return revisions.getCurrentRevision();
	}

	/**
	 * Returns the current {@link State state} of this module.
	 * @return the current state of this module.
	 */
	public final State getState() {
		return state;
	}

	final void setState(State state) {
		this.state = state;
	}

	@Override
	public final int getStartLevel() {
		checkValid();
		return this.startlevel;
	}

	@Override
	public final void setStartLevel(int startLevel) {
		revisions.getContainer().setStartLevel(this, startLevel);
	}

	@Override
	public final boolean isPersistentlyStarted() {
		checkValid();
		return settings.contains(Settings.AUTO_START);
	}

	@Override
	public final boolean isActivationPolicyUsed() {
		checkValid();
		return settings.contains(Settings.USE_ACTIVATION_POLICY);
	}

	final void storeStartLevel(int newStartLevel) {
		this.startlevel = newStartLevel;
	}

	/**
	 * Returns the time when this module was last modified.  A module is considered
	 * to be modified when it is installed, updated or uninstalled.
	 * <p>
	 * The time value is a the number of milliseconds since January 1, 1970, 00:00:00 UTC.
	 * @return the time when this bundle was last modified.
	 */
	public final long getLastModified() {
		return this.lastModified;
	}

	final void setlastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	private static final EnumSet<ModuleEvent> VALID_RESOLVED_TRANSITION = EnumSet.of(ModuleEvent.STARTED);
	private static final EnumSet<ModuleEvent> VALID_STOPPED_TRANSITION = EnumSet.of(ModuleEvent.UPDATED, ModuleEvent.UNRESOLVED, ModuleEvent.UNINSTALLED);

	/**
	 * Acquires the module lock for state changes by the current thread for the specified
	 * transition event.  Certain transition events locks may be nested within other
	 * transition event locks.  For example, a resolved transition event lock may be
	 * nested within a started transition event lock.  A stopped transition lock
	 * may be nested within an updated, unresolved or uninstalled transition lock.
	 * @param transitionEvent the transition event to acquire the lock for.
	 * @throws BundleException
	 */
	protected final void lockStateChange(ModuleEvent transitionEvent) throws BundleException {
		boolean previousInterruption = Thread.interrupted();
		boolean invalid = false;
		try {
			boolean acquired = stateChangeLock.tryLock(revisions.getContainer().getModuleLockTimeout(), TimeUnit.SECONDS);
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
					invalid = true;
					stateChangeLock.unlock();
				} else {
					stateTransitionEvents.add(transitionEvent);
					return;
				}
			}
			throw new BundleException(Msg.Module_LockError + toString() + " " + transitionEvent + " " + stateTransitionEvents + (invalid ? " invalid" : ""), BundleException.STATECHANGE_ERROR); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BundleException(Msg.Module_LockError + toString() + " " + transitionEvent, BundleException.STATECHANGE_ERROR, e); //$NON-NLS-1$
		} finally {
			if (previousInterruption) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Releases the lock for state changes for the specified transition event.
	 * @param transitionEvent
	 */
	protected final void unlockStateChange(ModuleEvent transitionEvent) {
		if (stateChangeLock.getHoldCount() == 0 || !stateTransitionEvents.contains(transitionEvent))
			throw new IllegalMonitorStateException("Current thread does not hold the state change lock for: " + transitionEvent); //$NON-NLS-1$
		stateTransitionEvents.remove(transitionEvent);
		stateChangeLock.unlock();
	}

	/**
	 * Returns true if the current thread holds the state change lock for the specified transition event.
	 * @param transitionEvent
	 * @return true if the current thread holds the state change lock for the specified transition event.
	 */
	public final boolean holdsTransitionEventLock(ModuleEvent transitionEvent) {
		return stateChangeLock.getHoldCount() > 0 && stateTransitionEvents.contains(transitionEvent);
	}

	/**
	 * Returns the thread that currently owns the state change lock for this module, or 
	 * <code>null</code> if not owned.
	 * @return the owner, or <code>null</code> if not owned.
	 */
	public final Thread getStateChangeOwner() {
		return stateChangeLock.getOwner();
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
		ModuleEvent event;
		if (StartOptions.LAZY_TRIGGER.isContained(options)) {
			setTrigger();
			if (stateChangeLock.getHoldCount() > 0 && stateTransitionEvents.contains(ModuleEvent.STARTED)) {
				// nothing to do here; the current thread is activating the bundle.
				return;
			}
		}
		BundleException startError = null;
		boolean lockedStarted = false;
		lockStateChange(ModuleEvent.STARTED);
		try {
			lockedStarted = true;
			checkValid();
			if (StartOptions.TRANSIENT_IF_AUTO_START.isContained(options) && !settings.contains(Settings.AUTO_START)) {
				// Do nothing
				return;
			}
			checkFragment();
			persistStartOptions(options);
			if (getStartLevel() > getRevisions().getContainer().getStartLevel()) {
				if (StartOptions.TRANSIENT.isContained(options)) {
					throw new BundleException(Msg.Module_Transient_StartError, BundleException.START_TRANSIENT_ERROR);
				}
				// DO nothing
				return;
			}
			if (State.ACTIVE.equals(getState()))
				return;
			if (getState().equals(State.INSTALLED)) {
				// must unlock to avoid out of order locks when multiple unresolved
				// bundles are started at the same time from different threads
				unlockStateChange(ModuleEvent.STARTED);
				lockedStarted = false;
				ResolutionReport report;
				try {
					inStartResolve.set(Boolean.TRUE);
					report = getRevisions().getContainer().resolve(Arrays.asList(this), true);
				} finally {
					inStartResolve.set(Boolean.FALSE);
					lockStateChange(ModuleEvent.STARTED);
					lockedStarted = true;
				}
				// need to check valid again in case someone uninstalled the bundle
				checkValid();
				ResolutionException e = report.getResolutionException();
				if (e != null) {
					if (e.getCause() instanceof BundleException) {
						throw (BundleException) e.getCause();
					}
				}
				if (State.ACTIVE.equals(getState()))
					return;
				if (getState().equals(State.INSTALLED)) {
					String reportMessage = report.getResolutionReportMessage(getCurrentRevision());
					throw new BundleException(Msg.Module_ResolveError + reportMessage, BundleException.RESOLVE_ERROR);
				}
			}

			try {
				event = doStart(options);
			} catch (BundleException e) {
				// must return state to resolved
				setState(State.RESOLVED);
				startError = e;
				// must always publish the STOPPED event on error
				event = ModuleEvent.STOPPED;
			}
		} finally {
			inStartResolve.set(Boolean.FALSE);
			if (lockedStarted) {
				unlockStateChange(ModuleEvent.STARTED);
			}
		}

		if (event != null) {
			if (!EnumSet.of(ModuleEvent.STARTED, ModuleEvent.LAZY_ACTIVATION, ModuleEvent.STOPPED).contains(event))
				throw new IllegalStateException("Wrong event type: " + event); //$NON-NLS-1$
			publishEvent(event);
		}

		if (startError != null) {
			throw startError;
		}
	}

	final void publishEvent(ModuleEvent type) {
		revisions.getContainer().getAdaptor().publishModuleEvent(type, this, this);
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
		ModuleEvent event;
		BundleException stopError = null;
		lockStateChange(ModuleEvent.STOPPED);
		try {
			checkValid();
			checkFragment();
			persistStopOptions(options);
			if (!Module.ACTIVE_SET.contains(getState()))
				return;
			try {
				event = doStop();
			} catch (BundleException e) {
				stopError = e;
				// must always publish the STOPPED event
				event = ModuleEvent.STOPPED;
			}
		} finally {
			unlockStateChange(ModuleEvent.STOPPED);
		}

		if (event != null) {
			if (!ModuleEvent.STOPPED.equals(event))
				throw new IllegalStateException("Wrong event type: " + event); //$NON-NLS-1$
			publishEvent(event);
		}
		if (stopError != null)
			throw stopError;
	}

	private void checkFragment() throws BundleException {
		ModuleRevision current = getCurrentRevision();
		if ((current.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
			throw new BundleException(Msg.Module_Fragment_InvalidOperation, BundleException.INVALID_OPERATION);
		}
	}

	@Override
	public final int compareTo(Module o) {
		int slcomp = this.startlevel - o.startlevel;
		if (slcomp != 0) {
			return slcomp;
		}
		long idcomp = getId() - o.getId();
		return (idcomp < 0L) ? -1 : ((idcomp > 0L) ? 1 : 0);
	}

	final void checkValid() {
		if (getState().equals(State.UNINSTALLED))
			throw new IllegalStateException(Msg.Module_UninstalledError);
	}

	private ModuleEvent doStart(StartOptions... options) throws BundleException {
		boolean isLazyTrigger = StartOptions.LAZY_TRIGGER.isContained(options);
		if (isLazyTrigger) {
			if (!State.LAZY_STARTING.equals(getState())) {
				// need to make sure we transition through the lazy starting state
				setState(State.LAZY_STARTING);
				// need to publish the lazy event
				unlockStateChange(ModuleEvent.STARTED);
				try {
					publishEvent(ModuleEvent.LAZY_ACTIVATION);
				} finally {
					lockStateChange(ModuleEvent.STARTED);
				}
				if (State.ACTIVE.equals(getState())) {
					// A sync listener must have caused the bundle to activate
					return null;
				}
				// continue on to normal starting
			}
			if (getContainer().DEBUG_MONITOR_LAZY) {
				Debug.printStackTrace(new Exception("Module is being lazy activated: " + this)); //$NON-NLS-1$
			}
		} else {
			if (isLazyActivate(options) && !isTriggerSet()) {
				if (State.LAZY_STARTING.equals(getState())) {
					// a sync listener must have tried to start this module again with the lazy option
					return null; // no event to publish; nothing to do
				}
				// set the lazy starting state and return lazy activation event for firing
				setState(State.LAZY_STARTING);
				return ModuleEvent.LAZY_ACTIVATION;
			}
		}

		// time to actual start the module
		if (!State.STARTING.equals(getState())) {
			// TODO this starting state check should not be needed
			// but we do it because of the way the system module init works
			setState(State.STARTING);
			publishEvent(ModuleEvent.STARTING);
		}
		try {
			startWorker();
			setState(State.ACTIVE);
			return ModuleEvent.STARTED;
		} catch (Throwable t) {
			// must fire stopping event
			setState(State.STOPPING);
			publishEvent(ModuleEvent.STOPPING);
			if (t instanceof BundleException)
				throw (BundleException) t;
			throw new BundleException(Msg.Module_StartError, BundleException.ACTIVATOR_ERROR, t);
		}
	}

	private void setTrigger() {
		ModuleLoader loader = getCurrentLoader();
		if (loader != null) {
			loader.getAndSetTrigger();
		}
	}

	private boolean isTriggerSet() {
		ModuleLoader loader = getCurrentLoader();
		return loader == null ? false : loader.isTriggerSet();
	}

	private ModuleLoader getCurrentLoader() {
		ModuleRevision current = getCurrentRevision();
		if (current == null) {
			return null;
		}
		ModuleWiring wiring = current.getWiring();
		if (wiring == null) {
			return null;
		}
		try {
			return wiring.getModuleLoader();
		} catch (UnsupportedOperationException e) {
			// just ignore and return null;
			return null;
		}
	}

	/**
	 * Performs any work associated with starting a module.  For example,
	 * loading and calling start on an activator.
	 * @throws BundleException if there was an exception starting the module
	 */
	protected void startWorker() throws BundleException {
		// Do nothing
	}

	private ModuleEvent doStop() throws BundleException {
		setState(State.STOPPING);
		publishEvent(ModuleEvent.STOPPING);
		try {
			stopWorker();
			return ModuleEvent.STOPPED;
		} catch (Throwable t) {
			if (t instanceof BundleException)
				throw (BundleException) t;
			throw new BundleException(Msg.Module_StopError, BundleException.ACTIVATOR_ERROR, t);
		} finally {
			// must always set the state to stopped
			setState(State.RESOLVED);
		}
	}

	/**
	 * Performs any work associated with stopping a module.  For example,
	 * calling stop on an activator.
	 * @throws BundleException if there was an exception stopping the module
	 */
	protected void stopWorker() throws BundleException {
		// Do nothing
	}

	@Override
	public String toString() {
		return getCurrentRevision() + " [id=" + id + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void persistStartOptions(StartOptions... options) {
		if (StartOptions.TRANSIENT.isContained(options) || StartOptions.TRANSIENT_RESUME.isContained(options) || StartOptions.LAZY_TRIGGER.isContained(options)) {
			return;
		}

		if (StartOptions.USE_ACTIVATION_POLICY.isContained(options)) {
			settings.add(Settings.USE_ACTIVATION_POLICY);
		} else {
			settings.remove(Settings.USE_ACTIVATION_POLICY);
		}
		settings.add(Settings.AUTO_START);
		revisions.getContainer().moduleDatabase.persistSettings(settings, this);
	}

	private void persistStopOptions(StopOptions... options) {
		if (StopOptions.TRANSIENT.isContained(options))
			return;
		settings.clear();
		revisions.getContainer().moduleDatabase.persistSettings(settings, this);
	}

	/**
	 * The container is done with the revision and it has been completely removed.
	 * This method allows the resources behind the revision to be cleaned up.
	 * @param revision the revision to clean up
	 */
	abstract protected void cleanup(ModuleRevision revision);

	final boolean isLazyActivate(StartOptions... options) {
		if (StartOptions.TRANSIENT.isContained(options)) {
			if (!StartOptions.USE_ACTIVATION_POLICY.isContained(options)) {
				return false;
			}
		} else if (!settings.contains(Settings.USE_ACTIVATION_POLICY)) {
			return false;
		}
		return hasLazyActivatePolicy();
	}

	final boolean hasLazyActivatePolicy() {
		ModuleRevision current = getCurrentRevision();
		return current == null ? false : current.hasLazyActivatePolicy();
	}

	final boolean inStartResolve() {
		Boolean value = inStartResolve.get();
		if (value == null) {
			return false;
		}
		return value.booleanValue();
	}
}
