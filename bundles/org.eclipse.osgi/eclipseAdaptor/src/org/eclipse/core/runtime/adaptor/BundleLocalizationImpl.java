/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.File;
import java.net.*;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.core.AbstractBundleData;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.defaultadaptor.DevClassPathHelper;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

/**
 * The implementation of the service that gets ResourceBundle objects from a given 
 * bundle with a given locale. 
 */

public class BundleLocalizationImpl implements BundleLocalization {
	ArrayList encountered = new ArrayList();
	/**
	 * The getLocalization method gets a ResourceBundle object for the given
	 * locale and bundle.
	 * 
	 * @return A <code>ResourceBundle</code> object for the given bundle and locale.
	 * If null is passed for the locale parameter, the default locale is used.
	 */
	public ResourceBundle getLocalization(Bundle bundle, String locale) {
		if (hasRuntime21(bundle)) {		
			return ResourceBundle.getBundle("plugin", locale == null ? Locale.getDefault() : new Locale("en_us"), createTempClassloader(bundle)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return ((org.eclipse.osgi.framework.internal.core.AbstractBundle) (bundle)).getResourceBundle(locale);
	}
	
	private boolean hasRuntime21(Bundle b) {
		try {
			ManifestElement[] prereqs = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, (String) b.getHeaders().get(Constants.REQUIRE_BUNDLE));
			if (prereqs==null)
				return false;
			for (int i = 0; i < prereqs.length; i++) {
				if("2.1".equals(prereqs[i].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)) && "org.eclipse.core.runtime".equals(prereqs[i].getValue())) {  //$NON-NLS-1$//$NON-NLS-2$
					return true;
				}
			}
			System.out.println(prereqs);
		} catch (BundleException e) {
			return false;
		}
		return false;
	}
	
	private ClassLoader createTempClassloader(Bundle b) {
		ArrayList classpath = new ArrayList();
		addClasspathEntries(b, classpath);
		addBundleRoot(b,classpath);
		addDevEntries(b, classpath);
		addFragments(b, classpath);
		URL[] urls = new URL[classpath.size()];
		return new URLClassLoader((URL[]) classpath.toArray(urls));
	}
	
	private void addFragments(Bundle host, ArrayList classpath) {
		Bundle[] fragments = ((AbstractBundle) host).getFragments();
		if (fragments==null)
			return;
		
		ManifestElement[] classpathElements;
		for (int i = 0; i < fragments.length; i++) {
			try {
				classpathElements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, (String) fragments[i].getHeaders().get(Constants.BUNDLE_CLASSPATH));
			} catch (BundleException e) {
				break;
			}
			if (classpathElements != null) {
				for (int j = 0; j < classpathElements.length; j++)
					classpath.add(fragments[i].getEntry(classpathElements[j].getValue()));
			}			
		}
	}
	
	private void addClasspathEntries(Bundle b, ArrayList classpath) {
		ManifestElement[] classpathElements;
		try {
			classpathElements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, (String) b.getHeaders().get(Constants.BUNDLE_CLASSPATH));
			for (int i = 0; i < classpathElements.length; i++) {
				URL classpathEntry = b.getEntry(classpathElements[i].getValue());
				if (classpathEntry!=null)
					classpath.add(classpathEntry);
			}
		} catch (BundleException e) {
			//ignore
		}
	}
	private void addBundleRoot(Bundle b, ArrayList classpath) {
		File bundleBase = new File(((AbstractBundleData) ((AbstractBundle)b).getBundleData()).getFileName());
		try {
			classpath.add(bundleBase.toURL());
		} catch (MalformedURLException e) {
			//Ignore
		}
	}
	private void addDevEntries(Bundle b, ArrayList classpath) {
		if (!DevClassPathHelper.inDevelopmentMode())
			return;
		
		String[] binaryPaths = DevClassPathHelper.getDevClassPath(b.getSymbolicName());
		File bundleBase = new File(((AbstractBundleData) ((AbstractBundle)b).getBundleData()).getFileName());
		for (int i = 0; i < binaryPaths.length; i++) {
			try {
				classpath.add(new File(bundleBase, binaryPaths[i]).toURL());
			} catch (MalformedURLException e) {
				//Ignore
			}
		}		
	}
}