/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.baseadaptor;

import java.io.File;
import java.io.IOException;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.internal.resolver.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * The StateManager manages the system state for the framework.  It also provides the implementation
 * to the PlatformAdmin service.
 * <p>
 * Clients may extend this class.
 * </p>
 * @since 3.1
 */
public class StateManager implements PlatformAdmin, Runnable {
	/**
	 * General debug flag
	 */
	public static boolean DEBUG = false;
	/**
	 * Reader debug flag
	 */
	public static boolean DEBUG_READER = false;
	/**
	 * PlatformAdmin debug flag
	 */
	public static boolean DEBUG_PLATFORM_ADMIN = false;
	/**
	 * PlatformAdmin resolver debug flag
	 */
	public static boolean DEBUG_PLATFORM_ADMIN_RESOLVER = false;
	/**
	 * Monitor PlatformAdmin debug flag
	 */
	public static boolean MONITOR_PLATFORM_ADMIN = false;
	/**
	 * System property used to disable lazy state loading
	 */
	public static String PROP_NO_LAZY_LOADING = "osgi.noLazyStateLoading"; //$NON-NLS-1$
	/**
	 * System property used to specify to amount time before lazy data can be flushed from memory
	 */
	public static String PROP_LAZY_UNLOADING_TIME = "osgi.lazyStateUnloadingTime"; //$NON-NLS-1$
	private long expireTime = 300000; // default to five minutes
	private long readStartupTime;
	private StateImpl systemState;
	private final StateObjectFactoryImpl factory;
	private long lastTimeStamp;
	private boolean cachedState = false;
	private final File stateFile;
	private final File lazyFile;
	private final long expectedTimeStamp;
	private final BundleContext context;
	private Thread dataManagerThread;

	/**
	 * Constructs a StateManager using the specified files and context
	 * @param stateFile a file with the data required to persist in memory
	 * @param lazyFile a file with the data that may be lazy loaded and can be flushed from memory
	 * @param context the bundle context of the system bundle
	 */
	public StateManager(File stateFile, File lazyFile, BundleContext context) {
		// a negative timestamp means no timestamp checking
		this(stateFile, lazyFile, context, -1);
	}

	/**
	 * Constructs a StateManager using the specified files and context
	 * @param stateFile a file with the data required to persist in memory
	 * @param lazyFile a file with the data that may be lazy loaded and can be flushed from memory
	 * @param context the bundle context of the system bundle
	 * @param expectedTimeStamp the expected timestamp of the persisted system state.  A negative
	 * value indicates that no timestamp checking is done
	 */
	public StateManager(File stateFile, File lazyFile, BundleContext context, long expectedTimeStamp) {
		this.stateFile = stateFile;
		this.lazyFile = lazyFile;
		this.context = context;
		this.expectedTimeStamp = expectedTimeStamp;
		factory = new StateObjectFactoryImpl();
	}

	/**
	 * Shutsdown the state manager.  If the timestamp of the system state has changed
	 * @param saveStateFile
	 * @param saveLazyFile
	 * @throws IOException
	 */
	public void shutdown(File saveStateFile, File saveLazyFile) throws IOException {
		writeState(systemState, saveStateFile, saveLazyFile);
		stopDataManager();
	}

	/**
	 * Update the given target files with the state data in memory.
	 * @param updateStateFile
	 * @param updateLazyFile
	 * @throws IOException
	 */
	public void update(File updateStateFile, File updateLazyFile) throws IOException {
		writeState(systemState, updateStateFile, updateLazyFile);
		// Need to use the timestamp of the original state here
		lastTimeStamp = systemState.getTimeStamp();
		// TODO consider updating the state files for lazy loading
	}

	private void internalReadSystemState() {
		if (stateFile == null || !stateFile.isFile())
			return;
		if (DEBUG_READER)
			readStartupTime = System.currentTimeMillis();
		try {
			boolean lazyLoad = !Boolean.valueOf(FrameworkProperties.getProperty(PROP_NO_LAZY_LOADING)).booleanValue();
			systemState = factory.readSystemState(context, stateFile, lazyFile, lazyLoad, expectedTimeStamp);
			// problems in the cache (corrupted/stale), don't create a state object
			if (systemState == null || !initializeSystemState()) {
				systemState = null;
				return;
			}
			cachedState = true;
			try {
				expireTime = Long.parseLong(FrameworkProperties.getProperty(PROP_LAZY_UNLOADING_TIME, Long.toString(expireTime)));
			} catch (NumberFormatException nfe) {
				// default to not expire
				expireTime = 0;
			}
			if (lazyLoad && expireTime > 0)
				startDataManager();
		} catch (IOException ioe) {
			// TODO: how do we log this?
			ioe.printStackTrace();
		} finally {
			if (DEBUG_READER)
				System.out.println("Time to read state: " + (System.currentTimeMillis() - readStartupTime)); //$NON-NLS-1$
		}
	}

