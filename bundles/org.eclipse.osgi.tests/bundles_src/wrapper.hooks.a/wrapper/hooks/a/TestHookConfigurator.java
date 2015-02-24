/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wrapper.hooks.a;

import java.net.URL;
import java.util.Collection;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.internal.hookregistry.*;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class TestHookConfigurator implements HookConfigurator {
	public void addHooks(HookRegistry hookRegistry) {
		BundleFileWrapperFactoryHook noop = new BundleFileWrapperFactoryHook() {
			@Override
			public BundleFileWrapper wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base) {
				return new BundleFileWrapper(bundleFile) {
					// Do nothing to test multiple wrappers
				};
			}
		};
		// add no-op before the getResourceURL override
		hookRegistry.addBundleFileWrapperFactoryHook(noop);

		hookRegistry.addBundleFileWrapperFactoryHook(new BundleFileWrapperFactoryHook() {

			@Override
			public BundleFileWrapper wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base) {
				return new BundleFileWrapper(bundleFile) {
					@Override
					public URL getResourceURL(String path, Module hostModule, int index) {
						URL url = super.getResourceURL(path, hostModule, index);
						if (url != null) {
							try {
								return converter(hostModule, url);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						return null;
					}

					private URL converter(Module module, URL url) throws Exception {
						// This is just test code, don't get the URLConverter this way in real code
						BundleContext systemContext = module.getContainer().getModule(0).getBundle().getBundleContext();
						Collection<ServiceReference<URLConverter>> converters = systemContext.getServiceReferences(URLConverter.class, "(protocol=" + url.getProtocol() + ")");
						return systemContext.getService(converters.iterator().next()).resolve(url);
					}

				};
			}
		});

		// And add no-op after
		hookRegistry.addBundleFileWrapperFactoryHook(noop);
	}
}
