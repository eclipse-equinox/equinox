/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.verifier;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.security.Security;
import java.util.Hashtable;
import java.util.Properties;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.bundlefile.*;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.provisional.verifier.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implements signed bundle hook support for the framework
 */
public class SignedBundleHook implements AdaptorHook, BundleFileWrapperFactoryHook, HookConfigurator, CertificateVerifierFactory {
	static final int VERIFY_CERTIFICATE = 0x01;
	static final int VERIFY_TRUST = 0x02;
	static final int VERIFY_RUNTIME = 0x04;
	static final int VERIFY_ALL = VERIFY_CERTIFICATE | VERIFY_TRUST | VERIFY_RUNTIME;
	private static String SUPPORT_CERTIFICATE = "certificate"; //$NON-NLS-1$
	private static String SUPPORT_TRUST = "trust"; //$NON-NLS-1$
	private static String SUPPORT_RUNTIME = "runtime"; //$NON-NLS-1$
	private static String SUPPORT_ALL = "all"; //$NON-NLS-1$
	private static String SUPPORT_TRUE = "true"; //$NON-NLS-1$
	private static ServiceTracker trustAuthorityTracker;
	private static BaseAdaptor ADAPTOR;
	private static String SIGNED_BUNDLE_SUPPORT = "osgi.support.signature.verify"; //$NON-NLS-1$
	private static int supportSignedBundles;
	private static CertificateTrustAuthority trustAuthority = new DefaultTrustAuthority(VERIFY_ALL);
	private ServiceRegistration certVerifierReg;
	private ServiceRegistration trustAuthorityReg;

	public boolean matchDNChain(String pattern, String dnChain[]) {
		boolean satisfied = false;
		if (dnChain != null) {
			for (int i = 0; i < dnChain.length; i++)
				if (DNChainMatching.match(dnChain[i], pattern)) {
					satisfied = true;
					break;
				}
		}
		return satisfied;
	}

	public void initialize(BaseAdaptor adaptor) {
		SignedBundleHook.ADAPTOR = adaptor;
	}

	public void frameworkStart(BundleContext context) throws BundleException {
		certVerifierReg = context.registerService(CertificateVerifierFactory.class.getName(), this, null);
		Hashtable properties = new Hashtable(7);
		properties.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
		properties.put(JarVerifierConstant.TRUST_AUTHORITY, JarVerifierConstant.DEFAULT_TRUST_AUTHORITY);
		trustAuthorityReg = context.registerService(CertificateTrustAuthority.class.getName(), trustAuthority, properties);
	}

	public void frameworkStop(BundleContext context) throws BundleException {
		if (certVerifierReg != null) {
			certVerifierReg.unregister();
			certVerifierReg = null;
		}
		if (trustAuthorityReg != null) {
			trustAuthorityReg.unregister();
			trustAuthorityReg = null;
		}
		if (trustAuthorityTracker != null) {
			trustAuthorityTracker.close();
			trustAuthorityTracker = null;
		}	
	}

	public void frameworkStopping(BundleContext context) {
		// do nothing
	}

	public void addProperties(Properties properties) {
		// do nothing
	}

	public URLConnection mapLocationToURLConnection(String location) throws IOException {
		return null;
	}

	public void handleRuntimeError(Throwable error) {
		// do nothing
	}

	public FrameworkLog createFrameworkLog() {
		return null;
	}

