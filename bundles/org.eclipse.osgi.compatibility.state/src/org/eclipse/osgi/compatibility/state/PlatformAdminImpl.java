/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.compatibility.state;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleDatabase;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.module.ResolverImpl;
import org.eclipse.osgi.internal.resolver.StateHelperImpl;
import org.eclipse.osgi.internal.resolver.StateObjectFactoryImpl;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.DisabledInfo;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.Resolver;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class PlatformAdminImpl implements PlatformAdmin {
	private final StateObjectFactory factory = new StateObjectFactoryImpl();
	private final Object monitor = new Object();
	private EquinoxContainer equinoxContainer;
	private BundleContext bc;
	private State systemState;
	private PlatformBundleListener synchronizer;
	private ServiceRegistration<PlatformAdmin> reg;

	void start(BundleContext context) {
		synchronized (this.monitor) {
			equinoxContainer = ((BundleContextImpl) context).getContainer();
			this.bc = context;
		}
		this.reg = context.registerService(PlatformAdmin.class, this, null);
	}

	void stop(BundleContext context) {
		synchronized (this.monitor) {
			if (synchronizer != null) {
				context.removeBundleListener(synchronizer);
				context.removeFrameworkListener(synchronizer);
			}
			synchronizer = null;
			systemState = null;
		}
		this.reg.unregister();
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
			return equinoxContainer.getStorage().getModuleDatabase().getRevisionsTimestamp();
		}
	}

	private State createSystemState() {
		State state = factory.createState(true);
		StateConverter converter = new StateConverter(state);
		ModuleDatabase database = equinoxContainer.getStorage().getModuleDatabase();
		database.readLock();
		try {
			ModuleContainer container = equinoxContainer.getStorage().getModuleContainer();
			List<Module> modules = equinoxContainer.getStorage().getModuleContainer().getModules();
			for (Module module : modules) {
				ModuleRevision current = module.getCurrentRevision();
				BundleDescription description = converter.createDescription(current);
				state.addBundle(description);
			}
			state.setPlatformProperties(asDictionary(equinoxContainer.getConfiguration().getInitialConfig()));
			synchronizer = new PlatformBundleListener(state, converter, database, container);
			state.setResolverHookFactory(synchronizer);
			bc.addBundleListener(synchronizer);
			bc.addFrameworkListener(synchronizer);
			state.resolve();
			state.setTimeStamp(database.getRevisionsTimestamp());
		} finally {
			database.readUnlock();
		}
		return state;
	}

	private Dictionary<String, Object> asDictionary(Map<String, ?> map) {
		return new Hashtable<>(map);
	}

	@Override
	public StateHelper getStateHelper() {
		return StateHelperImpl.getInstance();
	}

	@Override
	public void commit(State state) {
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
