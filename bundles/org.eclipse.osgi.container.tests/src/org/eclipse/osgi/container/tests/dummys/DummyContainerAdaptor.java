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
package org.eclipse.osgi.container.tests.dummys;

import java.util.EnumSet;
import java.util.Map;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.Module.Settings;
import org.eclipse.osgi.container.tests.dummys.DummyModuleDatabase.DummyContainerEvent;
import org.eclipse.osgi.container.tests.dummys.DummyModuleDatabase.DummyModuleEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

public class DummyContainerAdaptor extends ModuleContainerAdaptor {

	private final ModuleCollisionHook collisionHook;
	private final ResolverHookFactory resolverHookFactory;
	private final Map<String, String> configuration;
	private final DummyModuleDatabase moduleDatabase;
	private final ModuleContainer container;

	public DummyContainerAdaptor(ModuleCollisionHook collisionHook, ResolverHookFactory resolverHookFactory, Map<String, String> configuration) {
		this.collisionHook = collisionHook;
		this.resolverHookFactory = resolverHookFactory;
		this.configuration = configuration;
		this.moduleDatabase = new DummyModuleDatabase(this);
		this.container = new ModuleContainer(this, moduleDatabase);
	}

	@Override
	public ModuleCollisionHook getModuleCollisionHook() {
		return collisionHook;
	}

	@Override
	public ResolverHookFactory getResolverHookFactory() {
		return resolverHookFactory;
	}

	@Override
	public void publishContainerEvent(ContainerEvent type, Module module,
			Throwable error, FrameworkListener... listeners) {
		moduleDatabase.addEvent(new DummyContainerEvent(type, module, error, listeners));

	}

	@Override
	public String getProperty(String key) {
		return configuration.get(key);
	}

	@Override
	public Module createModule(String location, long id, EnumSet<Settings> settings, int startlevel) {
		return new DummyModule(id, location, container, moduleDatabase, settings, startlevel);
	}

	@Override
	public SystemModule createSystemModule() {
		return new DummySystemModule(container, moduleDatabase);
	}

	public ModuleContainer getContainer() {
		return container;
	}

	public DummyModuleDatabase getDatabase() {
		return moduleDatabase;
	}

	@Override
	public void publishEvent(ModuleEvent type, Module module) {
		moduleDatabase.addEvent(new DummyModuleEvent(module, type, module.getState()));
	}
}