	public BundleFile wrapBundleFile(BundleFile bundleFile, Object content, BaseData data, boolean base) {
		try {
			if (bundleFile != null) {
				SignedStorageHook hook = (SignedStorageHook) data.getStorageHook(SignedStorageHook.KEY);
				SignedBundleFile signedBaseFile;
				if (base && hook != null) {
					if (hook.signedBundleFile == null)
						hook.signedBundleFile = new SignedBundleFile();
					signedBaseFile = hook.signedBundleFile;
				} else
					signedBaseFile = new SignedBundleFile();
				signedBaseFile.setBundleFile(bundleFile, supportSignedBundles);
				if (signedBaseFile.isSigned()) // only use the signed file if there are certs
					bundleFile = signedBaseFile;
				else if (base) // if the base is not signed null out the hook.signedBundleFile
					hook.signedBundleFile = null;
			}
		} catch (IOException e) {
			// do nothing; its not your responsibility the error will be addressed later
		}
		return bundleFile;
	}

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addAdaptorHook(this);
		String[] support = ManifestElement.getArrayFromList(FrameworkProperties.getProperty(SIGNED_BUNDLE_SUPPORT), ","); //$NON-NLS-1$
		for (int i = 0; i < support.length; i++) {
			if (SUPPORT_CERTIFICATE.equals(support[i]))
				supportSignedBundles |= VERIFY_CERTIFICATE;
			else if (SUPPORT_TRUST.equals(support[i]))
				supportSignedBundles |= VERIFY_CERTIFICATE | VERIFY_TRUST;
			else if (SUPPORT_RUNTIME.equals(support[i]))
				supportSignedBundles |= VERIFY_CERTIFICATE | VERIFY_RUNTIME;
			else if (SUPPORT_TRUE.equals(support[i]) || SUPPORT_ALL.equals(support[i]))
				supportSignedBundles |= VERIFY_ALL;
		}
		if ((supportSignedBundles & VERIFY_CERTIFICATE) != 0) {
			hookRegistry.addStorageHook(new SignedStorageHook());
			hookRegistry.addBundleFileWrapperFactoryHook(this);
		}
	}

	public CertificateVerifier getVerifier(File content) throws IOException {
		if (content == null)
			throw new IllegalArgumentException("null content"); //$NON-NLS-1$
		BundleFile contentBundleFile;
		if (content.isDirectory())
			contentBundleFile = new DirBundleFile(content);
		else
			contentBundleFile = new ZipBundleFile(content, null);
		SignedBundleFile result = new SignedBundleFile();
		result.setBundleFile(contentBundleFile, VERIFY_ALL);
		return result;
	}

	public CertificateVerifier getVerifier(Bundle bundle) throws IOException {
		BundleData data = ((AbstractBundle) bundle).getBundleData();
		if (!(data instanceof BaseData))
			throw new IllegalArgumentException("Invalid bundle object.  No BaseData found."); //$NON-NLS-1$
		SignedStorageHook hook = (SignedStorageHook) ((BaseData)data).getStorageHook(SignedStorageHook.KEY);
		SignedBundleFile signedBundle = hook != null ? hook.signedBundleFile : null;
		if (signedBundle != null)
			return signedBundle; // just reuse the verifier from the bundle file
		return getVerifier(((BaseData)data).getBundleFile().getBaseFile()); // must create a new verifier using the raw file
	}

	static void log(String msg, int severity, Throwable t) {
		if (SignedBundleHook.ADAPTOR == null) {
			System.err.println(msg);
			t.printStackTrace();
			return;
		}
		FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, severity, 0, msg, 0, t, null);
		SignedBundleHook.ADAPTOR.getFrameworkLog().log(entry);
	}

	static BundleContext getContext() {
		if (ADAPTOR == null)
			return null;
		return ADAPTOR.getContext();
	}

	static CertificateTrustAuthority getTrustAuthority() {
		// read the certs chain security property and open the service tracker if not null
		BundleContext context = SignedBundleHook.getContext();
		if (context == null)
			return trustAuthority;
		if (trustAuthorityTracker == null) {
			// read the trust provider security property
			String trustAuthorityProp = Security.getProperty(JarVerifierConstant.TRUST_AUTHORITY);
			Filter filter = null;
			if (trustAuthorityProp != null)
				try {
					filter = FrameworkUtil.createFilter("(&(" + Constants.OBJECTCLASS + "=" + CertificateTrustAuthority.class.getName() + ")(" + JarVerifierConstant.TRUST_AUTHORITY + "=" + trustAuthorityProp + "))");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
				} catch (InvalidSyntaxException e) {
					e.printStackTrace();
					// do nothing just use no filter TODO we may want to log something
				}
			if (filter != null) {
				trustAuthorityTracker = new ServiceTracker(context, filter, null);
			}
			else
				trustAuthorityTracker = new ServiceTracker(context, CertificateTrustAuthority.class.getName(), null);
			trustAuthorityTracker.open();
		}
		return (CertificateTrustAuthority) trustAuthorityTracker.getService();
	}
}
