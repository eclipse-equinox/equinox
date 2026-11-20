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

import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.osgi.service.resolver.Resolver;

/**
 * The {@link ResolverFactory} abstracts the creation of resolver instances,
 * this allows to exchange or further customize the instance based on the
 * context in which it is used.
 * <p>
 * The implementation can be chosen with framework properties. For every use
 * case a specific property is consulted first, if it is not set the global
 * {@link #PROP_RESOLVER_IMPLEMENTATION} is used. If none is set (or the value
 * is {@link #DEFAULT_RESOLVER}) the built-in resolver is used. Any other value
 * is the fully qualified class name of a {@link Resolver} that must be
 * discoverable with the {@link ServiceLoader} (see
 * <code>META-INF/services/org.osgi.service.resolver.Resolver</code>) and
 * provide a public no-arg constructor.
 */
public class ResolverFactory {

	/** Global property selecting the resolver for all use cases. */
	public static final String PROP_RESOLVER_IMPLEMENTATION = "equinox.resolver.implementation"; //$NON-NLS-1$
	/** Selects the resolver registered as OSGi service. */
	public static final String PROP_RESOLVER_IMPLEMENTATION_SERVICE = PROP_RESOLVER_IMPLEMENTATION + ".service"; //$NON-NLS-1$
	/** Selects the resolver used by the framework to resolve bundles. */
	public static final String PROP_RESOLVER_IMPLEMENTATION_FRAMEWORK = PROP_RESOLVER_IMPLEMENTATION + ".framework"; //$NON-NLS-1$
	/** Selects the resolver used to resolve dynamic imports. */
	public static final String PROP_RESOLVER_IMPLEMENTATION_DYNAMIC = PROP_RESOLVER_IMPLEMENTATION + ".dynamic"; //$NON-NLS-1$
	/** Property value that selects the built-in resolver. */
	public static final String DEFAULT_RESOLVER = "default"; //$NON-NLS-1$

	/**
	 * Creates a resolver instance that is used to be registered in the OSGi service
	 * registry, this method is usually only called once in the life-time of a
	 * framework.
	 *
	 * @param config provides the framework configuration properties
	 * @return a new resolver instance
	 */
	public static Resolver createResolverService(Function<String, String> config) {
		Resolver custom = load(config, PROP_RESOLVER_IMPLEMENTATION_SERVICE);
		return custom != null ? custom : new ResolverImpl(new Logger(0), null);
	}

	/**
	 * Creates a resolver instance that is used in the Equinox framework directly,
	 * this method is called whenever a resolve of bundles is performed.
	 *
	 * @param config   provides the framework configuration properties
	 * @param logger   a logger to emit log messages, only used by the built-in
	 *                 resolver
	 * @param executor an executor to use to perform session related tasks, only
	 *                 used by the built-in resolver
	 * @return a new resolver instance
	 */
	public static Resolver createFrameworkResolver(Function<String, String> config, Logger logger,
			Executor executor) {
		Resolver custom = load(config, PROP_RESOLVER_IMPLEMENTATION_FRAMEWORK);
		return custom != null ? custom : new ResolverImpl(logger, executor);
	}

	/**
	 * Creates a resolver instance for dynamic requirement resolving, this method is
	 * called whenever a new dynamic requirement resolve operation is needed.
	 *
	 * @param config provides the framework configuration properties
	 * @return a new resolver instance
	 */
	public static Resolver createDynamicResolver(Function<String, String> config) {
		Resolver custom = load(config, PROP_RESOLVER_IMPLEMENTATION_DYNAMIC);
		return custom != null ? custom : new ResolverImpl(new Logger(0), null);
	}

	private static Resolver load(Function<String, String> config, String specificProperty) {
		String name = config.apply(specificProperty);
		if (name == null || name.trim().isEmpty()) {
			name = config.apply(PROP_RESOLVER_IMPLEMENTATION);
		}
		if (name == null || name.trim().isEmpty() || DEFAULT_RESOLVER.equals(name.trim())) {
			return null;
		}
		name = name.trim();
		for (Resolver resolver : ServiceLoader.load(Resolver.class, ResolverFactory.class.getClassLoader())) {
			if (resolver.getClass().getName().equals(name)) {
				return resolver;
			}
		}
		throw new IllegalStateException("No resolver implementation '" + name //$NON-NLS-1$
				+ "' found by the ServiceLoader, check the framework properties " + PROP_RESOLVER_IMPLEMENTATION //$NON-NLS-1$
				+ " and " + specificProperty); //$NON-NLS-1$
	}

}
