/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.io.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;

public class StateManager implements PlatformAdmin {
	public static boolean DEBUG = false;
	public static boolean DEBUG_READER = false;
	public static boolean DEBUG_PLATFORM_ADMIN = false;
	public static boolean DEBUG_PLATFORM_ADMIN_RESOLVER = false;
	public static boolean MONITOR_PLATFORM_ADMIN = false;
	private long readStartupTime;
	private StateImpl systemState;
	private StateObjectFactoryImpl factory;
	private long lastTimeStamp;
	private BundleInstaller installer;
	private boolean cachedState = false;

	public StateManager(File stateLocation) {
		// a negative timestamp means no timestamp checking
		this(stateLocation, -1);
	}

	public StateManager(File stateLocation, long expectedTimeStamp) {
		factory = new StateObjectFactoryImpl();
		readState(stateLocation, expectedTimeStamp);
	}

	public void shutdown(File stateLocation) throws IOException {
		writeState(stateLocation);
		//systemState should not be set to null as when the framework
		//is restarted from a shutdown state, the systemState variable will
		//not be reset, resulting in a null pointer exception
		//systemState = null;
	}

	private void readState(File stateLocation, long expectedTimeStamp) {
		if (!stateLocation.isFile())
			return;
		if (DEBUG_READER)
			readStartupTime = System.currentTimeMillis();
		FileInputStream fileInput;
		try {
			fileInput = new FileInputStream(stateLocation);
		} catch (FileNotFoundException e) {
			// TODO: log before bailing
			e.printStackTrace();
			return;
		}
		try {
			InputStream input = new BufferedInputStream(fileInput, 65536);
			systemState = factory.readSystemState(input, expectedTimeStamp);
			// problems in the cache (corrupted/stale), don't create a state object
			if (systemState == null)
				return;
			initializeSystemState();
			cachedState = true;
		} catch (IOException ioe) {
			// TODO: how do we log this?
			ioe.printStackTrace();
		} finally {
			if (DEBUG_READER)
				System.out.println("Time to read state: " + (System.currentTimeMillis() - readStartupTime)); //$NON-NLS-1$
		}
	}

	private void writeState(File stateLocation) throws IOException {
		if (systemState == null)
			return;
		if (cachedState && lastTimeStamp == systemState.getTimeStamp())
			return;
		factory.writeState(systemState, new BufferedOutputStream(new FileOutputStream(stateLocation)));
	}

	public StateImpl createSystemState() {
		systemState = factory.createSystemState();
		initializeSystemState();
		return systemState;
	}

	private void initializeSystemState() {
		systemState.setResolver(new ResolverImpl());
		lastTimeStamp = systemState.getTimeStamp();
	}

	public StateImpl getSystemState() {
		return systemState;
	}

	public State getState(boolean mutable) {
		return mutable ? factory.createState(systemState) : new ReadOnlyState(systemState);
	}

	public State getState() {
		return getState(true);
	}

	public StateObjectFactory getFactory() {
		return factory;
	}

	public synchronized void commit(State state) throws BundleException {
		// no installer have been provided - commit not supported
		if (installer == null)
			throw new IllegalArgumentException("PlatformAdmin.commit() not supported"); //$NON-NLS-1$
		if (!(state instanceof UserState))
			throw new IllegalArgumentException("Wrong state implementation"); //$NON-NLS-1$		
		if (state.getTimeStamp() != systemState.getTimeStamp())
			throw new BundleException(StateMsg.formatter.getString("COMMIT_INVALID_TIMESTAMP")); //$NON-NLS-1$		
		StateDelta delta = state.compare(systemState);
		BundleDelta[] changes = delta.getChanges();
		for (int i = 0; i < changes.length; i++)
			if ((changes[i].getType() & BundleDelta.ADDED) > 0)
				installer.installBundle(changes[i].getBundle());
			else if ((changes[i].getType() & BundleDelta.REMOVED) > 0)
				installer.uninstallBundle(changes[i].getBundle());
			else if ((changes[i].getType() & BundleDelta.UPDATED) > 0)
				installer.updateBundle(changes[i].getBundle());
			else {
				// bug in StateDelta#getChanges
			}
	}

	public Resolver getResolver() {
		return new ResolverImpl();
	}

	public StateHelper getStateHelper() {
		return StateHelperImpl.getInstance();
	}

	public BundleInstaller getInstaller() {
		return installer;
	}

	public void setInstaller(BundleInstaller installer) {
		this.installer = installer;
	}
}