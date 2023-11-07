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
package org.eclipse.osgi.container;

import java.io.DataInputStream;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.osgi.container.Module.Settings;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Adapts the behavior of a container.
 * @since 3.10
 */
public abstract class ModuleContainerAdaptor {
	private static Executor defaultExecutor = new Executor() {
		@Override
		public void execute(Runnable command) {
			command.run();
		}
	};

	/**
	 * Event types that may be {@link #publishContainerEvent(ContainerEvent, Module, Throwable, FrameworkListener...) published}
	 * for a container.
	 */
	public enum ContainerEvent {
		/**
		 * A container {@link ModuleContainer#refresh(java.util.Collection) refresh} operation has completed
		 */
		REFRESH,

		/**
		 * A container {@link ModuleContainer#getFrameworkStartLevel() start level} change has completed.
		 */
		START_LEVEL,

		/**
		 * The container has been started.
		 */
		STARTED,

		/**
		 * This event is returned by {@link SystemModule#waitForStop(long)}
		 * to indicate that the container has stopped.
		 */
		STOPPED,

		/**
		 * This event is returned by {@link SystemModule#waitForStop(long)}
		 * to indicate that the container has stopped because of an update
		 * operation.
		 */
		STOPPED_UPDATE,

		/**
		 * This event is returned by {@link SystemModule#waitForStop(long)}
		 * to indicate that the container has stopped because of an refresh
		 * operation.
		 */
		STOPPED_REFRESH,

		/**
		 * This event is returned by {@link SystemModule#waitForStop(long)}
		 * to indicate that the wait operation has timed out..
		 */
		STOPPED_TIMEOUT,

		/**
		 * An event fired for an error condition.
		 */
		ERROR,

		/**
		 * An event fired for a warning condition.
		 */
		WARNING,

		/**
		 * An event fired for informational purposes only.
		 */
		INFO
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
	 * @param current the current module loader associated with the wiring, may be <code>null</code>.
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

	/**
	 * This is called when the {@link SystemModule#init()} is running.
	 */
	public void initBegin() {
		// do nothing by default
	}

	/**
	 * This is called just before the {@link SystemModule#init()} returns.
	 */
	public void initEnd() {
		// do nothing by default
	}

	/**
	 * Returns the debug options for the module container.
	 * @return the debug options for the module container, or null if there are no debug options.
	 */
	public DebugOptions getDebugOptions() {
		// be default there are no debug options
		return null;
	}

	/**
	 * Returns the executor used to perform resolve operations
	 * @return the executor used to perform resolve operations
	 * @since 3.11
	 */
	public Executor getResolverExecutor() {
		return defaultExecutor;
	}

	/**
	 * Returns the executor used to by the
	 * {@link ModuleContainer#getFrameworkStartLevel() FrameworkStartLevel} implementation to
	 * start bundles that have the same start level.  This allows bundles to be
	 * started in parallel.
	 * @return the executor used by the {@link FrameworkStartLevel} implementation.
	 * @since 3.14
	 */
	public Executor getStartLevelExecutor() {
		return defaultExecutor;
	}

	/**
	 * Allows a builder to be modified before it is used by the container. This gets
	 * call when a new module is {@link ModuleContainer#install(Module, String, ModuleRevisionBuilder, Object) installed}
	 * into the container or when an existing module is {@link ModuleContainer#update(Module, ModuleRevisionBuilder, Object) updated}
	 * with a new revision.  The container does not call any methods on the builder before calling this method.
	 * @param operation The lifecycle operation event that is in progress using the supplied builder.
	 * This will be either {@link ModuleEvent#INSTALLED installed} or {@link ModuleEvent#UPDATED updated}.
	 * @param origin The module which originated the lifecycle operation. The origin may be {@code null} for
	 * {@link ModuleEvent#INSTALLED installed} operations.  This is the module
	 * passed to the {@link ModuleContainer#install(Module, String, ModuleRevisionBuilder, Object) install} or
	 * {@link ModuleContainer#update(Module, ModuleRevisionBuilder, Object) update} method.
	 * @param builder the builder that will be used to create a new {@link ModuleRevision}.
	 * @param revisionInfo the revision info that will be used for the new revision, may be {@code null}.
	 * @return The modified builder or a completely new builder to be used by the bundle.  A {@code null} value
	 * indicates the original builder should be used, which may have been modified by adding requirements or
	 * capabilities.
	 * @since 3.12
	 */
	public ModuleRevisionBuilder adaptModuleRevisionBuilder(ModuleEvent operation, Module origin, ModuleRevisionBuilder builder, Object revisionInfo) {
		// do nothing by default
		return null;
	}

	/**
	 * Returns the scheduled executor that may be used by the
	 * container to schedule background tasks.
	 * @return the scheduled executor, or null if background tasks are not supported
	 * @since 3.13
	 */
	public ScheduledExecutorService getScheduledExecutor() {
		return null;
	}
}
