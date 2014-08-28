/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.loader;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.BundleException;

/**
 * The System Bundle's BundleLoader.  This BundleLoader is used by ImportClassLoaders
 * to load a resource that is exported by the System Bundle.
 */
public class SystemBundleLoader extends BundleLoader {
	public static final String EQUINOX_EE = "x-equinox-ee"; //$NON-NLS-1$
	final ClassLoader classLoader;
	final ModuleClassLoader moduleClassLoader;

	public SystemBundleLoader(ModuleWiring wiring, EquinoxContainer container, ClassLoader frameworkLoader) {
		super(wiring, container, frameworkLoader.getParent());
		this.classLoader = frameworkLoader;
		this.moduleClassLoader = new SystemModuleClassLoader(classLoader.getParent(), container.getConfiguration(), this, (Generation) wiring.getRevision().getRevisionInfo());
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the class.
	 * This method never gets called because there is no BundleClassLoader for the framework.
	 */
	public Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> result = findLocalClass(name);
		if (result == null)
			throw new ClassNotFoundException(name);
		return result;
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the class. 
	 */
	public Class<?> findLocalClass(String name) {
		try {
			return classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			// do nothing
			return null;
		}
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 */
	public URL findLocalResource(String name) {
		return classLoader.getResource(name);
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 */
	public Enumeration<URL> findLocalResources(String name) {
		try {
			return classLoader.getResources(name);
		} catch (IOException e) {
			// do nothing
			return null;
		}
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 * This method never gets called because there is no ModuleClassLoader for the framework.
	 */
	public URL findResource(String name) {
		return findLocalResource(name);
	}

	/**
	 * The ClassLoader that loads OSGi framework classes is used to find the resource.
	 * This method never gets called because there is no ModuleClassLoader for the framework.
	 * @throws IOException 
	 */
	public Enumeration<URL> findResources(String name) throws IOException {
		return findLocalResources(name);
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public ModuleClassLoader getModuleClassLoader() {
		return moduleClassLoader;
	}

	@Override
	void loadClassLoaderFragments(Collection<ModuleRevision> fragments) {
		moduleClassLoader.loadFragments(fragments);
	}

	class SystemModuleClassLoader extends EquinoxClassLoader {

		public SystemModuleClassLoader(ClassLoader parent, EquinoxConfiguration configuration, BundleLoader delegate, Generation generation) {
			super(parent, configuration, delegate, generation);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			return SystemBundleLoader.this.findClass(name);
		}

		@Override
		public void loadFragments(Collection<ModuleRevision> fragments) {
			Module systemModule = getWiring().getRevision().getRevisions().getModule();
			for (ModuleRevision fragment : fragments) {
				try {
					this.getGeneration().getBundleInfo().getStorage().getExtensionInstaller().addExtensionContent(fragment, getWiring().getRevision().getRevisions().getModule());
				} catch (BundleException e) {
					systemModule.getContainer().getAdaptor().publishContainerEvent(ContainerEvent.ERROR, systemModule, e);
				}
			}
			getClasspathManager().loadFragments(fragments);
		}
	}
}
