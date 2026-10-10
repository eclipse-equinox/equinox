/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.framework;

import java.util.concurrent.Executor;
import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.eclipse.osgi.container.ModuleContainerAdaptor;
import org.osgi.service.resolver.Resolver;

/**
 * The {@link ResolverFactory} abstract the creation of resolver instance, this
 * allows to exchange or further customize the instance based on e.g. the caller
 * or context.
 */
public class ResolverFactory {

	/**
	 * Creates a resolver instance that is used to be registered in the OSGi service
	 * factory, this method is usually only called once in the life-time of a
	 * framework.
	 * 
	 * @param container the container this instance is created for to maybe further
	 *                  customize the instance
	 * @return a new resolver instance
	 */
	static Resolver createResolverService(EquinoxContainer container) {
		return new ResolverImpl(new Logger(0), null);
	}

	/**
	 * Creates a resolver instance that is used in the Equinox framework directly,
	 * this method is called whenever a resolve of bundles is performed
	 * 
	 * @param container the container this instance is created for to maybe further
	 *                  customize the instance
	 * @param logger    a logger to emit log messages
	 * @param executor  an executor to use to perform session related tasks
	 * @return a new resolver instance
	 */
	public static Resolver createFrameworkResolver(ModuleContainerAdaptor container, Logger logger, Executor executor) {
		return new ResolverImpl(logger, executor);
	}

	/**
	 * Creates a resolver instance for dynamic requirement resolving, this method is
	 * called whenever a new dynmic requirement resolve operation is needed
	 * 
	 * @param container the container this instance is created for to maybe further
	 *                  customize the instance
	 * @return a new resolver instance
	 */
	public static Resolver createDynamicResolver(ModuleContainerAdaptor container) {
		return new ResolverImpl(new Logger(0), null);
	}

}
