/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.adaptor.core.*;
import org.eclipse.osgi.framework.internal.core.Msg;
import org.eclipse.osgi.framework.internal.defaultadaptor.DefaultClassLoader;
import org.eclipse.osgi.framework.internal.defaultadaptor.DevClassPathHelper;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.stats.ClassloaderStats;
import org.eclipse.osgi.framework.stats.ResourceBundleStats;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

public class EclipseClassLoader extends DefaultClassLoader {
	private static String[] NL_JAR_VARIANTS = buildNLJarVariants(System.getProperties().getProperty("osgi.nl"));
	// from Eclipse-AutoStart element value
	private boolean autoStart;
	// from Eclipse-AutoStart's "exceptions" attribute
	private List exceptions;	//TODO This could easily be changed to an array
	// should we try to activate the bundle?
	private boolean tryActivating;
	public EclipseClassLoader(ClassLoaderDelegate delegate, ProtectionDomain domain, String[] classpath, ClassLoader parent, BundleData bundleData) {
		super(delegate, domain, classpath, parent, (org.eclipse.osgi.framework.internal.defaultadaptor.DefaultBundleData) bundleData);
		parseAutoStart(bundleData);
		// unless autoStart == false and there are no exceptions, we should try activating
		tryActivating = autoStart || exceptions != null;
	}
	private void parseAutoStart(BundleData bundleData) {
		try {
			String automationHeader = (String) bundleData.getManifest().get(EclipseAdaptorConstants.ECLIPSE_AUTOSTART);
			ManifestElement[] allElements = ManifestElement.parseHeader(EclipseAdaptorConstants.ECLIPSE_AUTOSTART, automationHeader);
			//Eclipse-AutoStart not found... look for the Legacy header instead		//TODO This is old code, this can be removed
			if (allElements == null) {
				autoStart = "true".equalsIgnoreCase((String) bundleData.getManifest().get(EclipseAdaptorConstants.LEGACY)); //$NON-NLS-1$
				return;
			}
			// the single value for this element should be true|false
			autoStart = "true".equalsIgnoreCase(allElements[0].getValue()); //$NON-NLS-1$
			// look for any exceptions (the attribute) to the autoActivate setting
			String exceptionsValue = allElements[0].getAttribute(EclipseAdaptorConstants.EXCEPTIONS_ATTRIBUTE);
			if (exceptionsValue != null) {
				exceptions = new ArrayList();
				StringTokenizer tokenizer = new StringTokenizer(exceptionsValue, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens())
					exceptions.add(tokenizer.nextToken().trim());
			}
		} catch (BundleException e) {
			// just use the default settings (no auto activation)
			String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_CLASSLOADER_CANNOT_GET_HEADERS", bundleData.getLocation()); //$NON-NLS-1$
			EclipseAdaptor.getDefault().getFrameworkLog().log(new FrameworkLogEntry(EclipseAdaptorConstants.PI_ECLIPSE_OSGI, message, 0, e, null));
		}
	}
	//TODO Need to fix the startup problem identified. Need to have a synchronisation pattern similar to the one in beginStateChange()
	public Class findLocalClass(String name) throws ClassNotFoundException {
		// See if we need to do autoactivation. We don't if autoActivation is turned off
		// or if we have already activated this bundle.
		if (EclipseAdaptor.MONITOR_CLASSES)
			ClassloaderStats.startLoadingClass(getClassloaderId(), name);
		boolean found = true;
		try {
			// Shot-circuit on tryActivating to avoid always calling a method.
			if (tryActivating && shouldActivateFor(name)) {
				int state = hostdata.getBundle().getState();
				// Assume that if we get to this point the bundle is installed, resolved, ... so
				// we just need to check that it is not already started or being started. There is a
				// small window where two thread race to start. One will make it first, the other will
				// throw an exception. Below we catch the exception and ignore it if it is
				// of this nature.
				// Ensure that we do the activation outside of any synchronized blocks to avoid deadlock.
				if (state != Bundle.STARTING && state != Bundle.ACTIVE)
					try {
						hostdata.getBundle().start();
						tryActivating = false;
					} catch (BundleException e) {
						tryActivating = false; //if a 
						// TODO do nothing for now but we need to filter the type of exception here and
						// sort the bad from the ok. Basically, failing to start the bundle should not be damning.
						// Automagic activation is currently a best effort deal.
						//TODO: log when a log service is available
						//
						e.printStackTrace(); //Note add this to help debugging
						if (e.getNestedException() != null)
							e.getNestedException().printStackTrace();
					}
				// once we have tried, there is no need to try again.
				// TODO revisit this when considering what happens when a bundle is stopped
				// and then subsequently accessed. That is, should it be restarted?
			}
			return super.findLocalClass(name);
		} catch (ClassNotFoundException e) {
			found = false;
			throw e;
		} finally {
			if (EclipseAdaptor.MONITOR_CLASSES)
				ClassloaderStats.endLoadingClass(getClassloaderId(), name, found);
		}
	}
	/**
	 * Determines if for loading the given class we should activate the bundle. 
	 */
	private boolean shouldActivateFor(String className) {
		// no exceptions, it is easy to figure it out
		if (exceptions == null)
			return autoStart;
		// otherwise, we need to check if the package is in the exceptions list
		int dotPosition = className.lastIndexOf('.');
		// the class has no package name... no exceptions apply
		if (dotPosition == -1)
			return autoStart;
		String packageName = className.substring(0, dotPosition);
		// should activate if autoStart and package not in exceptions, or if !autoStart and package in exceptions
		return autoStart ^ exceptions.contains(packageName);
	}
	/**
	 * Override defineClass to allow for package defining.
	 */
	protected Class defineClass(String name, byte[] classbytes, int off, int len, ClasspathEntry classpathEntry) throws ClassFormatError {
		// Define the package if it is not the default package.
		int lastIndex = name.lastIndexOf('.');
		if (lastIndex != -1) {
			String packageName = name.substring(0, lastIndex);
			Package pkg = getPackage(packageName);
			if (pkg == null) {
				// get info about the package from the classpath entry's manifest.
				String specTitle = null, specVersion = null, specVendor = null, implTitle = null, implVersion = null, implVendor = null;
				Manifest mf = ((EclipseClasspathEntry) classpathEntry).getManifest();
				if (mf != null) {
					Attributes mainAttributes = mf.getMainAttributes();
					String dirName = packageName.replace('.', '/') + "/";
					Attributes packageAttributes = mf.getAttributes(dirName);
					boolean noEntry = false;
					if (packageAttributes == null) {
						noEntry = true;
						packageAttributes = mainAttributes;
					}
					specTitle = packageAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
					if (specTitle == null && !noEntry)
						specTitle = mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
					specVersion = packageAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
					if (specVersion == null && !noEntry)
						specVersion = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
					specVendor = packageAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
					if (specVendor == null && !noEntry)
						specVendor = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
					implTitle = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
					if (implTitle == null && !noEntry)
						implTitle = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
					implVersion = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
					if (implVersion == null && !noEntry)
						implVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
					implVendor = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
					if (implVendor == null && !noEntry)
						implVendor = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
				}
				// The package is not defined yet define it before we define the class.
				// TODO still need to seal packages.
				definePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, null);
			}
		}
		return super.defineClass(name, classbytes, off, len, classpathEntry);
	}
	private String getClassloaderId() {
		return hostdata.getBundle().getSymbolicName();
	}
	public URL getResouce(String name) {
		URL result = super.getResource(name);
		if (EclipseAdaptor.MONITOR_RESOURCE_BUNDLES) {
			if (result != null && name.endsWith(".properties")) { //$NON-NLS-1$
				ClassloaderStats.loadedBundle(getClassloaderId(), new ResourceBundleStats(getClassloaderId(), name, result));
			}
		}
		return result;
	}
	protected void findClassPathEntry(ArrayList result, String entry, AbstractBundleData bundledata, ProtectionDomain domain) {
		String var = hasPrefix(entry);
		if (var == null) {
			super.findClassPathEntry(result, entry, bundledata, domain);
			return;
		}
		if (var.equals("ws")) { //$NON-NLS-1$
			super.findClassPathEntry(result, "ws/" + System.getProperties().getProperty("osgi.ws") + entry.substring(4), bundledata, domain); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return;
		}
		if (var.equals("os")) { //$NON-NLS-1$
			super.findClassPathEntry(result, "os/" + System.getProperties().getProperty("osgi.os") + entry.substring(4), bundledata, domain); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return;
		}
		if (var.equals("nl")) { //$NON-NLS-1$
			entry = entry.substring(4);
			for (int i = 0; i < NL_JAR_VARIANTS.length; i++) {
				if (addClassPathEntry(result, "nl/" + NL_JAR_VARIANTS[i] + entry, bundledata, domain)) //$NON-NLS-1$ //$NON-NLS-2$
					return;
			}
			// is we are not in development mode, post some framework errors.
			if (!DevClassPathHelper.inDevelopmentMode()) {
				BundleException be = new BundleException(Msg.formatter.getString("BUNDLE_CLASSPATH_ENTRY_NOT_FOUND_EXCEPTION", entry, hostdata.getLocation())); //$NON-NLS-1$
				bundledata.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, bundledata.getBundle(), be);
			}
		}
	}
	private static String[] buildNLJarVariants(String nl) {
		ArrayList result = new ArrayList();
		nl = nl.replace('_', '/');
		while (nl.length() > 0) {
			result.add("nl/" + nl + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			int i = nl.lastIndexOf('/'); //$NON-NLS-1$
			nl = (i < 0) ? "" : nl.substring(0, i); //$NON-NLS-1$
		}
		result.add(""); //$NON-NLS-1$
		return (String[]) result.toArray(new String[result.size()]);
	}
	//return a String representing the string found between the $s
	private String hasPrefix(String libPath) {
		if (libPath.startsWith("$ws$")) //$NON-NLS-1$
			return "ws"; //$NON-NLS-1$
		if (libPath.startsWith("$os$")) //$NON-NLS-1$
			return "os"; //$NON-NLS-1$
		if (libPath.startsWith("$nl$")) //$NON-NLS-1$
			return "nl"; //$NON-NLS-1$
		return null;
	}
	/**
	 * Override to create EclipseClasspathEntry objects.  EclipseClasspathEntry
	 * allows access to the manifest file for the classpath entry.
	 */
	protected ClasspathEntry createClassPathEntry(BundleFile bundlefile, ProtectionDomain domain) {
		return new EclipseClasspathEntry(bundlefile, domain);
	}
	/**
	 * A ClasspathEntry that has a manifest associated with it.
	 */
	protected class EclipseClasspathEntry extends ClasspathEntry {
		Manifest mf;
		boolean initMF = false;
		protected EclipseClasspathEntry(BundleFile bundlefile, ProtectionDomain domain) {
			super(bundlefile, domain);
		}
		public Manifest getManifest() {
			if (initMF)
				return mf;

			BundleEntry mfEntry = getBundleFile().getEntry(org.eclipse.osgi.framework.internal.core.Constants.OSGI_BUNDLE_MANIFEST);
			if (mfEntry != null)
				try {
					InputStream manIn = mfEntry.getInputStream();
					mf = new Manifest(manIn);
					manIn.close();
				} catch (IOException e) {
					// do nothing
				}
			initMF = true;
			return mf;
		}
	}
}