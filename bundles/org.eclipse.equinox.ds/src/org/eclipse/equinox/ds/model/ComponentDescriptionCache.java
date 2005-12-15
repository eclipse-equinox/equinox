/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.model;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.ds.Activator;
import org.eclipse.equinox.ds.Log;
import org.eclipse.equinox.ds.parser.Parser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * 
 * Cache of component descriptions.
 * 
 * @version $Revision: 1.2 $
 */
public class ComponentDescriptionCache {

	private static final String CACHE_FILE = "cdcache.ser";
	private static final String CACHE_XML_PROPERTY = "declarativeservices.cacheXML";

	private File cacheFile;
	private final boolean CACHE_XML;
	private Activator main;
	/**
	 * Cache of BundleId:CD
	 */
	private Map cdCache;

	/**
	 * Cache of BundleId:BundleLastModifiedTime
	 */
	private Map lastModifiedTimes;

	/**
	 * Parser object to use if the bundle's information is not in the cache or
	 * is stale.
	 */
	private Parser parser;

	/**
	 * Create the cache object.
	 * 
	 * @param main Main object.
	 */
	public ComponentDescriptionCache(Activator main) {
		this.main = main;
		parser = new Parser(main);

		//check if we're supposed to cache the XML (defaults to true)
		CACHE_XML = Boolean.valueOf(System.getProperty(CACHE_XML_PROPERTY, "true")).booleanValue();

		//load cache
		cacheFile = main.context.getDataFile(CACHE_FILE);
		if (cacheFile.exists() && CACHE_XML) {
			try {
				FileInputStream in = new FileInputStream(cacheFile);
				ObjectInputStream cacheStream = new ObjectInputStream(in);
				cdCache = (Hashtable) cacheStream.readObject();
				lastModifiedTimes = (Hashtable) cacheStream.readObject();
				cacheStream.close();
				in.close();
			} catch (Exception e) {
				//should this be an info, warning, or error?
				//some devices don't have filesystems
				Log.log(LogService.LOG_WARNING, "Could not read Service Component XML cache", e);

				cdCache = new Hashtable();
				lastModifiedTimes = new Hashtable();
			}
		} else {
			cdCache = new Hashtable();
			lastModifiedTimes = new Hashtable();
		}
	}

	/**
	 * Return the component descriptions for the specified bundle.
	 * 
	 * @param bundle Bundle for which component description are to be returns
	 * @return An array list of the component descriptions for the specified
	 *         bundle.
	 */
	public List getComponentDescriptions(BundleContext bundleContext) {
		//check to see if we already have the cds for this bundle up-to-date in cache
		Bundle bundle = bundleContext.getBundle();
		Long bundleId = new Long(bundle.getBundleId());
		Long cacheTime = (Long) lastModifiedTimes.get(bundleId);
		Long lastModified = new Long(bundle.getLastModified());

		List cds = null;
		if (cacheTime != null && cacheTime.equals(lastModified)) {
			//we have the bundle cached
			cds = (List) cdCache.get(bundleId);
		} else {
			//not cached or out of date
			cds = parser.getComponentDescriptions(bundleContext);
			if (!cds.isEmpty()) {
				lastModifiedTimes.put(bundleId, lastModified);
				cdCache.put(bundleId, cds);
			}
		}

		//set the bundle context of each CD - these can not be cached because
		//a bundle gets a new bundle context every time it is initialized (started)
		Iterator it = cds.iterator();
		while (it.hasNext()) {
			((ComponentDescription) it.next()).setBundleContext(bundleContext);
		}

		return cds;
	}

	public void dispose() {
		parser.dispose();

		//write cache
		if (CACHE_XML) {

			//clean out cache - only save entries for bundles that are
			//still installed
			Bundle[] installedBundles = main.context.getBundles();
			Set installedBundleIDs = new HashSet(installedBundles.length);
			for (int i = 0; i < installedBundles.length; i++) {
				installedBundleIDs.add(new Long(installedBundles[i].getBundleId()));
			}
			cdCache.keySet().retainAll(installedBundleIDs);
			lastModifiedTimes.keySet().retainAll(installedBundleIDs);

			//write to disk
			try {
				FileOutputStream out = new FileOutputStream(cacheFile);
				ObjectOutputStream cacheStream = new ObjectOutputStream(out);
				cacheStream.writeObject(cdCache);
				cacheStream.writeObject(lastModifiedTimes);
				cacheStream.flush();
				cacheStream.close();
				out.close();
			} catch (Exception e) {
				//should this be an info, warning, or error?
				//some devices don't have filesystems
				Log.log(LogService.LOG_WARNING, "Could not write Service Component XML cache", e);

				//delete the cache file if possible
				try {
					cacheFile.delete();
				} catch (Exception e2) {
					Log.log(LogService.LOG_INFO, "Could not delete Service Component XML cache", e2);
				}
			}
		} //end if(CACHE_XML)
		cacheFile = null;
		cdCache = null;
		lastModifiedTimes = null;
	}

}
