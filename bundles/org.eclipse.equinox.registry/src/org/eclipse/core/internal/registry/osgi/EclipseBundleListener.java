/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry.osgi;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.internal.registry.RegistryMessages;
import org.eclipse.core.internal.runtime.ResourceTranslator;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

/**
 * A listener for bundle events.  When a bundles come and go we look to see 
 * if there are any extensions or extension points and update the registry accordingly.
 * Using a Synchronous listener here is important. If the
 * bundle activator code tries to access the registry to get its extension
 * points, we need to ensure that they are in the registry before the
 * bundle start is called. By listening sync we are able to ensure that
 * happens.
 */
public class EclipseBundleListener implements SynchronousBundleListener {
	private static final String PLUGIN_MANIFEST = "plugin.xml"; //$NON-NLS-1$
	private static final String FRAGMENT_MANIFEST = "fragment.xml"; //$NON-NLS-1$	

	private ExtensionRegistry registry;
	private RegistryStrategyOSGI strategy;
	private Object token;
	private HashMap dynamicAddStateStamps = new HashMap();
	private long currentStateStamp[] = new long[] {0};

	public EclipseBundleListener(ExtensionRegistry registry, Object key, RegistryStrategyOSGI strategy) {
		this.registry = registry;
		this.token = key;
		this.strategy = strategy;
	}

	public void bundleChanged(BundleEvent event) {
		/* Only should listen for RESOLVED and UNRESOLVED events.  
		 * 
		 * When a bundle is updated the Framework will publish an UNRESOLVED and 
		 * then a RESOLVED event which should cause the bundle to be removed 
		 * and then added back into the registry.  
		 * 
		 * When a bundle is uninstalled the Framework should publish an UNRESOLVED 
		 * event and then an UNINSTALLED event so the bundle will have been removed 
		 * by the UNRESOLVED event before the UNINSTALLED event is published.
		 * 
		 * When a bundle is refreshed from PackageAdmin an UNRESOLVED event will be
		 * published which will remove the bundle from the registry.  If the bundle
		 * can be RESOLVED after a refresh then a RESOLVED event will be published 
		 * which will add the bundle back.  This is required because the classloader
		 * will have been refreshed for the bundle so all extensions and extension
		 * points for the bundle must be refreshed.
		 */
		Bundle bundle = event.getBundle();
		switch (event.getType()) {
			case BundleEvent.RESOLVED :
				synchronized (currentStateStamp) {
					long newStateStamp = registry.computeState();
					if (currentStateStamp[0] != newStateStamp) {
						// new state stamp; clear the dynamicaddStateStamps
						currentStateStamp[0] = newStateStamp;
						dynamicAddStateStamps.clear();
					}
				}
				addBundle(bundle, true);
				break;
			case BundleEvent.UNRESOLVED :
				removeBundle(bundle);
				break;
		}
	}

	public void processBundles(Bundle[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			if (isBundleResolved(bundles[i]))
				addBundle(bundles[i], false);
			else
				removeBundle(bundles[i]);
		}
	}

	private boolean isBundleResolved(Bundle bundle) {
		return (bundle.getState() & (Bundle.RESOLVED | Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING)) != 0;
	}

	private void removeBundle(Bundle bundle) {
		long timestamp = 0;
		if (strategy.checkContributionsTimestamp()) {
			URL pluginManifest = getExtensionURL(bundle, false);
			if (pluginManifest != null)
				timestamp = strategy.getExtendedTimestamp(bundle, pluginManifest);
		}
		registry.remove(Long.toString(bundle.getBundleId()), timestamp);
	}

	static public URL getExtensionURL(Bundle bundle, boolean report) {
		// bail out if system bundle
		if (bundle.getBundleId() == 0)
			return null;
		// bail out if the bundle does not have a symbolic name
		if (bundle.getSymbolicName() == null)
			return null;

		boolean isFragment = OSGIUtils.getDefault().isFragment(bundle);
		String manifestName = isFragment ? FRAGMENT_MANIFEST : PLUGIN_MANIFEST;
		URL extensionURL = bundle.getEntry(manifestName);
		if (extensionURL == null)
			return null;

		// If the bundle is not a singleton, then it is not added
		if (!isSingleton(bundle)) {
			if (report && !isGeneratedManifest(bundle)) {
				String message = NLS.bind(RegistryMessages.parse_nonSingleton, bundle.getSymbolicName());
				RuntimeLog.log(new Status(IStatus.WARNING, RegistryMessages.OWNER_NAME, 0, message, null));
			}
			return null;
		}
		if (!isFragment)
			return extensionURL;

		// If the bundle is a fragment being added to a non singleton host, then it is not added
		Bundle[] hosts = OSGIUtils.getDefault().getHosts(bundle);
		if (hosts == null)
			return null; // should never happen?

		if (isSingleton(hosts[0]))
			return extensionURL;

		if (report) {
			// if the host is not a singleton we always report the error; even if the host has a generated manifest
			String message = NLS.bind(RegistryMessages.parse_nonSingletonFragment, bundle.getSymbolicName(), hosts[0].getSymbolicName());
			RuntimeLog.log(new Status(IStatus.WARNING, RegistryMessages.OWNER_NAME, 0, message, null));
		}
		return null;
	}

