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

import java.util.Collections;
import java.util.Map;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.tests.dummys.DummyModuleDataBase.DummyContainerEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

public class DummyContainerAdaptor extends ModuleContainerAdaptor {

	private final ModuleCollisionHook collisionHook;
	private final ResolverHookFactory resolverHookFactory;
	private final DummyModuleDataBase moduleDataBase;
	private final Map<String, Object> configuration;

	public DummyContainerAdaptor(ModuleCollisionHook collisionHook, ResolverHookFactory resolverHookFactory, DummyModuleDataBase moduleDataBase, Map<String, Object> configuration) {
		this.collisionHook = collisionHook;
		this.resolverHookFactory = resolverHookFactory;
		this.moduleDataBase = moduleDataBase;
		this.configuration = Collections.unmodifiableMap(configuration);
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
		moduleDataBase.addEvent(new DummyContainerEvent(type, module, error, listeners));

	}

	@Override
	public Map<String, Object> getConfiguration() {
		return configuration;
	}
}
