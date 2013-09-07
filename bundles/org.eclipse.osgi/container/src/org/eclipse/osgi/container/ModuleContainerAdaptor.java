/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
import java.util.EnumSet;
import org.eclipse.osgi.container.Module.Settings;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

/**
 * Adapts the behavior of a container.
 * @since 3.10
 */
public abstract class ModuleContainerAdaptor {
	public enum ContainerEvent {
		REFRESH, START_LEVEL, STARTED, STOPPED, STOPPED_UPDATE, STOPPED_REFRESH, STOPPED_TIMEOUT, ERROR, WARNING, INFO
	}

	/**
	 * Event types that may be {@link #publishModuleEvent(ModuleEvent, Module, Module) published} for a module
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
	 * No locks are held by the container when this method is called
	 * @param type the type of event
	 * @param module the module associated with the event
	 * @param error the error associated with the event, may be {@code null}
	 * @param listeners additional listeners to publish the event to synchronously
	 */
	public abstract void publishContainerEvent(ContainerEvent type, Module module, Throwable error, FrameworkListener... listeners);

	/**
	 * Publishes the specified module event type for the specified module.
	 * No locks are held by the container when this method is called
	 * @param type the event type to publish
	 * @param module the module the event is associated with
	 * @param origin the module which is the origin of the event. For the event
	 *        type {@link ModuleEvent#INSTALLED}, this is the module whose context was used
	 *        to install the module. Otherwise it is the module itself. May be null only
	 *        when the event is not of type {@link ModuleEvent#INSTALLED}.
	 */
	public abstract void publishModuleEvent(ModuleEvent type, Module module, Module origin);

	/**
	 * Returns the specified configuration property value
	 * @param key the key of the configuration property
	 * @return the configuration property value
	 */
	public String getProperty(String key) {
		return null;
	}

	/**
	 * Creates a new {@link ModuleLoader} for the specified wiring.
	 * @param wiring the module wiring to create a module loader for
	 * @return a new {@link ModuleLoader} for the specified wiring.
	 */
	public ModuleLoader createModuleLoader(ModuleWiring wiring) {
		throw new UnsupportedOperationException("Container adaptor does not support module class loaders."); //$NON-NLS-1$
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

	/**
	 * Returns the current revision info for a module with the specified location and id
	 * @param location the location of the module
	 * @param id the id of the module
	 * @return the revision info, may be {@code null}
	 */
	public Object getRevisionInfo(String location, long id) {
		return null;
	}

	/**
	 * After a revision is created this method is called with the specified revision info.
	 * @param revision the newly created revision
	 * @param revisionInfo the revision info that got associated with the revision
	 */
	public void associateRevision(ModuleRevision revision, Object revisionInfo) {
		// do nothing by default
	}

	/**
	 * This is called when a wiring is made invalid and allows the adaptor to react 
	 * to this.  This method is called while holding state change lock for the
	 * module as well as for the module database.  Care must be taken not to introduce
	 * deadlock.
	 * @param moduleWiring the module wiring being invalidated
	 * @param current the current module loader associated with the wiring
	 */
	public void invalidateWiring(ModuleWiring moduleWiring, ModuleLoader current) {
		// do nothing by default
	}

	/**
	 * This is called if a request to refresh modules causes the system module
	 * to be refreshed.  This causes the system module to be stopped in a back
	 * ground thread.  This method is called before the background thread is 
	 * started to stop the system module.
	 */
	public void refreshedSystemModule() {
		// do nothing by default
	}

	/**
	 * This is called whenever the module database has been updated.
	 */
	public void updatedDatabase() {
		// do nothing by default
	}
}
