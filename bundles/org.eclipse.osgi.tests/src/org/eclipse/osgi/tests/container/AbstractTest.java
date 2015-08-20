/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.tests.container.dummys.*;
import org.eclipse.osgi.util.ManifestElement;
import org.junit.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;

public abstract class AbstractTest {
	protected Set<ServiceRegistration<?>> serviceRegistrations;

	@Before
	public void setUp() {
		serviceRegistrations = new HashSet<ServiceRegistration<?>>();
	}

	@After
	public void tearDown() {
		for (ServiceRegistration<?> serviceRegistration : serviceRegistrations) {
			try {
				serviceRegistration.unregister();
			} catch (IllegalStateException e) {
				// Service was already unregistered.
			}
		}
	}

	protected DummyContainerAdaptor createDummyAdaptor() {
		return new DummyContainerAdaptor(new DummyCollisionHook(false), Collections.<String, String> emptyMap());
	}

	protected DummyContainerAdaptor createDummyAdaptor(ResolverHook hook) {
		return new DummyContainerAdaptor(new DummyCollisionHook(false), Collections.<String, String> emptyMap(), new DummyResolverHookFactory(hook));
	}

	protected DummyContainerAdaptor createDummyAdaptor(DebugOptions debugOptions) {
		return new DummyContainerAdaptor(new DummyCollisionHook(false), Collections.<String, String> emptyMap(), new DummyResolverHookFactory(), debugOptions);
	}

	protected Bundle getBundle() {
		return ((BundleReference) getClass().getClassLoader()).getBundle();
	}

	protected BundleContext getBundleContext() {
		return getBundle().getBundleContext();
	}

	protected Map<String, String> getManifest(String manifestFile) throws IOException, BundleException {
		URL manifest = getBundle().getEntry("/test_files/containerTests/" + manifestFile);
		Assert.assertNotNull("Could not find manifest: " + manifestFile, manifest);
		return ManifestElement.parseBundleManifest(manifest.openStream(), null);
	}

	protected Bundle getSystemBundle() {
		return getBundleContext().getBundle(0);
	}

	protected BundleContext getSystemBundleContext() {
		return getSystemBundle().getBundleContext();
	}

	protected Module installDummyModule(String manifestFile, String location, ModuleContainer container) throws BundleException, IOException {
		return installDummyModule(manifestFile, location, null, null, null, container);
	}

	protected Module installDummyModule(String manifestFile, String location, String alias, String extraExports, String extraCapabilities, ModuleContainer container) throws BundleException, IOException {
		Map<String, String> manifest = getManifest(manifestFile);
		ModuleRevisionBuilder builder = OSGiManifestBuilderFactory.createBuilder(manifest, alias, extraExports, extraCapabilities);
		Module system = container.getModule(0);
		return container.install(system, location, builder, null);
	}

	protected void registerService(Class<?> clazz, Object service) {
		serviceRegistrations.add(getSystemBundleContext().registerService(clazz.getName(), service, null));
	}

	protected void unregisterService(Object service) {
		for (ServiceRegistration<?> serviceRegistration : serviceRegistrations)
			if (getSystemBundleContext().getService(serviceRegistration.getReference()).equals(service))
				serviceRegistration.unregister();
	}
}
