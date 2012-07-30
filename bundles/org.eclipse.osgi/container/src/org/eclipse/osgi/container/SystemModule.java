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
import org.eclipse.osgi.container.ModuleContainer.ContainerStartLevel;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.osgi.framework.*;
import org.osgi.service.resolver.ResolutionException;

/**
 * @since 3.10
 */
public abstract class SystemModule extends Module {
	private final Map<Thread, ContainerEvent> forStop = new HashMap<Thread, ContainerEvent>(2);

	public SystemModule(ModuleContainer container) {
		super(new Long(0), Constants.SYSTEM_BUNDLE_LOCATION, container, EnumSet.of(Settings.AUTO_START, Settings.USE_ACTIVATION_POLICY), new Integer(0));
	}

	public final void init() throws BundleException {
		getRevisions().getContainer().checkAdminPermission(getBundle(), AdminPermission.EXECUTE);
		lockStateChange(ModuleEvent.STARTED);
		try {
			checkValid();
			if (ACTIVE_SET.contains(getState()))
				return;
			getRevisions().getContainer().open();
			if (getState().equals(State.INSTALLED)) {
				try {
					getRevisions().getContainer().resolve(Arrays.asList((Module) this), true);
				} catch (ResolutionException e) {
					throw new BundleException("Could not resolve module.", BundleException.RESOLVE_ERROR, e);
				}
			}
			if (getState().equals(State.INSTALLED)) {
				throw new BundleException("Could not resolve module.", BundleException.RESOLVE_ERROR);
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
				throw new BundleException("Error initializing container.", BundleException.ACTIVATOR_ERROR, t);
			}
		} finally {
			unlockStateChange(ModuleEvent.STARTED);
		}
	}

	public ContainerEvent waitForStop(long timeout) throws InterruptedException {
		final boolean waitForEver = timeout == 0;
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
		timeLeft = waitForEver ? 0 : start + timeout - System.currentTimeMillis();
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
				if (event == null) {
					do {
						forStop.wait(timeLeft);
						event = forStop.remove(current);
						if (!waitForEver) {
							timeLeft = start + timeout - System.currentTimeMillis();
							if (timeLeft == 0) {
								timeLeft = -1;
							}
						}
					} while (event == null && timeLeft >= 0);
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

	@SuppressWarnings("unused")
	@Override
	public void stop(StopOptions... options) throws BundleException {
		try {
			// Always transient
			super.stop(StopOptions.TRANSIENT);
		} catch (BundleException e) {
			getRevisions().getContainer().adaptor.publishContainerEvent(ContainerEvent.ERROR, this, e);
			// must continue on
		}
		ContainerEvent containerEvent;
		if (holdsTransitionEventLock(ModuleEvent.UPDATED)) {
			containerEvent = ContainerEvent.STOPPED_UPDATE;
		} else if (holdsTransitionEventLock(ModuleEvent.UNRESOLVED)) {
			containerEvent = ContainerEvent.STOPPED_REFRESH;
		} else {
			containerEvent = ContainerEvent.STOPPED;
		}
		getRevisions().getContainer().adaptor.publishContainerEvent(containerEvent, this, null);
		getRevisions().getContainer().close();

		notifyWaitForStop(containerEvent);
	}

	public void update() throws BundleException {
		getContainer().checkAdminPermission(getBundle(), AdminPermission.LIFECYCLE);
		lockStateChange(ModuleEvent.UPDATED);
		try {
			stop();
		} finally {
			unlockStateChange(ModuleEvent.UPDATED);
		}
		// would publish an updated event here but the listener services are down
		start();
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
