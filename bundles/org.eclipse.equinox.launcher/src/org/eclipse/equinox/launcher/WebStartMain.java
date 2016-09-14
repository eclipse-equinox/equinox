/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.launcher;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

/**
 * The launcher to start eclipse using webstart. To use this launcher, the client 
 * must accept to give all security permissions.
 * <p>
 * <b>Note:</b> This class should not be referenced programmatically by
 * other Java code. This class exists only for the purpose of launching Eclipse
 * using Java webstart. To launch Eclipse programmatically, use 
 * org.eclipse.core.runtime.adaptor.EclipseStarter. The fields and methods
 * on this class are not API.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
//The bundles are discovered by finding all the jars on the classpath. Then they are added with their full path to the osgi.bundles list.
public class WebStartMain extends Main {
	private static final String PROP_WEBSTART_AUTOMATIC_INSTALLATION = "eclipse.webstart.automaticInstallation"; //$NON-NLS-1$
	private static final String DEFAULT_OSGI_BUNDLES = "org.eclipse.equinox.common@2:start, org.eclipse.core.runtime@start"; //$NON-NLS-1$
	private static final String PROP_OSGI_BUNDLES = "osgi.bundles"; //$NON-NLS-1$
	private static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration"; //$NON-NLS-1$

	private Map allBundles = null; // Map of all the bundles found on the classpath. Id -> ArrayList of BundleInfo
	private List bundleList = null; //The list of bundles found on the osgi.bundle list 

	protected class BundleInfo {
		String bsn;
		String version;
		String startData;
		String location;
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static void main(String[] args) {
		System.setSecurityManager(null); //TODO Hack so that when the classloader loading the fwk is created we don't have funny permissions. This should be revisited. 
		int result = new WebStartMain().run(args);
		if (!Boolean.getBoolean(PROP_NOSHUTDOWN))
			System.exit(result);
	}

	private void setDefaultBundles() {
		if (System.getProperty(PROP_OSGI_BUNDLES) != null)
			return;
		System.getProperties().put(PROP_OSGI_BUNDLES, DEFAULT_OSGI_BUNDLES);
	}

	protected void basicRun(String[] args) throws Exception {
		setDefaultBundles();
		initializeBundleListStructure();
		discoverBundles();
		//Set the fwk location since the regular lookup would not find it
		String fwkURL = searchFor(framework, null);
		if (fwkURL == null) {
			//MESSAGE CAN"T FIND THE FWK
		}
		allBundles.remove(framework);
		System.getProperties().put(PROP_FRAMEWORK, fwkURL);
		super.basicRun(args);
	}

	protected void beforeFwkInvocation() {
		// set the check config option so we pick up modified bundle jars (bug 152825)
		if (System.getProperty(PROP_CHECK_CONFIG) == null)
			System.getProperties().put(PROP_CHECK_CONFIG, "true"); //$NON-NLS-1$
		buildOSGiBundleList();
		cleanup();
	}

	/*
	 * Null out all the fields containing data 
	 */
	private void cleanup() {
		allBundles = null;
		bundleList = null;
	}

	/*
	 * Find the target bundle among all the bundles that are on the classpath.
	 * The start parameter is not used in this context
	 */
	protected String searchFor(final String target, String start) {
		ArrayList matches = (ArrayList) allBundles.get(target);
		if (matches == null)
			return null;
		int numberOfMatches = matches.size();
		if (numberOfMatches == 1) {
			return ((BundleInfo) matches.get(0)).location;
		}
		if (numberOfMatches == 0)
			return null;

		String[] versions = new String[numberOfMatches];
		int highest = 0;
		for (int i = 0; i < versions.length; i++) {
			versions[i] = ((BundleInfo) matches.get(i)).version;
		}
		highest = findMax(null, versions);
		return ((BundleInfo) matches.get(highest)).location;
	}

	private BundleInfo findBundle(final String target, String version, boolean removeMatch) {
		ArrayList matches = (ArrayList) allBundles.get(target);
		int numberOfMatches = matches != null ? matches.size() : 0;
		if (numberOfMatches == 1) {
			//TODO Need to check the version
			return (BundleInfo) (removeMatch ? matches.remove(0) : matches.get(0));
		}
		if (numberOfMatches == 0)
			return null;

		if (version != null) {
			for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
				BundleInfo bi = (BundleInfo) iterator.next();
				if (bi.version.equals(version)) {
					if (removeMatch)
						iterator.remove();
					return bi;
				}
			}
			//TODO Need to log the fact that we could not find the version mentioned
			return null;
		}
		String[] versions = new String[numberOfMatches];
		int highest = 0;
		for (int i = 0; i < versions.length; i++) {
			versions[i] = ((BundleInfo) matches.get(i)).version;
		}
		highest = findMax(null, versions);
		return (BundleInfo) (removeMatch ? matches.remove(highest) : matches.get(highest));
	}

	/* 
	 * Get all the bundles available on the webstart classpath
	 */
	private void discoverBundles() {
		allBundles = new HashMap();
		try {
			Enumeration resources = WebStartMain.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
			while (resources.hasMoreElements()) {
				BundleInfo found = getBundleInfo((URL) resources.nextElement());
				if (found == null)
					continue;
				ArrayList matching = (ArrayList) allBundles.get(found.bsn);
				if (matching == null) {
					matching = new ArrayList(1);
					allBundles.put(found.bsn, matching);
				}
				matching.add(found);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String extractInnerURL(URL url) {
		try {
			URLConnection connection = null;
			try {
				connection = url.openConnection();
				if (connection instanceof JarURLConnection) {
					JarFile jarFile = ((JarURLConnection) connection).getJarFile();
					String name = jarFile.getName();
					// Some VMs may not return a jar name as a security precaution
					if (name == null || name.length() == 0)
						name = getJarNameByReflection(jarFile);

					if (name != null && name.length() > 0)
						return "file:" + name; //$NON-NLS-1$
				}
			} finally {
				if (connection != null)
					connection.getInputStream().close();
			}
		} catch (IOException e) {
			//Ignore and return the external form
		}
		return url.toExternalForm();
	}

	/*
	 *  Get a value of the ZipFile.name field using reflection.
	 *  For this to succeed, we need the "suppressAccessChecks" permission.
	 */
	private String getJarNameByReflection(JarFile jarFile) {
		if (jarFile == null)
			return null;

		Field nameField = null;
		try {
			nameField = ZipFile.class.getDeclaredField("name"); //$NON-NLS-1$
		} catch (NoSuchFieldException e1) {
			try {
				nameField = ZipFile.class.getDeclaredField("fileName"); //$NON-NLS-1$
			} catch (NoSuchFieldException e) {
				//ignore
			}
		}

		if (nameField == null || Modifier.isStatic(nameField.getModifiers()) || nameField.getType() != String.class)
			return null;

		try {
			nameField.setAccessible(true);
			return (String) nameField.get(jarFile);
		} catch (SecurityException e) {
			// Don't have permissions, ignore
		} catch (IllegalArgumentException e) {
			// Shouldn't happen
		} catch (IllegalAccessException e) {
			// Shouldn't happen
		}

		return null;
	}

	/*
	 * Construct bundle info objects from items found on the osgi.bundles list
	 */
	private void initializeBundleListStructure() {
		final char STARTLEVEL_SEPARATOR = '@';

		//In webstart the bundles list can only contain bundle names with or without a version.
		String prop = System.getProperty(PROP_OSGI_BUNDLES);
		if (prop == null || prop.trim().equals("")) { //$NON-NLS-1$
			bundleList = new ArrayList(0);
			return;
		}

		bundleList = new ArrayList(10);
		StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			String bundleId = token;
			if (token.equals("")) //$NON-NLS-1$
				continue;
			int startLevelSeparator;
			BundleInfo toAdd = new BundleInfo();
			toAdd.bsn = bundleId;
			if ((startLevelSeparator = token.lastIndexOf(STARTLEVEL_SEPARATOR)) != -1) {
				toAdd.bsn = token.substring(0, startLevelSeparator);
				toAdd.startData = token.substring(startLevelSeparator);
				//Note that here we don't try to parse the start attribute since this info is then used to recompose the value for osgi.bundles
			}
			bundleList.add(toAdd);
		}
	}

	private BundleInfo getBundleInfo(URL manifestURL) {
		final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName"; //$NON-NLS-1$
		final String BUNDLE_VERSION = "Bundle-Version"; //$NON-NLS-1$
		final String DEFAULT_VERSION = "0.0.0"; //$NON-NLS-1$

		Manifest mf;
		try {
			mf = new Manifest(manifestURL.openStream());
			String symbolicNameString = mf.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
			if (symbolicNameString == null)
				return null;

			BundleInfo result = new BundleInfo();
			String version = mf.getMainAttributes().getValue(BUNDLE_VERSION);
			result.version = (version != null) ? version : DEFAULT_VERSION;
			result.location = extractInnerURL(manifestURL);
			int pos = symbolicNameString.lastIndexOf(';');
			if (pos != -1) {
				result.bsn = symbolicNameString.substring(0, pos);
				return result;
			}
			result.bsn = symbolicNameString;
			return result;
		} catch (IOException e) {
			if (debug)
				e.printStackTrace();
		}
		return null;
	}

	//Build the osgi bundle list. The allbundles data structure is changed during the process. 
	private void buildOSGiBundleList() {
		StringBuffer finalBundleList = new StringBuffer(allBundles.size() * 30);
		//First go through all the bundles of the bundle
		for (Iterator iterator = bundleList.iterator(); iterator.hasNext();) {
			BundleInfo searched = (BundleInfo) iterator.next();
			BundleInfo found = findBundle(searched.bsn, searched.version, true);
			if (found != null)
				finalBundleList.append(REFERENCE_SCHEME).append(found.location).append(searched.startData).append(',');
		}

		if (!Boolean.FALSE.toString().equalsIgnoreCase(System.getProperties().getProperty(PROP_WEBSTART_AUTOMATIC_INSTALLATION))) {
			for (Iterator iterator = allBundles.values().iterator(); iterator.hasNext();) {
				ArrayList toAdd = (ArrayList) iterator.next();
				for (Iterator iterator2 = toAdd.iterator(); iterator2.hasNext();) {
					BundleInfo bi = (BundleInfo) iterator2.next();
					finalBundleList.append(REFERENCE_SCHEME).append(bi.location).append(',');
				}
			}
		}
		System.getProperties().put(PROP_OSGI_BUNDLES, finalBundleList.toString());
	}
}
