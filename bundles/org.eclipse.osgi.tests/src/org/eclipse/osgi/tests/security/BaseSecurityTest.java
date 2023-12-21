/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Hashtable;
import junit.framework.TestCase;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.internal.provisional.service.security.AuthorizationEngine;
import org.eclipse.osgi.internal.service.security.KeyStoreTrustEngine;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class BaseSecurityTest extends TestCase {

	private static char[] PASSWORD_DEFAULT = {'c', 'h', 'a', 'n', 'g', 'e', 'i', 't'};
	private static String TYPE_DEFAULT = "JKS";

	protected static final String BUNDLE_SECURITY_TESTS = "org.eclipse.osgi.tests"; //$NON-NLS-1$

	public BaseSecurityTest() {
		super();
	}

	public BaseSecurityTest(String name) {
		super(name);
	}

	private static KeyStore supportStore;
	static {
		try {
			URL supportUrl = OSGiTestsActivator.getBundle().getEntry("test_files/security/keystore.jks");
			supportStore = KeyStore.getInstance(TYPE_DEFAULT);
			supportStore.load(supportUrl.openStream(), PASSWORD_DEFAULT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ServiceRegistration trustReg = null;

	protected static Certificate getTestCertificate(String alias) throws KeyStoreException {
		return supportStore.getCertificate(alias);
	}

	protected static Certificate[] getTestCertificateChain(String[] aliases) throws KeyStoreException {
		ArrayList certs = new ArrayList(aliases.length);
		for (String alias : aliases) {
			certs.add(getTestCertificate(alias));
		}
		return (Certificate[]) certs.toArray(new Certificate[] {});
	}

	protected void registerEclipseTrustEngine() throws Exception {
		// make a copy of cacerts file and use that at runtime
		URL eclipseURL = OSGiTestsActivator.getBundle().getEntry("test_files/security/eclipse.jks");
		File tempEngine = OSGiTestsActivator.getContext().getDataFile("temp.jks");

		copy(eclipseURL.openStream(), tempEngine);

		KeyStoreTrustEngine dummyTE = new KeyStoreTrustEngine(tempEngine.getAbsolutePath(), "JKS", "changeit".toCharArray(), "temp.jks", null);
		Hashtable properties = new Hashtable(7);
		properties.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));

		trustReg = OSGiTestsActivator.getContext().registerService(TrustEngine.class.getName(), dummyTE, properties);
	}

	protected void tearDown() throws Exception {
		if (trustReg != null)
			trustReg.unregister();
	}

	public static void copy(InputStream in, File dst) throws IOException {
		//		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	protected SignedContentFactory getSignedContentFactory() {
		ServiceReference ref = OSGiTestsActivator.getContext().getServiceReference(SignedContentFactory.class.getName());
		assertNotNull("No SignedContentFactory service", ref);
		SignedContentFactory factory = (SignedContentFactory) OSGiTestsActivator.getContext().getService(ref);
		OSGiTestsActivator.getContext().ungetService(ref);
		return factory;
	}

	protected TrustEngine getTrustEngine() {
		ServiceReference ref = OSGiTestsActivator.getContext().getServiceReference(TrustEngine.class.getName());
		assertNotNull("No TrustEngine available", ref);
		TrustEngine engine = (TrustEngine) OSGiTestsActivator.getContext().getService(ref);
		OSGiTestsActivator.getContext().ungetService(ref);
		return engine;
	}

	protected AuthorizationEngine getAuthorizationEngine() {
		ServiceReference ref = OSGiTestsActivator.getContext().getServiceReference(AuthorizationEngine.class.getName());
		assertNotNull("No AuthorizationEngine available", ref);
		AuthorizationEngine engine = (AuthorizationEngine) OSGiTestsActivator.getContext().getService(ref);
		OSGiTestsActivator.getContext().ungetService(ref);
		return engine;
	}

	protected Bundle installBundle(String bundlePath) throws BundleException, IOException {
		URL bundleURL = OSGiTestsActivator.getBundle().getEntry(bundlePath);
		assertNotNull("Bundle URL is null " + bundlePath, bundleURL);
		return OSGiTestsActivator.getContext().installBundle(bundlePath, bundleURL.openStream());
	}

	protected static File getEntryFile(String entryPath) throws IOException {
		URL entryURL = OSGiTestsActivator.getBundle().getEntry(entryPath);
		if (entryURL == null)
			return null;
		return new File(FileLocator.toFileURL(entryURL).toExternalForm().substring(5));
	}

	protected static File copyEntryFile(String entryPath) throws IOException {
		URL entryURL = OSGiTestsActivator.getBundle().getEntry(entryPath);
		if (entryURL == null)
			return null;
		File tempFolder = OSGiTestsActivator.getContext().getDataFile("temp");
		tempFolder.mkdirs();
		File result = File.createTempFile("entry", ".jar", tempFolder);
		readFile(entryURL.openStream(), result);
		return result;
	}

	protected static String getTestJarPath(String jarName) {
		return "test_files/security/bundles/" + jarName + ".jar";
	}

	protected static void setEclipseTrustEngine(ConfigurationSessionTestSuite suite) {
		try {
			URL eclipseURL = OSGiTestsActivator.getBundle().getEntry("test_files/security/eclipse.jks");
			File tempFile = File.createTempFile("keystore", ".jks");

			copy(eclipseURL.openStream(), tempFile);

			suite.getSetup().setSystemProperty("osgi.framework.keystore", tempFile.toURL().toExternalForm()); //$NON-NLS-1$//$NON-NLS-2$
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void readFile(InputStream in, File file) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);

			byte buffer[] = new byte[1024];
			int count;
			while ((count = in.read(buffer, 0, buffer.length)) > 0) {
				fos.write(buffer, 0, count);
			}

			fos.close();
			fos = null;

			in.close();
			in = null;
		} catch (IOException e) {
			// close open streams
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}

			if (in != null) {
				try {
					in.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}

			throw e;
		}
	}
}
