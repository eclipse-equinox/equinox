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
import java.util.Dictionary;

import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.BundleException;

public class StateManager implements PlatformAdmin {
	public static boolean DEBUG_READER = false;
	private long readStartupTime;
	
	private StateImpl state;
	private File stateLocation;
	private StateObjectFactory factory;

	public StateManager(File bundleRootDir) {
		stateLocation = new File(bundleRootDir, ".state"); //$NON-NLS-1$
		readState();
	}
	
	public void shutdown() throws IOException {
		writeState();
		state = null;
	}
	private void readState() {
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
			input = new DataInputStream(new BufferedInputStream(fileInput,65536));
			StateReader reader = new StateReader();
			state = reader.loadState(input);
			state.setResolver(new ResolverImpl());
		} catch (IOException ioe) {
			// TODO: how do we log this?
			ioe.printStackTrace();
		} finally {			
			if (DEBUG_READER)
				System.out.println("Time to read state: " + (System.currentTimeMillis() - readStartupTime));
		}		
	}
	private void writeState() throws IOException {
		if (state == null)
			return;		
		DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(stateLocation)));
		StateWriter writer = new StateWriter();
		writer.saveState(state, output);
	}
	public State getSystemState() {
		if (state == null) {
			state = (StateImpl) factory.createState();
			state.setResolver(new ResolverImpl());
		}
		return state;
	}
	public State getState() {
		return factory.createState(getSystemState());		
	}
	public BundleDescription uninstall(BundleData bundledata) {
		long bundleId = bundledata.getBundleID();
		return getSystemState().removeBundle(bundleId);
	}
	public BundleDescription update(Dictionary manifest, String location, long bundleId) throws BundleException {
 		State systemState = getSystemState();
 		systemState.removeBundle(bundleId);
		BundleDescription newDescription = getFactory().createBundleDescription(manifest, location,bundleId);
		systemState.addBundle(newDescription);
		return newDescription;
	}
	public BundleDescription install(Dictionary manifest, String location, long bundleId) throws BundleException {
		BundleDescription bundleDescription = getFactory().createBundleDescription(manifest, location,bundleId);
		getSystemState().addBundle(bundleDescription);
		return bundleDescription;
	}
	public StateObjectFactory getFactory() {
		if (factory == null)
			factory = new StateObjectFactoryImpl();		
		return factory;
	}
	public void commit(State state) throws BundleException {
		//TODO: implement this
		throw new UnsupportedOperationException("not implemented yet");
	}
	public Resolver getResolver() {
		return new ResolverImpl();
	}
	
}