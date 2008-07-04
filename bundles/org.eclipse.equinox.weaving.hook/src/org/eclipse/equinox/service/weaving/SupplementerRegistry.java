/*******************************************************************************
 * Copyright (c) 2008 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Matthew Webster           initial implementation
 *   Martin Lippert            supplementing mechanism reworked     
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.weaving.hooks.AbstractAspectJHook;
import org.eclipse.equinox.weaving.hooks.Supplementer;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;

public class SupplementerRegistry {
	
	/**
	 * Manifest header (named &quot;Supplement-Importer&quot;) identifying the names
	 * (and optionally, version numbers) of the packages that the bundle supplements.
	 * All importers of one of these packages will have the exported packages of this
	 * bundle added to their imports in addition.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	SUPPLEMENT_IMPORTER	= "Eclipse-SupplementImporter"; //$NON-NLS-1$

	/**
	 * Manifest header (named &quot;Supplement-Exporter&quot;) identifying the names
	 * (and optionally, version numbers) of the packages that the bundle supplements.
	 * All exporters of one of these packages will have the exported packages of this bundle 
	 * added to their imports list.
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	SUPPLEMENT_EXPORTER	= "Eclipse-SupplementExporter"; //$NON-NLS-1$

	//knibb
	/**
	 * Manifest header (named &quot;Supplement-Bundle&quot;) identifying the names
	 * (and optionally, version numbers) of any bundles supplemented by this bundle.
	 * All supplemented bundles will have all the exported packages of this bundle 
	 * added to their imports list
	 * 
	 * <p>
	 * The attribute value may be retrieved from the <code>Dictionary</code>
	 * object returned by the <code>Bundle.getHeaders</code> method.
	 */
	public static final String	SUPPLEMENT_BUNDLE = "Eclipse-SupplementBundle"; //$NON-NLS-1$

	private final Map supplementers; // keys of type String (symbolic name of supplementer bundle), values of type Supplementer
	private BundleContext context;
	private PackageAdmin packageAdmin;

	public SupplementerRegistry() {
		this.supplementers = new HashMap();
	}
	
	public void setBundleContext(BundleContext context) {
		this.context = context;
	}
	
	public void setPackageAdmin(PackageAdmin packageAdmin) {
		this.packageAdmin = packageAdmin;
	}
	
	public void addSupplementer(Bundle bundle) {
		Dictionary manifest = bundle.getHeaders();
		try {
			// First analyze which supplementers already exists for this bundle
			ManifestElement[] imports = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, (String) manifest.get(Constants.IMPORT_PACKAGE));
			ManifestElement[] exports = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, (String) manifest.get(Constants.EXPORT_PACKAGE));
			List supplementers = getSupplementers(bundle.getSymbolicName(), imports, exports);
			if (supplementers.size() > 0) {
				this.addSupplementedBundle(bundle, supplementers);
			}

			// Second analyze if this bundle itself is a supplementer
			ManifestElement[] supplementBundle = ManifestElement.parseHeader(SUPPLEMENT_BUNDLE, (String) manifest.get(SUPPLEMENT_BUNDLE));
			ManifestElement[] supplementImporter = ManifestElement.parseHeader(SUPPLEMENT_IMPORTER, (String) manifest.get(SUPPLEMENT_IMPORTER));
			ManifestElement[] supplementExporter = ManifestElement.parseHeader(SUPPLEMENT_EXPORTER, (String) manifest.get(SUPPLEMENT_EXPORTER));
	
			if (supplementBundle != null || supplementImporter != null || supplementExporter != null) {
				Supplementer newSupplementer = new Supplementer(bundle, supplementBundle, supplementImporter, supplementExporter);

				this.supplementers.put(bundle.getSymbolicName(), newSupplementer);
				resupplementInstalledBundles(newSupplementer);
			}
		}
		catch (BundleException e) {
		}
	}
	
	public void removeSupplementer(Bundle bundle) {
		// if this bundle is itself supplemented by others, remove the bundle from those lists
		removeSupplementedBundle(bundle);

		// if this bundle supplements other bundles, remove this supplementer and update the other bundles
		if (supplementers.containsKey(bundle.getSymbolicName())) {
			Supplementer supplementer = (Supplementer) supplementers.get(bundle.getSymbolicName());
			supplementers.remove(bundle.getSymbolicName());
			if (AbstractAspectJHook.verbose) System.err.println("[org.aspectj.osgi] info removing supplementer " + bundle.getSymbolicName());
			
			Bundle[] supplementedBundles = supplementer.getSupplementedBundles();
			for (int i = 0; i < supplementedBundles.length; i++) {
				Bundle supplementedBundle = supplementedBundles[i];
				if (supplementedBundle != null) {
					updateInstalledBundle(supplementedBundle);
				}
			}
		}
	}

	public List getSupplementers (String symbolicName, ManifestElement[] imports, ManifestElement[] exports) {
		List result = Collections.EMPTY_LIST;

		if (supplementers.size() > 0) {
			result = new LinkedList();
			for (Iterator i = supplementers.values().iterator(); i.hasNext();) {
				Supplementer supplementer = (Supplementer) i.next();
				if (isSupplementerMatching(symbolicName, imports, exports, supplementer)) {
					result.add(supplementer.getSymbolicName());
				}
			}
		}
		
		return result;
	}
	
	public Bundle[] getSupplementers(Bundle bundle) {
		List result = Collections.EMPTY_LIST;

		if (supplementers.size() > 0) {
			result = new ArrayList();
			for (Iterator i = supplementers.values().iterator(); i.hasNext();) {
				Supplementer supplementer = (Supplementer) i.next();
				if (supplementer.isSupplemented(bundle)) {
					result.add(supplementer.getSupplementerBundle());
				}
			}
		}
		
		return (Bundle[]) result.toArray(new Bundle[result.size()]);
	}

	public void addSupplementedBundle(Bundle supplementedBundle, List supplementers) {
		for (Iterator iterator = supplementers.iterator(); iterator.hasNext();) {
			String supplementersName = (String) iterator.next();
			if (this.supplementers.containsKey(supplementersName)) {
				Supplementer supplementer = (Supplementer) this.supplementers.get(supplementersName);
				supplementer.addSupplementedBundle(supplementedBundle);
			}
		}
	}

	public void removeSupplementedBundle(Bundle bundle) {
		for (Iterator iterator = this.supplementers.values().iterator(); iterator.hasNext();) {
			Supplementer supplementer = (Supplementer) iterator.next();
			supplementer.removeSupplementedBundle(bundle);
		}
	}

	private boolean isSupplementerMatching(String symbolicName,
			ManifestElement[] imports, ManifestElement[] exports, Supplementer supplementer) {
		String supplementerName = supplementer.getSymbolicName();
		if (!supplementerName.equals(symbolicName)) {
			if (supplementer.matchSupplementer(symbolicName)
					|| (imports != null && supplementer.matchesSupplementImporter(imports))
					|| (exports != null && supplementer.matchesSupplementExporter(exports))) {
				return true;
			}
		}
		return false;
	}
	
	private void resupplementInstalledBundles(Supplementer supplementer) {
		Bundle[] installedBundles = context.getBundles();
		
		for (int i = 0; i < installedBundles.length; i++) {
			try {
				Bundle bundle = installedBundles[i];
				
				if (bundle.getSymbolicName().equals(supplementer.getSymbolicName())) {
					continue;
				}
				
				Dictionary manifest = bundle.getHeaders();
				ManifestElement[] imports = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, (String) manifest.get(Constants.IMPORT_PACKAGE));
				ManifestElement[] exports = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, (String) manifest.get(Constants.EXPORT_PACKAGE));
				
				if (isSupplementerMatching(bundle.getSymbolicName(), imports, exports, supplementer)) {
					boolean alreadyRequired = false;
					ManifestElement[] requires = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, (String) manifest.get(Constants.REQUIRE_BUNDLE));
					if (requires != null) {
						for (int j = 0; j < requires.length; j++) {
							if (requires[j].getValue().equals(supplementer.getSymbolicName())) {
								alreadyRequired = true;
							}
						}
					}
					
					if (!alreadyRequired) {
						updateInstalledBundle(bundle);
					}
				}
				
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void updateInstalledBundle(Bundle bundle) {
		
		String symbolicName = bundle.getSymbolicName();
		
		if (symbolicName.equals("org.eclipse.osgi")) return;
		if (symbolicName.equals("org.aspectj.osgi")) return;
		if (symbolicName.startsWith("org.eclipse.update")) return;
		if (symbolicName.startsWith("org.eclipse.core.runtime")) return;
		if (symbolicName.startsWith("org.aspectj.osgi.service")) return;
		if (symbolicName.startsWith("org.eclipse.equinox")) return;

		if (AbstractAspectJHook.verbose) System.err.println("[org.aspectj.osgi] info triggering update for re-supplementing " + symbolicName);

		try {
			int initialstate = (bundle.getState() | (Bundle.ACTIVE | Bundle.STARTING));
			if (initialstate != 0 && packageAdmin != null && packageAdmin.getBundleType(bundle) != PackageAdmin.BUNDLE_TYPE_FRAGMENT)
				bundle.stop(Bundle.STOP_TRANSIENT);
			bundle.update();
		}
		catch (BundleException e) {
			e.printStackTrace();
		}
	}

}
