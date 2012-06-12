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
package org.eclipse.osgi.container;

import java.io.DataInputStream;
import java.util.*;
import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.eclipse.osgi.container.Module.Settings;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.service.resolver.Resolver;

/**
 * Adapts the behavior of a container.
 * @since 3.10
 */
public abstract class ModuleContainerAdaptor {
	public enum ContainerEvent {
		REFRESH, START_LEVEL, STARTED, STOPPED, STOPPED_UPDATE, STOPPED_REFRESH, ERROR, WARNING, INFO
	}

	/**
	 * Event types that may be {@link #publishEvent(ModuleEvent, Module) published} for a module
	 * indicating a {@link Module#getState() state} change has occurred for a module.
	 */
	public static enum ModuleEvent {
		/**
		 * The module has been installed
		 */
		INSTALLED,
		/**
		 * The module has been activated with the lazy activation policy and
		 * is waiting a {@link Module.StartOptions#LAZY_TRIGGER trigger} class load.
		 */
		LAZY_ACTIVATION,
		/**
		 * The module has been resolved.
		 */
		RESOLVED,
		/**
		 * The module has beens started.
		 */
		STARTED,
		/**
		 * The module is about to be activated.
		 */
		STARTING,
		/**
		 * The module has been stopped.
		 */
		STOPPED,
		/**
		 * The module is about to be deactivated.
		 */
		STOPPING,
		/**
		 * The module has been uninstalled.
		 */
		UNINSTALLED,
		/**
		 * The module has been unresolved.
		 */
		UNRESOLVED,
		/**
		 * The module has been updated.
		 */
		UPDATED
	}

	/**
	 * Returns the resolver the container will use.  This implementation will
	 * return the default implementation of the resolver.  Override this method
	 * to provide an alternative resolver implementation for the container.
	 * @return the resolver the container will use.
	 */
	public Resolver getResolver() {
		return new ResolverImpl(new Logger(4));
	}

	/**
	 * Returns the collision hook the container will use.
	 * @return the collision hook the container will use.
	 */
	public abstract ModuleCollisionHook getModuleCollisionHook();

	/**
	 * Returns the resolver hook factory the container will use.
	 * @return the resolver hook factory the container will use.
	 */
	public abstract ResolverHookFactory getResolverHookFactory();

	/**
	 * Publishes the specified container event.
	 * @param type the type of event
	 * @param module the module associated with the event
	 * @param error the error associated with the event, may be {@code null}
	 * @param listeners additional listeners to publish the event to synchronously
	 */
	public abstract void publishContainerEvent(ContainerEvent type, Module module, Throwable error, FrameworkListener... listeners);

	/**
	 * Publishes the specified module event type for the specified module.
	 * @param type the event type to publish
	 * @param module the module the event is associated with
	 */
	abstract public void publishEvent(ModuleEvent type, Module module);

	/**
	 * Returns an unmodifiable map of the configuration for the container
	 * @return an unmodifiable map of the configuration for the container
	 */
	public Map<String, Object> getConfiguration() {
		return Collections.emptyMap();
	}

	/**
	 * Creates a new {@link ModuleClassLoader} for the specified wiring.
	 * @param wiring the module wiring to create a module class loader for
	 * @return a new {@link ModuleClassLoader} for the specified wiring.
	 */
	public ModuleClassLoader createClassLoader(ModuleWiring wiring) {
		throw new UnsupportedOperationException("Container adaptor does not support module class loaders.");
	}

	/**
	 * Creates a new module.  This gets called when a new module is installed
	 * or when {@link ModuleDatabase#load(DataInputStream) loading} persistent data into this
	 * database.
	 * @param location the location for the module
	 * @param id the id for the module
	 * @param settings the settings for the module.  May be {@code null} if there are no settings.
	 * @param startlevel the start level for the module
	 * @return the Module
	 */
	public abstract Module createModule(String location, long id, EnumSet<Settings> settings, int startlevel);

	/**
	 * Creates the system module.  This gets called when the system module is installed
	 * or when {@link ModuleDatabase#load(DataInputStream) loading} persistent data into this
	 * database.
	 * <p>
	 * The returned system module must have an {@link Module#getId() id} of zero and a location
	 * of {@link Constants#SYSTEM_BUNDLE_LOCATION System Bundle}.
	 * @return the system module
	 */
	public abstract SystemModule createSystemModule();
}
