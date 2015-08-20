/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container.dummys;

import java.util.EnumSet;
import java.util.Map;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.Module.Settings;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.tests.container.dummys.DummyModuleDatabase.DummyContainerEvent;
import org.eclipse.osgi.tests.container.dummys.DummyModuleDatabase.DummyModuleEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

public class DummyContainerAdaptor extends ModuleContainerAdaptor {
	private final ModuleCollisionHook collisionHook;
	private final Map<String, String> configuration;
	private final DummyModuleDatabase moduleDatabase;
	private final ModuleContainer container;
	private final ResolverHookFactory resolverHookFactory;
	private final DebugOptions debugOptions;

	public DummyContainerAdaptor(ModuleCollisionHook collisionHook, Map<String, String> configuration) {
		this(collisionHook, configuration, new DummyResolverHookFactory());
	}

	public DummyContainerAdaptor(ModuleCollisionHook collisionHook, Map<String, String> configuration, ResolverHookFactory resolverHookFactory) {
		this(collisionHook, configuration, resolverHookFactory, null);
	}

	public DummyContainerAdaptor(ModuleCollisionHook collisionHook, Map<String, String> configuration, ResolverHookFactory resolverHookFactory, DebugOptions debugOptions) {
		this.collisionHook = collisionHook;
		this.configuration = configuration;
		this.resolverHookFactory = resolverHookFactory;
		this.moduleDatabase = new DummyModuleDatabase(this);
		this.debugOptions = debugOptions;
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
	public void publishContainerEvent(ContainerEvent type, Module module, Throwable error, FrameworkListener... listeners) {
		moduleDatabase.addEvent(new DummyContainerEvent(type, module, error, listeners));

	}

	@Override
	public String getProperty(String key) {
		return configuration.get(key);
	}

	@Override
	public Module createModule(String location, long id, EnumSet<Settings> settings, int startlevel) {
		return new DummyModule(id, location, container, settings, startlevel);
	}

	@Override
	public SystemModule createSystemModule() {
		return new DummySystemModule(container);
	}

	public ModuleContainer getContainer() {
		return container;
	}

	public DummyModuleDatabase getDatabase() {
		return moduleDatabase;
	}

	@Override
	public void publishModuleEvent(ModuleEvent type, Module module, Module origin) {
		moduleDatabase.addEvent(new DummyModuleEvent(module, type, module.getState()));
	}

	@Override
	public DebugOptions getDebugOptions() {
		return this.debugOptions;
	}

}
