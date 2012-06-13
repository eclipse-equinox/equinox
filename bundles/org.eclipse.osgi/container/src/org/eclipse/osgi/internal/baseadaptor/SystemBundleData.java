/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.baseadaptor;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.BundleException;

public class SystemBundleData extends BaseData {
	private static final String OSGI_FRAMEWORK = "osgi.framework"; //$NON-NLS-1$

	public SystemBundleData(BaseAdaptor adaptor) throws BundleException {
		super(0, adaptor);
		File osgiBase = getOsgiBase();
		createBundleFile(osgiBase);
		manifest = createManifest(osgiBase);
		setMetaData();
		setLastModified(System.currentTimeMillis()); // just set the lastModified to the current time
		StorageHook[] hooks = adaptor.getHookRegistry().getStorageHooks();
		StorageHook[] instanceHooks = new StorageHook[hooks.length];
		for (int i = 0; i < hooks.length; i++)
			instanceHooks[i] = hooks[i].create(this);
		setStorageHooks(instanceHooks);
	}

	private File getOsgiBase() {
		String frameworkLocation = FrameworkProperties.getProperty(SystemBundleData.OSGI_FRAMEWORK);
		if (frameworkLocation != null && frameworkLocation.startsWith("file:")) //$NON-NLS-1$
			// TODO assumes the location is a file URL
			return new File(frameworkLocation.substring(5));
		try {
			URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
			// assumes file URL
			if ("file".equals(url.getProtocol())) //$NON-NLS-1$
				return new File(url.getPath());
		} catch (Throwable e) {
			// do nothing
		}
		return null;
	}

	private Headers<String, String> createManifest(File osgiBase) throws BundleException {
		InputStream in = null;

		if (osgiBase != null && osgiBase.exists())
			try {
				BundleEntry entry = getBundleFile().getEntry(Constants.OSGI_BUNDLE_MANIFEST);
				if (entry != null)
					in = entry.getInputStream();
			} catch (IOException e) {
				// do nothing here.  in == null
			}

		// If we cannot find the Manifest file from the baseBundleFile then
		// search for the manifest as a classloader resource
		// This allows an adaptor to package the SYSTEMBUNDLE.MF file in a jar.
		if (in == null)
			in = getManifestAsResource();
		if (Debug.DEBUG_GENERAL)
			if (in == null)
				Debug.println("Unable to find system bundle manifest " + Constants.OSGI_BUNDLE_MANIFEST); //$NON-NLS-1$

		if (in == null)
			throw new BundleException(AdaptorMsg.SYSTEMBUNDLE_MISSING_MANIFEST, BundleException.MANIFEST_ERROR);
		return Headers.parseManifest(in);
	}

	private InputStream getManifestAsResource() {
		URL url = getManifestURL();
		try {
			return url == null ? null : url.openStream();
		} catch (IOException e) {
			return null;
		}
	}

	private URL getManifestURL() {
		ClassLoader cl = getClass().getClassLoader();
		try {
			// get all manifests in your classloader delegation
			Enumeration<URL> manifests = cl != null ? cl.getResources(Constants.OSGI_BUNDLE_MANIFEST) : ClassLoader.getSystemResources(Constants.OSGI_BUNDLE_MANIFEST);
			while (manifests.hasMoreElements()) {
				URL url = manifests.nextElement();
				try {
					// check each manifest until we find one with the Eclipse-SystemBundle: true header
					Headers<String, String> headers = Headers.parseManifest(url.openStream());
					if ("true".equals(headers.get(Constants.ECLIPSE_SYSTEMBUNDLE))) //$NON-NLS-1$
						return url;
				} catch (BundleException e) {
					// ignore and continue to next URL
				}
			}
		} catch (IOException e) {
			// ignore and return null
		}
		return null;
	}

	private void createBundleFile(File osgiBase) {
		if (osgiBase != null)
			try {
				bundleFile = getAdaptor().createBundleFile(osgiBase, this);
			} catch (IOException e) {
				// should not happen
			}
		else
			bundleFile = new BundleFile(osgiBase) {
				public File getFile(String path, boolean nativeCode) {
					return null;
				}

				public BundleEntry getEntry(String path) {
					if (Constants.OSGI_BUNDLE_MANIFEST.equals(path)) {
						System.err.println("Getting system bundle manifest: " + path);
						return new BundleEntry() {

							public InputStream getInputStream() throws IOException {
								return getManifestURL().openStream();
							}

							public long getSize() {
								return 0;
							}

							public String getName() {
								return Constants.OSGI_BUNDLE_MANIFEST;
							}

							public long getTime() {
								return 0;
							}

							public URL getLocalURL() {
								return getManifestURL();
							}

							public URL getFileURL() {
								return null;
							}
						};
					}
					return null;
				}

				public Enumeration<String> getEntryPaths(String path) {
					return null;
				}

				public void close() {
					// do nothing
				}

				public void open() {
					// do nothing
				}

				public boolean containsDir(String dir) {
					return false;
				}
			};
	}

	private void setMetaData() throws BundleException {
		setLocation(Constants.SYSTEM_BUNDLE_LOCATION);
		BaseStorageHook.loadManifest(this, manifest);
	}

	public BundleClassLoader createClassLoader(ClassLoaderDelegate delegate, BundleProtectionDomain domain, String[] bundleclasspath) {
		return null;
	}

	public File createGenerationDir() {
		return null;
	}

	public String findLibrary(String libname) {
		return null;
	}

	/**
	 * @throws BundleException  
	 */
	public void installNativeCode(String[] nativepaths) throws BundleException {
		// do nothing
	}

	public int getStartLevel() {
		return 0;
	}

	public int getStatus() {
		return Constants.BUNDLE_STARTED;
	}

	public void save() {
		// do nothing
	}

}
