/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.equinox.internal.transforms;

import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import org.osgi.framework.*;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Extender that scans for bundle header {@value #EQUINOX_TRANSFORMER_HEADER}
 * that declare a bundle wants to transform resources. The header can be either
 * a resource in the bundle (e.g. <code>/transform.csv</code>) in which case it
 * is assumed to use the default replace transformer, or can specify a
 * transformation type e.g. <code>xslt;/transform.csv</code>
 */
class TransformerBundleExtender implements BundleTrackerCustomizer<ServiceRegistration<URL>> {

	private static final String EQUINOX_TRANSFORMER_HEADER = "Equinox-Transformer"; //$NON-NLS-1$
	private BundleContext bundleContext;

	public TransformerBundleExtender(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public ServiceRegistration<URL> addingBundle(Bundle bundle, BundleEvent event) {
		TransformerInfo info = getTransformerInfo(bundle);
		if (info != null) {
			return bundleContext.registerService(URL.class, info.url(),
					FrameworkUtil.asDictionary(Map.of(TransformTuple.TRANSFORMER_TYPE, info.type())));
		}
		return null;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<URL> registration) {
		// state modifications are not relevant here
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<URL> registration) {
		registration.unregister();
	}

	private static TransformerInfo getTransformerInfo(Bundle bundle) {
		Dictionary<String, String> headers = bundle.getHeaders(""); //$NON-NLS-1$
		String header = headers.get(EQUINOX_TRANSFORMER_HEADER);
		if (header != null && !header.isBlank()) {
			String[] split = header.split(";", 2); //$NON-NLS-1$
			if (split.length == 2) {
				URL entry = bundle.getEntry(split[1]);
				if (entry != null) {
					return new TransformerInfo(split[0], entry);
				}
			} else {
				URL entry = bundle.getEntry(header);
				if (entry != null) {
					return new TransformerInfo(ReplaceTransformer.TYPE, entry);
				}
			}
		}
		return null;
	}

	private static record TransformerInfo(String type, URL url) {

	}

	static void start(BundleContext bundleContext) {
		BundleTracker<ServiceRegistration<URL>> tracker = new BundleTracker<>(bundleContext,
				Bundle.RESOLVED | Bundle.ACTIVE | Bundle.STARTING, new TransformerBundleExtender(bundleContext));
		tracker.open();
	}

}
