/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
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
import org.eclipse.osgi.container.ModuleContainer.ContainerStartLevel;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.service.resolver.ResolutionException;

/**
 * A special kind of module that represents the system module for the container.  Additinal
 * methods are available on the system module for operations that effect the whole container.
 * For example, initializing the container, restarting and waiting for the container to stop.
 * @since 3.10
 */
public abstract class SystemModule extends Module {
	private final Map<Thread, ContainerEvent> forStop = new HashMap<Thread, ContainerEvent>(2);

	public SystemModule(ModuleContainer container) {
		super(new Long(0), Constants.SYSTEM_BUNDLE_LOCATION, container, EnumSet.of(Settings.AUTO_START, Settings.USE_ACTIVATION_POLICY), new Integer(0));
	}

	/**
	 * Initializes the module container
	 * @throws BundleException if an exeption occurred while initializing
	 */
	public final void init() throws BundleException {
		getRevisions().getContainer().checkAdminPermission(getBundle(), AdminPermission.EXECUTE);

		boolean lockedStarted = false;
		// Indicate we are in the middle of a start.
		// This must be incremented before we acquire the STARTED lock the first time.
		inStart.incrementAndGet();
		try {
			lockStateChange(ModuleEvent.STARTED);
			lockedStarted = true;
			getContainer().getAdaptor().initBegin();
			checkValid();
			if (ACTIVE_SET.contains(getState()))
				return;
			getRevisions().getContainer().open();
			if (getState().equals(State.INSTALLED)) {
				// must unlock to avoid out of order locks when multiple unresolved
				// bundles are started at the same time from different threads
				unlockStateChange(ModuleEvent.STARTED);
				lockedStarted = false;
				ResolutionReport report;
				try {
					report = getRevisions().getContainer().resolve(Arrays.asList((Module) this), true);
				} finally {
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
				if (ACTIVE_SET.contains(getState()))
					return;
				if (getState().equals(State.INSTALLED)) {
					String reportMessage = report.getResolutionReportMessage(getCurrentRevision());
					throw new BundleException(Msg.Module_ResolveError + reportMessage, BundleException.RESOLVE_ERROR);
				}
			}

			setState(State.STARTING);
			publishEvent(ModuleEvent.STARTING);
			try {
				initWorker();
			} catch (Throwable t) {
				setState(State.STOPPING);
				publishEvent(ModuleEvent.STOPPING);
				setState(State.RESOLVED);
				publishEvent(ModuleEvent.STOPPED);
				getRevisions().getContainer().close();
				if (t instanceof BundleException) {
					throw (BundleException) t;
				}
				throw new BundleException("Error initializing container.", BundleException.ACTIVATOR_ERROR, t); //$NON-NLS-1$
			}
		} finally {
			getContainer().getAdaptor().initEnd();
			if (lockedStarted) {
				unlockStateChange(ModuleEvent.STARTED);
			}
			inStart.decrementAndGet();
		}
	}

	/**
	 * Waits until the module container has stopped.
	 * @param timeout The amount of time to wait.
	 * @return The container event indicated why the framework stopped
	 * or if there was a time out waiting for stop.
	 * @see Framework#waitForStop(long)
	 * @throws InterruptedException if the thread was interrupted while waiting
	 */
	public ContainerEvent waitForStop(long timeout) throws InterruptedException {
		final boolean waitForever = timeout == 0;
		final long start = System.currentTimeMillis();
		final Thread current = Thread.currentThread();
		long timeLeft = timeout;
		ContainerEvent event = null;
		boolean stateLocked;
		if (timeout == 0) {
			stateChangeLock.lockInterruptibly();
			stateLocked = true;
		} else {
			stateLocked = stateChangeLock.tryLock(timeLeft, TimeUnit.MILLISECONDS);
		}
		if (stateLocked) {
			synchronized (forStop) {
				try {
					if (!Module.ACTIVE_SET.contains(getState())) {
						return ContainerEvent.STOPPED;
					}
					event = forStop.remove(current);
					if (event == null) {
						forStop.put(current, null);
					}
				} finally {
					stateChangeLock.unlock();
				}
				timeLeft = waitForever ? 0 : start + timeout - System.currentTimeMillis();
				while (event == null && (waitForever || timeLeft > 0)) {
					forStop.wait(timeLeft);
					event = forStop.remove(current);
					if (!waitForever) {
						timeLeft = start + timeout - System.currentTimeMillis();
					}
				}
			}
		}
		return event == null ? ContainerEvent.STOPPED_TIMEOUT : event;
	}

	private void notifyWaitForStop(ContainerEvent event) {
		synchronized (forStop) {
			Collection<Thread> waiting = new ArrayList<Thread>(forStop.keySet());
			for (Thread t : waiting) {
				forStop.put(t, event);
			}
			forStop.notifyAll();
		}
	}

	/**
	 * @throws BundleException  
	 */
	protected void initWorker() throws BundleException {
		// Do nothing
	}

	@Override
	public void start(StartOptions... options) throws BundleException {
		// make sure to init if needed
		init();
		// Always transient
		super.start(StartOptions.TRANSIENT, StartOptions.USE_ACTIVATION_POLICY);
		getRevisions().getContainer().adaptor.publishContainerEvent(ContainerEvent.STARTED, this, null);
	}

	@Override
	public void stop(StopOptions... options) throws BundleException {
		ContainerEvent containerEvent = ContainerEvent.STOPPED_TIMEOUT;
		// Need to lock the state change lock with no state to prevent 
		// other threads from starting the framework while we are shutting down
		try {
			if (stateChangeLock.tryLock(10, TimeUnit.SECONDS)) {
				try {
					try {
						// Always transient
						super.stop(StopOptions.TRANSIENT);
					} catch (BundleException e) {
						getRevisions().getContainer().adaptor.publishContainerEvent(ContainerEvent.ERROR, this, e);
						// must continue on
					}
					if (holdsTransitionEventLock(ModuleEvent.UPDATED)) {
						containerEvent = ContainerEvent.STOPPED_UPDATE;
					} else if (holdsTransitionEventLock(ModuleEvent.UNRESOLVED)) {
						containerEvent = ContainerEvent.STOPPED_REFRESH;
					} else {
						containerEvent = ContainerEvent.STOPPED;
					}
					getRevisions().getContainer().adaptor.publishContainerEvent(containerEvent, this, null);
					getRevisions().getContainer().close();
				} finally {
					stateChangeLock.unlock();
				}
			} else {
				throw new BundleException(Msg.SystemModule_LockError);
			}
		} catch (InterruptedException e) {
			getRevisions().getContainer().adaptor.publishContainerEvent(ContainerEvent.ERROR, this, e);
		}
		notifyWaitForStop(containerEvent);
	}

	/**
	 * Restarts the module container.
	 * @see Framework#update()
	 * @throws BundleException
	 */
	public void update() throws BundleException {
		getContainer().checkAdminPermission(getBundle(), AdminPermission.LIFECYCLE);
		State previousState;
		lockStateChange(ModuleEvent.UPDATED);
		try {
			previousState = getState();
			stop();
		} finally {
			unlockStateChange(ModuleEvent.UPDATED);
		}
		// would publish an updated event here but the listener services are down
		switch (previousState) {
			case STARTING :
				init();
				break;
			case ACTIVE :
				start();
			default :
				break;
		}
	}

	@Override
	protected void startWorker() throws BundleException {
		super.startWorker();
		((ContainerStartLevel) getRevisions().getContainer().getFrameworkStartLevel()).doContainerStartLevel(this, ContainerStartLevel.USE_BEGINNING_START_LEVEL);
	}

	@Override
	protected void stopWorker() throws BundleException {
		super.stopWorker();
		((ContainerStartLevel) getRevisions().getContainer().getFrameworkStartLevel()).doContainerStartLevel(this, 0);
	}
}
