/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
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
	public static boolean DEBUG_READER = false;
	private long readStartupTime;

	private StateImpl systemState;
	private File stateLocation;
	private StateObjectFactoryImpl factory;
	private long lastTimeStamp;

	public StateManager(File bundleRootDir) {
		// a negative timestamp means no timestamp checking
		this(bundleRootDir, -1);
	}
	public StateManager(File bundleRootDir, long expectedTimeStamp) {
		factory = new StateObjectFactoryImpl();
		stateLocation = new File(bundleRootDir, ".state"); //$NON-NLS-1$
		readState(expectedTimeStamp);
	}	
	public void shutdown() throws IOException {
		writeState();
		systemState = null;
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
			systemState = (StateImpl) factory.readState(input, expectedTimeStamp);
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
		systemState = (StateImpl) factory.createState();
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
	public State getState() {
		return factory.createState(getSystemState());
	}
	public StateObjectFactory getFactory() {
		return factory;
	}
	public void commit(State state) throws BundleException {
		//TODO: implement this
		throw new UnsupportedOperationException("not implemented yet");
	}
	public Resolver getResolver() {
		return new ResolverImpl();
	}
	public File getStateLocation() {
		return stateLocation;
	}

}