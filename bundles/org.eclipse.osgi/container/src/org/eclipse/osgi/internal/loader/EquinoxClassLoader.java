/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.loader;

import java.lang.reflect.Method;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.storage.BundleInfo.Generation;

public class EquinoxClassLoader extends ModuleClassLoader {
	private static final boolean EQUINOX_REGISTERED_AS_PARALLEL;

	static {
		boolean registeredAsParallel;
		try {
			Method parallelCapableMetod = ClassLoader.class.getDeclaredMethod("registerAsParallelCapable", (Class[]) null); //$NON-NLS-1$
			parallelCapableMetod.setAccessible(true);
			registeredAsParallel = ((Boolean) parallelCapableMetod.invoke(null, (Object[]) null)).booleanValue();
		} catch (Throwable e) {
			// must do everything to avoid failing in clinit
			registeredAsParallel = false;
		}
		EQUINOX_REGISTERED_AS_PARALLEL = registeredAsParallel;
	}

	private final EquinoxConfiguration configuration;
	private final Debug debug;
	private final BundleLoader delegate;
	private final Generation generation;
	// TODO Note that PDE has internal dependency on this field type/name (bug 267238)
	private final ClasspathManager manager;
	private final boolean isRegisteredAsParallel;

	/**
	 * Constructs a new DefaultClassLoader.
	 * @param parent the parent classloader
	 * @param configuration the equinox configuration
	 * @param delegate the delegate for this classloader
	 * @param generation the generation for this class loader
	 */
	public EquinoxClassLoader(ClassLoader parent, EquinoxConfiguration configuration, BundleLoader delegate, Generation generation) {
		super(parent);
		this.configuration = configuration;
		this.debug = configuration.getDebug();
		this.delegate = delegate;
		this.generation = generation;
		this.manager = new ClasspathManager(generation, this);
		this.isRegisteredAsParallel = (ModuleClassLoader.REGISTERED_AS_PARALLEL && EQUINOX_REGISTERED_AS_PARALLEL) || this.configuration.PARALLEL_CAPABLE;
	}

	protected final Generation getGeneration() {
		return this.generation;
	}

	public final ClasspathManager getClasspathManager() {
		return manager;
	}

	public final boolean isRegisteredAsParallel() {
		return isRegisteredAsParallel;
	}

	public final BundleLoader getBundleLoader() {
		return delegate;
	}

	@Override
	protected final Debug getDebug() {
		return debug;
	}

	@Override
	protected final EquinoxConfiguration getConfiguration() {
		return configuration;
	}
}
