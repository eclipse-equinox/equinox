/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.configuration;

import java.io.*;
import java.net.URL;
import java.util.*;
import junit.framework.*;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.session.*;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.osgi.framework.Bundle;

public class ConfigurationSessionTestSuite extends SessionTestSuite {
	private static final String PROP_SHARED_CONFIG_AREA = "osgi.sharedConfiguration.area";
	private static final String PROP_CONFIG_AREA_READ_ONLY = InternalPlatform.PROP_CONFIG_AREA + ".readOnly";
	private static final String PROP_CONFIG_CASCADED = "osgi.configuration.cascaded";
	private Collection bundles = new ArrayList();

	private IPath configurationLocation = FileSystemHelper.getRandomLocation(FileSystemHelper.getTempDir());

	private boolean first = true;
	private boolean readOnly;
	private boolean cascaded;
	// should the test cases be run in alphabetical order?
	private boolean shouldSort;

	public ConfigurationSessionTestSuite(String pluginId) {
		super(pluginId);
	}

	public ConfigurationSessionTestSuite(String pluginId, Class theClass) {
		super(pluginId, theClass);
		this.shouldSort = true;
	}

	public ConfigurationSessionTestSuite(String pluginId, Class theClass, String name) {
		super(pluginId, theClass, name);
		this.shouldSort = true;
	}

	public ConfigurationSessionTestSuite(String pluginId, String name) {
		super(pluginId, name);
	}

	public void addBundle(String id) {
		bundles.add(getURL(id));
	}

	private void createConfigINI() throws IOException {
		Assert.assertTrue("1.0", !bundles.isEmpty());
		Properties contents = new Properties();
		StringBuffer osgiBundles = new StringBuffer();
		for (Iterator i = this.bundles.iterator(); i.hasNext();) {
			osgiBundles.append(i.next());
			osgiBundles.append(',');
		}
		osgiBundles.deleteCharAt(osgiBundles.length() - 1);
		contents.put("osgi.bundles", osgiBundles.toString());
		String osgiFramework = getURL("org.eclipse.osgi");
		contents.put("osgi.framework", osgiFramework);
		contents.put("osgi.bundles.defaultStartLevel", "4");
		contents.put("osgi.install.area", Platform.getInstallLocation().getURL().toExternalForm());
		contents.put(PROP_CONFIG_CASCADED, Boolean.toString(cascaded));
		if (cascaded)
			contents.put(PROP_SHARED_CONFIG_AREA, Platform.getConfigurationLocation().getURL().toExternalForm());
		contents.put(PROP_CONFIG_AREA_READ_ONLY, Boolean.toString(readOnly));
		// save the properties
		File configINI = configurationLocation.append("config.ini").toFile();
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(configINI));
			contents.store(out, null);
		} finally {
			if (out != null)
				out.close();
		}
	}

	protected void fillTestDescriptor(TestDescriptor test) throws SetupException {
		super.fillTestDescriptor(test);
		if (first) {
			test.getSetup().setSystemProperty(PROP_CONFIG_AREA_READ_ONLY, Boolean.FALSE.toString());
			first = false;
		}
	}

	private String getURL(String id) {
		String suffix = "";
		int atIndex = id.indexOf("@");
		if (atIndex >= 0) {
			suffix = id.substring(atIndex);
			id = id.substring(0, atIndex);
		}
		Bundle bundle = Platform.getBundle(id);
		Assert.assertNotNull("0.1 " + id, bundle);
		URL url = bundle.getEntry("/");
		Assert.assertNotNull("0.2 " + id, url);		
		try {
			url = Platform.resolve(url);
		} catch (IOException e) {
			CoreTest.fail("0.3 " + url, e);
		}
		String externalForm;		
		if (url.getProtocol().equals("jar")) {
			// if it is a JAR'd plug-in, URL is jar:file:/path/file.jar!/ - see bug 86195
			String path = url.getPath();
			// change it to be file:/path/file.jar
			externalForm = path.substring(0, path.length() - 2);
		} else
			externalForm  = url.toExternalForm();
		// workaround for bug 88070		
		externalForm = "reference:" + externalForm;
		return externalForm + suffix;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Ensures setup uses this suite's instance location.
	 * @throws SetupException
	 */
	protected Setup newSetup() throws SetupException {
		Setup base = super.newSetup();
		// the base implementation will have set this to the host configuration
		base.setEclipseArgument(Setup.CONFIGURATION, null);
		base.setSystemProperty(InternalPlatform.PROP_CONFIG_AREA, configurationLocation.toOSString());
		return base;
	}

	/**
	 * Ensures workspace location is empty before running the first test, and after
	 * running the last test. Also sorts the test cases to be run if this suite was
	 * created by reifying a test case class.
	 */
	public void run(TestResult result) {
		configurationLocation.toFile().mkdirs();
		try {
			try {
				createConfigINI();
			} catch (IOException e) {
				CoreTest.fail("0.1", e);
			}
			if (!shouldSort) {
				super.run(result);
				return;
			}
			// we have to sort the tests cases
			Test[] allTests = getTests(true);
			// now run the tests in order			
			for (int i = 0; i < allTests.length && !result.shouldStop(); i++)
				runTest(allTests[i], result);
		} finally {
			FileSystemHelper.clear(configurationLocation.toFile());
		};

	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isCascaded() {
		return cascaded;
	}

	public void setCascaded(boolean cascaded) {
		this.cascaded = cascaded;
	}

}
