package org.eclipse.osgi.compatibility.state;

import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleDatabase;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.module.ResolverImpl;
import org.eclipse.osgi.internal.resolver.StateHelperImpl;
import org.eclipse.osgi.internal.resolver.StateObjectFactoryImpl;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.storage.Storage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class PlatformAdminImpl implements PlatformAdmin {
	private final StateHelper stateHelper = new StateHelperImpl();
	private final StateObjectFactory factory = new StateObjectFactoryImpl();
	private volatile ModuleContainer container;
	private volatile ModuleDatabase database;

	private void activate(BundleContext context) {
		Storage storage = ((BundleContextImpl) context).getContainer().getStorage();
		this.container = storage.getModuleContainer();
		this.database = storage.getModuleDatabase();
		context.registerService(PlatformAdmin.class, this, null);
	}

	@Override
	public State getState() {
		return getState(true);
	}

	@Override
	public State getState(boolean mutable) {
		if (mutable) {
			return stateCopy();
		}
		return new ReadOnlySystemState(container, database);
	}

	private State stateCopy() {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public StateHelper getStateHelper() {
		return stateHelper;
	}

	@Override
	public void commit(State state) throws BundleException {
		throw new UnsupportedOperationException();
	}

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
