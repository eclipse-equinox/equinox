/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.compatibility.state;

import java.util.*;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.module.ResolverImpl;
import org.eclipse.osgi.internal.resolver.StateHelperImpl;
import org.eclipse.osgi.internal.resolver.StateObjectFactoryImpl;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class PlatformAdminImpl implements PlatformAdmin {
	private final StateHelper stateHelper = new StateHelperImpl();
	private final StateObjectFactory factory = new StateObjectFactoryImpl();
	private final Object monitor = new Object();
	private EquinoxContainer equinoxContainer;
	private State systemState;

	@SuppressWarnings("unused")
	// this is used by DS to activate us
	private void activate(BundleContext context) {
		synchronized (this.monitor) {
			equinoxContainer = ((BundleContextImpl) context).getContainer();
		}
		context.registerService(PlatformAdmin.class, this, null);
	}

	@Override
	public State getState() {
		return getState(true);
	}

	@Override
	public State getState(boolean mutable) {
		if (mutable) {
			return factory.createState(getSystemState());
		}
		return new ReadOnlyState(this);
	}

	State getSystemState() {
		synchronized (this.monitor) {
			if (systemState == null) {
				systemState = createSystemState();
			}
			return systemState;
		}
	}

	long getTimeStamp() {
		synchronized (this.monitor) {
			return equinoxContainer.getStorage().getModuleDatabase().getTimestamp();
		}
	}

	private State createSystemState() {
		State state = factory.createState(true);
		StateConverter converter = new StateConverter(state);
		ModuleDatabase database = equinoxContainer.getStorage().getModuleDatabase();
		database.readLock();
		try {
			List<Module> modules = equinoxContainer.getStorage().getModuleContainer().getModules();
			for (Module module : modules) {
				ModuleRevision current = module.getCurrentRevision();
				BundleDescription description = converter.createDescription(current);
				state.addBundle(description);
				state.setPlatformProperties(asDictionary(equinoxContainer.getConfiguration().getInitialConfig()));
			}
			state.setTimeStamp(database.getRevisionsTimestamp());
			// TODO add hooks to get the resolution correct
			// TODO add listeners to keep state copy in sync
		} finally {
			database.readUnlock();
		}
		return state;
	}

	private Dictionary<String, String> asDictionary(Map<String, String> map) {
		return new Hashtable<String, String>(map);
	}

	@Override
	public StateHelper getStateHelper() {
		return StateHelperImpl.getInstance();
	}

	/**
	 * @throws BundleException  
	 */
	@Override
	public void commit(State state) throws BundleException {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public Resolver getResolver() {
		return createResolver();
	}

	@Override
	public Resolver createResolver() {
		return new ResolverImpl(false);
	}

	@Override
	public StateObjectFactory getFactory() {
		return factory;
	}

	@Override
	public void addDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeDisabledInfo(DisabledInfo disabledInfo) {
		throw new UnsupportedOperationException();
	}

}
