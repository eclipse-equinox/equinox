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

import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.service.resolver.Resolver;

/**
 * Adapts the behavior of a container.
 */
public abstract class ModuleContainerAdaptor {
	public enum ContainerEvent {
		REFRESH, START_LEVEL, ERROR, WARNING, INFO
	}

	/**
	 * Returns the resolver the container will use.
	 * @return the resolver the container will use.
	 */
	public abstract Resolver getResolver();

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
	 * 
	 * @param type the type of event
	 * @param module the module associated with the event
	 * @param error the error associated with the event, may be {@code null}
	 * @param listeners additional listeners to publish the event to synchronously
	 */
	public abstract void publishContainerEvent(ContainerEvent type, Module module, Throwable error, FrameworkListener... listeners);
}