	private static boolean isGeneratedManifest(Bundle bundle) {
		return bundle.getHeaders("").get("Generated-from") != null; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void addBundle(Bundle bundle, boolean checkNLSFragments) {
		if (checkNLSFragments)
			checkForNLSFragment(bundle);
		// if the given bundle already exists in the registry then return.
		// note that this does not work for update cases.
		IContributor contributor = ContributorFactoryOSGi.createContributor(bundle);
		if (registry.hasContributor(contributor))
			return;
		URL pluginManifest = getExtensionURL(bundle, true);
		if (pluginManifest == null)
			return;
		InputStream is;
		try {
			is = new BufferedInputStream(pluginManifest.openStream());
		} catch (IOException ex) {
			is = null;
		}
		if (is == null)
			return;

		ResourceBundle translationBundle = null;
		try {
			translationBundle = ResourceTranslator.getResourceBundle(bundle);
		} catch (MissingResourceException e) {
			//Ignore the exception
		}
		long timestamp = 0;
		if (strategy.checkContributionsTimestamp())
			timestamp = strategy.getExtendedTimestamp(bundle, pluginManifest);
		registry.addContribution(is, contributor, true, pluginManifest.getPath(), translationBundle, token, timestamp);
	}

	private void checkForNLSFragment(Bundle bundle) {
		if (!OSGIUtils.getDefault().isFragment(bundle)) {
			// only need to worry about fragments
			synchronized (currentStateStamp) {
				// mark this host as processed for the current state stamp.
				dynamicAddStateStamps.put(Long.toString(bundle.getBundleId()), new Long(currentStateStamp[0]));
			}
			return;
		}
		Bundle[] hosts = OSGIUtils.getDefault().getHosts(bundle);
		if (hosts == null)
			return;
		// check to see if the hosts should be refreshed because the fragment contains NLS properties files.
		for (int i = 0; i < hosts.length; i++)
			checkForNLSFiles(hosts[i], bundle);
	}

	private void checkForNLSFiles(Bundle host, Bundle fragment) {
		String hostID = Long.toString(host.getBundleId());

		synchronized (currentStateStamp) {
			Long hostStateStamp = (Long) dynamicAddStateStamps.get(hostID);
			if (hostStateStamp != null && currentStateStamp[0] == hostStateStamp.longValue())
				return; // already processed this host
		}

		Bundle[] fragments = OSGIUtils.getDefault().getFragments(host);
		boolean refresh = false;
		// check host first
		if (hasNLSFilesFor(host, fragment)) {
			refresh = true;
		} else {
			// check the fragment provides NLS for other fragments of this host
			for (int i = 0; i < fragments.length && !refresh; i++) {
				if (fragment.equals(fragments[i]))
					continue; // skip fragment that was just resolved; it will be added in by the caller
				if (hasNLSFilesFor(fragments[i], fragment)) {
					refresh = true;
				}
			}
		}
		if (refresh) {
			// force the host and fragments to be removed and added back
			removeBundle(host);
			addBundle(host, false);
			for (int i = 0; i < fragments.length; i++) {
				if (fragment.equals(fragments[i]))
					continue; // skip fragment that was just resolved; it will be added in by the caller
				removeBundle(fragments[i]);
				addBundle(fragments[i], false);
			}
			synchronized (currentStateStamp) {
				// mark this host as processed for the current state stamp.
				dynamicAddStateStamps.put(hostID, new Long(currentStateStamp[0]));
			}
		}
	}

	private boolean hasNLSFilesFor(Bundle target, Bundle fragment) {
		if (!registry.hasContributor(Long.toString(target.getBundleId())))
			return false;
		// get the base localization path from the target
		Dictionary targetHeaders = target.getHeaders(""); //$NON-NLS-1$
		String localization = (String) targetHeaders.get(Constants.BUNDLE_LOCALIZATION);
		if (localization == null)
			// localization may be empty in which case we should check the default
			localization = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
		// we do a simple check to make sure the default nls path exists in the target; 
		// this is for performance reasons, but I'm not sure it is valid because a target could ship without the default nls properties file but this seems very unlikely
		URL baseNLS = target.getEntry(localization + ".properties"); //$NON-NLS-1$
		if (baseNLS == null)
			return false;
		int lastSlash = localization.lastIndexOf('/');
		if (lastSlash == localization.length() - 1)
			return false; // just to be safe
		String baseDir = lastSlash < 0 ? "" : localization.substring(0, lastSlash); //$NON-NLS-1$
		String filePattern = (lastSlash < 0 ? localization : localization.substring(lastSlash + 1)) + "_*.properties"; //$NON-NLS-1$
		Enumeration nlsFiles = fragment.findEntries(baseDir, filePattern, false);
		return nlsFiles != null;
	}

	private static boolean isSingleton(Bundle bundle) {
		Dictionary allHeaders = bundle.getHeaders(""); //$NON-NLS-1$
		String symbolicNameHeader = (String) allHeaders.get(Constants.BUNDLE_SYMBOLICNAME);
		try {
			if (symbolicNameHeader != null) {
				ManifestElement[] symbolicNameElements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameHeader);
				if (symbolicNameElements.length > 0) {
					String singleton = symbolicNameElements[0].getDirective(Constants.SINGLETON_DIRECTIVE);
					if (singleton == null)
						singleton = symbolicNameElements[0].getAttribute(Constants.SINGLETON_DIRECTIVE);

					if (!"true".equalsIgnoreCase(singleton)) { //$NON-NLS-1$
						String manifestVersion = (String) allHeaders.get(org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION);
						if (manifestVersion == null) {//the header was not defined for previous versions of the bundle
							//3.0 bundles without a singleton attributes are still being accepted
							if (OSGIUtils.getDefault().getBundle(symbolicNameElements[0].getValue()) == bundle)
								return true;
						}
						return false;
					}
				}
			}
		} catch (BundleException e1) {
			//This can't happen because the fwk would have rejected the bundle
		}
		return true;
	}
}
