/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
import org.eclipse.osgi.internal.provisional.verifier.CertificateVerifier;
import org.eclipse.osgi.internal.provisional.verifier.CertificateVerifierFactory;
import org.osgi.framework.*;

/**
 * Implements signed bundle hook support for the framework
 */
public class SignedBundleHook implements AdaptorHook, BundleFileWrapperFactoryHook, HookConfigurator, CertificateVerifierFactory {
	private static BaseAdaptor ADAPTOR;
	private static String SIGNED_BUNDLE_SUPPORT = "osgi.support.signature.verify"; //$NON-NLS-1$
	private static boolean supportSignedBundles = false;
	private ServiceRegistration reg;

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
		reg = context.registerService(CertificateVerifierFactory.class.getName(), this, null);
	}

	public void frameworkStop(BundleContext context) throws BundleException {
		if (reg != null) {
			reg.unregister();
			reg = null;
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
				if (base && hook != null && hook.signedBundleFile != null)
					signedBaseFile = hook.signedBundleFile;
				else
					signedBaseFile = new SignedBundleFile();
				signedBaseFile.setBundleFile(bundleFile);
				if (signedBaseFile.isSigned()) // only use the signed file if there are certs
					bundleFile = signedBaseFile;
			}
		} catch (IOException e) {
			// do nothing; its not your responsibility the error will be addressed later
		}
		return bundleFile;
	}


	public void addHooks(HookRegistry hookRegistry) {
		supportSignedBundles = "true".equals(FrameworkProperties.getProperty(SIGNED_BUNDLE_SUPPORT)); //$NON-NLS-1$
		hookRegistry.addAdaptorHook(this);
		if (supportSignedBundles) {
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
		result.setBundleFile(contentBundleFile);
		return result;
	}

	public CertificateVerifier getVerifier(Bundle bundle) throws IOException {
		BundleData data = ((AbstractBundle) bundle).getBundleData();
		if (!(data instanceof BaseData))
			throw new IllegalArgumentException("Invalid bundle object.  No BaseData found."); //$NON-NLS-1$
		BundleFile bundleFile = ((BaseData) data).getBundleFile();
		if (bundleFile instanceof SignedBundleFile)
			return (SignedBundleFile) bundleFile; // just reuse the verifier from the bundle file
		return getVerifier(bundleFile.getBaseFile()); // must create a new verifier using the raw file
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
}
