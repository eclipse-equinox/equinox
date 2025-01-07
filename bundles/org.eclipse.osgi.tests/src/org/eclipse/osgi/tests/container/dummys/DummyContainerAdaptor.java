/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container.dummys;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.Module.Settings;
import org.eclipse.osgi.container.ModuleCollisionHook;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleContainerAdaptor;
import org.eclipse.osgi.container.SystemModule;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.tests.container.dummys.DummyModuleDatabase.DummyContainerEvent;
import org.eclipse.osgi.tests.container.dummys.DummyModuleDatabase.DummyModuleEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

public class DummyContainerAdaptor extends ModuleContainerAdaptor {
	private final AtomicBoolean slowdownEvents = new AtomicBoolean(false);
	private Runnable runForEvents = null;
	private final ModuleCollisionHook collisionHook;
	private final Map<String, String> configuration;
	private final DummyModuleDatabase moduleDatabase;
	private final ModuleContainer container;
	private final ResolverHookFactory resolverHookFactory;
	private final DebugOptions debugOptions;
	private final AtomicReference<CountDownLatch> startLatch = new AtomicReference<>();
	private final AtomicReference<CountDownLatch> stopLatch = new AtomicReference<>();
	private final Queue<String> traceMessages = new ConcurrentLinkedQueue<>();
	private final Queue<Throwable> traceThrowables = new ConcurrentLinkedQueue<>();
	private volatile Executor resolverExecutor;
	private volatile ScheduledExecutorService timeoutExecutor;

	public DummyContainerAdaptor(ModuleCollisionHook collisionHook, Map<String, String> configuration) {
		this(collisionHook, configuration, new DummyResolverHookFactory());
	}

	public DummyContainerAdaptor(ModuleCollisionHook collisionHook, Map<String, String> configuration,
			ResolverHookFactory resolverHookFactory) {
		this(collisionHook, configuration, resolverHookFactory, null);
	}

	public DummyContainerAdaptor(ModuleCollisionHook collisionHook, Map<String, String> configuration,
			ResolverHookFactory resolverHookFactory, DebugOptions debugOptions) {
		this.collisionHook = collisionHook;
		this.configuration = configuration == null ? new HashMap<>() : configuration;
		this.resolverHookFactory = resolverHookFactory;
		this.moduleDatabase = new DummyModuleDatabase(this);
		this.debugOptions = debugOptions;
		this.container = new ModuleContainer(this, moduleDatabase);
	}

	public void setConfiguration(String key, String value) {
		this.configuration.put(key, value);
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
	public void publishContainerEvent(ContainerEvent type, Module module, Throwable error,
			FrameworkListener... listeners) {
		moduleDatabase.addEvent(new DummyContainerEvent(type, module, error, listeners));

	}

	@Override
	public String getProperty(String key) {
		return configuration.get(key);
	}

	@Override
	public Module createModule(String location, long id, EnumSet<Settings> settings, int startlevel) {
		return new DummyModule(id, location, container, settings, startlevel, startLatch.get(), stopLatch.get());
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

	public void setSlowdownEvents(boolean slowdown) {
		slowdownEvents.set(slowdown);
	}

	@Override
	public void publishModuleEvent(ModuleEvent type, Module module, Module origin) {
		if (type == ModuleEvent.STARTING && slowdownEvents.get()) {
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				// ignore
				Thread.currentThread().interrupt();
			}
		}
		Runnable current = runForEvents;
		if (current != null) {
			current.run();
		}
		moduleDatabase.addEvent(new DummyModuleEvent(module, type, module.getState()));
	}

	public void setRunForEvents(Runnable runForEvents) {
		this.runForEvents = runForEvents;
	}

	@Override
	public DebugOptions getDebugOptions() {
		return this.debugOptions;
	}

	public void setResolverExecutor(Executor executor) {
		this.resolverExecutor = executor;
	}

	@Override
	public Executor getResolverExecutor() {
		Executor current = this.resolverExecutor;
		if (current != null) {
			return current;
		}
		return super.getResolverExecutor();
	}

	public void setTimeoutExecutor(ScheduledExecutorService timeoutExecutor) {
		this.timeoutExecutor = timeoutExecutor;
	}

	@Override
	public ScheduledExecutorService getScheduledExecutor() {
		return this.timeoutExecutor;
	}

	public void setStartLatch(CountDownLatch startLatch) {
		this.startLatch.set(startLatch);
	}

	public void setStopLatch(CountDownLatch stopLatch) {
		this.stopLatch.set(stopLatch);
	}

	@Override
	public void trace(String topic, String message) {
		traceMessages.add(message);
	}

	@Override
	public void traceThrowable(String topic, Throwable t) {
		traceThrowables.add(t);
	}

	public List<String> getTraceMessages() {
		return new ArrayList<>(traceMessages);
	}

	public List<Throwable> getTraceThrowables() {
		return new ArrayList<>(traceThrowables);
	}
}
