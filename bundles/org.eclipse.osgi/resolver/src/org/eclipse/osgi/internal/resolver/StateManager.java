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
	private File stateLocation;
	private StateObjectFactoryImpl factory;
	private long lastTimeStamp;
	private BundleInstaller installer;
	public StateManager(BundleInstaller installer, File  bundleRootDir) {
		// a negative timestamp means no timestamp checking
		this(installer, bundleRootDir, -1);
	}
	public StateManager(BundleInstaller installer, File bundleRootDir, long expectedTimeStamp) {
		factory = new StateObjectFactoryImpl();
		this.installer = installer;
		stateLocation = new File(bundleRootDir, ".state"); //$NON-NLS-1$
		readState(expectedTimeStamp);
	}
	public void shutdown() throws IOException {
		writeState();
		//systemState should not be set to null as when the framework
		//is restarted from a shutdown state, the systemState variable will
		//not be reset, resulting in a null pointer exception
		//systemState = null;
	}
	private void readState(long expectedTimeStamp) {
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
		DataInputStream input = null;
		try {
			input = new DataInputStream(new BufferedInputStream(fileInput, 65536));
			systemState = factory.readSystemState(input, expectedTimeStamp);
			// problems in the cache (corrupted/stale), don't create a state object
			if (systemState == null)
				return;
			initializeSystemState();
		} catch (IOException ioe) {
			// TODO: how do we log this?
			ioe.printStackTrace();
		} finally {
			if (DEBUG_READER)
				System.out.println("Time to read state: " + (System.currentTimeMillis() - readStartupTime));
		}
	}
	private void writeState() throws IOException {
		if (systemState == null)
			return;
		if (stateLocation.isFile() && lastTimeStamp == systemState.getTimeStamp())
			return;
		DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(stateLocation)));
		factory.writeState(systemState, output);
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
		// client trying to sneak in some alien implementation
		if (!(state instanceof UserState))
			throw new IllegalArgumentException("Wrong state implementation"); //$NON-NLS-1$
		// no installer have been provided - commit not supported
		if (installer == null)
			throw new IllegalArgumentException("PlatformAdmin.commit() not supported"); //$NON-NLS-1$
		if (state.getTimeStamp() != systemState.getTimeStamp())
			throw new BundleException(StateMsg.formatter.getString("COMMIT_INVALID_TIMESTAMP")); //$NON-NLS-1$
		UserState userState = (UserState) state;
		Long[] allAdded = userState.getAllAdded();
		for (int i = 0; i < allAdded.length; i++) {
			BundleDescription added = userState.getBundle(allAdded[i].longValue());
			// ensure it has not been added then removed
			if (added != null)
				installer.installBundle(added);
		}
		Long[] allRemoved = userState.getAllRemoved();
		for (int i = 0; i < allRemoved.length; i++) {
			long removedId = allRemoved[i].longValue();
			BundleDescription removedFromUserState = userState.getBundle(removedId);
			// ensure it has not been removed then added
			if (removedFromUserState == null) {
				BundleDescription existingSystemState = systemState.getBundle(removedId);
				if (existingSystemState != null)
					installer.uninstallBundle(existingSystemState);
			}
		}
	}
	public Resolver getResolver() {
		return new ResolverImpl();
	}
	public File getStateLocation() {
		return stateLocation;
	}
}