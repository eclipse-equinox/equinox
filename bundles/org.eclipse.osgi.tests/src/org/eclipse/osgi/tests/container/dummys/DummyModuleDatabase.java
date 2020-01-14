/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
import java.util.List;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.ModuleContainerAdaptor;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ModuleEvent;
import org.eclipse.osgi.container.ModuleDatabase;
import org.osgi.framework.FrameworkListener;

public class DummyModuleDatabase extends ModuleDatabase {

	public DummyModuleDatabase(ModuleContainerAdaptor adaptor) {
		super(adaptor);
	}

	private List<DummyModuleEvent> moduleEvents = new ArrayList<>();
	private List<DummyContainerEvent> containerEvents = new ArrayList<>();

	void addEvent(DummyModuleEvent event) {
		synchronized (moduleEvents) {
			moduleEvents.add(event);
			moduleEvents.notifyAll();
		}
	}

	void addEvent(DummyContainerEvent event) {
		synchronized (containerEvents) {
			containerEvents.add(event);
			containerEvents.notifyAll();
		}
	}

	public List<DummyModuleEvent> getModuleEvents() {
		return getEvents(moduleEvents);
	}

	public List<DummyContainerEvent> getContainerEvents() {
		return getEvents(containerEvents);
	}

	private static <E> List<E> getEvents(List<E> events) {
		synchronized (events) {
			List<E> result = new ArrayList<>(events);
			events.clear();
			return result;
		}
	}

	public List<DummyModuleEvent> getModuleEvents(int expectedNum) {
		return getEvents(expectedNum, moduleEvents);
	}

	public List<DummyContainerEvent> getContainerEvents(int expectedNum) {
		return getEvents(expectedNum, containerEvents);
	}

	private static <E> List<E> getEvents(int expectedNum, List<E> events) {
		synchronized (events) {
			long timeout = 5000;
			while (events.size() < expectedNum && timeout > 0) {
				long startTime = System.currentTimeMillis();
				try {
					events.wait(timeout);
				} catch (InterruptedException e) {
					// continue to wait
				}
				timeout = timeout - (System.currentTimeMillis() - startTime);
			}
			List<E> result = new ArrayList<>(events);
			events.clear();
			return result;
		}
	}

	public static class DummyModuleEvent {
		public final Module module;
		public final ModuleEvent event;
		public final State state;

		public DummyModuleEvent(Module module, ModuleEvent event, State state) {
			this.module = module;
			this.event = event;
			this.state = state;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof DummyModuleEvent))
				return false;
			DummyModuleEvent that = (DummyModuleEvent) o;
			return this.event.equals(that.event) && this.module.equals(that.module) && this.state.equals(that.state);
		}

		@Override
		public String toString() {
			return module + ": " + event + ": " + state;
		}
	}

	public static class DummyContainerEvent {
		public final ContainerEvent type;
		public final Module module;
		public final Throwable error;
		public final FrameworkListener[] listeners;

		public DummyContainerEvent(ContainerEvent type, Module module, Throwable error, FrameworkListener... listeners) {
			this.type = type;
			this.module = module;
			this.error = error;
			this.listeners = listeners;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof DummyContainerEvent))
				return false;
			DummyContainerEvent that = (DummyContainerEvent) o;
			return this.type.equals(that.type) && this.module.equals(that.module);
		}

		@Override
		public String toString() {
			return module + ": " + type;
		}
	}
}