	private synchronized void startDataManager() {
		if (dataManagerThread != null)
			return;
		dataManagerThread = new Thread(this, "State Data Manager"); //$NON-NLS-1$
		dataManagerThread.setDaemon(true);
		dataManagerThread.start();
	}

	/**
	 * Stops the active data manager thread which is used to unload unused
	 * state objects from memory.
	 */
	public synchronized void stopDataManager() {
		if (dataManagerThread == null)
			return;
		dataManagerThread.interrupt();
		dataManagerThread = null;
	}

	private void writeState(StateImpl state, File saveStateFile, File saveLazyFile) throws IOException {
		if (state == null)
			return;
		if (cachedState && !saveNeeded())
			return;
		state.fullyLoad(); // make sure we are fully loaded before saving
		factory.writeState(state, saveStateFile, saveLazyFile);
	}

	private boolean initializeSystemState() {
		systemState.setResolver(createResolver(System.getSecurityManager() != null));
		lastTimeStamp = systemState.getTimeStamp();
		return !systemState.setPlatformProperties(FrameworkProperties.getProperties());
	}

	/**
	 * Creates a new State used by the system.  If the system State already 
	 * exists then a new system State is not created.
	 * @return the State used by the system.
	 */
	public synchronized State createSystemState() {
		if (systemState == null) {
			systemState = factory.createSystemState(context);
			initializeSystemState();
		}
		return systemState;
	}

	/**
	 * Reads the State used by the system.  If the system State already
	 * exists then the system State is not read from a cache.  If the State could 
	 * not be read from a cache then <code>null</code> is returned.
	 * @return the State used by the system or <code>null</code> if the State
	 * could not be read from a cache.
	 */
	public synchronized State readSystemState() {
		if (systemState == null)
			internalReadSystemState();
		return systemState;
	}

	/**
	 * Returns the State used by the system.  If the system State does 
	 * not exist then <code>null</code> is returned.
	 * @return the State used by the system or <code>null</code> if one
	 * does not exist.
	 */
	public State getSystemState() {
		return systemState;
	}

	/**
	 * Returns the cached time stamp of the system State.  This value is the 
	 * original time stamp of the system state when it was created or read from
	 * a cache.
	 * @see State#getTimeStamp()
	 * @return the cached time stamp of the system State
	 */
	public long getCachedTimeStamp() {
		return lastTimeStamp;
	}

	public boolean saveNeeded() {
		return systemState.getTimeStamp() != lastTimeStamp || systemState.dynamicCacheChanged();
	}

	/**
	 * @see PlatformAdmin#getState(boolean)
	 */
	public State getState(boolean mutable) {
		return mutable ? factory.createState(systemState) : new ReadOnlyState(systemState);
	}

	/**
	 * @see PlatformAdmin#getState()
	 */
	public State getState() {
		return getState(true);
	}

	/**
	 * @see PlatformAdmin#getFactory()
	 */
	public StateObjectFactory getFactory() {
		return factory;
	}

	/**
	 * @throws BundleException 
	 * @see PlatformAdmin#commit(State)
	 */
	public synchronized void commit(State state) throws BundleException {
		throw new IllegalArgumentException("PlatformAdmin.commit() not supported"); //$NON-NLS-1$
	}

	/**
	 * @see PlatformAdmin#getResolver()
	 * @deprecated
	 */
	public Resolver getResolver() {
		return createResolver(false);
	}

	/**
	 * @see PlatformAdmin#createResolver()
	 */
	public Resolver createResolver() {
		return createResolver(false);
	}

	private Resolver createResolver(boolean checkPermissions) {
		return new org.eclipse.osgi.internal.module.ResolverImpl(checkPermissions);
	}

	/**
	 * @see PlatformAdmin#getStateHelper()
	 */
	public StateHelper getStateHelper() {
		return StateHelperImpl.getInstance();
	}

	public void run() {
		long timeStamp = lastTimeStamp; // cache the original timestamp incase of updates
		while (true) {
			try {
				Thread.sleep(expireTime);
			} catch (InterruptedException e) {
				return;
			}
			if (systemState != null)
				synchronized (systemState) {
					if (!systemState.unloadLazyData(timeStamp))
						return;
				}
		}
	}

	public void addDisabledInfo(DisabledInfo disabledInfo) {
		if (systemState == null)
			throw new IllegalStateException(); // should never happen
		systemState.addDisabledInfo(disabledInfo);
	}

	public void removeDisabledInfo(DisabledInfo disabledInfo) {
		if (systemState == null)
			throw new IllegalStateException(); // should never happen
		systemState.removeDisabledInfo(disabledInfo);
	}
}
