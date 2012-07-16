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

import java.util.Arrays;
import java.util.EnumSet;
import org.eclipse.osgi.container.ModuleContainer.ContainerStartLevel;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.resolver.ResolutionException;

/**
 * @since 3.10
 */
public abstract class SystemModule extends Module {

	public SystemModule(ModuleContainer container) {
		super(new Long(0), Constants.SYSTEM_BUNDLE_LOCATION, container, EnumSet.of(Settings.AUTO_START, Settings.USE_ACTIVATION_POLICY), new Integer(0));
	}

	public final void init() throws BundleException {
		getRevisions().getContainer().open();
		lockStateChange(ModuleEvent.STARTED);
		try {
			checkValid();
			if (ACTIVE_SET.contains(getState()))
				return;
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
		// Always transient
		super.stop(StopOptions.TRANSIENT);
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
